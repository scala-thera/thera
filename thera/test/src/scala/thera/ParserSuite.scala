package thera

import utest._
import utils._
import parser._
import fastparse._
import sourcecode.File
import thera.reporting.FileInfo

object ParserSuite extends TestSuite {
  val tests = Tests {
    def check(name: String): Unit = {
      val path = s"/parser/$name"
      val (input, expected) = readIO(path)
      // TODO fix
      val result = p(input, module(input)(_, FileInfo(File(path), isExternal = true)))
      if (result != expected) {
        println(s"Result:\n$result\n===\nExpected:\n$expected")
        assert(false)
      }
    }

    def p[A](str: String, p: P[_] => P[A]): String =
      parse(str, p(_)).get.value.toString


    test("Named") {
      test("fun-frag") - check("fun-frag")
      // test("html-template") - check("html-template")
      test("index") - check("index")
      test("tree-frag") - check("tree-frag")
    }

    test("tree-args") {
      val result = p("${f: a ${b} c, ${d}}", expr(_))
      assert(
        result ==
         "Call(List(f),List(Body(List(Text(a ), Variable(List(b)), Text( c))), Body(List(Variable(List(d))))))"
      )
    }

    test("zero-arg-calls") {
      val result = p("${f:}"  , expr(_))
      assert(
        result ==
         "Call(List(f),List())"
      )
    }

    test("zero-arg-calls-spaces") {
      val result = p("${f:  }", expr(_))
      assert(
        result ==
         "Call(List(f),List())"
      )
    }

    test("should ignore the first escaped newline character") {
      val result = p("${f: \\\n foo}", expr(_))
      assert(
        result ==
         "Call(List(f),List(Body(List(Text( foo)))))"
      )
    }

    test("should support the $name syntax") {
      val result = p("$foo", body()(_))
      assert(
        result ==
        "Body(List(Variable(List(foo))))"
      )
    }

    test("should not extend the $name syntax to field access operator") {
      val result = p("$foo.bar", body()(_))
      assert(
        result ==
        "Body(List(Variable(List(foo)), Text(.bar)))"
      )
    }

    test("should omit whitespaces that follow the \\s control") {
      val result = p("${f: foo, ${bar}\\s  }", expr(_))
      assert(
        result ==
        "Call(List(f),List(Body(List(Text(foo))), Body(List(Variable(List(bar))))))"
      )
    }

    test("functions may have trailing whitespaces which are ignored")  {
      val result = p("${f: foo, ${f => x} }", expr(_))
      assert(
        result ==
        "Call(List(f),List(Body(List(Text(foo))), Lambda(List(f),Body(List(Text(x))))))"
      )
    }
  }
}
