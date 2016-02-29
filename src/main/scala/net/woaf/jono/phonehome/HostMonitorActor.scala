package net.woaf.jono.phonehome

import java.net.InetAddress
import java.util.concurrent.TimeUnit

import akka.actor.{ Actor, Props }
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration.Duration

case class CheckResult(result: Boolean, reschedule: Boolean)

object HostMonitorActor {
  def props(target: String, interface: String): Props = Props(new HostMonitorActor(target, interface))
}

class HostMonitorActor(target: String, interface: String) extends Actor with LazyLogging {
  import context.dispatcher // scalastyle:ignore

  var isAlive: Option[Boolean] = None
  var tries = 0

  val statusChecker = new Pinger(InetAddress.getByName(target), interface)

  logger.info(s"Configured to monitor $target")

  private val aliveAsString = Map(true -> "alive", false -> "dead")

  private def makeStateChange(result: Boolean) = {
    isAlive = Some(result)
    context.parent ! HostStateChange(target, result)
    tries = 0
    logger.info(s"Marking $target as ${aliveAsString(result)}")
  }

  private def handleResult(result: Boolean): Unit = {
    if (isAlive.isEmpty) {
      tries = 0
      isAlive = Some(result)
      context.parent ! HostStateChange(target, result)
      logger.info(s"Got initial state ${aliveAsString(result)} for $target")
    } else if (isAlive.get != result) {
      if (result) {
        makeStateChange(result)
      } else {
        tries += 1
        if (tries > 3) {
          makeStateChange(result)
        } else {
          logger.info(s"Worried about $target being ${aliveAsString(result)}")
        }
      }
    } else {
      tries = 0
    }
  }

  override def receive: Actor.Receive = {
    case m: CheckHost => statusChecker.checkHostAlive.onSuccess({
      case result: Any =>
        self ! CheckResult(result, m.reschedule)
    })
    case m: CheckResult =>
      val result = m.result
      handleResult(result)
      if (m.reschedule) {
        context.system.scheduler.scheduleOnce(result match {
          case true => Duration.create(1, TimeUnit.MINUTES)
          case false => Duration.create(10, TimeUnit.SECONDS)
        }, self, CheckHost(true))
      }
    case m: Any =>
      logger.warn(s"HostMonitorActor got unknown msg: $m (${m.getClass.getName})")
  }

  context.system.scheduler.scheduleOnce(Duration.Zero, self, CheckHost(true))

}
