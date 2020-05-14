package thera.runtime

import io.circe.Json
import cats._, cats.implicits._, cats.data._

sealed trait Runtime {
  def as[T](name: String)(implicit m: Manifest[T]): T =
    if (m.runtimeClass.equals(this.getClass)) this.asInstanceOf[T]
    else throw new RuntimeException(s"$this is not a $name")

  def asFunc  : Function = as[Function]("function")
  def asText  : Text     = as[Text    ]("text"    )
  def asData  : Data     = as[Data    ]("data"    )

  def evalThunk: Ef[Runtime] = this match {
    case Function(f, true) => f(Nil) >>= (_.evalThunk)
    case x => State.pure(x)
  }
}

case class Text(value: String) extends Runtime
case class Data(value: Json  ) extends Runtime
case class Function(f: (Context, Args) => Runtime, zeroArity: Boolean = false) extends Runtime with Function1[Args, Ef[Runtime]] {
  def apply(as: Args)(implicit ctx: Context): Runtime = f(ctx, as)

  def apply(r1: Runtime)(implicit ctx: Context): Runtime = apply(r1 :: Nil)
  def apply(r1: Runtime, r2: Runtime)(implicit ctx: Context): Runtime = apply(r1 :: r2 :: Nil)
  def apply(r1: Runtime, r2: Runtime, r3: Runtime)(implicit ctx: Context): Runtime = apply(r1 :: r2 :: r3 :: Nil)
}

object Runtime {

  implicit def stringToRuntime(str : String): Runtime = Text(str )
  implicit def jsonToRuntime  (json: Json  ): Runtime =
    json.asString.map(Text(_)).getOrElse(Data(json))

  implicit val monoid: Monoid[Runtime] = new Monoid[Runtime] {
    def combine(x: Runtime, y: Runtime): Runtime = (x, y) match {
      case (Text(s1), Text(s2)) => Text(s1 + s2)
      case _ => throw new RuntimeException(s"Attempt to combine $x and $y failed. It is only possible to combine strings at the moment.")
    }

    def empty: Runtime = Text("")
  }
}
