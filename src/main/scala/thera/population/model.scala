package thera.population

object model {

  trait Function {
    def apply(args: List[Node])(implicit ctx: Context): Node
  }

  trait Context {
    def resolveVal(name: List[String]): Option[Json]
    def resolveFun(name: List[String]): Option[Function]

    def resolveValForce(name: List[String]): Json = resolveVal(name)
      .getOrElse(throw new RuntimeException(s"Value not found: ${name.mkString(".")}"))

    def resolveFunForce(name: List[String]): Json = resolveFun(name)
      .getOrElse(throw new RuntimeException(s"Function not found: ${name.mkString(".")}"))

    def addVal   (name: String, value: Json    ): Context
    def addFun   (name: String, value: Function): Context
    def addValStr(name: String, value: String  ): Context = addVal(name, Json.fromString(value))
  }

  sealed trait Node {
    def ctx: Context

    def step: Either[Node, String] = this match {
      case Leafs(n :: ns)       => Cont(n, nr => Cont(Leafs(ns), nsr => n + nsr))
      case Leafs(Nil)           => Right("")
      case Cont(Cont(x, f), ff) => Cont(x, xRes => Cont(f(xRes), ff))
      case Cont(x, f)           => step(x, ctx).fold(node => Cont(node, f), text => f(text))

      case Text(result    ) => Right(result)
      case Call(path, args) => ctx.resolveFunForce(path)(args)
      case Variable(path  ) =>
        val json = ctx.resolveValForce(name)
        json.asString.getOrElse(json.toString)
    }

    @annotation.tailrec
    def compute: String = step match {
      case Right(result) => result
      case Left (node  ) => node.compute
    }
  }

  import ast.{
    Function => AstFunction
  , Leafs    => AstLeafs
  , Text     => AstText
  , Call     => AstCall
  , Variable => AstVariable
  }
  def astToFunction(f: AstFunction): Function = new Function {
    def apply(args: List[Node])(implicit ctx: Context): Node = {
      f.args.zip(args).foldLeft(ctx.addJson(f.vars)) { case (ctxAccum, (name, arg)) => arg match {
        case f: AstFunction    => ctx.addFun   (name, astToFunction(f)         )
        case AstVariable(path) => ctx.addVal   (name, ctx.resolveValForce(path))
        case Text(value)       => ctx.addValStr(name, value                    )
        case n                 => ctx.addValStr(name, fold(n, ctx)             )
      }}

      f.body
    }
  }

  def astToModel(n: ast.Node): Node = n match {
    case 
  }
}

