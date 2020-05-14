package thera.runtime

trait Context { self =>
  /** Returns null of the value does not exist */
  protected def resolveKey(name: List[String]): Runtime

  final def apply(name: List[String]): Runtime =
    val res = resolveKey(name) match {
      case null => name match {
        case h :: t =>
          resolveKey(h :: Nil) match {
            case null => null
            case Data(raw) => Context.json(raw)(t)
          }
        case Nil => null
      }
      case x => x
    }
    if (res eq null) throw new RuntimeException(s"Symbol not found: ${name.mkString(".")}")
    res

  def +(other: Context): Context = Context { name =>
    this(name) match {
      case null => other(name)
      case x => x
    }
  }
}

object Context {
  def apply(f: List[String] => Runtime): Context = new Context {
    protected def resolveKey(name: List[String]): Runtime = f(name)
  }

  def map(m: Map[List[String], Runtime]) = Context { name =>
    m.get(name).orNull
  }

  def names(m: Map[String, Runtime]): Context =
    map(m.map { case (k, v) => List(k) -> v })

  def names(ns: (String, Runtime)*): Context =
    names(ns.toMap)

  def json(vars: Json): Context = ???

  def empty: Context = Context { _ => null }
}
