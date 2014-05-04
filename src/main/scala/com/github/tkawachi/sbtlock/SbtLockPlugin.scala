package com.github.tkawachi.sbtlock

import sbt._
import sbt.Keys._
import SbtLock.DEFAULT_LOCK_FILE_NAME

object SbtLockPlugin extends Plugin {
  val sbtLockLockFile = settingKey[String]("A version locking file name")

  override val settings = Seq(
    commands ++= Seq(
      Command.command("lock") { state =>

        val extracted = Project.extract(state)
        val buildStruct: BuildStructure = extracted.structure
        val buildUnit = buildStruct.units(buildStruct.root)

        val lockFileName = EvaluateTask.getSetting(sbtLockLockFile, DEFAULT_LOCK_FILE_NAME, extracted, buildStruct)
        val lockFile = new File(buildUnit.localBase, lockFileName)

        val definedModules = buildUnit.defined.flatMap {
          case (id, _) =>
            val projectRef = ProjectRef(extracted.currentProject.base, id)
            projectID.in(projectRef).get(buildStruct.data)
        }.toList

        def isDefinedModule(moduleId: ModuleID): Boolean = definedModules.exists { m =>
          val binVer = scalaBinaryVersion.in(extracted.currentRef).get(buildStruct.data)
          m.organization == moduleId.organization &&
            // Better comparison for name with binary version?
            (m.name == moduleId.name || binVer.exists(m.name + "_" + _ == moduleId.name)) &&
            m.revision == moduleId.revision
        }

        val allModules = buildUnit.defined.flatMap {
          case (id, _) =>
            val projectRef = ProjectRef(extracted.currentProject.base, id)
            // Evaluate update task then collect modules in result reports.
            EvaluateTask(buildStruct, update, state, projectRef).map(_._2) match {
              case Some(Value(report)) => report.allModules.filterNot(isDefinedModule)
              case _ => Seq.empty
            }
        }
          // Exclude dependencies in scala-tool configuration
          .filterNot(_.configurations.exists(_ == "scala-tool"))
          .toList

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
