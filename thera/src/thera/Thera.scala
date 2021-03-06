package thera

import java.io.File

import fastparse.Parsed.{Failure, Success}
import thera.reporting.Utils.{getCodeSnippetFromParsingFailure, getColumnFromParsingFailure, getLine, isLambda}
import thera.reporting._

import scala.io.Source
import scala.util.Using

object Thera {

  def apply(src: String)(implicit file: sourcecode.File): Template = buildTemplate(src, FileInfo(file, isExternal = false))

  private def buildTemplate(src: String, fileInfo: FileInfo): Template =
    fastparse.parse(src, parser.module(src)(_, fileInfo)) match {
      case Success(result, _) => result
      case f: Failure =>
        val code = getCodeSnippetFromParsingFailure(f)
        val ((line, lineNb), column) = (getLine(code, fileInfo.file.value), getColumnFromParsingFailure(f))
        val filename = fileInfo.file.value

        throw if (isLambda(code)) EvaluationError(filename, lineNb, column, line, InvalidLambdaUsageError)
        else ParserError(filename, lineNb, column, line, SyntaxError)
    }

  def apply(src: File): Template = {
    val srcString = Using.resource(Source.fromFile(src)) { _.mkString }
    buildTemplate(srcString, FileInfo(sourcecode.File(src.getAbsolutePath), isExternal = true))
  }

  def split(src: String): (String, String) = {
    val header = src.linesIterator.drop(1).takeWhile(_ != "---").mkString("\n")
    val body = src.linesIterator.drop(1).dropWhile(_ != "---").drop(1).mkString("\n")
    (header, body)
  }

  def join(header: String, body: String) =
    s"---\n$header\n---\n$body"

  def quote(str: String): String = {
    val pattern = """([\$\{\}\\])""".r
    pattern.replaceAllIn(str, m => {
      scala.util.matching.Regex.quoteReplacement(s"\\${m.group(1)}")
    })
  }
}
