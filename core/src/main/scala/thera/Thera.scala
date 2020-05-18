package thera

import fastparse.Parsed.{ Success, Failure }

object Thera extends Function1[String, Template] {
  def apply(src: String): Template =
    fastparse.parse(src, parser.module(_)) match {
      case Success(result, _) => result
      case f: Failure => throw new RuntimeException(f.toString)
    }
}
