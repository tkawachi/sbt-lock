package com.github.tkawachi.sbtlock

import sbt.{ ModuleID, _ }

object SbtLockKeys {
  val excludeDependencies = settingKey[Seq[SbtExclusionRule]]("Dependencies to be ignored by sbt-lock plugin (by organization or organization+name)")
  val sbtLockLockFile = settingKey[String]("A version locking file name. Must ends with `.sbt`.")
  val lock = taskKey[File]("Create a version locking file for sbt-lock")
  val unlock = taskKey[Unit]("Delete a version locking file for sbt-lock")
  val collectLockModuleIDs = taskKey[Seq[ModuleID]]("Collect ModuleIDs to lock")
  val checkLockUpdate = taskKey[Unit]("Check whether a version locking file needs update")
}
