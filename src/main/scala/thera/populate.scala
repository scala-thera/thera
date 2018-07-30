package thera

import java.io.File
import scala.util.matching.Regex

import org.apache.commons.io.{ FileUtils, IOUtils }

import cats._, cats.implicits._, cats.effect._, cats.data.{ EitherT => ET }
import io.circe._


object populate {
  type Populator = (String, Json) => Ef[String]

  val cmdRegex     = """([\w\d_\-]+)\s+([\w\d_\-\.\s]+)"""
  val varNameRegex = """[\w\d_\-\.]+"""

  /**
   * Given a set of variables and commands to recognize,
   * populates the template*/
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
            command.commandProcessors.get(cmdName)
          , s"Missing command processor for command `$cmdName`")

          body     = tml.substring(mtch.end, endMtch.start)
          bodyRes <- cmdProcessor(body, vars, cmdArgs)

          _ = println(s"Body of $cmdName: $bodyRes")
          cmdRes = tml.substring(0, mtch.start) + bodyRes + tml.substring(endMtch.end)
        } yield Left(cmdRes)
      }
    }
}
