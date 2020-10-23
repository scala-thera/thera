package thera

import io.circe._
import org.yaml.snakeyaml.error.MarkedYAMLException
import thera.reporting._

/**
 * A value is data you can refer to in a template.
 */
sealed trait Value {
  def asStr = asInstanceOf[Str]
  def asArr = asInstanceOf[Arr]
  def asFunction = asInstanceOf[Function]
  def asValueHierarchy = asInstanceOf[ValueHierarchy]
}

case class Str(value: String) extends Value {
  def +(that: Str) = Str(value + that.value)
}

object Str {
  def empty = Str("")
}

case class Arr(value: List[Value]) extends Value
object Arr {
  def empty = Arr(Nil)
}

case class Function(f: List[Value] => Str) extends Value with Function1[List[Value], Value] {
  def apply(x: List[Value]): Str = f(x)
}

object Function {

  def function[R1 <: Value](f: (R1) => Str)(implicit m1: Manifest[R1]) = Function {
    case m1(r1) :: Nil =>
      f(r1)
    case r1 :: Nil =>
      val m1Class = m1.runtimeClass
      val r1Class = r1.getClass
      throw InternalEvaluationError(WrongArgumentTypeError(m1Class.getTypeName, r1Class.getTypeName))
    case x =>
      throw InternalEvaluationError(WrongNumberOfArgumentsError(1, x.length))
  }

  def function[R1 <: Value, R2 <: Value](f: (R1, R2) => Str)(implicit m1: Manifest[R1], m2: Manifest[R2]) = Function {
    case m1(r1) :: m2(r2) :: Nil =>
      f(r1, r2)
    case r1 :: r2 :: Nil =>
      val m1Class = m1.runtimeClass
      val r1Class = r1.getClass
      val (expected, found) = if (r1Class != m1Class) (m1Class.getTypeName, r1Class.getTypeName) else (m2.runtimeClass.getTypeName, r2.getClass.getTypeName)
      throw InternalEvaluationError(WrongArgumentTypeError(expected, found))
    case x =>
      throw InternalEvaluationError(WrongNumberOfArgumentsError(2, x.length))
  }

  def function[R1 <: Value, R2 <: Value, R3 <: Value](f: (R1, R2, R3) => Str)(implicit m1: Manifest[R1], m2: Manifest[R2], m3: Manifest[R3]) = Function {
    case m1(r1) :: m2(r2) :: m3(r3) :: Nil =>
      f(r1, r2, r3)
    case r1 :: r2 :: r3 :: Nil =>
      val m1Class = m1.runtimeClass
      val r1Class = r1.getClass
      val m2Class = m2.runtimeClass
      val r2Class = r2.getClass
      val (expected, found) = {
        if (r1Class != m1Class) (m1Class.getTypeName, r1Class.getTypeName)
        else if (r2Class != m1Class) (m2Class.getTypeName, r2Class.getTypeName)
        else (m3.runtimeClass.getTypeName, r3.getClass.getTypeName)
      }
      throw InternalEvaluationError(WrongArgumentTypeError(expected, found))
    case x =>
      throw InternalEvaluationError(WrongNumberOfArgumentsError(3, x.length))
  }
}

/**
 * A tree of values. The values are indexable by their path.
 */
trait ValueHierarchy extends Value {
  protected def resolvePath(path: List[String]): Value

