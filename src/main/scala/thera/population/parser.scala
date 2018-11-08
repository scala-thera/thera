package thera.population

import fastparse._, NoWhitespace._
import io.circe.{ Json, yaml }

import ast._

object parser extends HeaderParser with BodyParser with UtilParser {
  val t = token
  def module[_: P]: P[Module] = (header.? ~ tree).map { case (h, t) => Module(h, t) }
}

trait HeaderParser { this: parser.type =>
  def header[_: P]: P[Json] =
    (t.tripleDash ~/ t.nl ~ lines ~ t.nl ~ t.tripleDash).flatMap { lines =>
      yaml.parser.parse(lines.mkString("\n")).fold(
        error   => Fail
      , success => Pass(success))
    }

  def lines[_: P]: P[Seq[String]] = t.line.!.rep(sep = t.nl)
}

trait BodyParser { this: parser.type =>
  def tree[_: P]: P[Tree] = child.rep.map(cs => Tree(cs.toList))

  def child[_: P]: P[Node] = expr | text
  def text [_: P]: P[Text] = CharsWhile(_ != '$').!.map(Text(_))
  def expr [_: P]: P[Node] = (
    "$$".!.map(Text(_))
  | "${" ~ exprBody ~ "}" )

  def exprBody[_: P]: P[Expr] = variable

  def variable[_: P]: P[Variable] = t.name.!.rep(min = 1, sep = ws("."))
    .map { path => Variable(path.toList) }
}

trait UtilParser { this: parser.type =>
  def ws[_: P, A](that: => P[A]): P[A] = t.ws.rep(0) ~ that ~ t.ws.rep(0)
}

object token {
  def tripleDash[_: P] = P("---")

  def line[_: P] = !tripleDash ~ CharsWhile(_ != '\n')

  def nl[_: P] = "\n"
  def ws[_: P] = CharIn(" \t")

  def name[_: P] = CharIn("a-zA-Z0-9\\-_").rep(1)
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
I have numbers ${one}, ${three . four} and ${two}""".tail

  parse(testExpr, module(_)).fold(
    (str, pos, extra) => println(s"Failure: $str, $pos, $extra")
  , (result, pos) => println(result)
  )
}
