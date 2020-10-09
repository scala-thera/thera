package thera

import java.net.URL

import fastparse.Parsed.{Failure, Success}
import thera.reporting.Utils.{getCodeSnippetFromParsingFailure, getColumnFromParsingFailure, getLine}
import thera.reporting.{FileInfo, ParserError, SyntaxError}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.io.Source
import scala.util.Using

object Thera {
  private def buildTemplate(src: String, fileInfo: FileInfo): Template =
    fastparse.parse(src, parser.module(_, fileInfo)) match {
      case Success(result, _) => result
      case f: Failure =>
        val code = getCodeSnippetFromParsingFailure(f)

        val ((line, lineNb), column) = {
          val ln = Future { getLine(code, fileInfo.file.value) }
          val col = Future { getColumnFromParsingFailure(f) }
          (Await.result(ln, Duration.Inf), Await.result(col, Duration.Inf))
        }

        throw ParserError(fileInfo.file.value, lineNb, column, line, SyntaxError)
        // TODO if it was a lambda, InvalidLambdaUsageError
    }

  def apply(src: String)(implicit file: sourcecode.File): Template = buildTemplate(src, FileInfo(file, isExternal = false))

  // TODO apply (src: Java File)

  def apply(src: URL): Template = { // TODO drop it
    val srcString = Using.resource(Source.fromURL(src)){ _.mkString}
    buildTemplate(srcString, FileInfo(sourcecode.File(src.getPath), isExternal = true))
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
