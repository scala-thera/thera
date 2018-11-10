package thera.population

import ast._
import io.circe._

object fold {
//   def apply(n: Node)(implicit ctx: Module): String = this match {
//     case Text    (str       ) => str
//     case Tree    (nodes     ) => nodes.foldLeft("") { (accum, n) => accum + fold(n) }
//     case Variable(path      ) => resolve(path)
//     case Call    (path, args) => resolveFun(path)(args)
//   }

//   def resolve(path: List[String])(implicit ctx: Module): String =
//     ctx.header.map { h =>
//       path.foldLeft(h.hcursor: ACursor)
//         { (hc, pathElement) => hc.downField(pathElement) }
//         .get[String]
//     }
}
