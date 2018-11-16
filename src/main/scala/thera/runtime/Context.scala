package thera.runtime

import cats._, cats.implicits._, cats.data._, cats.effect._
import io.circe._

trait Context {
  def applyOpt(name: List[String]): Option[Runtime]
  def apply(name: List[String]): Runtime =
    applyOpt(name).getOrElse(throw new RuntimeException(s"Symbol not found: ${name.mkString(".")}"))
}

object Context {
  def apply(f: List[String] => Option[Runtime]): Context = new Context {
    def applyOpt(name: List[String]): Option[Runtime] = f(name)
  }

  def map(m: Map[List[String], Runtime]) = Context(m.get)

  def names(m: Map[String, Runtime]): Context = map(m.map { case (k, v) => List(k) -> v })
  def names(ns: (String, Runtime)*): Context = names(ns.toMap)

  def json(vars: Json): Context = Context { name =>
    name.foldLeft(vars.hcursor: ACursor) { (cur, next) => cur.downField(next) }
      .focus.map { j => j.asString.map(Text(_)).getOrElse(Data(j)) } }

  implicit val monoid: Monoid[Context] = new Monoid[Context] {
    def combine(x: Context, y: Context): Context = Context { name =>
      y.applyOpt(name) orElse x.applyOpt(name) }

    def empty: Context = Context { _ => None }
  }

}
