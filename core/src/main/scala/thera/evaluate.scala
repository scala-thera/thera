package thera

import thera.parser.module
import fastparse.Parsed.{ Success, Failure }

/**
An evaluation of a Template is a process of resolving all its
variables and calling all the functions in it. If a template
does not have any arguments, it is evaluated to Str. If it does,
it is evaluated to a Function. It is then possible to pass the
arguments to that Function and thus finish the evaluation of the
Template.

A Template is evaluated against a context which is a ValueHierarchy.
The context contains the variables and functions that the Template
refers to. The rules for evaluating a template against a context `ctxInit`
are as follows:

- The variables predefined in the template form a context `templateContext`.
  The variable references in the template are resolved against a
  final context `ctx` = `ctxInit + templateContext`. @see `ValueHierarchy.+`.
- The template body consists of a combination of plain text, variable references
  and function calls.
- Variable references
  - Are resolved against `ctx` to Str.
  - If they are arguments to  a function call,
    they may resolve to any other Value.
  - An unresolved variable reference is an error.
  - If the unresolved variable is an argument to the Template,
    the Variable stays unresolved without an error.
- A function call is resolved as follows:
  - The name is resolved to a Function against `ctx`
  - The arguments, which can be either Str, a Call or a Variable,
    are resolved as specified in these rules, recursively.
  - The function is called with the resolved arguments and the
    resulting Str is substituted in place of the function call.
- If the template does not have any arguments, its body must consist
  only of Str nodes after resolution is done. These nodes are concatenated
  to form a single result Str node.
- If the template has arguments, the result of the template
  evaluation is a Function that takes the corresponding arguments and computes
  the resulting Str.
*/
object evaluate {
  def apply(tmlSource: String, ctxInit: ValueHierarchy =
      ValueHierarchy.empty): Either[List[Value] => String, String] =
    fastparse.parse(tmlSource, module(_)) match {
      case Success(result, _) => evaluate(result, ctxInit)
      case f: Failure => throw new RuntimeException(f.toString)
    }

  def apply(tml: Template, ctxInit: ValueHierarchy =
      ValueHierarchy.empty): Either[List[Value] => String, String] = {
    var ctx = ctxInit + tml.templateContext
    if (tml.argNames.nonEmpty) Left( { argValues =>
      ctx = ctx + ValueHierarchy.names(tml.argNames.zip(argValues).toMap)
      evaluateBody(tml.body)(ctx)
    })
    else Right(evaluateBody(tml.body)(ctx))
  }

  private def evaluateBody(body: List[Node])(implicit ctx: ValueHierarchy): String = {
    val evaluatedValues: List[Value] = body.map(evaluateNode(_, inFunctionCall = false))
    evaluatedValues.foldLeft("") {
      case (accum, Str(str)) => accum + str
      case (_, x) => throw new RuntimeException(
        s"Error evaluating template: non-text value $x encountered. " +
        s"Full evaluation:\n$evaluatedValues")
    }
  }

  private def evaluateNode(node: Node, inFunctionCall: Boolean)(
      implicit ctx: ValueHierarchy): Value = node match {
    case x: Str => x
    case Variable(path) => ctx(path) match {
      case x: Str => x
      case x if !inFunctionCall => throw new RuntimeException(
        s"Variables outside function calls can only resolve to text. " +
        s"Variable ${path.mkString(".")} was resolved to $x")
    }
    case Call(path, argsNodes) =>
      val f: Function = ctx(path) match {
        case x: Function => x
        case x => throw new RuntimeException(
          s"Expected function at path ${path.mkString(".")}, got: $x")
      }
      val args: List[Value] = argsNodes.map(evaluateNode(_, inFunctionCall = true))
      f(args)
    case Lambda(argNames, body) =>
      if (!inFunctionCall) throw new RuntimeException(
        s"Lambda nodes are only permitted as arguments to functions")
      Function { argValues =>
        val ctx2 = ctx + ValueHierarchy.names(argNames.zip(argValues).toMap)
        Str(evaluateBody(body)(ctx2))
      }
  }
}