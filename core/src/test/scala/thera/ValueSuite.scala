package thera

import utest._
import utils._
import ValueHierarchy._

object ValueSuite extends TestSuite {
  val tests = Tests {
    test("ValueHierarchy") {
      val ctx1 = ValueHierarchy.names(
        "a" -> Str("foo"),
        "b" -> Str("bar")
      )
      val ctx2 = ValueHierarchy.names(
        "c" -> Str("aaa")
      )
      val ctx3 = ctx1 + ctx2

      def check(v: Value, e: String) = v match {
        case Str(str) => assert(str == e)
      }

      test("lookup") - check(ctx1("a" :: Nil), "foo")
      test("composition") {
        check(ctx3("a" :: Nil), "foo")
        check(ctx3("c" :: Nil), "aaa")
      }
    }
  }
}
