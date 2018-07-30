import scala.util.Try
import cats._, cats.implicits._, cats.effect._, cats.data.{ NonEmptyList => NEL, _ }

package object thera {
  type Ef[A] = Either[String, A]

  /** Option */
  def opt[A](o: Option[A], msg: String = "Empty option error"): Ef[A] = o
    .map       { Right(_ ) }
    .getOrElse { Left(msg) }

  /** Exception under Either */
  def exn[A, E <: Throwable](e: Either[E, A]): Ef[A] =
    e.leftMap { x => x.getMessage + "\n" + x.getStackTrace.mkString("\n") }

  /** Attempt to run an error-prone computation */
  def att[A](a  : => A  ): Ef[A] = exn { Try(a).toEither }
  def pur[A](a  : A     ): Ef[A] = Right(a)
  def err[A](msg: String): Ef[A] = Left(msg)

  /** Extract A out of Ef[A], run all side effects */
  def run[A](ef: Ef[A]): A = ef match {
    case Right(a) => a
    case Left (e) =>
      println(s"Error happened:\n$e")
      throw new RuntimeException("Ef execution error")
  }
}
