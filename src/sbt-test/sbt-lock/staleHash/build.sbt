scalaVersion := "2.10.4"

val transitiveProject = project // see transitiveProject/build.sbt
val otherProject = project.dependsOn(transitiveProject)
val thisProject = project.dependsOn(otherProject)

sbtLockIgnoreOverridesOnStaleHash in ThisBuild := true
