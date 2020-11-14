import mill._, scalalib._, publish._

object thera extends ScalaModule with PublishModule {
  def scalaVersion = "2.13.2"
  def publishVersion = "0.2.0-M3"
  def artifactName = "thera"

  def ivyDeps = Agg(
    ivy"com.lihaoyi::fastparse:2.3.0",
    ivy"com.lihaoyi::sourcecode:0.1.9",

    ivy"io.circe::circe-core:0.13.0",
    ivy"io.circe::circe-generic:0.13.0",
    ivy"io.circe::circe-parser:0.13.0",
    ivy"io.circe::circe-yaml:0.13.0",
  )

  object test extends Tests {
    def ivyDeps = Agg(ivy"com.lihaoyi::utest:0.7.4")
    def testFrameworks = Seq("utest.runner.Framework")
  }

  def pomSettings = PomSettings(
    description = "A template engine for Scala",
    organization = "com.akmetiuk",
    url = "https://github.com/scala-thera/thera",
    licenses = Seq(License.MIT),
    scm = SCM(
      "git://github.com/scala-thera/thera.git",
      "scm:git://github.com/scala-thera/thera.git"
    ),
    developers = Seq(
      Developer("anatoliykmetyuk", "Anatolii Kmetiuk","https://github.com/anatoliykmetyuk"),
      Developer("Gondolav", "Andrea Veneziano","https://github.com/Gondolav")
    )
  )
}
