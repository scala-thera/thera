package matryoshkaintro

object C1NonDry extends App with C1Defs {
  // start snippet NatEx
  // Nat to Int
  def natToInt(n: Nat): Int = n match {
    case Succ(x) => 1 + natToInt(x)
    case Zero    => 0
  }
  val nat = Succ(Succ(Succ(Zero)))
  val natRes: Int = natToInt(nat)
  println(natRes)  // 3
  // end snippet NatEx

  // start snippet ListEx
  // Sum a list of ints
  def sumList(l: IntList): Int = l match {
    case Cons(head, tail) => head + sumList(tail)
    case Empty            => 0
  }
  val lst = Cons(1, Cons(2, Cons(3, Empty)))
  val listRes: Int = sumList(lst)
  println(listRes)  // 6
  // end snippet ListEx

  // start snippet ExprEx
  // Evaluate an expression
  def eval(e: Expr): Int = e match {
    case Add (x1, x2) => eval(x1) + eval(x2)
    case Mult(x1, x2) => eval(x1) * eval(x2)
    case Num (x)      => x
  }
  val expr = Add(Mult(Num(2), Num(3)), Num(3))
  val exprRes: Int = eval(expr)
  println(exprRes)  // 9
  // end snippet ExprEx
}

trait C1Defs {
  // start snippet NatDef
  // Nat
  sealed trait Nat
  case class   Succ(previous: Nat) extends Nat
  case object  Zero                extends Nat
  // end snippet NatDef

  // start snippet ListDef
  // List
  sealed trait IntList
  case class   Cons(head: Int, tail: IntList) extends IntList
  case object  Empty                          extends IntList
  // end snippet ListDef

  // start snippet ExprDef
  // Expr
  sealed trait Expr
  case class   Add (expr1  : Expr, expr2: Expr) extends Expr
  case class   Mult(expr1  : Expr, expr2: Expr) extends Expr
  case class   Num (literal: Int              ) extends Expr
  // end snippet ExprDef
}