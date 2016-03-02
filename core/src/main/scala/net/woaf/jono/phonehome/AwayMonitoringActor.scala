package net.woaf.jono.phonehome

import akka.actor.{ Props, Actor }
import akka.routing.{ ActorRefRoutee, BroadcastRoutingLogic, Router }
import com.typesafe.scalalogging.LazyLogging

object AwayMonitoringActor {
  def props(nestToken: String, firebaseURL: String, hosts: Seq[String], interface: String): Props
                = Props(new AwayMonitoringActor(nestToken, firebaseURL, hosts, interface))
}

class AwayMonitoringActor(nestToken: String, firebaseURL: String, hosts: Seq[String], interface: String) extends Actor with LazyLogging {

  private val nestActor = context.actorOf(NestActor.props(nestToken, firebaseURL))

  private val monitorRouter = {
    Router(BroadcastRoutingLogic(), hosts.map(host => {
      val r = context.actorOf(HostMonitorActor.props(host, interface))
      ActorRefRoutee(r)
    }).toVector)
  }

  private def common: Actor.Receive = {
    case m: Any =>
      logger.warn(s"AwayMonitoringActor got unknown msg: $m (${m.getClass.getName})")
  }

  def awayUnacceptable(hostsAlive: Set[String]): Actor.Receive = ({
    case StructureUpdate(_, AwayStates.AUTO_AWAY) =>
        logger.info(s"Nest went auto-away, but we think someone's in")
        nestActor ! SetAwayState(AwayStates.HOME)
        monitorRouter.route(CheckHost(false), self)
    case StructureUpdate(_, _) => // NOOP
    case s: HostStateUp =>
      context.become(awayUnacceptable(hostsAlive + s.host))
    case s: HostStateDown =>
      val hostsNowAlive = hostsAlive - s.host
      if (hostsNowAlive.isEmpty) {
        logger.info(s"No hosts are alive, away is OK")
        context.become(awayOkNotAutoAway(hostsNowAlive))
      } else {
        context.become(awayUnacceptable(hostsNowAlive))
      }

  }: Actor.Receive) orElse common

  def awayOkNotAutoAway(hostsAlive: Set[String]): Actor.Receive = ({
    case s: HostStateUp =>
      logger.info(s"Away no longer acceptable as ${s.host} has recovered")
      context.become(awayUnacceptable(hostsAlive + s.host))
    case s: HostStateDown =>
      context.become(awayOkNotAutoAway(hostsAlive - s.host))
    case StructureUpdate(_, AwayStates.AUTO_AWAY) =>
      context.become(awayOkIsAutoAway(hostsAlive))
    case StructureUpdate(_, _) => // NOOP
  }: Actor.Receive) orElse common

  def awayOkIsAutoAway(hostsAlive: Set[String]): Actor.Receive = ({
    case s: HostStateUp =>
      logger.info(s"Away no longer acceptable as ${s.host} has recovered")
      context.become(awayUnacceptable(hostsAlive + s.host))
      nestActor ! SetAwayState(AwayStates.HOME)
    case s: HostStateDown =>
      context.become(awayOkIsAutoAway(hostsAlive - s.host))
    case StructureUpdate(_, AwayStates.AUTO_AWAY) => // NOOP
    case StructureUpdate(_, _) =>
      context.become(awayOkNotAutoAway(hostsAlive))

  }: Actor.Receive) orElse common

  override def receive: Actor.Receive = ({
    case s: HostStateUp =>
      logger.info(s"Initial state is away not acceptable as at least ${s.host} is up")
      context.become(awayUnacceptable(Set(s.host)))
    case s: HostStateDown =>
      logger.info(s"Initial state is no hosts alive, and assuming we're not auto-away")
      context.become(awayOkNotAutoAway(Set()))
    case StructureUpdate(_, AwayStates.AUTO_AWAY) =>
      logger.info(s"Initial state is no hosts alive, in auto-away")
      context.become(awayOkIsAutoAway(Set()))
    case StructureUpdate(_, _) =>
      logger.info(s"Initial state is no hosts alive, not auto-away")
      context.become(awayOkNotAutoAway(Set()))
  }: Actor.Receive) orElse common



}
