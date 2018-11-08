package thera.population

import org.scalatest._

import parser._
import better.files._, better.files.File._, java.io.{ File => JFile }

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

  }
}

trait ParserSuiteHelpers {
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
I have numbers ), Variable(List(one)), Text(, ), Variable(List(two)), Text( and ), Variable(List(three, four)), Text(. If I add them, here is what I get: ), Variable(List(three-f)), Text(. I can also do ), Call(List(fun_frag),List(Text(simple), Text(nice))), Text( and ), Call(List(fun_frag),List(Text(complex ,
args), Text(awesome))), Text( calls. I hope to make $1,000,000 on this stuff I can also call ), Call(List(fun_frag),List(Call(List(fun_frag),List(Text($1,000,000), Text(good))), Text(recursive))), Text(. We can also escape with "\".
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

"html-template" -> """fail""".tail,

"three-frag" -> """
Module(None,Tree(List(Text(I've got three as a result!))))""".tail
  )
}