package thera

import Function._

object predef { self =>
  val id = function[Value] { identity }

  val foreachSep = function[Arr, Text, Function] { (data, sep, f) =>
    val texts: List[Text] = data.value.map(d => f(d :: Nil))
    @annotation.tailrec def loop(ls: List[Text]): Text = ls match {
      case x :: y :: xs => x + sep + loop(y :: xs)
      case x :: Nil => x
      case Nil => Text.empty
    }
    loop(texts)
  }

  val foreach = function[Arr, Function] { (data, f) =>
    foreachSep(data :: Text.empty :: f :: Nil)
  }

  val ifFunc = function[Text, Text, Text] { (cond, ifCond, elseCond) =>
    if (cond == "true") ifCond
    else elseCond
  }

  val outdent = function[Text, Text] { (sizeStr, text) =>
    val outdentSize = sizeStr.value.toInt
    val lines = text.value.split("\n").toList
    lines.map { _.drop(outdentSize)).mkString("\n") }
  }

  val context = ValueHierarchy.names(
    "id"         -> id
  , "foreachSep" -> foreachSep
  , "foreach"    -> foreach
  , "if"         -> ifFunc
  , "outdent"    -> outdent)
}
