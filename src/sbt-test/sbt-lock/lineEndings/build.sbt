// https://github.com/playframework/playframework/blob/2.2.3/framework/project/Dependencies.scala#L101
libraryDependencies += "com.typesafe.play" %% "play" % "2.2.3"

resolvers += Resolver.typesafeRepo("releases")

scalaVersion := "2.10.4"

TaskKey[Unit]("assertSbtLockHashIsUpToDate") := {
  assert(
    sbtLockHashIsUpToDate.value,
    s"sbtLockHashIsUpToDate.value was not true"
  )
}

TaskKey[Unit]("convertToWindowsLineEndings") := {
  val lockFile = new File(Keys.baseDirectory.value, "lock.sbt")
  IO.write(lockFile, IO.read(lockFile).replace("\n", "\r\n"))
}