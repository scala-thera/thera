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
  def tree[_: P]: P[Tree] = node().rep.map(cs => Tree(cs.toList))

  def node[_: P](specialChars: String = ""): P[Node] =
    expr | text(specialChars + '$')
  
  def text[_: P](specialChars: String): P[Text] =
    textOne(specialChars).rep(1).map { texts => texts.foldLeft(Text("")) { (accum, t) =>
      Text(accum.value + t.value) } }

  def textOne[_: P](specialChars: String): P[Text] = (
    oneOf(specialChars.toList.map {c => () => LiteralStr(s"$c$c").!.map(_.head.toString)})
  | CharsWhile(c => !specialChars.contains(c)).! ).map(Text)

  def expr[_: P]: P[Node] = "${" ~ exprBody ~ "}"

  def exprBody[_: P]: P[Expr] = call | variable

  def path[_: P]: P[List[String]] = t.ws0 ~ t.name.!.rep(min = 1, sep = wsnl(".")).map(_.toList)

  def call[_: P]: P[Call] = (path ~ t.wsnl1 ~ node(",}").rep(min = 1, sep = "," ~ t.wsnl1))
    .map { case (path, args) => Call(path, args.toList) }

  def variable[_: P]: P[Variable] = (path ~ t.ws0).map(Variable(_))
}

trait UtilParser { this: parser.type =>
  def ws[_: P, A](that: => P[A]): P[A] = t.ws0 ~ that ~ t.ws0
  def oneOf[_: P, A](that: Seq[() => P[A]]): P[A] =
    that.foldLeft(Fail: P[A]) { (accum, next) => accum | next() }

  def wsnl[_: P, A](that: => P[A]): P[A] = t.wsnl0 ~ that ~ t.wsnl0
}

object token {
  def tripleDash[_: P] = P("---")

  def line[_: P] = !tripleDash ~ CharsWhile(_ != '\n')

  def nl1[_: P] = "\n"
  def nl [_: P] = nl1.rep(1)
  def nl0[_: P] = nl1.rep(0)

  def ws1[_: P] = CharIn(" \t")
  def ws [_: P] = ws1.rep(1)
  def ws0[_: P] = ws1.rep(0)

  def wsnl1[_: P] = ws1 | nl1
  def wsnl [_: P] = wsnl1.rep(1)
  def wsnl0[_: P] = wsnl1.rep(0)

  def name[_: P] = CharIn("a-zA-Z0-9\\-_").rep(1)
}

object ParserTest extends App {
  import parser._
  import better.files._, better.files.File._, java.io.{ File => JFile }

  val toParse = List("html-template")

  toParse.map(name => file"example/$name.html").foreach { file =>
    println(s"=== Parsing $file ===")
    parse(file.contentAsString, module(_)).fold(
      (str, pos, extra) => println(s"Failure: $str, $pos, $extra")
    , (result, pos) => println(result)
    )
    println()
  }
}
