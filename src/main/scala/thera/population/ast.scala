package thera.population

import io.circe.Json

object ast {
  case class Module(header: Option[Json], body: Tree)

  sealed trait Node
  case class   Text(value: String) extends Node
  sealed trait Expr extends Node
  case class   Tree(children: List[Node]) extends Node

  // Expr
  case class Variable(path: List[String]                  ) extends Expr
  case class Call    (path: List[String], args: List[Node]) extends Expr
  case class Function(args: List[String], body: Tree      ) extends Expr
}
