package thera

import Function._

object predef { self =>
  val id = function[Str] { identity }

  val foreachSep = function[Arr, Str, Function] { (data, sep, f) =>
    val texts: List[Str] = data.value.map(d => f(d :: Nil))
    def loop(ls: List[Str]): Str = ls match {
      case x :: y :: xs => x + sep + loop(y :: xs)
      case x :: Nil => x
      case Nil => Str.empty
    }
    loop(texts)
  }

  val foreach = function[Arr, Function] { (data, f) =>
    foreachSep(data :: Str.empty :: f :: Nil)
  }

  val ifFunc = function[Str, Str, Str] { (cond, ifCond, elseCond) =>
    if (cond.value == "true") ifCond
    else elseCond
  }

  val outdent = function[Str, Str] { (sizeStr, text) =>
    val outdentSize: Int = sizeStr.value.toInt
    val lines: List[String] = text.value.split("\n").toList
    Str(lines.map { _.drop(outdentSize) }.mkString("\n"))
  }

  val context = ValueHierarchy.names(
    "id"         -> id
  , "foreachSep" -> foreachSep
  , "foreach"    -> foreach
  , "if"         -> ifFunc
  , "outdent"    -> outdent)
}
