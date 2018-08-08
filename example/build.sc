import $ivy.`com.github.pathikrit::better-files:3.6.0`
import $ivy.`com.functortech::thera:0.0.2`
import $ivy.`io.circe::circe-core:0.10.0-M1`
import $ivy.`io.circe::circe-generic:0.10.0-M1`

import better.files._, File._
import io.circe._, io.circe.generic.auto._, io.circe.syntax._

val processed: Either[String, String] = thera.template(
  tmlPath = file"index.html"
, fragmentResolver = name => file"${name}.html"
, templateResolver = name => file"${name}.html"
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
    file"_site".createDirectoryIfNotExists()
    file"_site/index.html" write result

  case Left (error ) => println(s"Error happened: $error")
}
