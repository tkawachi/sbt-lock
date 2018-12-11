def check(module: Seq[String], dependencies: Set[ModuleID]): Boolean = {
  val Seq(org, name, v) = module
  dependencies.exists { m =>
    m.organization == org && m.name == name && m.revision == v
  }
}

InputKey[Unit]("checkExistsDependencyClasspath") := {
  val module = complete.Parsers.spaceDelimited("").parsed
  val exists = check(module, (dependencyClasspath in Compile).value.flatMap(_.get(moduleID.key)).toSet)
  assert(
    exists,
    s"${module.mkString(":")} should exist in dependencyClasspath: ${(dependencyClasspath in Compile).value}"
  )
}

InputKey[Unit]("checkAbsentDependencyClasspath") := {
  val module = complete.Parsers.spaceDelimited("").parsed
  val exists = check(module, (dependencyClasspath in Compile).value.flatMap(_.get(moduleID.key)).toSet)
  assert(
    !exists,
    s"${module.mkString(":")} should not exist in dependencyClasspath: ${(dependencyClasspath in Compile).value}"
  )
}

TaskKey[Unit]("assertSbtLockHashIsUpToDate") := {
  assert(
    sbtLockHashIsUpToDate.value,
    s"sbtLockHashIsUpToDate.value was not true"
  )
}

TaskKey[Unit]("assertSbtLockHashIsUpToDateInThisBuild") := {
  assert(
    (sbtLockHashIsUpToDate in ThisBuild).value,
    s"(sbtLockHashIsUpToDate in ThisBuild).value was not true"
  )
}
