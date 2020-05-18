package thera

/**
 * A template is a mixture of text, variables and calls to other templates.
 * The variables and names of other templates are resolved against a runtime
 * Context.
 *
 * @param argNames – the names of the arguments to this template.
 *                   Upon evaluation, the argument values are bound to
 *                   the given names.
 * @param templateContext – constant variables defined in the template.
 * @param body – the body of the template. Can refer to the variables and
 *               templates defined in predefinedVars and bound to argNames.
 */
case class Template(argNames: List[String], context: ValueHierarchy, body: Body) {
  def mkString(implicit ctx: ValueHierarchy =
    ValueHierarchy.empty): String =
    evaluate.template(this, ctx) match {
      case Right(str) => str
      case Left(_) => throw new RuntimeException(
        s"Expected string, found function")
    }

  def mkFunction(implicit ctx: ValueHierarchy =
    ValueHierarchy.empty): List[Value] => String =
    evaluate.template(this, ctx) match {
      case Left(f) => f
      case Right(_) => throw new RuntimeException(
        s"Expected function, found string")
    }

  def mkStrValue(implicit ctx: ValueHierarchy =
    ValueHierarchy.empty): Str = Str(mkString)

  def mkFunctionValue(implicit ctx: ValueHierarchy =
    ValueHierarchy.empty): Function = {
    val f = mkFunction
    Function { args => Str(f(args)) }
  }
}

sealed trait Node
case class Text(value: String) extends Node
/**
 * A call to a template located at a given path and with provided arguments.
 *
 * @param path – the path to the template to be called.
 * @param args – these nodes will be bound to the argument names of the
 *               template being called.
 */
case class Call(path: List[String], args: List[CallArg]) extends Node
case class Variable(path: List[String]) extends Node

sealed trait CallArg
case class Body(nodes: List[Node]) extends CallArg
case class Lambda(argNames: List[String], body: Body) extends CallArg
