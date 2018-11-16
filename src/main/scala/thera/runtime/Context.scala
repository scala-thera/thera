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

  def apply(vars: Json): Context = Context { name =>
    name.foldLeft(vars.hcursor: ACursor) { (cur, next) => cur.downField(next) }
      .focus.map { j => Data(j.asString.getOrElse(j.toString)) } }

  def apply(names: List[String], args: List[Runtime]): Context = new Context {
    val map: Map[String, Runtime] = names.zip(args).toMap

    def applyOpt(name: List[String]): Option[Runtime] = name match {
      case name :: Nil => map.get(name)
      case _ => None
    }
  }

  implicit val monoid: Monoid[Context] = new Monoid[Context] {
    def combine(x: Context, y: Context): Context = Context { name =>
      y.applyOpt(name) orElse x.applyOpt(name) }

    def empty: Context = Context { _ => None }
  }

}
