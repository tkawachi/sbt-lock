package com.github.tkawachi.sbtlock

import sbt.{ ModuleID, _ }

object SbtLockKeys {
  val sbtLockLockFile = settingKey[String]("A version locking file name. Must ends with `.sbt`.")
  val lock = taskKey[File]("Create a version locking file for sbt-lock")
  val unlock = taskKey[Unit]("Delete a version locking file for sbt-lock")
  val collectLockModuleIDs = taskKey[Seq[ModuleID]]("Collect ModuleIDs to lock")
  val checkLockUpdate = taskKey[Unit]("Check whether a version locking file needs update")
  val sbtLockHashIsUpToDate = settingKey[Boolean]("lock.sbt file is up to date with libraryDependencies in the current project. " +
    "(sbtLockHashIsUpToDate in ThisProject) does NOT consider the libraryDependencies of project dependencies! " +
    "(sbtLockHashIsUpToDate in ThisBuild) considers the libraryDependencies across all projects.")
  val sbtLockIgnoreOverridesOnStaleHash = settingKey[Boolean]("When libraryDependencies are changed, ignores the lock.sbt file.")
}
