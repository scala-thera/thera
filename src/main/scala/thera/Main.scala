package thera

import java.io.File
import org.apache.commons.io.FileUtils

import cats._, cats.implicits._

import io.circe._


object Main {
  def main(args: Array[String]): Unit = {
    val input  = new File("site-src/posts/2017-03-10-matryoshka-intro.md")
    val output = new File("_site/result.html")
    val data   = new File("site-src/data/data.yml")
    val assets = new File("site-src/assets")

    run { for {
      // Config
      configRaw <- att { FileUtils.readFileToString(data, settings.enc) }
      config    <- exn { yaml.parser.parse(configRaw) }
      vars      <- exn { config.as[Map[String, String]] }
      _         <- att { println(s"Vars parsed:\n${vars.mkString("\n")}") }

      // Assemble assets
      _ <- att { FileUtils.copyDirectory(assets, new File("_site", "assets")) }

      // Process input post
      res <- templates(input, vars)
      _   <- att { FileUtils.writeStringToFile(output, res, settings.enc) }
    } yield () }
  }
}
