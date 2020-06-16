import sbt._

resolvers += Resolver.typesafeRepo("releases")

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "org.yaml" % "snakeyaml" % "1.15",
  "com.typesafe.play" %% "play" % "2.5.6", // use a guava 16
  "com.google.guava" % "guava" % "15.0" force (), //force  older version of guava
  "org.junit.jupiter" % "junit-jupiter-engine" % "5.5.2" % "test"
)

def check(module: Seq[String], dependencies: Set[ModuleID]): Boolean = {
  val Seq(org, name, v) = module
  dependencies.exists { m =>
    m.organization == org && m.name == name && m.revision == v
  }
}

InputKey[Unit]("checkExistsDependency") := {
  val module = complete.Parsers.spaceDelimited("").parsed
  val exists = check(module, (dependencyOverrides in Compile).value.toSet)
  assert(
    exists,
    s"${module.mkString(":")} should exist in dependencyOverrides: ${(dependencyOverrides in Compile).value}")
}

InputKey[Unit]("checkExistsTestDependency") := {
  val module = complete.Parsers.spaceDelimited("").parsed
  val exists = check(module, (dependencyOverrides in Test).value.toSet)
  assert(
    exists,
    s"${module.mkString(":")} should exist in dependencyOverrides: ${(dependencyOverrides in Test).value}")
}
