package thera
package predef

import cats._, cats.implicits._, cats.data._
import org.scalatest._
import predef.implicits._


class PredefSuite extends FlatSpec with Matchers with PredefSuiteHelpers {
  "id" should "be an identity" in {
    thera.compile("""${id: foo}""").asString shouldBe "foo" 
  }

  "foreachSep" should "work" in {
    thera.compile("""
    |---
    |keywords: [scala, lambda, calculus]
    |---
    |${foreachSep: ${keywords}, \,  , ${id}}
    |""".fmt).asString shouldBe "scala,  lambda,  calculus"
  }

  it should "work in case of empty collection" in {
    thera.compile("""
    |---
    |keywords: []
    |---
    |${foreachSep: ${keywords}, \,  , ${id}}
    |""".fmt).asString shouldBe ""
  }

  it should "work in case of arrays of JSON objects" in {
    thera.compile("""
    |---
    |keywords:
    |  - {keyword: scala , score: "10"}
    |  - {keyword: lambda, score: "12"}
    |---
    |${foreachSep: ${keywords}, \, , ${kwrd => ${kwrd.keyword}(${kwrd.score})}}
    |""".fmt).asString shouldBe "scala(10), lambda(12)"
  }

  "foreach" should "work" in {
    thera.compile("""
    |---
    |keywords: [scala, lambda, calculus]
    |---
    |${foreach: ${keywords}, ${k => ${k},}}
    |""".fmt).asString shouldBe "scala,lambda,calculus,"
  }

  "if" should "substitute the 'if' branch if the predicate variable exists in the context" in {
    thera.compile("""
    |---
    |stuff: I exist
    |---
    |${if: stuff, it exists, it does not exist}
    |""".fmt).asString shouldBe "it exists"
  }

  it should "substitute the 'else' branch if the predicate variable does not exists in the context" in {
    thera.compile("""
    |---
    |stuff: I exist
    |---
    |${if: foo, it exists, it does not exist}
    |""".fmt).asString shouldBe "it does not exist"
  }

  it should "work for nested variables" in {
    thera.compile("""
    |---
    |foo:
    |  bar: I exist
    |---
    |${if: foo.bar, it exists, it does not exist}
    |""".fmt).asString shouldBe "it exists"
  }

  "outdent" should "outdent the argument text by the specified value" in {
    thera.compile("""
    |${outdent: 2, \
    |    It is an amazing weather today.}
    |""".fmt).asString shouldBe "  It is an amazing weather today."
  }

  it should "have a cumulative effect" in {
    thera.compile("""
    |${outdent: 2,
    |  ${outdent: 2, \
    |    It is an amazing weather today.}}
    |""".fmt).asString shouldBe "It is an amazing weather today."
  }
}

trait PredefSuiteHelpers {
  implicit class StringOps(str: String) {
    def fmt = str.tail.stripMargin.dropRight(1)
  }
}
