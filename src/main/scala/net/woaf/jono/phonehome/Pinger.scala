package net.woaf.jono.phonehome

import java.net.{ Inet6Address, InetAddress }
import scala.sys.process._ // scalastyle:ignore

class Pinger(addr: InetAddress, interface: String) extends HostStatusChecker {

  private val isWindows = System.getProperty("os.name").toLowerCase().contains("win")

  private val isv6 = addr match {
    case a: Inet6Address => true
    case _ => false
  }

  override def isHostAlive: Boolean =
    (
      if (isWindows) {
        Seq("ping", "-w", "1000", "-n", "1", addr.getHostAddress)
      } else if (isv6) {
        Seq("ping6", if (!interface.isEmpty) s"-I$interface" else "", "-W", "1", "-n", "-c", "1", addr.getHostAddress)
      } else {
        Seq("ping", "-W", "1", "-n", "-c", "1", addr.getHostAddress)
      }
    ) ! ProcessLogger(
        line => {},
        line => {}
      ) == 0
}
