package matryoshkaintro

import matryoshka.{ Recursive, Corecursive }
import matryoshka.data.Fix
import matryoshka.implicits._

object C3Matryoshka extends App with C2Defs {
  // start snippet Examples
  // Nat to Int
  def natToInt[T](n: T)(implicit T: Recursive.Aux[T, Nat]): Int = n.cata[Int] {
    case Succ(x) => 1 + x
    case Zero    => 0
  }
  def nat[T](implicit T: Corecursive.Aux[T, Nat]): T =
    Succ(
      Succ(
        Succ(
          Zero.embed
        ).embed
      ).embed
    ).embed
  val natRes = natToInt(nat[Fix[Nat]])
  println(natRes)  // 3

  // Sum a list of ints
  def sumList[T](l: T)(implicit T: Recursive.Aux[T, IntList]): Int = l.cata[Int] {
    case Cons(head, tail) => head + tail
    case Empty            => 0
  }
  def lst[T](implicit T: Corecursive.Aux[T, IntList]): T =
    Cons(1,
      Cons(2,
        Cons(3,
          Empty.embed
        ).embed
      ).embed
    ).embed
  val listRes = sumList(lst[Fix[IntList]])
  println(listRes)  // 6

  // Evaluate an expression
  def eval[T](e: T)(implicit T: Recursive.Aux[T, Expr]): Int = e.cata[Int] {
    case Add (x1, x2) => x1 + x2
    case Mult(x1, x2) => x1 * x2
    case Num (x)      => x
  }
  def expr[T](implicit T: Corecursive.Aux[T, Expr]): T =
    Add(
      Mult(
        Num(2).embed,
        Num(3).embed
      ).embed,
      Num(3).embed
    ).embed
  val exprRes = eval(expr[Fix[Expr]])
  println(exprRes)  // 9
  // end snippet Examples
}
