lazy val commonSettings = Seq(
  scalaVersion := "2.11.7",
  organization := "net.woaf.jono",
  scalacOptions ++=  Seq(
    "-Xlint",
    "-deprecation",
    "-unchecked",
    "-feature"
  )
) ++ scalariformSettings

lazy val scalaReflect = Def.setting { "org.scala-lang" % "scala-reflect" % scalaVersion.value }

enablePlugins(JavaAppPackaging)
enablePlugins(UniversalPlugin)
enablePlugins(JDebPackaging)

lazy val core = (project in file("core")).
  dependsOn(macroSub).
  settings(commonSettings: _*).
  settings(
    name := "phone-is-home",
    version := "1.0",
    libraryDependencies ++= Seq(
      "com.firebase" % "firebase-client-jvm" % "1.1.1",
      "com.typesafe.akka" %% "akka-actor" % "2.4.2",
      "org.scalactic" %% "scalactic" % "2.2.6",
      "org.scalatest" %% "scalatest" % "2.2.6" % "test",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
      "ch.qos.logback" % "logback-classic" % "1.1.2",
      "com.github.scopt" %% "scopt" % "3.4.0"
    ),
    resolvers += Resolver.sonatypeRepo("public")
  )

lazy val macroSub = (project in file("macro")).
  settings(commonSettings: _*).
  settings(
    libraryDependencies += scalaReflect.value
  )

