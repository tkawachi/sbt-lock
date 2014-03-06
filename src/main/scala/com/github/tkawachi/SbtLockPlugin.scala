package com.github.tkawachi

import sbt._
import sbt.Keys._

object SbtLockPlugin extends Plugin {
  val lockFile = settingKey[File]("A version locking file.")
  val lock = taskKey[Unit]("Create a version locking file.")
  val unlock = taskKey[Unit]("Remove a version locking file.")

  override val projectSettings = Seq(
    lockFile := baseDirectory {_ / "lock.sbt"}.value,
    lock := SbtLock.doLock(update.value.allModules, lockFile.value, streams.value),
    unlock := {
      val f = lockFile.value
      val deleted = f.delete()
      if (deleted) streams.value.log.info(s"$f was deleted.")
    }
  )
}
