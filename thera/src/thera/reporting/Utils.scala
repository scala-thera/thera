package thera.reporting

import scala.io.Source
import scala.util.Using

object Utils {

  def getLine(s: String, filename: String): Int = {
    val source = Using.resource(Source.fromFile(filename)) { _.getLines().toList.zipWithIndex }

    for ((line, lineNb) <- source) {
      if (line.contains(s)) return lineNb + 1 // lines start at 1, indices at 0
    }

    0 // should never occur
  }
}
