package com.github.tkawachi.sbtlock

import sbt._
import sbt.Keys._
import SbtLock.DEFAULT_LOCK_FILE_NAME

object SbtLockPlugin extends AutoPlugin {

  override def trigger: PluginTrigger = AllRequirements

  object autoImport {
    val sbtLockLockFile = SbtLockKeys.sbtLockLockFile
    val lock = SbtLockKeys.lock
    val unlock = SbtLockKeys.unlock
    val checkLockUpdate = SbtLockKeys.checkLockUpdate
  }

  import autoImport._

  override lazy val projectSettings = Seq(
    SbtLockKeys.collectLockModuleIDs := {
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

    lock := {
      val lockFile = new File(baseDirectory.value, sbtLockLockFile.value)
      val allModules = SbtLockKeys.collectLockModuleIDs.value
      val depsHash = ModificationCheck.hash((libraryDependencies in Compile).value)
      SbtLock.doLock(allModules, depsHash, lockFile, sLog.value)
    },

    unlock := {
      val lockFile = new File(baseDirectory.value, sbtLockLockFile.value)
      val deleted = lockFile.delete()
      if (!deleted) {
        sLog.value.warn(s"Failed to delete $lockFile")
      }
    },

    checkLockUpdate := {
      val lockFile = new File(baseDirectory.value, sbtLockLockFile.value)
      val currentHash = ModificationCheck.hash((libraryDependencies in Compile).value)
      SbtLock.readDepsHash(lockFile) match {
        case Some(hashInFile) =>
          if (hashInFile != currentHash) {
            sLog.value.debug(s"hashInFile: $hashInFile, currentHash: $currentHash")
            sLog.value.warn(s"libraryDependencies is updated after ${lockFile.name} was created.")
            sLog.value.warn(s"Run `;unlock ;reload ;lock` to re-create ${lockFile.name}.")
            sLog.value.warn(s"Run just `lock` instead if you want to keep existing library versions.")
          } else {
            sLog.value.info(s"${lockFile.name} is up to date.")
          }
        case None =>
          if (lockFile.isFile) {
            sLog.value.warn(s"${lockFile.name} seems to be created with old version of ${BuildInfo.name}.")
            sLog.value.warn(s"Run `;unlock ;reload ;lock` to re-create ${lockFile.name}.")
            sLog.value.warn(s"Run just `lock` instead if you want to keep existing library versions.")
          } else if (!lockFile.exists()) {
            sLog.value.info(s"${lockFile.name} doesn't exist. Run `lock` to create one.")
          }
      }
    }
  )

  override val globalSettings = Seq(
    onLoad in Global ~= { _ compose SbtLock.checkDepUpdates },
    sbtLockLockFile := DEFAULT_LOCK_FILE_NAME
  )
}
