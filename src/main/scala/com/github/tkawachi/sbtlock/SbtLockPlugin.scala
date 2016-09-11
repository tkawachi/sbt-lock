package com.github.tkawachi.sbtlock

import sbt._
import sbt.Keys._
import SbtLock.DEFAULT_LOCK_FILE_NAME

object SbtLockPlugin extends Plugin {
  val sbtLockLockFile = settingKey[String]("A version locking file name")

  val createLockFile = taskKey[Unit]("Create lock.sbt file")
  val deleteLockFile = taskKey[Unit]("Delete lock.sbt file")
  val collectLockModuleIDs = taskKey[Seq[ModuleID]]("Collect ModuleIDs to lock")

  override lazy val projectSettings = Seq(
    collectLockModuleIDs := {
      val classpath: Seq[Attributed[File]] =
        Classpaths.managedJars(Compile, classpathTypes.value, update.value)

      classpath.flatMap { entry =>
        for {
          art: Artifact <- entry.get(artifact.key)
          mod: ModuleID <- entry.get(moduleID.key)
        } yield {
          sLog.value.debug(s"""[Lock] "${mod.organization}" % "${mod.name}" % "${mod.revision}"""")
          mod
        }
      }
    },

    createLockFile := {
      val lockFile = new File(baseDirectory.value, sbtLockLockFile.value)
      val allModules = collectLockModuleIDs.value
      val depsHash = ModificationCheck.hash(allModules)
      SbtLock.doLock(allModules, depsHash, lockFile, sLog.value)
    },

    deleteLockFile := {
      val lockFile = new File(baseDirectory.value, sbtLockLockFile.value)
      val deleted = lockFile.delete()
      if (!deleted) {
        sLog.value.warn(s"Failed to delete $lockFile")
      }
    }
  )

  override val globalSettings = Seq(
    commands ++= Seq(
      Command.command("lock")(state => "createLockFile" :: "reload" :: state),
      Command.command("unlock")(state => "deleteLockFile" :: "reload" :: state),
      Command.command("relock")(state => "unlock" :: "lock" :: state)
    ),
    onLoad in Global ~= { _ compose SbtLock.checkDepUpdates },
    sbtLockLockFile := DEFAULT_LOCK_FILE_NAME
  )
}
