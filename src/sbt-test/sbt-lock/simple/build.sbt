// https://github.com/playframework/playframework/blob/2.2.3/framework/project/Dependencies.scala#L101
libraryDependencies += "com.typesafe.play" %% "play" % "2.2.3"

resolvers += Resolver.typesafeRepo("releases")

scalaVersion := "2.10.4"

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

InputKey[Unit]("checkAbsentDependency") := {
  val module = complete.Parsers.spaceDelimited("").parsed
  val exists = check(module, (dependencyOverrides in Compile).value.toSet)
  assert(
    !exists,
    s"${module.mkString(":")} should not exist in dependencyOverrides: ${(dependencyOverrides in Compile).value}")
}

InputKey[Unit]("addDependency") := {
  val module = complete.Parsers.spaceDelimited("").parsed
  val Seq(org, name, v) = module
  val line = "libraryDependencies += \"" + org + "\" %% \"" + name + "\" % \"" + v + "\""
  IO.append(new File(Keys.baseDirectory.value, "build.sbt"), "\n\n" + line)
}
