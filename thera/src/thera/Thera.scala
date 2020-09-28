package thera

import fastparse.Parsed.{ Success, Failure }

object Thera {
  def apply(src: String)(implicit file: sourcecode.File): Template =
    fastparse.parse(src, parser.module(_, file)) match {
      case Success(result, _) => result
      case f: Failure =>
        // TODO SyntaxError
        // TODO if it was a lambda, InvalidLambdaUsageError
        throw new RuntimeException(f.toString)
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
