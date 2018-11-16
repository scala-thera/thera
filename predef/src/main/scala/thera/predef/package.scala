package thera

import cats._, cats.implicits._, cats.data._
import thera.runtime._, Context.names

package object predef {
  lazy val foreachSep = function[Data, Text, Function] { (data, sep, f) =>
    data.value.asArray.get.toList.map(Runtime.jsonToRuntime)
      .traverse(d => f(d :: Nil)).map {
        case x :: xs => (x :: xs.flatMap(sep :: _ :: Nil)).combineAll
        case Nil     => Monoid[Runtime].empty
      }
  } 

  implicit val ctx = names(
    "id" -> function[Runtime] { rt => State.pure(rt) }

  , "foreach" -> function[Data, Function] { (data, f) =>
      foreachSep(data :: Text("") :: f :: Nil) }

  , "foreachSep" -> foreachSep

  , "if" -> function[Text, Runtime, Runtime] { (varName, ifCond, elseCond) =>
      State.get[Context] >>= { ctx =>
        if (ctx.applyOpt(varName.value.split("\\.").toList).isDefined) State.pure(ifCond)
        else State.pure(elseCond) } }

  , "outdent" -> function[Text, Text] { (sizeStr, text) =>
      State.pure(Text(text.value.split("\n").toList.map(_.drop(sizeStr.value.toInt)).mkString("\n"))) }
  )
}
