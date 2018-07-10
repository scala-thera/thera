package thera

import java.io.File

import org.apache.commons.io.FileUtils

import cats._, cats.implicits._, cats.effect._, cats.data.{ EitherT => ET }


case class TemplateLoopState(body: String, vars: Map[String, String], nextTemplatePath: Option[File])

case class Template(body: String, vars: Map[String, String], nextTemplateName: Option[String])

object templates {
  val varNameRegex = """[\w\d_-]+"""

  def resolveTemplate(name: String): File =
     new File(s"site-src/templates/$name.html")

  def resolveFragment(value: String): File =
    new File(s"site-src/fragments/$value.html")

  /** Get template' templateBody, templateVars and nextTemplate */
  def parseTemplate(raw: String): Ef[Template] = att {
    // Parse header, as an Option
    val rawLines: List[String] = raw.split("\n").toList
    val (header: Option[List[String]], body: List[String]) =
      if (rawLines.headOption.exists { _.startsWith("---") })
        Some(rawLines.head :: rawLines.tail.takeWhile(_ != "---")) -> rawLines.reverse.takeWhile(_ != "---").reverse
      else None -> rawLines

    // Take the first line, parse an optional next template
    val nextTemplateName: Option[String] =
      header.map(_.head.dropWhile(c => c == '-' || c == ' ')).filter(_.nonEmpty)
    
    // Drop first and last lines
    val varsLines: List[String] = header.map(lines => lines.tail).getOrElse(List.empty)
    
    val varDef  = s"""($varNameRegex)\\s*=\\s*(.*)""".r
    val fragDef = s"""($varNameRegex)\\s*:=\\s*(.*)""".r
    val ident   = s"""\\s{2}(.*)""".r

    // For each line:
    // If it follows the `a = b` pattern, create a new variable `a`
    // If it follows the `a := b` pattern, read a fragment named `b` and save the contents under the variable `a`
    // If it starts with an indent, append the line to the last encountered var
    @annotation.tailrec
    def loop(lines: List[String], accum: Map[String, String], lastVar: Option[String]): Map[String, String] = lines match {
      case varDef (name, value) :: ls => loop(ls, accum.updated(name, value), Some(name))
      case fragDef(name, value) :: ls =>
        val fragContents = FileUtils.readFileToString(resolveFragment(value), "utf8")
        loop(ls, accum.updated(name, fragContents), Some(name))

      case ident(contents)      :: ls => loop(ls, accum.updated(lastVar.get, accum(lastVar.get) + "\n" + contents), lastVar)
      case Nil => accum
    }

    val vars = loop(varsLines, Map(), None)
    Template(body.mkString("\n"), vars, nextTemplateName)
  }

  def populate(tml: String, vars: Map[String, String]): Ef[String] = att {
    raw"#\{($varNameRegex)\}".r
      .replaceAllIn(tml, m => vars.getOrElse(m.group(1), m.group(0)).toString) }

  def apply(tmlPath: File, initialVars: Map[String, String] = Map()): Ef[String] =
    Monad[Ef].tailRecM[TemplateLoopState, String](TemplateLoopState("", initialVars, Some(tmlPath)))
    { case TemplateLoopState(body, _   , None                  ) =>  // Terminal case: no next template
        att { Either.right { raw"#\{($varNameRegex)\}".r.replaceAllIn(body, _ => "") } }
      case TemplateLoopState(body, vars, Some(nextTemplatePath)) =>
        for {
          // Read the template from the path.
          tmlRaw <- att { FileUtils.readFileToString(nextTemplatePath, settings.enc) }
          parsed <- parseTemplate(tmlRaw)

          // Update the `body` variable in the `vars`, merge it with `templateVars`. Populate the templateBody with the combined `vars`. This is the new `body`.
          newVars  = vars.updated("body", body) ++ parsed.vars
          newBody <- populate(parsed.body, newVars)

          // Reiterate
          newTmlPath = parsed.nextTemplateName.map(resolveTemplate)
        } yield Left(TemplateLoopState(newBody, newVars, newTmlPath))
    }
}
