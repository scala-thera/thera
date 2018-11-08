package thera.population

import fastparse._, NoWhitespace._

import ast._

object parser {
  val t = token

  def module[_: P]: P[(Option[String], String)] = header.? ~ body.!

  def header[_: P]: P[String] =
    (t.tripleDash ~ t.nl ~ lines ~ t.nl ~ t.tripleDash).map(_.mkString("\n"))

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
foo bar char
var char far
---
body""".tail

  println(parse(testExpr, module(_)))
}
