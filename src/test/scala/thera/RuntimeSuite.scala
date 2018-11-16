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
      "isay" -> Function { case Data(what) :: Nil => State.pure(Data(s"I Say: $what")) }
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
      |My name is ${name}
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
}

trait RuntiemSuiteHelpers {
  def processCtx(ctx: Context)(str: String): String =
    (State.set(ctx) >> toRT(parse(str))).runEmptyA.value.asString

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