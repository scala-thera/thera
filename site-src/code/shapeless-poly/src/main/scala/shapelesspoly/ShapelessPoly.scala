package shapelesspoly


object ShapelessPoly extends App {
  // start snippet body
  import shapeless._
  import poly._

  object f extends Poly1 {
    implicit val intCase    = at[Int   ] { x => "It Works! " * x}
    implicit val stringCase = at[String] { x => x.length        }
  }

  println(f(3))
  println(f("Foo"))
  // end snippet body
}
