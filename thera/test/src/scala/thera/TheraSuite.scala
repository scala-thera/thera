package thera

import ValueHierarchy._, Function._

import utest._
import utils._

object TheraSuite extends TestSuite {
  val tests = Tests {
    def read(name: String) = readResource(s"/evaluate/$name")

    test("File-defined") {
      def check(name: String, ctx: ValueHierarchy = ValueHierarchy.empty): Unit = {
        implicit val c = ctx
        val (input, expected) = readIO(s"/evaluate/$name")
        val result = Thera(input).mkString(c)
        if (result != expected) {
          println(s"Result:\n$result\n===\nExpected:\n$expected")
          assert(false)
        }
      }

      test("identity") - check("identity")
      test("variables") - check("variables")
      test("functions") - check("functions", names(
        "isay" ->
            function[Str] { case Str(what) => Str(s"I Say: $what") }
      ))

      test("templates") - check("templates/template", names(
        "header" -> Thera(read("/templates/weekday")).mkValue
      ))

      test("caller-context") {
        val input = read("/caller-context/source")
        val expected = read("/caller-context/source.check")
        val functionSrc = read("/caller-context/function")

        val source = Thera(input)
        val function = Thera(functionSrc).mkValue(source.context)
        val result = source.mkString(names("weekday" -> function))
        assert(result == expected)
      }
    }

    test("wrong-number-of-params") {
      val f = function[Value] { _ => Str("") }
      val e = intercept[RuntimeException] {
        Thera("${f: a, b}").mkString(names("f" -> f))
      }
      assert(e.getMessage == "Argument list List(Str(a), Str(b)) is inapplicable to 1-ary function")
    }

    test("templating capabilities") {
      val bodySrc = read("/templating-capabilities/body")
      val bodyExpected = read("/templating-capabilities/body.check")
      val templateSrc = read("/templating-capabilities/template")

      val body = Thera(bodySrc)
      val func = Thera(templateSrc).mkFunction(body.context)
      val result = func(body.mkValue :: Nil)

      assert(result == bodyExpected)
    }

    test("Variables can evaluate to functions") {
      val ctx = names(
        "header" -> Thera("""
        |---
        |[name, surname]
        |day: Sunday
        |---
        |My name is ${name} ${surname}. Today is ${day}.
        |""".fmt).mkValue

      , "content" -> Thera("""
        |---
        |[f]
        |name: Jupiter
        |---
        |${f: $name, Mars}
        |Hello World
        |""".fmt).mkValue
      )

      assertValue(Thera("""
      |${content: ${header}}
      |""".fmt).mkString(ctx), """
      |My name is Jupiter Mars. Today is Sunday.
      |Hello World
      |""".fmt)
    }

    test("possibility of variables evaluated to JSON") {
      implicit val ctx = names(
        "specs" -> Thera("""
        |---
        |[meta]
        |---
        |Radius: ${meta.radius}
        |Atmosphere: ${meta.atmosphere}
        |""".fmt).mkValue
      )

      assert(Thera("""
      |---
      |name: Moon
      |meta:
      |  radius: small
      |  atmosphere: nonexistent
      |---
      |This is ${name}. Its specs:
      |${specs: ${meta}}
      |""".fmt).mkString == """
      |This is Moon. Its specs:
      |Radius: small
      |Atmosphere: nonexistent
      |""".fmt)
    }

    test("support passing lambdas into functions") {
      implicit val ctx = names(
        "wrap" -> Thera("""
        |---
        |[wrappee, wrapper]
        |---
        |Wrapped value: ${wrapper: ${wrappee}}
        |""".fmt).mkValue
      )

      assert(Thera("""
      |${wrap: Hello World, ${x => <h1>${x}</h1>}}
      |""".fmt).mkString == "Wrapped value: <h1>Hello World</h1>")
    }

    test("\\n escape character") - assert(
      Thera("Hello\\nWorld").mkString == "Hello\nWorld")

    test("Thera.split") {
      val output = Thera.split("""
      |---
      |name: Moon
      |meta:
      |  radius: small
      |  atmosphere: nonexistent
      |---
      |This is ${name}. Its specs:
      |${specs: ${meta}}
      |""".fmt)
      val expected = ("""
      |name: Moon
      |meta:
      |  radius: small
      |  atmosphere: nonexistent
      |""".fmt, """
      |This is ${name}. Its specs:
      |${specs: ${meta}}
      |""".fmt)
      assert(output == expected)
    }

    test("Thera.join") {
      assert(Thera.join("""
      |name: Moon
      |meta:
      |  radius: small
      |  atmosphere: nonexistent
      |""".fmt, """
      |This is ${name}. Its specs:
      |${specs: ${meta}}
      |""".fmt) == """
      |---
      |name: Moon
      |meta:
      |  radius: small
      |  atmosphere: nonexistent
      |---
      |This is ${name}. Its specs:
      |${specs: ${meta}}
      |""".fmt
      )
    }

    test("Thera.quote") {
      val result = Thera.quote("""
      |```scala
      |object Test {
      |  extension StrDeco on (tree: String) {
      |    inline def show(given DummyImplicit): String = ${spliced}
      |    def show(color: Boolean)(given DummyImplicit): String = ???
      |  }
      |
      |  val c: Any = "foo".show
      |}
      |""".fmt)
      val expected = """
      |```scala
      |object Test \{
      |  extension StrDeco on (tree: String) \{
      |    inline def show(given DummyImplicit): String = \$\{spliced\}
      |    def show(color: Boolean)(given DummyImplicit): String = ???
      |  \}
      |
      |  val c: Any = "foo".show
      |\}
      |""".fmt
      assert(result == expected)
    }
  }
}
