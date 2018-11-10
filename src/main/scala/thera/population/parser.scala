package thera.population

import fastparse._, NoWhitespace._, Parsed.{ Failure, Success }
import io.circe.{ Json, yaml }

import ast._

object parser extends HeaderParser with BodyParser with UtilParser {
  val t = token
  def module[_: P]: P[Module] = (header.? ~ tree()).map {
    case (Some((args, h)), t) => Module(args, h   , t)
    case (None           , t) => Module(Nil , None, t)
  }
}

trait HeaderParser { this: parser.type =>
  def header[_: P]: P[(List[String], Option[Json])] =
    (wsnl(t.tripleDash) ~/ moduleArgs.? ~/ lines ~ wsnl(t.tripleDash)).flatMap {
      case (args, Nil  ) => Pass(args.getOrElse(Nil) -> None)
      case (args, lines) =>
        yaml.parser.parse(lines.mkString("\n")).fold(
          error   => Fail
        , success => Pass(args.getOrElse(Nil) -> Some(success)))
    }

  def lines[_: P]: P[Seq[String]] = t.line.!.rep(min = 0, sep = t.nl)

  def moduleArgs[_: P]: P[List[String]] =
    (wsnl("[") ~/ t.name.!.rep(sep = wsnl(",")) ~ wsnl("]")).map(_.toList)
}

trait BodyParser { this: parser.type =>
  def tree[_: P](specialChars: String = t.defaultSpecialChars): P[Tree] =
    node((specialChars ++ t.defaultSpecialChars).distinct).rep.map(cs => Tree(cs.toList))

  def node[_: P](specialChars: String): P[Node] =
    expr | text(specialChars)
  
  def text[_: P](specialChars: String): P[Text] =
    textOne(specialChars).rep(1).map { texts => texts.foldLeft(Text("")) { (accum, t) =>
      Text(accum.value + t.value) } }

  def textOne[_: P](specialChars: String): P[Text] = (
    oneOf(specialChars.toList.map {c => () => LiteralStr(s"\\$c").!.map(_.tail.head.toString)})
  | CharsWhile(c => !specialChars.contains(c)).! ).map(Text)

  def expr[_: P]: P[Node] = "${" ~/ exprBody ~ "}"

  def exprBody[_: P]: P[Expr] = function | call | variable

  def path[_: P]: P[List[String]] = t.name.!.rep(min = 1, sep = wsnl(".")).map(_.toList)

  def function[_: P]: P[Function] = (args ~ wsnl("=>") ~/ wsnl0Esc ~ tree())
    .map { case (args, body) => Function(args, body) }

  def arg [_: P]: P[     String ] = wsnl(t.name.!)
  def args[_: P]: P[List[String]] = arg.rep(min = 1, sep = ",").map(_.toList)

  def call[_: P]: P[Call] = (wsnl(path) ~ ":" ~/ wsnl0Esc ~ tree(",").rep(min = 1, sep = "," ~ wsnl0Esc))
    .map { case (path, args) => Call(path, args.toList) }

  def variable[_: P]: P[Variable] = wsnl(path).map(Variable(_))
}

trait UtilParser { this: parser.type =>
  def ws[_: P, A](that: => P[A]): P[A] = t.ws0 ~ that ~ t.ws0
  def oneOf[_: P, A](that: Seq[() => P[A]]): P[A] =
    that.foldLeft(Fail: P[A]) { (accum, next) => accum | next() }

  def wsnl[_: P, A](that: => P[A]): P[A] = t.wsnl0 ~ that ~ t.wsnl0

  def wsnl0Esc[_: P] = t.wsnl0 ~ ("\\" ~ &(t.wsnl1)).? 
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

  val defaultSpecialChars = "${}\\"
}

object ParserTest extends App {
  import parser._
  import better.files._, better.files.File._, java.io.{ File => JFile }

  val toParse = List("html-template")

  toParse.map(name => file"example/$name.html").foreach { file =>
    println(s"=== Parsing $file ===")
    parse(file.contentAsString, module(_)) match {
      case Success(result, pos) => println(result)
      case f: Failure => println(f)
    }
    println()
  }
  println(parse("""
---
---""".tail, header(_)))
}
