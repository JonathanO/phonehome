package net.woaf.jono.phonehome

import java.net.InetAddress
import java.util.concurrent.TimeUnit

import akka.actor.{ Actor, Props }
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration.{FiniteDuration, Duration}

final case class CheckHost(reschedule: Boolean = false)

sealed abstract class HostStateChange(host: String)
final case class HostStateUp(host: String) extends HostStateChange(host)
final case class HostStateDown(host: String) extends HostStateChange(host)

object HostMonitorActor {
  def props(target: String, interface: String): Props = Props(new HostMonitorActor(target, interface))
}

class HostMonitorActor(target: String, interface: String) extends Actor with LazyLogging {
  import context.dispatcher // scalastyle:ignore

  private sealed abstract class CheckResult(reschedule: Boolean)
  private final case class CheckResultUp(reschedule: Boolean) extends CheckResult(reschedule)
  private final case class CheckResultDown(reschedule: Boolean) extends CheckResult(reschedule)

  private val statusChecker = new Pinger(InetAddress.getByName(target), interface)

  private val fastCheck = Duration.create(10, TimeUnit.SECONDS)
  private val normalCheck = Duration.create(1, TimeUnit.MINUTES)

  logger.info(s"Configured to monitor $target")

  private def reschedule(when: FiniteDuration): Unit = {
    context.system.scheduler.scheduleOnce(when, self, CheckHost(true))
  }

  private def common: Actor.Receive = {
    case m: CheckHost => statusChecker.checkHostAlive.onSuccess({
      case true =>
        self ! CheckResultUp(m.reschedule)
      case false =>
        self ! CheckResultDown(m.reschedule)
    })
    case m: Any =>
      logger.warn(s"HostMonitorActor got unknown msg: $m (${m.getClass.getName})")
  }

  def hostUp: Actor.Receive = ({
    case CheckResultUp(needReschedule) =>
      if (needReschedule) reschedule(fastCheck)
    case CheckResultDown(needReschedule) =>
        logger.info(s"Worried about $target being dead")
        context.become(hostConcerning(0))
        if (needReschedule) reschedule(fastCheck)
  }: Actor.Receive) orElse common

  def hostDown: Actor.Receive = ({
    case CheckResultUp(needReschedule) =>
      logger.info(s"Marking $target as alive")
      context.become(hostUp)
      context.parent ! HostStateUp(target)
      if (needReschedule) reschedule(normalCheck)
    case CheckResultDown(needReschedule) =>
      if (needReschedule) reschedule(fastCheck)
  }: Actor.Receive) orElse common

  def hostConcerning(count: Int): Actor.Receive = ({
    case CheckResultUp(needReschedule) =>
      context.become(hostUp)
      if (needReschedule) reschedule(normalCheck)
    case CheckResultDown(needReschedule) =>
      if (count > 3) {
        logger.info(s"Marking $target as dead")
        context.become(hostDown)
        context.parent ! HostStateDown(target)
      } else {
        context.become(hostConcerning(count + 1))
      }
      if (needReschedule) reschedule(fastCheck)
  }: Actor.Receive) orElse common

  override def receive: Actor.Receive = ({
    case CheckResultUp(needReschedule) =>
      logger.info(s"Got initial state alive for $target")
      context.become(hostUp)
      context.parent ! HostStateUp(target)
      if (needReschedule) reschedule(normalCheck)
    case CheckResultDown(needReschedule) =>
      logger.info(s"Got initial state dead for $target")
      context.become(hostDown)
      context.parent ! HostStateDown(target)
      if (needReschedule) reschedule(fastCheck)
  }: Actor.Receive) orElse common

  reschedule(Duration.Zero)

}
