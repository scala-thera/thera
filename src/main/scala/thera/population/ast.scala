package thera.population

import io.circe.Json

object ast {
  sealed trait Node
  sealed trait Leaf                     extends Node
  case class   Leafs(nodes: List[Leaf]) extends Node

  case class Text    (value: String                              ) extends Leaf
  case class Call    (path : List[String], args: List[Node]      ) extends Leaf
  case class Variable(path : List[String]                        ) extends Leaf
  case class Function(args : List[String], vars: Json, body: Node) extends Node
}
