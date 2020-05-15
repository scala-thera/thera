package thera

import utest._

import parser._
import fastparse._, Parsed.{ Success, Failure }

class ParserSuite extends TestSuite with ParserSuiteHelpers {
  val tests = Tests {
    test("Parser") {

      test("Named tests") - toParse.foreach { case (name, input, output) =>
        fastparse.parse(input, module(_)) match {
          case Success(result, _) =>
            try assert(result.toString == fileResult(name))
            catch {
              case t: AssertError =>
                sys.err.println(s"Named test failure: $name")
                throw t
            }
          case f: Failure => throw new RuntimeException(s"Named test failure: $name\n${f.toString}")
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

  def toParse: List[(String, String, String)] = {
    val classLoader = classOf[ParserSuite].getClassLoader

    def listFilesInDir(dir: String): List[File] = {
      val url = loader.getResource(dir)
      val path = url.getPath
      new File(path).listFiles.asScala
    }

    val cases: List[File] = listFilesInDir("/parser/input")

    cases.map { inputFile =>
      val name = f.getName
      val outputFile = new File(inputFile.parentDirectory, "/output/$name")
      val input = Source.fromFile(inputFile).mkString
      val output = Source.fromFile(outputFile).mkString
      (name, input, output)
    }
  }
}