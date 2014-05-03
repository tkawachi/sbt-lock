package com.github.tkawachi.sbtlock

import sbt._
import sbt.Keys._

object SbtLockPlugin extends Plugin {
  private val DEFAULT_LOCK_FILE_NAME = "lock.sbt"

  val sbtLockLockFile = settingKey[String]("A version locking file name")

  override val settings = Seq(
    commands ++= Seq(
      Command.command("lock") { state =>

        val extracted = Project.extract(state)
        val buildStruct: BuildStructure = extracted.structure
        val buildUnit = buildStruct.units(buildStruct.root)

        val lockFileName = EvaluateTask.getSetting(sbtLockLockFile, DEFAULT_LOCK_FILE_NAME, extracted, buildStruct)
        val lockFile = new File(buildUnit.localBase, lockFileName)

        val allModules = buildUnit.defined.flatMap {
          case (id, project) =>
            val projectRef = ProjectRef(extracted.currentProject.base, id)
            // Evaluate update task then collect modules in result reports.
            EvaluateTask(buildStruct, update, state, projectRef).map(_._2) match {
              case Some(Value(report)) => report.allModules
              case _ => Seq.empty
            }
        }.toList

        SbtLock.doLock(allModules, lockFile, state.log)

        "reload" :: state
      },
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
    sbtLockLockFile := DEFAULT_LOCK_FILE_NAME
  )
}
