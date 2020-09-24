package thera

import fastparse.NoWhitespace._
import fastparse._

object parser extends HeaderParser with BodyParser with BodyUtilParser with UtilParser {
  val t = token
  def module[_: P](implicit file: sourcecode.File): P[Template] = (header.? ~ body() ~ End).map {
    case (Some((args, h)), t) => Template(args, h, t)
    case (None           , t) => Template(Nil , ValueHierarchy.empty, t)
  }
}

trait HeaderParser { this: parser.type =>
  def header[_: P](implicit file: sourcecode.File): P[(List[String], ValueHierarchy)] =
    (wsnl(t.tripleDash) ~/ moduleArgs.? ~/ lines ~ wsnl(t.tripleDash)).flatMap {
      case (args, Nil  ) => Pass(args.getOrElse(Nil) -> ValueHierarchy.empty)
      case (args, lines) => Pass(args.getOrElse(Nil) ->
        ValueHierarchy.yaml(lines.mkString("\n")))
    }

  def lines[_: P]: P[Seq[String]] = t.line.!.rep(min = 0, sep = t.nl)

  def moduleArgs[_: P]: P[List[String]] =
    (wsnl("[") ~/ t.name.!.rep(sep = wsnl(",")) ~ wsnl("]")).map(_.toList)
}

trait BodyParser { this: parser.type =>
  def body[_: P](specialChars: String = ""): P[Body] =
    node((specialChars ++ t.defaultSpecialChars).toSeq.distinct.unwrap).rep(1)
      .map(_.toList.filter { case Text("") => false case _ => true })
      .map(ns => Body(ns))

  def node[_: P](specialChars: String): P[Node] =
    expr | text(specialChars)


  def text[_: P](specialChars: String): P[Text] =
    textOne(specialChars).rep(1).map { texts => texts.foldLeft(Text("")) { (accum, t) =>
      Text(accum.value + t.value) } }

  def expr[_: P]: P[Node] = "$" ~/ (variableSimple | "{" ~/ exprBody ~ "}")

  def exprBody[_: P]: P[Node] = call | variable


  def call[_: P]: P[Call] = (wsnl(path) ~ ":" ~/ wsnl0Esc ~
    (function | body(",")).rep(min = 0, sep = "," ~ wsnl0Esc))
      .map { case (path, args) => Call(path, args.toList) }

  def variable[_: P]: P[Variable] = wsnl(path).map(Variable(_))


  def function[_: P]: P[Lambda] = ("${" ~ args ~ wsnl("=>") ~/ wsnl0Esc ~ body() ~ "}" ~ t.wsnl0)
    .map { case (args, body) => Lambda(args, body) }

  def variableSimple[_: P]: P[Variable] = t.name.!.map(n => Variable(n :: Nil))
}

trait BodyUtilParser { this: parser.type =>
  def textOne[_: P](specialChars: String): P[Text] = (
    "\\" ~/ (
      oneOf(specialChars.toList.map { c => () => LiteralStr(c.toString) }).!
    | "n".!.map { _ => "\n" }
    | ("s" ~ wsnl0Esc).map { _ => "" }
    )
  | CharsWhile(c => !specialChars.contains(c)).! ).map(Text)

  def path[_: P]: P[List[String]] = t.name.!.rep(min = 1, sep = wsnl(".")).map(_.toList)

  def arg [_: P]: P[     String ] = wsnl(t.name.!)
  def args[_: P]: P[List[String]] = arg.rep(min = 1, sep = ",").map(_.toList)
}

trait UtilParser { this: parser.type =>
  def ws[_: P, A](that: => P[A]): P[A] = t.ws0 ~ that ~ t.ws0
  def oneOf[_: P, A](that: Seq[() => P[A]]): P[A] =
    that.foldLeft(Fail: P[A]) { (accum, next) => accum | next() }

  def wsnl[_: P, A](that: => P[A]): P[A] = t.wsnl0 ~ that ~ t.wsnl0

  def wsnl0Esc[_: P] = t.wsnl0 ~ ("\\" ~ t.nl1.? ~ &(t.wsnl1)).?
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
