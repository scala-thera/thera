package shapelesspoly

object AdHocPoly extends App {
  // start snippet body
  trait Case[F, In] {
    type Out
    def apply(x: In): Out
  }

  trait Poly {
    def apply[T](x: T)(implicit cse: Case[this.type, T]): cse.Out = cse(x)
  }

  object f extends Poly {
    implicit val intCase = new Case[f.type, Int] {
      type Out = String
      def apply(x: Int) = "It works! " * x
    }

    implicit val stringCase = new Case[f.type, String] {
      type Out = Int
      def apply(x: String) = x.length
    }
  }

  println(f(3))
  println(f("Foo"))
  // end snippet body
}
