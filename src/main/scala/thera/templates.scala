package thera

import java.io.File
import scala.util.matching.Regex

import org.apache.commons.io.{ FileUtils, IOUtils }

import cats._, cats.implicits._, cats.effect._, cats.data.{ EitherT => ET }
import io.circe._


case class TemplateLoopState(body: String, vars: Json, nextTemplatePath: Option[File])

case class Template(body: String, vars: Json, nextTemplateName: Option[String], filters: List[String])

object templates {
  def resolveTemplate(name: String): File =
     new File(s"site-src/templates/$name.html")

  def resolveFragment(value: String): File =
    new File(s"site-src/fragments/$value.html")

  /** Get template' templateBody, templateVars and nextTemplate */
  def parseTemplate(raw: String, globalVars: Json, fragmentResolver: String => File): Ef[Template] = {
    // Parse header, as an Option
    val rawLines: List[String] = raw.split("\n").toList
    val (header: Option[List[String]], body: List[String]) =
      if (rawLines.headOption.exists { _.startsWith("---") })
        Some(rawLines.head :: rawLines.tail.takeWhile(_ != "---")) -> rawLines.reverse.takeWhile(_ != "---").reverse
      else None -> rawLines

    // Drop the first line
    val configRaw: Option[String] =
      header.map(lines => lines.tail.mkString("\n"))
    
    def getConfigFieldOpt[A: Decoder](config: Json, name: String): Ef[Option[A]] =
      exn { config.hcursor.get[Option[A]](name) }

    def getConfigFieldWithDefault[A: Decoder](config: Json, name: String, default: A): Ef[A] =
      getConfigFieldOpt(config, name).map(_.getOrElse(default))

    for {
      // Read the config and its significant fields
      config           <- exn { configRaw.map(yaml.parser.parse).getOrElse(Either.right { Json.obj() } ) }
      nextTemplateName <- getConfigFieldOpt[String](config, "template")
      localVars        <- getConfigFieldWithDefault[Json](config, "variables", Json.obj())
      fragments        <- getConfigFieldWithDefault[Json](config, "fragments", Json.obj())
      filters          <- getConfigFieldWithDefault[List[String]](config, "filters", Nil)

      // Populate fragments and variables. For each fragment in turn, resolve it with all current vars in scope
      allVars        = globalVars deepMerge localVars
      fragmentsMap  <- exn { fragments.as[Map[String, String]] }
      varsWithFrags <- fragmentsMap.toList.foldM(allVars) { case (vars, (k, v)) =>
        for {
          fragSource    <- att { fragmentResolver(v) }
          fragPopulated <- apply(fragSource, vars)
          varsUpdated    = vars.deepMerge(Json.obj(k -> Json.fromString(fragPopulated)))
        } yield varsUpdated }
    } yield Template(body.mkString("\n"), varsWithFrags, nextTemplateName, filters)    
  }

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
          parsed <- parseTemplate(tmlRaw, vars, fragmentResolver)

          // Apply appropriate for this type filters
          filters = parsed.filters.map(templateFilters.filters)
          tmlBodyFiltered <- filters.foldM(parsed.body) { (b, f) => f(b) }

          // Update the `body` variable in the `vars`, merge it with `templateVars`. Populate the templateBody with the combined `vars`. This is the new `body`.
          newVars = vars
            .deepMerge(parsed.vars)
            .deepMerge(Json.obj("body" -> Json.fromString(body)))
          newBody <- populate(tmlBodyFiltered, newVars)

          // Reiterate
          newTmlPath = parsed.nextTemplateName.map(resolveTemplate)
        } yield Left(TemplateLoopState(newBody, newVars, newTmlPath))
    }
}

object templateFilters {
  type TemplateFilter = String => Ef[String]

  lazy val filters: Map[String, TemplateFilter] = Map(
    "post" -> postFilter)

  def main(args: Array[String]): Unit = {
    val proc = sys.runtime.exec("../filters/postFilter.sh", null, new File("_site"))
    IOUtils.write("foo", proc.getOutputStream, settings.enc)
    proc.getOutputStream.close()
    val res = IOUtils.toString(proc.getInputStream, settings.enc)
    println(res)
  }

  val postFilter: TemplateFilter = tml => {
    val proc = sys.runtime.exec("../src/main/bash/postFilter.sh", null, new File("_site"))
    val is   = proc.getInputStream
    val os   = proc.getOutputStream
    val es   = proc.getErrorStream

    def closeAll(): Unit = {
      os.close()
      is.close()
      es.close()
    }

    (for {
      _   <- att { IOUtils.write(tml, os, settings.enc) }
      _   <- att { os.close() }
      res <- att { IOUtils.toString(is, settings.enc)}
      _   <- att { println(IOUtils.toString(es, settings.enc)) }
      _   <- att { closeAll() }
    } yield res).leftMap(e => { closeAll(); e })
  }
}

object populate {
  type Populator = (String, Json) => Ef[String]
  type CommandProcessor = (String, Json, String) => Ef[String]

  val cmdRegex     = """([\w\d_\-]+)\s+([\w\d_\-\.\s]+)"""
  val varNameRegex = """[\w\d_\-\.]+"""

  def apply(tml: String, vars: Json): Ef[String] =
    List[Populator](
      populateCommands  // Commands may use their own vars in the body, so first process commands and then variables
    , populateVars)
    .foldM(tml) { (t, f) => f(t, vars) }
  
  // Resolve variable name with the OOP-style dot ownership syntax
  def resolveVar(path: String, vars: Json): ACursor =
    path.split(raw"\.").toList.foldLeft(vars.hcursor: ACursor)
      { (hc, pathElement) => hc.downField(pathElement) }

  val populateVars: Populator = (tml, vars) => att {
    raw"#\{($varNameRegex)\}".r.replaceAllIn(tml, m => run { for {
      resCursor <- att { resolveVar(m.group(1), vars) }
      res       <- exn { resCursor.as[Option[String]] }.map(_.getOrElse(m.group(0)))
    } yield Regex.quoteReplacement(res) } ) }

  val populateCommands: Populator = (tml, vars) =>
    Monad[Ef].tailRecM(tml) { tml =>
      // Find first command
      val cmds = raw"#\{$cmdRegex\}".r.findAllMatchIn(tml).toList

      if (cmds.isEmpty) Monad[Ef].pure(Right(tml))
      else {
        val mtch     = cmds.head
        val cmdName  = mtch.group(1)
        val cmdArgs  = mtch.group(2)

        for {
          endMtch <- opt(
            cmds.find { m => m.group(1) == "end" && m.group(2) == cmdName }
          , s"Closing tag for command `$cmdName` not found")
          
          cmdProcessor <- opt(
            commandProcessors.get(cmdName)
          , s"Missing command processor for command `$cmdName`")

          body     = tml.substring(mtch.end, endMtch.start)
          bodyRes <- cmdProcessor(body, vars, cmdArgs)

          _ = println(s"Body of $cmdName: $bodyRes")
          cmdRes = tml.substring(0, mtch.start) + bodyRes + tml.substring(endMtch.end)
        } yield Left(cmdRes)
      }
    }

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
