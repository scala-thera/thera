package thera

import cats._, cats.implicits._, cats.data._
import State.{ get, set, pure, modify }

import thera.ast.{ Function => AstFunction, Text => AstText, _ }


package object runtime {
  type Ef[A] = State[Context, A ]
  type Args  = List [Runtime]
  val Ctx    = Context

  def bracket[A](f: Context => Ef[A]): Ef[A] =
    for {
      ctx <- get
      res <- f(ctx)
      _   <- set(ctx)
    } yield res

  def bracket0[A](fx: Ef[A]): Ef[A] = bracket { _ => fx }

  def toRTList(ns: List[Node]): Ef[List[Runtime]] =
    bracket { ctx => ns.traverse(n => bracket0 { toRT(n) }) }

  def toRT(n: Node): Ef[Runtime] = n match {
    case AstText (res ) => pure(Text(res))
    case Variable(name) => get[Context].map(_(name))
    case Call(name, as) => bracket { ctx => toRTList(as) >>= ctx(name).asFunc }

    case AstFunction(ns, vars, body) => pure(Function({ as =>
      modify[Context] { old => List(old, Context.json(vars), Context.names(ns.zip(as).toMap)).combineAll } >>
      toRT(body)
    }, ns.isEmpty))

    case Leafs(ls) => toRTList(ls).map(_.combineAll)
  }

  // Thera Metaprogramming syntax
  // ${foreach: ${fromTo: 1, 22}, ${id => ${indent: 2, \
  //   ${spec = ${deepMerge: ${map: ${fromTo: 1, $id}, ${i => {
  //     types       : R$i
  //     typeParams  : R$i <: Runtime
  //     params      : r$i
  //     paramsCasted: (r$i: R$i @unchecked)
  //   }}}}}
  //   def function[${mkString: ${spec.typeParams}, \, }](f: (${mkString: ${spec.types}, \, }) => Ef[Runtime]) = Function {
  //     case ${mkString: ${spec.paramsCasted}, \ :: } :: Nil => f(${mkString: ${spec.params}, \, })
  //     case x => throw new RuntimeException(s"Argument list $x is inapplicable to $id-ary function")
  //   }
  // }}}

  def function[R1 <: Runtime](f: (R1) => Ef[Runtime]) = Function {
    case (r1: R1 @unchecked) :: Nil => f(r1)
    case x => throw new RuntimeException(s"Argument list $x is inapplicable to 1-ary function")
  }

  def function[R1 <: Runtime, R2 <: Runtime](f: (R1, R2) => Ef[Runtime]) = Function {
    case (r1: R1 @unchecked) :: (r2: R2 @unchecked) :: Nil => f(r1, r2)
    case x => throw new RuntimeException(s"Argument list $x is inapplicable to 2-ary function")
  }

  def function[R1 <: Runtime, R2 <: Runtime, R3 <: Runtime](f: (R1, R2, R3) => Ef[Runtime]) = Function {
    case (r1: R1 @unchecked) :: (r2: R2 @unchecked) :: (r3: R3 @unchecked) :: Nil => f(r1, r2, r3)
    case x => throw new RuntimeException(s"Argument list $x is inapplicable to 3-ary function")
  }
}
