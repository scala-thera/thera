package thera

import thera.ast.{ Function => AstFunction, Text => AstText, _ }

package object runtime {
  type Args = List[Runtime]

  def toRT(n: Node)(implicit ctx: Context): Runtime = n match {
    case AstText (res ) => Text(res)
    case Variable(name) => ctx(name)
    case Call(name, as) => ctx(name).asFunc(as.map(toRt))

    case AstFunction(ns, vars, body) => Function({ (ctx, as) =>
      implicit val funcCtx = old + Context.json(vars) + Context.names(ns.zip(as).toMap)
      toRT(body)
    }, ns.isEmpty)

    case Leafs(ls) => toRTList(ls).foldLeft(Text(""))(_ + _)
  }

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
