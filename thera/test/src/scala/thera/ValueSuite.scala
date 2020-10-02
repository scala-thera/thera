package thera

import utest._
import utils._
import ValueHierarchy._
import thera.reporting.{FileInfo, ParserError, YamlError}

object ValueSuite extends TestSuite {
  val tests = Tests {
    test("ValueHierarchy") {
      val ctx1 = ValueHierarchy.names(
        "a" -> Str("foo"),
        "b" -> Str("bar"),
      )
      val ctx2 = ValueHierarchy.names(
        "c" -> Str("aaa"),
        "d" -> Arr(Str("1") :: Str("2") :: Str("3") :: Nil),
        "e" -> ValueHierarchy.names(
          "a" -> Str("e.aaa")
        )
      )
      val ctx3 = ctx1 + ctx2

      def check(v: Value, e: String) = v match {
        case Str(str) => assert(str == e)
      }

      test("lookup") {
        test - check(ctx1("a" :: Nil), "foo")
        test - check(ctx2("e.a"), "e.aaa")
      }
      test("composition") {
        check(ctx3("a" :: Nil), "foo")
        check(ctx3("c" :: Nil), "aaa")
      }
      test("arrays") {
        assertValue(ctx2("d" :: Nil),
          Arr(Str("1") :: Str("2") :: Str("3") :: Nil))
      }

      test("yaml") {
        val fileInfo = FileInfo(sourcecode.File(), isExternal = false)

        test("arrays") {
          val vh = ValueHierarchy.yaml("""
            |keywords: [one, two, three]
          """.stripMargin, fileInfo)
          val res = vh("keywords" :: Nil)
          val expected = Arr(List(Str("one"), Str("two"), Str("three")))
          assert(res == expected)
        }

        test("empty arrays") {
          val vh = ValueHierarchy.yaml("""
            |keywords: []
          """.stripMargin, fileInfo)
          val res = vh("keywords" :: Nil)
          val expected = Arr.empty
          assert(res == expected)
        }

        test("arrays of json objects") {
          val vh = ValueHierarchy.yaml("""
            |stuff: [{ "foo": "bar" }]
          """.stripMargin, fileInfo)
          val res =
            vh("stuff" :: Nil).asArr.value
            .head.asValueHierarchy("foo" :: Nil)
          val expected = Str("bar")
          assert(res == expected)
        }

        test("error reporting") {
          val error = intercept[reporting.Error] {
            ValueHierarchy.yaml("""
            |stuff: { "foo": "bar" }]
          """.stripMargin, fileInfo)
          }

          val expected = ParserError(fileInfo.file.value, 76, 23, """stuff: { "foo": "bar" }]""", YamlError)
          assert(error == expected)
        }
      }
    }
  }
}