  /**
   * Looks up a value by name in this value hierarchy. Note that
   * this value hierarchy may contain other value hierarchies.
   * Given a path p = n1.n2.<...>.nx, the resolution goes according to the
   * following rules:
   *
   * - Empty path never resolves to a Value
   * - If `p` resolves to a Value, return that value
   * - Otherwise, try to find a longest subpath `sp` of `p`, starting
   *   from `n1`, such that
   *   - `sp` resolves to a ValueHierarchy `h`, and
   *   - `p - sp` (that is, `p` with `sp` dropped), resolves to a Value in `h`

   * @return a resolved Value or null if the value does not exist.
   */
  final def get(path: List[String]): Value =
    if (path.isEmpty) null
    else resolvePath(path) match {
      case null =>
        @annotation.tailrec def resolveFromIntermediaryValueHierarchy(subpathLength: Int): Value = {
          val (subpath, leftover) = (path.take(subpathLength), path.drop(subpathLength))
          if (subpath.isEmpty) null
          else resolvePath(subpath) match {
            case ctx: ValueHierarchy => ctx.get(leftover) match {
              case null => resolveFromIntermediaryValueHierarchy(subpathLength - 1)
              case x => x
            }
            case _ => resolveFromIntermediaryValueHierarchy(subpathLength - 1)
          }
        }
        resolveFromIntermediaryValueHierarchy(path.length)
      case x => x
    }

  final def apply(path: List[String]): Value = get(path) match {
    case null =>
      val name = path.mkString(".")
      if (path.length == 1) throw InternalParserError(NonExistentFunctionError(name))
      else throw InternalParserError(NonExistentNonTopLevelVariableError(name))
    case x => x
  }

  final def apply(path: String): Value =
    apply(path.split('.').toList)

  final def get(path: String): Value =
    get(path.split('.').toList)

  /**
   * Creates a combined ValueHierarchy out of `this` and `other` ValueHierarchy.
   * In case of a name conflict, `other` has precedence during value resolution.
   * A name conflict is defined as a path that resolves to a Value in both hierarchies.
   */
  def +(other: ValueHierarchy): ValueHierarchy = ValueHierarchy { name =>
    other.get(name) match {
      case null => this.get(name)
      case x => x
    }
  }

  override def toString = "<valuehierarchy>"
}

object ValueHierarchy {
  def apply(f: List[String] => Value): ValueHierarchy = new ValueHierarchy {
    protected def resolvePath(name: List[String]): Value = f(name)
  }

  def map(m: Map[List[String], Value]): ValueHierarchy = ValueHierarchy { path =>
    m.get(path).orNull
  }

  def names(m: Map[String, Value]): ValueHierarchy =
    map(m.map { case (k, v) => List(k) -> v })

  def names(ns: (String, Value)*): ValueHierarchy =
    names(ns.toMap)

  def yaml(src: String, fileInfo: FileInfo): ValueHierarchy = {
    def valueFromNode(node: Json): Value = {
      @annotation.tailrec
      def searchInMapping(path: List[String], m: JsonObject): Value =
        path match {
          case Nil => null
          case name :: Nil => valueFromNode(m(name).orNull)
          case name :: rest =>
            m(name).orNull match {
              case null => throw InternalParserError(NonExistentTopLevelVariableError(path.mkString(".")))
              case x if x.isObject => searchInMapping(rest, x.asObject.orNull)
              case _ => null
          }
        }

      node match {
        case null => null

        case x if x.isString => Str(x.asString.get)
        case x if x.isBoolean => Str(x.asBoolean.get.toString)
        case x if x.isNumber => Str(x.asNumber.get.toInt.get.toString)
        case x if x.isNull => null

        case x if x.isArray => Arr(x.asArray.get.toList.map(valueFromNode))
        case x if x.isObject => ValueHierarchy { path => searchInMapping(path, x.asObject.get) }
      }
    }

    io.circe.yaml.parser.parse(src) match {
      case Left(ParsingFailure(_, e: MarkedYAMLException)) =>
        val filename = fileInfo.file.value
        val mark = e.getProblemMark
        val line = mark.getLine
        val code = src.linesIterator.toList(line)
        throw ParserError(filename, if (fileInfo.isExternal) line + 2 else Utils.getLineNb(code, filename),
          mark.getColumn + 1, code, YamlError)
      case Right(mapping) => valueFromNode(mapping).asInstanceOf[ValueHierarchy]
    }
  }

  def empty: ValueHierarchy = ValueHierarchy { _ => null }
}
