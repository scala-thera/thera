package thera

import scala.collection.JavaConverters._

import java.io.File
import java.util.Date
import java.text.SimpleDateFormat
import org.apache.commons.io.FileUtils

import cats._, cats.implicits._

import io.circe._


object Main {
  val postsIn  = new File("site-src/posts/")
  val postsOut = new File("_site/posts/")

  val data   = new File("site-src/data/data.yml")
  val assets = new File("site-src/assets")

  def main(args: Array[String]): Unit = run { for {
    // Config
    configRaw <- att { FileUtils.readFileToString(data, settings.enc) }
    config    <- exn { yaml.parser.parse(configRaw) }
    _         <- att { println(s"Config parsed:\n${config}") }

    // Assemble assets
    _   <- att { FileUtils.copyDirectory(assets, new File("_site", "assets")) }
    css <- templates(
      new File("site-src/private-assets/css/all.css")
    , fragmentResolver = name => new File(s"site-src/private-assets/css/${name}.css"))
    _   <- att { FileUtils.writeStringToFile(new File("_site/assets/all.css"), css, settings.enc) }

    // Copy code directory
    _ <- att { FileUtils.copyDirectory(new File("site-src/code"), new File("_site/code")) }

    // Process input posts
    allPosts <- att { readPosts(postsIn) }
    _ <- allPosts.traverse(processPost(_, config))


    // Delete the code directory
    _ <- att { FileUtils.deleteDirectory(new File("_site/code")) }
    // _ <- index(vars)
  } yield () }

  def readPosts(input: File): List[Post] =
    FileUtils.iterateFiles(input, Array("md"), false)
      .asScala.map(Post.fromFile).toList

  def processPost(post: Post, globalConfig: Json): Ef[Unit] =
    for {
      config <- att { globalConfig.deepMerge(post.asJson) }
      res    <- templates(post.inFile, config)
      _      <- att { FileUtils.writeStringToFile(new File(postsOut, post.htmlName), res, settings.enc) }
    } yield ()
}

case class Post(inFile: File, date: Date, title: String) {
  lazy val htmlName: String =
    inFile.getName.reverse.drop(2).reverse + "html"

  lazy val url: String = s"/posts/$htmlName"
  
  lazy val dateStr: String = Post.dateFormatter.format(date)

  lazy val asJson: Json = Json.obj(
    "date"  -> Json.fromString(dateStr)
  , "url"   -> Json.fromString(url    )
  , "title" -> Json.fromString(title  ) )
}

object Post {
  val dateParser    = new SimpleDateFormat("yyyy-MM-dd"    )
  val dateFormatter = new SimpleDateFormat("MMMMM dd, yyyy")
  
  def fromFile(f: File): Post = {
    val postName = """(\d{4}-\d{2}-\d{2})-(.*)\.md""".r
    f.getName match { case postName(dateStr, title) => Post(
      inFile = f
    , date   = dateParser.parse(dateStr)
    , title  = title) }
  }
}
