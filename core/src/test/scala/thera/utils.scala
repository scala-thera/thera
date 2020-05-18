package thera

import scala.io.Source

object utils {
  def readResource(path: String): String = {
    Source.fromURL(getClass.getResource(path))
      .mkString
  }

  def readIO(path: String): (String, String) = {
    val input = readResource(path)
    val output = readResource(s"$path.check")
    input -> output
  }

  implicit class StringOps(str: String) {
    def fmt = str.tail.stripMargin.dropRight(1)
  }
}
