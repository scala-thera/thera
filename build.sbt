lazy val publishingSettings = List(
  organizationHomepage := Some(url("https://akmetiuk.com/")),

  scmInfo := Some(
    ScmInfo(
      url("https://github.com/anatoliykmetyuk/thera"),
      "scm:git@github.com:anatoliykmetyuk/thera.git"
    )
  ),

  developers := List(
    Developer(
      id    = "anatoliykmetyuk",
      name  = "Anatolii Kmetiuk",
      email = "anatoliykmetyuk@gmail.com",
      url   = url("https://akmetiuk.com")
    )
  ),

  description := "A template processor for Scala",
  licenses := List("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt")),
  homepage := Some(url("https://github.com/anatoliykmetyuk/thera")),

  // Remove all additional repository other than Maven Central from POM
  pomIncludeRepository := { _ => false },
  publishMavenStyle := true,
  publishTo := sonatypePublishToBundle.value,

  credentials ++= (
    for {
      username <- sys.env.get("SONATYPE_USER")
      password <- sys.env.get("SONATYPE_PW")
    } yield Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", username, password)
  ).toList,

  Global / PgpKeys.gpgCommand := (baseDirectory.value / "project/scripts/gpg.sh").getAbsolutePath,
)


lazy val commonSettings = Seq(
  organization := "com.akmetiuk",
  version      := "0.2.0",
  scalaVersion := "2.13.2",

  libraryDependencies += "com.lihaoyi" %% "utest" % "0.7.4" % Test,
  testFrameworks += new TestFramework("utest.runner.Framework"),
)

lazy val root = (project in file("."))
  .aggregate(core)
  .settings(
    publish   := {}
  , publishTo := None
  )

lazy val core = (project in file("core"  ))
  .settings(commonSettings ++ publishingSettings)
  .settings(
    name := "thera-core",
    libraryDependencies += "com.lihaoyi" %% "fastparse" % "2.3.0",
    libraryDependencies += "com.amihaiemil.web" % "eo-yaml" % "4.3.5",
  )
