import mill._, scalalib._, publish._

object thera extends ScalaModule with PublishModule {
  def scalaVersion = "2.13.2"
  def publishVersion = "0.2.0"
  def artifactName = "thera"

  def ivyDeps = Agg(
    ivy"com.lihaoyi::fastparse:2.3.0",
    ivy"com.amihaiemil.web:eo-yaml:4.3.5",
  )

  object test extends Tests {
    def ivyDeps = Agg(ivy"com.lihaoyi::utest:0.7.4")
    def testFrameworks = Seq("utest.runner.Framework")
  }

  def pomSettings = PomSettings(
    description = "A template engine for Scala",
    organization = "com.akmetiuk",
    url = "https://github.com/anatoliykmetyuk/thera",
    licenses = Seq(License.MIT),
    scm = SCM(
      "git://github.com/anatoliykmetyuk/thera.git",
      "scm:git://github.com/anatoliykmetyuk/thera.git"
    ),
    developers = Seq(
      Developer("anatoliykmetyuk", "Anatolii Kmetiuk","https://github.com/anatoliykmetyuk")
    )
  )
}
