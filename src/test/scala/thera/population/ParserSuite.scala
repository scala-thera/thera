package thera.population

import org.scalatest._

import parser._
import better.files._, better.files.File._, java.io.{ File => JFile }
import fastparse._, Parsed.{ Success, Failure }

class ParserSuite extends FunSpec with Matchers with ParserSuiteHelpers {
  describe("Parser") {
    toParse.foreach { name =>
      val file = file"example/$name.html"

      it(s"should parse $file correctly") {
        fastparse.parse(file.contentAsString, module(_)) match {
          case Success(result, _) => result.toString shouldBe fileResult(name)
          case f: Failure => fail(f.toString)
        }
      }
    }

    it("should parse calls with trees as arguments") {
      p("${f: a ${b} c, ${d}}", expr(_)) shouldBe "Call(List(f),List(Leafs(List(Text(a ), Variable(List(b)), Text( c))), Variable(List(d))))"
    }
  }
}

trait ParserSuiteHelpers {
  def p[A](str: String, p: P[_] => P[A]): String =
    parse(str, p(_)).get.value.toString

  val toParse = List("index", "fun_frag", "html-template", "three-frag")

  val fileResult: Map[String, String] = Map(
"index" -> """
Function(List(),{
  "title" : "This stuff works!",
  "one" : "1",
  "two" : "2",
  "three" : {
    "four" : "4"
  }
},Leafs(List(Text(I have numbers ), Variable(List(one)), Text(, ), Variable(List(two)), Text( and ), Variable(List(three, four)), Text(. If I add them, here is what I get: ), Variable(List(three-frag)), Text(. I can also do ), Call(List(fun_frag),List(Text(simple), Text(nice))), Text( and ), Call(List(fun_frag),List(Text(complex ,
args), Text(awesome))), Text( calls. I hope to make $1,000,000 on this stuff I can also call ), Call(List(fun_frag),List(Call(List(fun_frag),List(Text($1,000,000), Text(good))), Text(recursive))), Text(. We can also escape with "\".
))))""".tail,

"fun_frag" -> """
Function(List(msg, msg2),{
  
},Leafs(List(Text(The very ), Variable(List(msg2)), Text( ), Variable(List(msg)), Text(
))))""".tail,

"html-template" -> """
Function(List(body),{
  
},Leafs(List(Text(<!DOCTYPE html>
<html>
<head>
  <title>), Variable(List(title)), Text(</title>
</head>
<body>
), Variable(List(body)), Text(
<div>
  ), Call(List(map),List(Function(List(dummy),{
  
},Text(buf)))), Text(

  <h1>Our users</h1>

  ), Call(List(map),List(Function(List(a, c),{
  
},Variable(List(a))))), Text(
  ), Call(List(map),List(Leafs(List(Text(b ), Variable(List(a)))))), Text(

  ), Call(List(map),List(Variable(List(our_users)), Function(List(u),{
  
},Leafs(List(Text(
    <p>
      Name : ), Variable(List(u, name)), Text(
      Email: ), Variable(List(u, email)), Text(
    </p>

    Warnings:
    <ul>
      ), Call(List(map),List(Variable(List(u, warnings)), Function(List(w),{
  
},Leafs(List(Text(
        <li>), Variable(List(w)), Text(</li>
      )))))), Text(

    </ul>

    Btw here's how a smiley mustache is drawn: }

    And here's some Scala code of that user:

    ```{.scala file="foo.scala"}
    ```
  )))))), Text(
</div>
</body>
</html>))))""".tail,

"three-frag" -> """
Function(List(),{
  
},Text(I've got three as a result!))""".tail
  )
}