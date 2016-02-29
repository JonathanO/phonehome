name := "phone-is-home"

version := "1.0"

scalaVersion := "2.11.7"

//uncomment the following line if you want cross build
// crossScalaVersions := Seq("2.10.4", "2.11.6")

scalacOptions ++=  Seq(
  "-Xlint",
  "-deprecation",
  "-unchecked",
  "-feature"
)

libraryDependencies ++= Seq(
  "com.firebase" % "firebase-client-jvm" % "1.1.1",
  "com.typesafe.akka" %% "akka-actor" % "2.4.2",
  "org.scalactic" %% "scalactic" % "2.2.6",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
  "ch.qos.logback" % "logback-classic" % "1.1.2",
  "com.github.scopt" %% "scopt" % "3.4.0"
)

resolvers += Resolver.sonatypeRepo("public")

scalariformSettings

enablePlugins(JavaAppPackaging)
enablePlugins(UniversalPlugin)
enablePlugins(JDebPackaging)