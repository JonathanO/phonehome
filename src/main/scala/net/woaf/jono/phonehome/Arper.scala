package net.woaf.jono.phonehome

import scala.sys.process._ // scalastyle:ignore

class Arper(addr: String) extends HostStatusChecker {

  override def isHostAlive: Boolean = 0 == (Seq("arping", "-c", "1", "-f", addr) ! ProcessLogger(
    line => {},
    line => {}
  ))

}
