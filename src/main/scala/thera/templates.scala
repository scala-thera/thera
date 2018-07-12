package thera

import java.io.File
import scala.util.matching.Regex

import org.apache.commons.io.FileUtils

import cats._, cats.implicits._, cats.effect._, cats.data.{ EitherT => ET }
import io.circe._


case class TemplateLoopState(body: String, vars: Json, nextTemplatePath: Option[File])

case class Template(body: String, vars: Json, nextTemplateName: Option[String])

object templates {
  val varNameRegex = """[\w\d_\-\.]+"""

  def resolveTemplate(name: String): File =
     new File(s"site-src/templates/$name.html")

  def resolveFragment(value: String): File =
    new File(s"site-src/fragments/$value.html")

  /** Get template' templateBody, templateVars and nextTemplate */
  def parseTemplate(raw: String, fragmentResolver: String => File): Ef[Template] = {
    // Parse header, as an Option
    val rawLines: List[String] = raw.split("\n").toList
    val (header: Option[List[String]], body: List[String]) =
      if (rawLines.headOption.exists { _.startsWith("---") })
        Some(rawLines.head :: rawLines.tail.takeWhile(_ != "---")) -> rawLines.reverse.takeWhile(_ != "---").reverse
      else None -> rawLines

    // Drop the first line
    val configRaw: Option[String] =
      header.map(lines => lines.tail.mkString("\n"))
    
    for {
      // Read the config and its significant fields
      config           <- exn { configRaw.map(yaml.parser.parse).getOrElse(Either.right { Json.obj() } ) }
      nextTemplateName <- exn { config.hcursor.get[Option[String]]("template" ) }
      variables        <- exn { config.hcursor.get[Option[Json  ]]("variables").map(_.getOrElse(Json.obj())) }
      fragments        <- exn { config.hcursor.get[Option[Json  ]]("fragments").map(_.getOrElse(Json.obj())) }

      // Populate fragments and variables. For each fragment in turn, resolve it with all current vars in scope

      fragmentsMap  <- exn { fragments.as[Map[String, String]] }
      varsWithFrags <- fragmentsMap.toList.foldM(variables) { case (vars, (k, v)) =>
        for {
          fragSource    <- att { fragmentResolver(v) }
          fragPopulated <- apply(fragSource, vars)
          varsUpdated    = vars.deepMerge(Json.obj(k -> Json.fromString(fragPopulated)))
        } yield varsUpdated }
    } yield Template(body.mkString("\n"), varsWithFrags, nextTemplateName)    
  }

  def populate(tml: String, vars: Json): Ef[String] = att {
    raw"#\{($varNameRegex)\}".r.replaceAllIn(tml, m => run { for {
      // Resolve variable name with the OOP-style dot ownership syntax
      varPath   <- att { m.group(1).split(raw"\.").toList }
      resCursor  = varPath.foldLeft(vars.hcursor: ACursor)
        { (hc, pathElement) => hc.downField(pathElement) }
      res       <- exn { resCursor.as[Option[String]] }.map(_.getOrElse(m.group(0)))
    } yield Regex.quoteReplacement(res) } ) }

  def apply(
        tmlPath         : File
      , initialVars     : Json = Json.obj()
      , fragmentResolver: String => File = resolveFragment): Ef[String] =
    Monad[Ef].tailRecM[TemplateLoopState, String](TemplateLoopState("", initialVars, Some(tmlPath)))
    { case TemplateLoopState(body, vars, None                  ) =>  // Terminal case: no next template
        // att { Either.right { raw"#\{($varNameRegex)\}".r.replaceAllIn(body, _ => "") } }
        populate(body, vars).map(Either.right(_))
      case TemplateLoopState(body, vars, Some(nextTemplatePath)) =>
        for {
          // Read the template from the path.
          tmlRaw <- att { FileUtils.readFileToString(nextTemplatePath, settings.enc) }
          parsed <- parseTemplate(tmlRaw, fragmentResolver)

          // Update the `body` variable in the `vars`, merge it with `templateVars`. Populate the templateBody with the combined `vars`. This is the new `body`.
          newVars = vars
            .deepMerge(parsed.vars)
            .deepMerge(Json.obj("body" -> Json.fromString(body)))
          newBody <- populate(parsed.body, newVars)

          // Reiterate
          newTmlPath = parsed.nextTemplateName.map(resolveTemplate)
        } yield Left(TemplateLoopState(newBody, newVars, newTmlPath))
    }
}
