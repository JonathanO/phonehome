package net.woaf.jono.phonehome

import scala.concurrent.{ExecutionContext, Future, blocking}

abstract class HostStatusChecker {

  def isHostAlive: Boolean

  def checkHostAlive(implicit executor: ExecutionContext): Future[Boolean] =
    Future {
      blocking {
        isHostAlive
      }
    }

}
