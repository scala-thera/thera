package thera

import cats._, cats.implicits._, cats.data._, cats.effect._
import State.{ get, set, pure, modify }

import thera.ast.{ Function => AstFunction, Text => AstText, _ }


package object runtime {
  type Fx[A] = State[Context, A ]
  type Args  = List [Runtime]
  val Ctx    = Context

  def bracket[A](f: Context => Fx[A]): Fx[A] =
    for {
      ctx <- get
      res <- f(ctx)
      _   <- set(ctx)
    } yield res

  def bracket0[A](fx: Fx[A]): Fx[A] = bracket { _ => fx }

  def toRTList(ns: List[Node]): Fx[List[Runtime]] =
    bracket { ctx => ns.traverse(n => bracket0 { toRT(n) }) }

  def toRT(n: Node): Fx[Runtime] = n match {
    case AstText (res ) => pure(Text(res))
    case Variable(name) => get[Context].map(_(name))
    case Call(name, as) => bracket { ctx => toRTList(as) >>= ctx(name).asFunc }

    case AstFunction(ns, vars, body) => pure(Function { as =>
      modify[Context] { old => List(old, Context.json(vars), Context.names(ns.zip(as).toMap)).combineAll } >>
      toRT(body)
    })

    case Leafs(ls) => toRTList(ls).map(_.combineAll)
  }
}
