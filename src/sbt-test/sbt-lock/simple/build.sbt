import sbt._


resolvers += Resolver.typesafeRepo("releases")

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "org.yaml" % "snakeyaml" % "1.15",
  "com.typesafe.play" %% "play" % "2.5.6", // use a guava 16
  "com.google.guava" % "guava" % "15.0" force() //force  older version of guava
)


def check(module: Seq[String], dependencies: Set[ModuleID]): Boolean = {
  val Seq(org, name, v) = module
  dependencies.exists{ m =>
    m.organization == org && m.name == name && m.revision == v
  }
}

InputKey[Unit]("checkExistsDependency") := {
  val module = complete.Parsers.spaceDelimited("").parsed
  val exists = check(module, dependencyOverrides.value)
  assert(exists, s"${module.mkString(":")} should exist in dependencyOverrides: ${dependencyOverrides.value}")
}

InputKey[Unit]("checkAbsentDependency") := {
  val module = complete.Parsers.spaceDelimited("").parsed
  val exists = check(module, dependencyOverrides.value)
  assert(!exists, s"${module.mkString(":")} should not exist in dependencyOverrides: ${dependencyOverrides.value}")
}

InputKey[Unit]("addDependency") := {
  val module = complete.Parsers.spaceDelimited("").parsed
  val Seq(org, name, v) = module
  val line = "libraryDependencies += \"" + org + "\" %% \"" + name + "\" % \"" + v + "\""
  IO.append(new File(Keys.baseDirectory.value, "build.sbt"), "\n\n" + line)
}
