val ScalaVer = "2.12.1"

lazy val commonSettings = Seq(
  name    := "Implicit Conversions"
, version := "0.1.0"
, scalaVersion := ScalaVer
, scalacOptions ++= Seq(
      "-deprecation",
      "-encoding", "UTF-8",
      "-feature",
      "-language:existentials",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-language:experimental.macros",
      "-unchecked",
      "-Xlint",
      "-Ywarn-dead-code",
      "-Xfuture")
)

lazy val root = (project in file("."))
  .settings(commonSettings)
  .settings(
    initialCommands := "import implicitconversions._"
  )
