package net.woaf.jono.phonehome

import akka.actor.{ Props, ActorSystem }
import com.typesafe.scalalogging.StrictLogging

final case class Config(firebaseUrl: String = "", nestToken: String = "", hosts: Seq[String] = Seq(), interface: String = "")

object Main extends App with StrictLogging {

  val parser = new scopt.OptionParser[Config]("phone-is-home") {
    head("phone-is-home", "0.1")
    opt[String]('f', "firebase") required () valueName "<url>" action { (x, c) =>
      c.copy(firebaseUrl = x)
    } text "firebase URL"
    opt[String]('t', "token") required () valueName "<token>" action { (x, c) =>
      c.copy(nestToken = x)
    } text "nest API token"
    opt[Seq[String]]('h', "hosts") valueName "<host>,<host>..." action { (x, c) =>
      c.copy(hosts = x)
    } text "hosts to monitor"
    opt[String]('i', "interface") valueName "<interface>" action { (x, c) =>
      c.copy(interface = x)
    } text "Linux network interface to ping from"
    help("help")
  }
  // parser.parse returns Option[C]
  parser.parse(args, Config()) match {
    case Some(config) =>
      // create a top level actor to connect the Nest API with the Twitter REST and Streaming APIs
      logger.info("Starting up actor system")
      val system = ActorSystem("mySystem")
      system.actorOf(Props(new AwayMonitoringActor(config.nestToken, config.firebaseUrl, config.hosts, config.interface)))

    case None =>
    // arguments are bad, error message will have been displayed
  }

}
