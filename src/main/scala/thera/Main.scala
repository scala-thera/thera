package thera

import java.io.File
import org.apache.commons.io.FileUtils

import cats._, cats.implicits._

object Main {
  def main(args: Array[String]): Unit = {
    val input  = new File("site-src/posts/2017-03-10-matryoshka-intro.md")
    val output = new File("_site/result.html")

    run { for {
      res <- templates(input)
      _   <- att { FileUtils.writeStringToFile(output, res, settings.enc) }
    } yield () }
  }
}
