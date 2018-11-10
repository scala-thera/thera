package thera.population

import ast._
import io.circe._
import fastparse.parse
import better.files._, better.files.File._, java.io.{ File => JFile }

object funOps {

  trait FunctionJvm {
    def apply(args: List[Node]): Node
    def apply(args: Node*): Node = apply(args.toList)
  }

  implicit class NodeOps(f: Node) {
    def step: Either[Node, String] = f match {
      case Text(str)        => Right(str)
      case Call(path, args) => resolveFun(path)(args)
      case Variable(path)   => Text(resolve(path))
      
      case Leafs(Nil    ) => ""
      case Leafs(n :: nx) => compute(n) + compute(Leafs(nx))
      
      case Function(args, vars, body) =>
    }

    def resolve(path: List[String]): String =
      path.foldLeft(f.vars.hcursor: ACursor)
        { (hc, pathElement) => hc.downField(pathElement) }.get[String]
  }

  def resolveFun(path: List[String]): FunctionJvm = ??? // {
  //   val file = File("example/" + path.mkString("/"))
  //   parse(file.contentAsString, module(_))
  // }
}
