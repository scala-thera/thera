package thera

import utest._
import utils._
import ValueHierarchy.names

object PredefSuite extends TestSuite {
  val tests = Tests {
    test("id") - assertValue(
      Thera("""${id: foo}""").mkString , "foo"
    )

    test("foreachSep") {
      test("basic") - assertValue(
        Thera("""
        |---
        |keywords: [scala, lambda, calculus]
        |---
        |${foreachSep: ${keywords}, \,  , ${id}}
        |""".fmt).mkString, "scala,  lambda,  calculus"
      )

      test("empty collection") - assertValue(
        Thera(
          """
          |---
          |keywords: []
          |---
          |${foreachSep: ${keywords}, \,  , ${id}}
          |""".fmt
        ).mkString, ""
      )

      test("arrays of JSON objects") - assertValue(
        Thera("""
        |---
        |keywords:
        |  - {keyword: scala , score: "10"}
        |  - {keyword: lambda, score: "12"}
        |---
        |${foreachSep: ${keywords}, \, , ${kwrd => ${kwrd.keyword}(${kwrd.score})}}
        |""".fmt).mkString , "scala(10), lambda(12)"
      )
    }

    test("foreach") - assertValue(
      Thera("""
      |---
      |keywords: [scala, lambda, calculus]
      |---
      |${foreach: ${keywords}, ${k => ${k},}}
      |""".fmt).mkString , "scala,lambda,calculus,"
    )

    test("if") {
      test("if should substitute the 'if' branch if the predicate variable equals to 'true'") - assertValue(
        Thera("""
        |---
        |stuff: true
        |---
        |${if: $stuff, it exists, it does not exist}
        |""".fmt).mkString , "it exists"
      )

      test("substitute the 'else' branch if the predicate variable is 'false'") - assertValue(
        Thera("""
        |---
        |stuff: false
        |---
        |${if: $stuff, it exists, it does not exist}
        |""".fmt).mkString , "it does not exist"
      )
    }

    test("outdent") {
      test("outdent the argument text by the specified value") - assertValue(
        Thera("""
        |${outdent: 2, \
        |    It is an amazing weather today.}
        |""".fmt).mkString , "  It is an amazing weather today."
      )

      test("have a cumulative effect") - assertValue(
        Thera("""
        |${outdent: 2,
        |  ${outdent: 2, \
        |    It is an amazing weather today.}}
        |""".fmt).mkString , "It is an amazing weather today."
      )
    }
  }
}
