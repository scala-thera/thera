addSbtPlugin("io.spray"         % "sbt-revolver" % "0.9.0"     )
addSbtPlugin("io.get-coursier"  % "sbt-coursier" % "1.0.0-RC13")
addSbtPlugin("org.scala-js"     % "sbt-scalajs"  % "0.6.21"    )
addSbtPlugin("org.xerial.sbt"   % "sbt-sonatype" % "2.0"       )
addSbtPlugin("com.jsuereth"     % "sbt-pgp"      % "1.1.0"     )  // 1.1.1 does not pick up the GPG keys due to a bug, so using 1.1.0 for now
