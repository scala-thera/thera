package thera

import better.files._, File._
import io.circe._, io.circe.syntax._


object Main extends App {

  val processed: Either[String, String] = thera.template(
    tmlPath = file"example/index.html"
  , fragmentResolver = name => file"example/${name}.html"
  , templateResolver = name => file"example/${name}.html"
  , templateFilters  = Map(
    "currentTimeFilter" -> { input =>
      Right(new java.util.Date() + " " + input) })
  , initialVars = Map(
      "our_users" -> List(
        Map("name" -> "John", "email" -> "john@functortech.com")
      , Map("name" -> "Ann" , "email" -> "ann@functortech.com" )
      )).asJson)

  processed match {
    case Right(result) =>
      file"example/_site".createDirectoryIfNotExists()
      file"example/_site/index.html" write result

    case Left (error ) => println(s"Error happened: $error")
  }

}