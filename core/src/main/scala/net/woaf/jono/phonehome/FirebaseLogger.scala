package net.woaf.jono.phonehome

import com.firebase.client.Logger.Level
import com.typesafe.scalalogging.Logger
import org.slf4j.{ MarkerFactory, LoggerFactory }

class FirebaseLogger extends com.firebase.client.Logger {

  protected val logger: Logger =
    Logger(LoggerFactory.getLogger("com.firebase"))

  override def onLogMessage(level: Level, tag: String, message: String, msTimestamp: Long): Unit = {
    val marker = MarkerFactory.getMarker(tag)
    level match {
      case Level.DEBUG => logger.debug(marker, message)
      case Level.INFO => logger.info(marker, message)
      case Level.WARN => logger.warn(marker, message)
      case Level.ERROR => logger.error(marker, message)
      case _ => logger.warn(s"Log message at unknown level $level: $message")
    }
  }

  override def getLogLevel: Level = {
    if (logger.underlying.isDebugEnabled) {
      Level.DEBUG
    } else if (logger.underlying.isInfoEnabled) {
      Level.INFO
    } else if (logger.underlying.isWarnEnabled) {
      Level.WARN
    } else if (logger.underlying.isErrorEnabled) {
      Level.ERROR
    } else {
      Level.NONE
    }
  }
}
