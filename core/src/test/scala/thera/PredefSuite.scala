package thera

import utest._
import utils._

class PredefSuite extends TestSuite {
  test("id") - assert(
    Thera("""${id: foo}""").mkString == "foo"
  )

  test("foreachSep") {
    test("basic") - assert(
      Thera("""
      |---
      |keywords: [scala, lambda, calculus]
      |---
      |${foreachSep: ${keywords}, \,  , ${id}}
      |""".fmt).mkString == "scala,  lambda,  calculus"
    )

    test("empty collection") – assert(
      Thera("""
      |---
      |keywords: []
      |---
      |${foreachSep: ${keywords}, \,  , ${id}}
      |""".fmt).mkString == ""
    )

    test("arrays of JSON objects") – assert(
      Thera("""
      |---
      |keywords:
      |  - {keyword: scala , score: "10"}
      |  - {keyword: lambda, score: "12"}
      |---
      |${foreachSep: ${keywords}, \, , ${kwrd => ${kwrd.keyword}(${kwrd.score})}}
      |""".fmt).mkString == "scala(10), lambda(12)"
    )
  }

  test("foreach") - assert(
    Thera("""
    |---
    |keywords: [scala, lambda, calculus]
    |---
    |${foreach: ${keywords}, ${k => ${k},}}
    |""".fmt).mkString == "scala,lambda,calculus,"
  )

  test("if") {
    test("if should substitute the 'if' branch if the predicate variable equals to 'true'") - assert(
      Thera("""
      |---
      |stuff: true
      |---
      |${if: $stuff, it exists, it does not exist}
      |""".fmt).mkString == "it exists"
    )

    test("substitute the 'else' branch if the predicate variable is 'false'") - assert(
      Thera("""
      |---
      |stuff: false
      |---
      |${if: $stuff, it exists, it does not exist}
      |""".fmt).mkString == "it does not exist"
    )
  }

  test("outdent") {
    test("outdent the argument text by the specified value") - assert(
      Thera("""
      |${outdent: 2, \
      |    It is an amazing weather today.}
      |""".fmt).mkString == "  It is an amazing weather today."
    )

    test("have a cumulative effect" - assert(
      Thera("""
      |${outdent: 2,
      |  ${outdent: 2, \
      |    It is an amazing weather today.}}
      |""".fmt).mkString == "It is an amazing weather today."
    )
  }
}
