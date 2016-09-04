package com.github.tkawachi.sbtlock

import sbt._
import sbt.Keys._
import SbtLock.DEFAULT_LOCK_FILE_NAME

object SbtLockPlugin extends Plugin {
  val sbtLockLockFile = settingKey[String]("A version locking file name")

  lazy val lock = taskKey[Unit]("lock file")
  override lazy val projectSettings = Seq(
    lock := {

      def managedJars() = {
        val classpath: Seq[Attributed[File]] =
          Classpaths.managedJars(Compile, classpathTypes.value, update.value)
        classpath.collect {
          case entry if entry.get(artifact.key).isDefined && entry.get(moduleID.key).isDefined =>
            val art: Artifact = entry.get(artifact.key).get
            val mod: ModuleID = entry.get(moduleID.key).get
            sLog.value.debug(s"""[Lock] "${mod.organization}" % "${mod.name}" % "${mod.revision}"""")
            mod
        }
      }

      val lockFile = new File(baseDirectory.value, sbtLockLockFile.value)
      val allModules = managedJars()
      val depsHash = ModificationCheck.hash(allModules)

      SbtLock.doLock(allModules, depsHash, lockFile, sLog.value)

    }
  )
  override val globalSettings = Seq(
    commands ++= Seq(
      Command.command("unlock") { state =>

        val extracted = Project.extract(state)
        val buildStruct = extracted.structure
        val buildUnit = buildStruct.units(buildStruct.root)

        val lockFileName = EvaluateTask.getSetting(sbtLockLockFile, DEFAULT_LOCK_FILE_NAME, extracted, buildStruct)
        val lockFile = new File(buildUnit.localBase, lockFileName)

        lockFile.delete()
        "reload" :: state
      },
      Command.command("relock") { state =>
        "unlock" :: "lock" :: state
      }
    ),
    onLoad in Global ~= { _ compose SbtLock.checkDepUpdates },
    sbtLockLockFile := DEFAULT_LOCK_FILE_NAME
  )
}
