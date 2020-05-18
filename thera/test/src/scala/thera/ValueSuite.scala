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
        "c" -> Str("aaa"),
        "d" -> Arr(Str("1") :: Str("2") :: Str("3") :: Nil)
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
      test("arrays") {
        assertValue(ctx2("d" :: Nil),
          Arr(Str("1") :: Str("2") :: Str("3") :: Nil))
      }

      test("yaml") {
        test("arrays") {
          val vh = ValueHierarchy.yaml("""
            |keywords: [one, two, three]
          """.stripMargin)
          val res = vh("keywords" :: Nil)
          val expected = Arr(List(Str("one"), Str("two"), Str("three")))
          assert(res == expected)
        }

        test("empty arrays") {
          val vh = ValueHierarchy.yaml("""
            |keywords: []
          """.stripMargin)
          val res = vh("keywords" :: Nil)
          val expected = Arr.empty
          assert(res == expected)
        }

        test("arrays of json objects") {
          val vh = ValueHierarchy.yaml("""
            |stuff: [{ "foo": "bar" }]
          """.stripMargin)
          val res =
            vh("stuff" :: Nil).asArr.value
            .head.asValueHierarchy("foo" :: Nil)
          val expected = Str("bar")
          assert(res == expected)
        }
      }
    }
  }
}
