package thera

import utest._

class EvaluateSuite extends TestSuite {
  val tests = Tests {
    def check(name: String, ctx: ValueHierarchy = ValueHierarchy.empty): Unit = {
      val (input, output) = readIO("/parser/input/$name")
      assert(evaluate(input) == output)
    }
    test("identity") - check("identity")
    test("variables") - check("variables")
    test("functions") - check("functions", names(
      "isay" ->
          function[Str] { case Str(what) => Str(s"I Say: $what") }
    ))

    it should "context of the caller accessible to the callee" in {
      implicit val ctx = names(
        "header" -> thera.compile("""
          |My name is ${name}.
          |""".fmt).value
      )

      thera.compile("""
      |---
      |name: Jupiter
      |---
      |${header:}
      |Hello World
      |""".fmt).asString shouldBe """
      |My name is Jupiter.
      |Hello World
      |""".fmt
    }

    it should "have the callee context accessible from the callee" in {
      implicit val ctx = names(
        "header" -> thera.compile("""
        |---
        |day: Sunday
        |---
        |My name is ${name}. Today is ${day}.
        |""".fmt).value
      )

      thera.compile("""
      |---
      |name: Jupiter
      |---
      |${header:}
      |Hello World
      |""".fmt).asString shouldBe """
      |My name is Jupiter. Today is Sunday.
      |Hello World
      |""".fmt
    }

    it should "be possible to call a fragment with arguments" in {
      implicit val ctx = names(
        "header" -> thera.compile("""
        |---
        |[surname]
        |day: Sunday
        |---
        |My name is ${name} ${surname}. Today is ${day}.
        |""".fmt).value
      )

      thera.compile("""
      |---
      |name: Jupiter
      |---
      |${header: Mars}
      |Hello World
      |""".fmt).asString shouldBe """
      |My name is Jupiter Mars. Today is Sunday.
      |Hello World
      |""".fmt
    }

    it should "produce an exception when trying to call a function with wrong number of arguments" in {
      implicit val ctx = names(
        "header" -> thera.compile("""
        |---
        |[surname]
        |day: Sunday
        |---
        |My name is ${name} ${surname}.
        |""".fmt).value
      )

      the [RuntimeException] thrownBy {
        thera.compile("""
        |---
        |name: Jupiter
        |---
        |${header:}
        |Hello World
        |""".fmt).asString
      } should have message "Symbol not found: surname"
    }

    it should "templating capabilities" in {
      val tml = thera.compile("""
      |---
      |[body]
      |---
      |<h1>${body}</h1>
      |""".fmt)

      (thera.compile("""
      |Hello World
      |""".fmt) >>= tml).asString shouldBe "<h1>Hello World</h1>"
    }

    it should "variables can evaluate to functions" in {
      implicit val ctx = names(
        "header" -> thera.compile("""
        |---
        |[surname]
        |day: Sunday
        |---
        |My name is ${name} ${surname}. Today is ${day}.
        |""".fmt).value

      , "content" -> thera.compile("""
        |---
        |[f]
        |name: Jupiter
        |---
        |${f: Mars}
        |Hello World
        |""".fmt).value
      )

      thera.compile("""
      |${content: ${header}}
      |""".fmt).asString shouldBe """
      |My name is Jupiter Mars. Today is Sunday.
      |Hello World
      |""".fmt
    }

    it should "possibility of variables evaluated to JSON" in {
      implicit val ctx = names(
        "specs" -> thera.compile("""
        |---
        |[meta]
        |---
        |Radius: ${meta.radius}
        |Atmosphere: ${meta.atmosphere}
        |""".fmt).value
      )

      thera.compile("""
      |---
      |name: Moon
      |meta:
      |  radius: small
      |  atmosphere: nonexistent
      |---
      |This is ${name}. Its specs:
      |${specs: ${meta}}
      |""".fmt).asString shouldBe """
      |This is Moon. Its specs:
      |Radius: small
      |Atmosphere: nonexistent
      |""".fmt
    }

    it should "support passing lambdas into functions" in {
      implicit val ctx = names(
        "wrap" -> thera.compile("""
        |---
        |[wrappee, wrapper]
        |---
        |Wrapped value: ${wrapper: ${wrappee}}
        |""".fmt).value
      )

      thera.compile("""
      |${wrap: Hello World, ${x => <h1>${x}</h1>}}
      |""".fmt).asString shouldBe "Wrapped value: <h1>Hello World</h1>"
    }

    it should "support filtering" in {
      thera.compile("Hello World").mapStr { s => s"<h1>$s</h1>" }
        .asString shouldBe "<h1>Hello World</h1>"
    }

    "Escape characters" should "include \\n" in {
      thera.compile("Hello\\nWorld").asString shouldBe "Hello\nWorld"
    }
  }
}
