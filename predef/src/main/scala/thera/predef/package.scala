package thera

import cats._, cats.implicits._, cats.data._
import thera.runtime._, Context.names

package object predef { self =>
  lazy val id = function[Runtime] { rt => State.pure(rt) }

  lazy val foreachSep = function[Data, Text, Function] { (data, sep, f) =>
    data.value.asArray.get.toList.map(Runtime.jsonToRuntime)
      .traverse(d => f(d :: Nil)).map { list =>
        def loop(ls: List[Runtime]): Runtime = ls match {
          case x :: y :: xs => x |+| sep |+| loop(y :: xs)
          case x :: Nil => x
          case Nil => Monoid[Runtime].empty
        }
        loop(list)
      }
  }

  lazy val foreach = function[Data, Function] { (data, f) =>
    foreachSep(data :: Text("") :: f :: Nil) }

  lazy val ifFunc = function[Text, Runtime, Runtime] { (varName, ifCond, elseCond) =>
    State.get[Context] >>= { ctx =>
      if (ctx.applyOpt(varName.value.split("\\.").toList).isDefined) State.pure(ifCond)
      else State.pure(elseCond) } }

  lazy val outdent = function[Text, Text] { (sizeStr, text) =>
    State.pure(Text(text.value.split("\n").toList.map(_.drop(sizeStr.value.toInt)).mkString("\n"))) }

  val predefContext = names(
    "id"         -> id
  , "foreachSep" -> foreachSep
  , "foreach"    -> foreach
  , "if"         -> ifFunc
  , "outdent"    -> outdent)
  
  object implicits {
    implicit val context = predefContext
  }
}
