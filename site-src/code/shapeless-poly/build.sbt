scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.chuusai"   %% "shapeless" % "2.3.2"
, "org.typelevel" %% "cats"      % "0.7.2"
, "org.scalatest" %% "scalatest" % "3.0.0" % Test
)

initialCommands := """
import shapeless._
"""