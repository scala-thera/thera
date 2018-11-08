package thera.population

import fastparse._, NoWhitespace._
import io.circe.{ Json, yaml }

import ast._

object parser {
  val t = token

  def module[_: P]: P[(Option[Json], String)] = header.? ~ body.!

  def header[_: P]: P[Json] =
    (t.tripleDash ~/ t.nl ~ lines ~ t.nl ~ t.tripleDash).flatMap { lines =>
      yaml.parser.parse(lines.mkString("\n")).fold(
        error   => Fail
      , success => Pass(success))
    }

  def lines[_: P]: P[Seq[String]] = t.line.!.rep(sep = t.nl)

  def body[_: P]: P[String] = (AnyChar.rep.!).map(_.mkString)
}

object token {
  def tripleDash[_: P] = P("---")

  def line[_: P] = !tripleDash ~ CharsWhile(_ != '\n')

  def nl[_: P] = "\n"
}

object ParserTest extends App {
  import parser._

  val testExpr = """
---
template: html-template
filters: [currentTimeFilter]
variables:
  title: This stuff works!
  one: "1"
  two: "2"
fragments:
  three: three-frag
---
body""".tail

  parse(testExpr, module(_)).fold(
    (str, pos, extra) => println(s"Failure: $str, $pos, $extra")
  , (result, pos) => println(result)
  )
}
