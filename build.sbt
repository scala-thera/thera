val ScalaVer = "2.12.7"

val CatsCore      = "1.4.0"
val KindProjector = "0.9.8"

val FastParse = "2.0.5"
val CirceCore = "0.10.1"
val CirceYaml = "0.9.0"

val CommonsIO   = "2.6"

val ScalaTest = "3.0.5"

lazy val commonSettings = Seq(
  name         := "thera"
, organization := "com.functortech"
, version      := "0.1.0-SNAPSHOT"
, scalaVersion := ScalaVer

// Publish to Sonatype
, useGpg := true
, pgpSecretRing := file("/Users/anatolii/.gnupg/secring.gpg")
, pgpPublicRing := file("/Users/anatolii/.gnupg/pubring.kbx")

, pomIncludeRepository := { _ => false }
, publishMavenStyle := true
, publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  }

, licenses := Seq("BSD-style" -> url("http://www.opensource.org/licenses/bsd-license.php"))
, homepage := Some(url("https://github.com/anatoliykmetyuk/thera"))

, scmInfo := Some(
    ScmInfo(
      url("https://github.com/anatoliykmetyuk/thera"),
      "scm:git@github.com/anatoliykmetyuk/thera.git"
    )
  )

, developers := List(
    Developer(
      id    = "anatoliykmetyuk",
      name  = "Anatolii Kmetiuk",
      email = "anatolii@functortech.com",
      url   = url("http://akmetiuk.com/")
    )
  )


, libraryDependencies ++= Seq(
    "org.typelevel"  %% "cats-core" % CatsCore

  , "com.lihaoyi" %% "fastparse"  % FastParse
  , "io.circe"    %% "circe-core" % CirceCore
  , "io.circe"    %% "circe-yaml" % CirceYaml

  // , "com.github.pathikrit" %% "better-files" % BetterFiles
  // , "commons-io"           %  "commons-io"   % CommonsIO

  , "org.scalatest" %% "scalatest" % ScalaTest % Test
  )

, addCompilerPlugin("org.spire-math" %% "kind-projector" % KindProjector)
, scalacOptions ++= Seq(
      "-deprecation"
    , "-encoding", "UTF-8"
    , "-feature"
    , "-language:existentials"
    , "-language:higherKinds"
    , "-language:implicitConversions"
    , "-language:experimental.macros"
    , "-unchecked"
    // , "-Xfatal-warnings"
    // , "-Xlint"
    // , "-Yinline-warnings"
    , "-Ywarn-dead-code"
    , "-Xfuture"
    , "-Ypartial-unification")
)

lazy val root = (project in file("."))
  .settings(commonSettings)
  .settings(
    initialCommands := "import thera._, Main._"
  )
