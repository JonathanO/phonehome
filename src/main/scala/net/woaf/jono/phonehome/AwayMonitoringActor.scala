package net.woaf.jono.phonehome

import akka.actor.{ Props, Actor }
import akka.routing.{ ActorRefRoutee, BroadcastRoutingLogic, Router }
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable

case class CheckHost(reschedule: Boolean)

case class AwayState(away: Boolean)

case class HostStateChange(host: String, state: Boolean)

case class StructureUpdate(name: String, state: String)

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

  private var awayAcceptable = true

  private val hostAlive = mutable.HashMap[String, Boolean]()

  override def receive: Actor.Receive = {
    case s: HostStateChange =>
      hostAlive.put(s.host, s.state)
      if (s.state) {
        logger.info(s"Away no longer acceptable as ${s.host} has recovered")
        awayAcceptable = false
        nestActor ! AwayState(false)
      } else if (!hostAlive.exists { case (k, v) => v }) {
        logger.info(s"No hosts are alive, away is OK")
        awayAcceptable = true
      }
    case s: StructureUpdate =>
      if (!awayAcceptable && s.state == "auto-away") {
        logger.info(s"Nest went auto-away, but we think someone's in")
        nestActor ! AwayState(false)
        monitorRouter.route(CheckHost(false), self)
      }
    case m: Any =>
      logger.warn(s"AwayMonitoringActor got unknown msg: $m (${m.getClass.getName})")
  }
}
