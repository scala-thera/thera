package thera.population

import io.circe.Json

object ast {
  case class Module(header: Option[Json], body: Tree)

  sealed trait Node
  case class   Text(value: String) extends Node
  sealed trait Expr extends Node
  case class   Tree(children: List[Node]) extends Node

  // Expr
  case class Variable(path : List[String  ]                      ) extends Expr
  case class Function(input: List[NamedArg], body: Tree          ) extends Expr
  case class Call    (path : List[String  ], args: List[Argument]) extends Expr

  sealed trait Argument
  case class PositionalArg(value: Node, position: Int) extends Argument
  case class NamedArg     (value: Node, name: String ) extends Argument
}
