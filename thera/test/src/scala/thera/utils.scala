package thera

import scala.io.Source
import scala.util.Using

object utils {

  def readIO(path: String): (String, String) = {
    val input = readResource(path)
    val output = readResource(s"$path.check")
    input -> output
  }

  def readResource(path: String): String = Using.resource(Source.fromURL(getClass.getResource(path))) {_.mkString }

  def assertValue[T](actual: T, expected: T) =
    assert(actual == expected)

  implicit class StringOps(str: String) {
    def fmt = str.tail.stripMargin.dropRight(1)
  }

}
