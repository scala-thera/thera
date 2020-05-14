package thera.runtime

import thera.ast.{ Function => AstFunction, Text => AstText, _ }

sealed trait Runtime {
  def as[T](name: String)(implicit m: Manifest[T]): T =
    if (m.runtimeClass.equals(this.getClass)) this.asInstanceOf[T]
    else throw new RuntimeException(s"$this is not a $name")

  def asFunc: Function = as[Function]("function")
  def asText: Text = as[Text]("text")
  def asData: Data = as[Data]("data")

  def evalThunk(implicit ctx: Context): Runtime = this match {
    case Function(f, 0) => f(Nil).evalThunk
    case x => x
  }
}

object Runtime extends Function1[Node, Runtime] {
  /**
   * Given an AST, evaluates that AST against a given Context.
   * Evaluation means that all the variables and functions are
   * resolved against the given Context and all the functions are
   * executed.
   */
  def apply(n: Node)(implicit ctx: Context): Runtime = n match {
    case AstText(res) => Text(res)
    case Variable(name) => ctx(name)
    case Call(name, as) => ctx(name).asFunc(as.map(toRt))

    case AstFunction(argNames, vars, body) =>
      Function({ (ctx, argValues) =>
        implicit val funcCtx = ctx + Context.json(vars) +
          Context.names(argNames.zip(argValues).toMap)
        Runtime(body)
      }, argNames.length)

    case Leafs(ls) => ls.map(Runtime).foldLeft(Text(""))(_ + _)
  }
}

case class Function(f: (Context, Args) => Text, arity: Int) extends Runtime {
  def apply(as: Args)(implicit ctx: Context): Runtime = f(ctx, as)

  def apply(r1: Runtime)(implicit ctx: Context): Runtime = apply(r1 :: Nil)
  def apply(r1: Runtime, r2: Runtime)(implicit ctx: Context): Runtime = apply(r1 :: r2 :: Nil)
  def apply(r1: Runtime, r2: Runtime, r3: Runtime)(implicit ctx: Context): Runtime = apply(r1 :: r2 :: r3 :: Nil)
}
