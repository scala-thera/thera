package thera

import utest._

import parser._
import fastparse._, Parsed.{ Success, Failure }

class ParserSuite extends TestSuite with ParserSuiteHelpers {
  val tests = Tests {
    test("Parser") {

      test("Named tests") - toParse.foreach { name =>
        val source = scala.io.Source.fromURL(classOf[ParserSuite].getResource(s"/example/$name.html")).mkString

        fastparse.parse(source, module(_)) match {
          case Success(result, _) => assert(result.toString == fileResult(name))
          case f: Failure => throw new RuntimeException(f.toString)
        }
      }

      test("should parse calls with trees as arguments") {
        assert(
          p("${f: a ${b} c, ${d}}", expr(_)) ==
           "Call(List(f),List(Leafs(List(Text(a ), Variable(List(b)), Text( c))), Variable(List(d))))"
        )
      }

      test("should parse calls with zero arguments") {
        assert(
          p("${f:}"  , expr(_)) ==
           "Call(List(f),List())"
        )
      }

      test("should parse calls with zero arguments even if the argument list contains spaces") {
        assert(
          p("${f:  }", expr(_)) ==
           "Call(List(f),List())"
        )
      }

      test("should ignore the first escaped newline character") {
        assert(
          p("${f: \\\n foo}", expr(_)) ==
           "Call(List(f),List(Text( foo)))"
        )
      }

      test("should support the $name syntax") {
        assert(
          p("$foo", node()(_)) ==
          ("Variable(List(foo))")
        )
      }

      test("should not extend the $name syntax to field access operator") {
        assert(
          p("$foo.bar", node()(_)) ==
          ("Leafs(List(Variable(List(foo)), Text(.bar)))")
        )
      }

      test("should omit whitespaces that follow the \\s control") {
        assert(
          p("${f: foo, ${bar}\\s  }", expr(_)) ==
          ("Call(List(f),List(Text(foo), Variable(List(bar))))")
        )
      }

      test("functions may have trailing whitespaces which are ignored")  {
        assert(
          p("${f: foo, ${f => x} }", expr(_)) ==
          ("Call(List(f),List(Text(foo), Function(List(f),{\n  \n},Text(x))))")
        )
      }
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

},Leafs(List(Text(        <li>), Variable(List(w)), Text(</li>
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