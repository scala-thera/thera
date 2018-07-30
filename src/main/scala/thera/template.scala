package thera

import better.files.File
import scala.util.matching.Regex

import cats._, cats.implicits._, cats.effect._, cats.data.{ EitherT => ET }
import io.circe._

import filter.TemplateFilter

object template {
  case class TemplateLoopState(body: String, vars: Json, nextTemplatePath: Option[File])
  case class Template(body: String, vars: Json, nextTemplateName: Option[String], filters: List[String])

  /**
   * Parse the YAML between the "---" lines at the start
   * of the file.
   */
  def parseConfig(lines: Traversable[String]): Ef[Json] =
    if (lines.headOption.contains("---")) exn { yaml.parser.parse {
      lines.tail.takeWhile(_ != "---").mkString("\n") }}
    else pur { Json.obj() }

  /**
   * If the file has a YAML header, drop it and return
   * the remainder. Otherwise, return the entire file unchanged.
   */
  def parseBody(lines: Traversable[String]): String =
    (if (lines.headOption.contains("---"))
      lines.tail.dropWhile(_ != "---").drop(1)
    else lines).mkString("\n")

  /**
   * Get the template's templateBody, templateVars and nextTemplate.
   * Resolve fragments and incorporate them to variables in process.
   */
  def parseTemplate(lines: Traversable[String], globalVars: Json, fragmentResolver: String => File): Ef[Template] = {    
    def getConfigFieldOpt[A: Decoder](config: Json, name: String): Ef[Option[A]] =
      exn { config.hcursor.get[Option[A]](name) }

    def getConfigFieldWithDefault[A: Decoder](config: Json, name: String, default: A): Ef[A] =
      getConfigFieldOpt(config, name).map(_.getOrElse(default))

    for {
      // Read the config and its significant fields
      config <- parseConfig(lines)
      body    = parseBody  (lines)

      // Process the config fields
      nextTemplateName <- getConfigFieldOpt[String](config, "template")
      localVars        <- getConfigFieldWithDefault[Json](config, "variables", Json.obj())
      fragments        <- getConfigFieldWithDefault[Json](config, "fragments", Json.obj())
      filters          <- getConfigFieldWithDefault[List[String]](config, "filters", Nil)

      // Populate fragments and variables.
      // For each fragment in turn, resolve it with all
      // current vars in scope. Fragments end up as ordinary variables.
      allVars        = globalVars deepMerge localVars
      fragmentsMap  <- exn { fragments.as[Map[String, String]] }
      varsWithFrags <- fragmentsMap.toList.foldM(allVars) { case (vars, (k, v)) =>
        for {
          fragSource    <- att { fragmentResolver(v) }
          fragPopulated <- apply(fragSource, vars)
          varsUpdated    = vars.deepMerge(Json.obj(k -> Json.fromString(fragPopulated)))
        } yield varsUpdated }
    } yield Template(body, varsWithFrags, nextTemplateName, filters)    
  }

  /**
   * Take an input file and populate it according to the
   * variables, fragments and templates specified in its header.
   */
  def apply(
        tmlPath         : File
      , initialVars     : Json = Json.obj()
      , templateResolver: String => File = default.templateResolver
      , fragmentResolver: String => File = default.fragmentResolver
      , templateFilters : String => TemplateFilter = Map()): Ef[String] =
    Monad[Ef].tailRecM[TemplateLoopState, String](TemplateLoopState("", initialVars, Some(tmlPath)))
    { case TemplateLoopState(body, vars, None) =>  // Terminal case: no next template
        populate(body, vars).map(Either.right(_))

      case TemplateLoopState(body, vars, Some(nextTemplatePath)) =>
        val tmlRaw = nextTemplatePath.lines
        for {
          // Parse the template of the current file. Apply its filters to this template.
          parsed          <- parseTemplate(tmlRaw, vars, fragmentResolver)
          filters          = parsed.filters.map(templateFilters)
          tmlBodyFiltered <- filters.foldM(parsed.body) { (b, f) => f(b) }

          // Merge template's variables and populate the template with the current body.
          newVars = vars  // Update the `body` variable in the `vars`, merge it with `templateVars`.
            .deepMerge(parsed.vars)
            .deepMerge(Json.obj("body" -> Json.fromString(body)))
          newBody <- populate(tmlBodyFiltered, newVars)  // Populate the templateBody with the combined `vars`. The result is the new `body`.

          // Reiterate
          newTmlPath = parsed.nextTemplateName.map(templateResolver)
        } yield Left(TemplateLoopState(newBody, newVars, newTmlPath))
    }
}
