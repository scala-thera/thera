package thera
package runtime

import cats._, cats.implicits._, cats.data._, cats.effect._
import org.scalatest._


class RuntimeSuite extends FlatSpec with Matchers with RuntiemSuiteHelpers {
  "Runtime capabilities should include" should "identity" in {
    process("Hello World") shouldBe "Hello World"
  }

  it should "variables" in {
    process("""
    |---
    |name: Moon
    |---
    |Hello ${name}
    |""".fmt) shouldBe "Hello Moon"
  }

  it should "functions" in {
    val ctx = Ctx.names(
      "isay" -> Function { case Text(what) :: Nil => State.pure(Text(s"I Say: $what")) }
    )

    processCtx(ctx)("""
    |---
    |name: Moon
    |---
    |${isay: Hello ${name}}
    |""".fmt) shouldBe "I Say: Hello Moon"
  }

  it should "context of the caller accessible to the callee" in {
    val ctx = Ctx.names(
      "header" -> toRT(parse("""
      |My name is ${name}.
      |""".fmt)).runEmptyA.value
    )

    processCtx(ctx)("""
    |---
    |name: Jupiter
    |---
    |${header:}
    |Hello World
    |""".fmt) shouldBe """
    |My name is Jupiter.
    |Hello World
    |""".fmt
  }

  it should "have the callee context accessible from the callee" in {
    val ctx = Ctx.names(
      "header" -> toRT(parse("""
      |---
      |day: Sunday
      |---
      |My name is ${name}. Today is ${day}.
      |""".fmt)).runEmptyA.value
    )

    processCtx(ctx)("""
    |---
    |name: Jupiter
    |---
    |${header:}
    |Hello World
    |""".fmt) shouldBe """
    |My name is Jupiter. Today is Sunday.
    |Hello World
    |""".fmt
  }

  it should "be possible to call a fragment with arguments" in {
    val ctx = Ctx.names(
      "header" -> toRT(parse("""
      |---
      |[surname]
      |day: Sunday
      |---
      |My name is ${name} ${surname}. Today is ${day}.
      |""".fmt)).runEmptyA.value
    )

    processCtx(ctx)("""
    |---
    |name: Jupiter
    |---
    |${header: Mars}
    |Hello World
    |""".fmt) shouldBe """
    |My name is Jupiter Mars. Today is Sunday.
    |Hello World
    |""".fmt
  }

  it should "produce an exception when trying to call a function with wrong number of arguments" in {
    val ctx = Ctx.names(
      "header" -> toRT(parse("""
      |---
      |[surname]
      |day: Sunday
      |---
      |My name is ${name} ${surname}.
      |""".fmt)).runEmptyA.value
    )

    the [RuntimeException] thrownBy {
      processCtx(ctx)("""
      |---
      |name: Jupiter
      |---
      |${header:}
      |Hello World
      |""".fmt)
    } should have message "Symbol not found: surname"
  }

  it should "templating capabilities" in {
    val tml = toRT(parse("""
    |---
    |[body]
    |---
    |<h1>${body}</h1>
    |""".fmt)).runEmptyA.value.asTemplate

    (toRT(parse("""
    |Hello World
    |""".fmt)) >>= (_.evalFuncEmpty) >>= tml).runEmptyA.value.asString shouldBe "<h1>Hello World</h1>"
  }

  it should "variables can evaluate to functions" in {
    val ctx = Ctx.names(
      "header" -> toRT(parse("""
      |---
      |[surname]
      |day: Sunday
      |---
      |My name is ${name} ${surname}. Today is ${day}.
      |""".fmt)).runEmptyA.value

    , "content" -> toRT(parse("""
      |---
      |[f]
      |name: Jupiter
      |---
      |${f: Mars}
      |Hello World
      |""".fmt)).runEmptyA.value
    )

    processCtx(ctx)("""
    |${content: ${header}}
    |""".fmt) shouldBe """
    |My name is Jupiter Mars. Today is Sunday.
    |Hello World
    |""".fmt
  }

  it should "possibility of variables evaluated to JSON" in {
    processCtx(Ctx.names(
      "specs" -> toRT(parse("""
      |---
      |[meta]
      |---
      |Radius: ${meta.radius}
      |Atmosphere: ${meta.atmosphere}
      |""".fmt)).runEmptyA.value
    ))("""
    |---
    |name: Moon
    |meta:
    |  radius: small
    |  atmosphere: nonexistent
    |---
    |This is ${name}. Its specs:
    |${specs: ${meta}}
    |""".fmt) shouldBe """
    |This is Moon. Its specs:
    |Radius: small
    |Atmosphere: nonexistent
    |""".fmt
  }
}

trait RuntiemSuiteHelpers {
  def processCtx(ctx: Context)(str: String): String =
    (State.set(ctx) >> toRT(parse(str)) >>= (_.evalFuncEmpty)).runEmptyA.value.asString

  def process(str: String): String =
    processCtx(Monoid[Context].empty)(str)

  def parse(str: String): ast.Node = {
    import parser.module
    import fastparse.Parsed.{ Success, Failure }
    
    fastparse.parse(str, module(_)) match {
      case Success(result, _) => result
      case f: Failure => throw new RuntimeException(f.toString)
    }
  }


  implicit class StringOps(str: String) {
    def fmt = str.tail.stripMargin.dropRight(1)
  }
}