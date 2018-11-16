package thera
package runtime

import cats._, cats.implicits._, cats.data._, cats.effect._
import org.scalatest._

import Context.names


class RuntimeSuite extends FlatSpec with Matchers with RuntiemSuiteHelpers {
  "Runtime capabilities should include" should "identity" in {
    thera.compile("Hello World").asString shouldBe "Hello World"
  }

  it should "variables" in {
    thera.compile("""
    |---
    |name: Moon
    |---
    |Hello ${name}
    |""".fmt).asString shouldBe "Hello Moon"
  }

  it should "functions" in {
    implicit val ctx = names(
      "isay" -> Function { case Text(what) :: Nil => State.pure(Text(s"I Say: $what")) }
    )

    thera.compile("""
    |---
    |name: Moon
    |---
    |${isay: Hello ${name}}
    |""".fmt).asString shouldBe "I Say: Hello Moon"
  }

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
}

trait RuntiemSuiteHelpers {
  // def processCtx(ctx: Context)(str: String): String =
  //   (State.set(ctx) >> toRT(parse(str)) >>= (_.evalFuncEmpty)).runEmptyA.value.asString

  // def process(str: String): String =
  //   processCtx(Monoid[Context].empty)(str)

  // def parse(str: String): ast.Node = {
  //   import parser.module
  //   import fastparse.Parsed.{ Success, Failure }
    
  //   fastparse.parse(str, module(_)) match {
  //     case Success(result, _) => result
  //     case f: Failure => throw new RuntimeException(f.toString)
  //   }
  // }


  implicit class StringOps(str: String) {
    def fmt = str.tail.stripMargin.dropRight(1)
  }
}