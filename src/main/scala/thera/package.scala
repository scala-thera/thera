import thera.runtime._
import thera.parser.module
import fastparse.Parsed.{ Success, Failure }

import cats._, cats.implicits._, cats.data._, cats.effect._, State.pure

package object thera {
  def compile(str: String): Ef[Runtime] =
    fastparse.parse(str, module(_)) match {
      case Success(result, _) => toRT(result)
      case f: Failure => throw new RuntimeException(f.toString)
    }

  implicit class EfOps(ef: Ef[Runtime])(implicit ctx: Context = Context.monoid.empty) {
    def value   : Runtime = ef    .runA(ctx).value
    def module  : Runtime = value .evalThunk.value
    def asString: String  = module.asText   .value
  }

  implicit class EfMonadOps(ef: Ef[Runtime]) {
    def evalThunk: Ef[Runtime] = ef.flatMap(_.evalThunk)

    def flatMap(that: Ef[Runtime]): Ef[Runtime] =
      for {
        self <- ef.evalThunk
        func <- that
        res  <- func.asFunc(self :: Nil)
      } yield res

    def flatMap(that: Runtime): Ef[Runtime] = flatMap(pure[Context, Runtime](that))

    def >>=(that: Ef[Runtime]): Ef[Runtime] = flatMap(that)
    def >>=(that:    Runtime ): Ef[Runtime] = flatMap(that)

    def mapStr(f: String => String): Ef[Runtime] =
      ef.evalThunk.map { rt => Text(f(rt.asText.value)) }
  }
}
