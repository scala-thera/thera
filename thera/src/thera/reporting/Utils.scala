package thera.reporting

import fastparse.Parsed.Failure

import scala.io.Source
import scala.util.Using

object Utils {

  private[thera] def getLineNb(s: String, filename: String): Int = getLine(s, filename)._2

  private[thera] def getLine(s: String, filename: String): (String, Int) = {
    val source = Using.resource(Source.fromFile(filename)) { _.getLines().toList.zipWithIndex }

    for ((line, lineNb) <- source) {
      if (line.contains(s)) return (line, lineNb + 1) // lines start at 1, indices at 0
    }

    (null, 0) // should never occur
  }

  private[thera] def getCodeSnippetFromParsingFailure(failure: Failure): String = failure.msg.dropWhile(_ != '"').drop(1).dropRight(1)

  private[thera] def getPositionFromParsingFailure(failure: Failure): (Int, Int) = {
    val pos = failure.msg.drop(9).takeWhile(_ != ',').split(':')
    (pos(0).toInt, pos(1).toInt)
  }

  private[thera] def getColumnFromParsingFailure(failure: Failure): Int = getPositionFromParsingFailure(failure)._2
}
