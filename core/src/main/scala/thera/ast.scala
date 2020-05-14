package thera

object ast {
  sealed trait Node
  sealed trait BodyNode extends Node
  sealed trait Leaf                     extends BodyNode
  case class   Leafs(nodes: List[Leaf]) extends BodyNode

  case class Text    (value: String) extends Leaf
  case class Call    (path: List[String], args: List[Node]) extends Leaf
  case class Variable(path: List[String]) extends Leaf
  case class Function(args: List[String], vars: Map[String, Any], body: Node) extends Node
}
