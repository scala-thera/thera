package thera.population

import org.scalatest._

import parser._
import better.files._, better.files.File._, java.io.{ File => JFile }
import fastparse._

class ParserSuite extends FunSpec with Matchers with ParserSuiteHelpers {
  describe("Parser") {
    toParse.foreach { name =>
      val file = file"example/$name.html"

      it(s"should parse $file correctly") {
        fastparse.parse(file.contentAsString, module(_)).fold(
          (str, pos, extra) => fail(s"Failure: $str, $pos, $extra")
        , (result, pos) => result.toString shouldBe fileResult(name)
        )
      }
    }

    it("should parse calls with trees as arguments") {
      p("${f: a ${b} c, ${d}}", expr(_)) shouldBe "Call(List(f),List(Tree(List(Text(a ), Variable(List(b)), Text( c))), Tree(List(Variable(List(d))))))"
    }
  }
}

trait ParserSuiteHelpers {
  def p[A](str: String, p: P[_] => P[A]): String =
    parse(str, p(_)).get.value.toString

  val toParse = List("index", "fun_frag", "html-template", "three-frag")

  val fileResult: Map[String, String] = Map(
"index" -> """
Module(Some({
  "template" : "html-template",
  "filters" : [
    "currentTimeFilter"
  ],
  "variables" : {
    "title" : "This stuff works!",
    "one" : "1",
    "two" : "2",
    "three" : {
      "four" : "4"
    }
  },
  "fragments" : {
    "three-f" : "three-frag",
    "fun_frag" : "fun_frag"
  }
}),Tree(List(Text(
I have numbers ), Variable(List(one)), Text(, ), Variable(List(two)), Text( and ), Variable(List(three, four)), Text(. If I add them, here is what I get: ), Variable(List(three-f)), Text(. I can also do ), Call(List(fun_frag),List(Tree(List(Text(simple))), Tree(List(Text(nice))))), Text( and ), Call(List(fun_frag),List(Tree(List(Text(complex ,
args))), Tree(List(Text(awesome))))), Text( calls. I hope to make $1,000,000 on this stuff I can also call ), Call(List(fun_frag),List(Tree(List(Call(List(fun_frag),List(Tree(List(Text($1,000,000))), Tree(List(Text(good))))))), Tree(List(Text(recursive))))), Text(. We can also escape with "\".
))))""".tail,

"fun_frag" -> """
Module(Some({
  "parameters" : [
    "msg",
    "msg2"
  ]
}),Tree(List(Text(
The very ), Variable(List(msg2)), Text( ), Variable(List(msg)), Text(
))))""".tail,

"html-template" -> """
Module(None,Tree(List(Text(<!DOCTYPE html>
<html>
<head>
  <title>), Variable(List(title)), Text(</title>
</head>
<body>
), Variable(List(body)), Text(
<div>
  ), Function(List(dummy),Tree(List(Text(buf)))), Text(

  <h1>Our users</h1>

  ), Function(List(a),Tree(List(Text(b)))), Text(
  ), Call(List(map),List(Tree(List(Text(b ), Variable(List(a)))))), Text(

  ), Call(List(map),List(Tree(List(Variable(List(our_users)))), Tree(List(Function(List(u),Tree(List(Text(
    <p>
      Name : ), Variable(List(u, name)), Text(
      Email: ), Variable(List(u, email)), Text(
    </p>

    Warnings:
    <ul>
      ), Call(List(map),List(Tree(List(Variable(List(u, warnings)))), Tree(List(Function(List(w),Tree(List(Text(
       <li>), Variable(List(w)), Text(</li>
      )))))))), Text(

    </ul>

    Btw here's how a smiley mustache is drawn: }

    And here's some Scala code of that user:

    ```{.scala file="foo.scala"}
    ```
  )))))))), Text(
</div>
</body>
</html>))))""".tail,

"three-frag" -> """
Module(None,Tree(List(Text(I've got three as a result!))))""".tail
  )
}