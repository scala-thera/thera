import mill._, scalalib._, publish._

object thera extends ScalaModule {
  def scalaVersion = "2.13.2"
  def ivyDeps = Agg(
    ivy"com.lihaoyi::fastparse:2.3.0",
    ivy"com.amihaiemil.web:eo-yaml:4.3.5",
  )

  object test extends Tests {
    def ivyDeps = Agg(ivy"com.lihaoyi::utest:0.7.4")
    def testFrameworks = Seq("utest.runner.Framework")
  }
}
