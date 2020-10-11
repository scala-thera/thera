package thera.reporting

import fastparse.Parsed.Failure

import scala.io.Source
import scala.util.Using

object Utils {

  private[thera] def getLineNb(s: String, filename: String): Int = getLine(s, filename)._2

  private[thera] def getLine(s: String, filename: String): (String, Int) = {
    val source = Using.resource(Source.fromFile(filename)) {
      _.getLines().toList.zipWithIndex
    }

    source.find {
      case (line, _) => line.contains(s)
    }.map {
      case (line, nb) => (line, nb + 1) // lines start at 1, indices at 0
    }.getOrElse((null, 0)) // should never occur
  }

  private[thera] def getCodeSnippetFromParsingFailure(failure: Failure): String = failure.msg.dropWhile(_ != '"').drop(1).dropRight(1)

  private[thera] def getColumnFromParsingFailure(failure: Failure): Int = getPositionFromParsingFailure(failure)._2

  private[thera] def getPositionFromParsingFailure(failure: Failure): (Int, Int) = {
    val pos = failure.msg.drop(9).takeWhile(_ != ',').split(':')
    (pos(0).toInt, pos(1).toInt)
  }

  private[thera] def isLambda(s: String): Boolean = {
    val lambdaRegex = """=>\s*\$\{[\s\S]+\}\s*""".r
    lambdaRegex.matches(s)
  }

  private[thera] def indexToPosition(input: String, index: Int): (Int, Int) = {
    val prefix = input.take(index)
    val lineNb = prefix.count(_ == '\n') + 1
    val colNb = prefix.lastIndexOf('\n') match {
      case -1 => index + 1
      case n => index - n
    }
    (lineNb, colNb)
  }
}
