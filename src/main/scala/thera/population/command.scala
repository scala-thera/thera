package thera
package population

import java.io.File
import scala.util.matching.Regex

import cats._, cats.implicits._, cats.effect._, cats.data.{ EitherT => ET }
import io.circe._


object command {
  import populate._

  type CommandProcessor = (String, Json, String) => Ef[String]

  lazy val commandProcessors: Map[String, CommandProcessor] = Map(
    "for"      -> forProcessor
  , "if"       -> ifProcessor
  , "mkstring" -> mkstringProcessor)

  val forProcessor: CommandProcessor = (tml, vars, args) =>
    for {
      arrayAndVar <- args.split(" ").toList match {
        case arr :: vn :: Nil => pur { arr -> vn }
        case x => err(s"Incorrect format for `for` command. Expected: `for <array> <variable>`, got: $x") }
      (array, varName) = arrayAndVar

      entries <- exn { vars.hcursor.get[List[Json]](array) }
      res     <- entries.foldM("") { (accum, e) =>
        populateVars(tml, vars.deepMerge { Json.obj(varName -> e) })
          .map(accum + _) }
    } yield res

  val ifProcessor: CommandProcessor = (tml, vars, args) => {
    def resolveAndThen(varName: String, todo: Option[Json] => String): Ef[String] =
      exn { resolveVar(varName, vars).as[Option[Json]] }.map(todo)

    args.split(" ").toList match {
      case varName :: Nil =>
        resolveAndThen(varName, _.fold("")(_ => tml))

      case "not" :: varName :: Nil =>
        resolveAndThen(varName, _.fold(tml)(_ => ""))
    }
  }

  val mkstringProcessor: CommandProcessor = (tml, vars, array) =>
    exn { vars.hcursor.get[List[String]](array) }.map(_.mkString(tml))
}
