package thera

object ast {
  sealed trait Node
  /**
   * A template is a mixture of text, variables and calls to other templates.
   * The variables and names of other templates are resolved against a runtime
   * Context.
   *
   * @param argNames – the names of the arguments to this template.
   *                   Upon evaluation, the argument values are bound to
   *                   the given names.
   * @param predefinedVars – constant variables defined for the function.
   * @param body – the body of the template. Can refer to the variables and
   *               templates defined in predefinedVars and bound to argNames.
   */
  case class Template(argNames: List[String], predefinedVars: Json, body: BodyNode) extends Node
  sealed trait BodyNode extends Node

  sealed trait Leaf extends BodyNode
  case class Leafs(nodes: List[Leaf]) extends BodyNode

  case class Text(value: String) extends Leaf

  /**
   * A call to a template located at a given path and with provided arguments.
   *
   * @param path – the path to the template to be called.
   * @param args – these nodes will be bound to the argument names of the
   *               template being called.
   */
  case class Call(path: List[String], args: List[Node]) extends Leaf
  case class Variable(path: List[String]) extends Leaf
}
