resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sbtPluginRepo("releases"),
  Resolver.jcenterRepo
)

ThisBuild / scalaVersion := "2.13.4"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "cz.jenda.alarm"

Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val root = (project in file("."))
  .settings(
    name := "alarm-garage-server",
    Settings.missingLinkSettings,
    Compile / mainClass := Some("cz.jenda.alarm.garage.Main"),
    Compile / PB.targets := Seq(
      scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"
    ),
    libraryDependencies ++= Seq(
      Dependencies.fs2,
      Dependencies.monix,
      Dependencies.mqttClient,
      // config
      Dependencies.config,
      Dependencies.pureconfig,
      // logging
      Dependencies.slog4sApi,
      Dependencies.slog4sSlf4j,
      Dependencies.logback,
      // metrics
      Dependencies.metricsApi,
      Dependencies.metricsScala,
      Dependencies.metricsStatsd,
      // testing
      Dependencies.scalaTest % Test
    )
  )

addCommandAlias("lint", "; scalafmtSbtCheck; scalafmtCheckAll")
addCommandAlias("check", "; lint; +missinglinkCheck; +test") // to have unified api (because there's no scalafix here)
