package thera.runtime

import cats._, cats.implicits._, cats.data._, cats.effect._

sealed trait Runtime {
  def asFunc: Function = this match {
    case f: Function => f
    case x => throw new RuntimeException(s"$x is not a function")
  }

  def asData: Data = this match {
    case d: Data => d
    case x => throw new RuntimeException(s"$x is not a data")
  }

  def asString: String = asData.value

  def evalFuncEmpty: Fx[Runtime] = asFunc(Nil)
}

case class Data(value: String) extends Runtime
case class Function(f: Args => Fx[Runtime]) extends Runtime with Function1[Args, Fx[Runtime]] {
  def apply(as: Args): Fx[Runtime] = f(as)
}

object Runtime {
  implicit val monoid: Monoid[Runtime] = new Monoid[Runtime] {
    def combine(x: Runtime, y: Runtime): Runtime = (x, y) match {
      case (Data(s1), Data(s2)) => Data(s1 + s2)
      case _ => throw new RuntimeException(s"Attempt to combine $x and $y failed. It is only possible to combine strings at the moment.")
    }

    def empty: Runtime = Data("")
  }
}
