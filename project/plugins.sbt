addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.7.0")
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.6")
//addSbtPlugin("com.github.tkawachi" % "sbt-lock" % "0.2.2")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.1.0-M1")
addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.7.1")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "2.0")

libraryDependencies += "org.scala-sbt" % "scripted-plugin" % sbtVersion.value
