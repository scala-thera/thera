package implicitconversions

object UserDatabase {
  // start snippet Setting
  type Json = Map[String, String]
  type ApiHandler = PartialFunction[String, Json]

  val apiPath = "/api/user"

  def serve(h: ApiHandler): Unit = println(h(s"$apiPath/42"))

  case class User(id: Int, username: String)

  trait UserDB {
    def write(u: User): Unit
    def read (id: Int): User
  }

  abstract class DummyUserDB(dbName: String) extends UserDB {
    private var persistence: Map[Int, User] = Map()

    def write(u: User): Unit = {
      persistence = persistence.updated(u.id, u)
      println(s"Wrote to $dbName: $u")
    }
    def read(id: Int): User = persistence(id)
  }
  object UserDBSql extends DummyUserDB("SQL")
  // end snippet Setting

  // start snippet MongoDB
  object UserDBMongo extends DummyUserDB("MongoDB")
  // end snippet MongoDB
}
import UserDatabase._


object E1_1_Force_Conversion extends App {
  // start snippet E1_1_Force_Conversion
  def userToJson(u: User): Json =
    Map("id" -> u.id.toString, "username" -> u.username)

  val u = User(42, "Bob")
  UserDBSql.write(u)

  val usersApiPath = s"$apiPath/(\\d+)".r
  val handler: ApiHandler = {
    case usersApiPath(id) =>
      val user = UserDBSql.read(id.toInt)
      userToJson(user)
  }
  serve(handler)
  // end snippet E1_1_Force_Conversion
}

object Implicits {
  // start snippet userToJson
  implicit def userToJson(u: User): Json =
    Map("id" -> u.id.toString, "username" -> u.username)
  // end snippet userToJson
}
import Implicits._

object E1_2_Conversion extends App {
  val u = User(42, "Bob")
  UserDBSql.write(u)
  
  val usersApiPath = s"$apiPath/(\\d+)".r
  // start snippet E1_2_Conversion
  val handler: ApiHandler = {
    case usersApiPath(id) =>
      val user = UserDBSql.read(id.toInt)
      user
  }
  // end snippet E1_2_Conversion
  serve(handler)
}

object E2_Wrapper extends App {
  // start snippet E2_Wrapper_conversions
  class RichUser(u: User) {
    def write() = UserDBSql.write(u)
  }
  implicit def augmentUser(u: User): RichUser = new RichUser(u)
  
  class RichId(x: Int) {
    def readUser(): User = UserDBSql.read(x)
  }
  implicit def augmentInt(x: Int): RichId = new RichId(x)
  // end snippet E2_Wrapper_conversions

  // start snippet E2_Wrapper_examples
  val u = User(42, "Bob")
  u.write()
  
  val usersApiPath = s"$apiPath/(\\d+)".r
  val handler: ApiHandler = {
    case usersApiPath(id) => id.toInt.readUser()
  }
  serve(handler)
  // end snippet E2_Wrapper_examples
}

object E3_1_Force_Context {
  // start snippet E3_1_Force_Context_conversions
  val db = UserDBMongo

  class RichUser(u: User) {
    def write() = db.write(u)
  }
  implicit def augmentUser(u: User): RichUser = new RichUser(u)
  
  class RichId(x: Int) {
    def readUser(): User = db.read(x)
  }
  implicit def augmentInt(x: Int): RichId = new RichId(x)
  // end snippet E3_1_Force_Context_conversions

  def main(args: Array[String]): Unit = {
    // start snippet E3_1_Force_Context_example
    val u = User(42, "Bob")
    u.write()
    
    val usersApiPath = s"$apiPath/(\\d+)".r
    val handler: ApiHandler = {
      case usersApiPath(id) => id.toInt.readUser()
    }
    serve(handler)
    // end snippet E3_1_Force_Context_example
  }
}

object E3_Context {
  // start snippet E3_Context_conversions
  class RichUser(u: User, db: UserDB) {
    def write() = db.write(u)
  }
  implicit def augmentUser(u: User)(implicit db: UserDB): RichUser = new RichUser(u, db)
  
  class RichId(x: Int, db: UserDB) {
    def readUser(): User = db.read(x)
  }
  implicit def augmentInt(x: Int)(implicit db: UserDB): RichId = new RichId(x, db)
  // end snippet E3_Context_conversions

  def main(args: Array[String]): Unit = {
    // start snippet E3_Context_example
    implicit val db = UserDBMongo

    val u = User(42, "Bob")
    u.write()
    
    val usersApiPath = s"$apiPath/(\\d+)".r
    val handler: ApiHandler = {
      case usersApiPath(id) => id.toInt.readUser()
    }
    serve(handler)
    // end snippet E3_Context_example
  }
}

object E4_WrapperShorthand {
  // start snippet E4_WrapperShorthand_conversions
  implicit class RichUser(u: User)(implicit db: UserDB) {
    def write() = db.write(u)
  }
  
  implicit class RichId(x: Int)(implicit db: UserDB) {
    def readUser(): User = db.read(x)
  }
  // end snippet E4_WrapperShorthand_conversions

  def main(args: Array[String]): Unit = {
    implicit val db = UserDBMongo

    val u = User(42, "Bob")
    u.write()
    
    val usersApiPath = s"$apiPath/(\\d+)".r
    val handler: ApiHandler = {
      case usersApiPath(id) => id.toInt.readUser()
    }
    serve(handler)
  }
}
