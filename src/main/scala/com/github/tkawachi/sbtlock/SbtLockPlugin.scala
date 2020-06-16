package com.github.tkawachi.sbtlock

import com.github.tkawachi.sbtlock.SbtLock.DEFAULT_LOCK_FILE_NAME
import sbt.Keys._
import sbt._

import scala.collection.mutable

object SbtLockPlugin extends AutoPlugin {

  override def trigger: PluginTrigger = AllRequirements

  object autoImport {
    val sbtLockLockFile: SettingKey[String] = SbtLockKeys.sbtLockLockFile
    val lock: TaskKey[File] = SbtLockKeys.lock
    val unlock: TaskKey[Unit] = SbtLockKeys.unlock
    val checkLockUpdate: TaskKey[Unit] = SbtLockKeys.checkLockUpdate
    val sbtLockHashIsUpToDate: SettingKey[Boolean] = SbtLockKeys.sbtLockHashIsUpToDate
    val sbtLockIgnoreOverridesOnStaleHash: SettingKey[Boolean] = SbtLockKeys.sbtLockIgnoreOverridesOnStaleHash
    val sbtLockScopes: SettingKey[Seq[String]] = SbtLockKeys.sbtLockScopes
  }

  import autoImport._

  override def buildSettings: Seq[Def.Setting[_]] = {
    Seq(
      sbtLockHashIsUpToDate := true, // Initially assume all projects are up to date. projectSettings may flip this to false.
      sbtLockIgnoreOverridesOnStaleHash := false)
  }

  override lazy val projectSettings = Seq(
    SbtLockKeys.collectLockModuleIDs := {

      val logger = streams.value.log

      def getModules(config: Configuration) = {
        val classpath: Seq[Attributed[File]] =
          Classpaths.managedJars(config, classpathTypes.value, update.value)

        classpath.flatMap { entry =>
          for {
            art: Artifact <- entry.get(artifact.key)
            mod: ModuleID <- entry.get(moduleID.key)
            if !(excludeDependencies in lock).value.exists { exclude =>
              exclude.organization == mod.organization && exclude.name == "*" && {
                logger.debug(s"""[Skipped] "${mod.organization}" % "${mod.name}" % "${mod.revision}" because of exclude by organization""");
                true
              } ||
                exclude.organization == mod.organization && exclude.name == mod.name && {
                  logger.debug(s"""[Skipped] "${mod.organization}" % "${mod.name}" % "${mod.revision}" because of exclude by organization/name""");
                  true
                }
            }
          } yield {
            logger.debug(
              s"""[Lock] "${mod.organization}" % "${mod.name}" % "${mod.revision}"""")
            mod
          }
        }
      }

      val allScopes = SbtLockKeys.sbtLockScopes.value
      // Configurations.default is sort from lower to higher scopes
      val allConfigModules = Configurations.default.collect {
        case config if allScopes.contains(config.name) => (config, getModules(config))
      }

      val allModules = allConfigModules.flatMap(_._2).distinct
      if (allConfigModules.isEmpty) {
        logger.debug(s"""[Lock] no scope match: $allScopes""")
        Map.empty
      } else {
        // init map on 'natural' order
        val moduleByScope: mutable.Map[String, Seq[ModuleID]] = mutable.LinkedHashMap[String, Seq[ModuleID]]()
        allConfigModules.foreach {
          case (config, _) => moduleByScope.put(config.name, Seq[ModuleID]())
        }

        allModules.map {
          m =>
            val lowerScope = allConfigModules.collectFirst {
              case (c, ms) if ms.contains(m) => c.name
            }.getOrElse(Configurations.default.last.name) // fallback should never append ;)

            moduleByScope.put(lowerScope, moduleByScope(lowerScope) :+ m)
            (m, lowerScope)
        }
        moduleByScope.toMap
      }
    },
    lock := {
      val lockFile = new File(baseDirectory.value, sbtLockLockFile.value)
      val allModules = SbtLockKeys.collectLockModuleIDs.value
      val depsHash = ModificationCheck.hash((libraryDependencies in Test).value)
      SbtLock.doLock(allModules, depsHash, lockFile, streams.value.log)
    },
    unlock := {
      val lockFile = new File(baseDirectory.value, sbtLockLockFile.value)
      val deleted = lockFile.delete()
      val logger = streams.value.log
      if (!deleted) {
        logger.warn(s"Failed to delete $lockFile")
      }
    },
    sbtLockHashIsUpToDate := {
      val lockFile = new File(baseDirectory.value, sbtLockLockFile.value)
      val maybeLockedHash = SbtLock.readDepsHash(lockFile)

      def currentHash: String = ModificationCheck.hash((libraryDependencies in Test).value)

      maybeLockedHash == Some(currentHash)
    },
    (sbtLockHashIsUpToDate in ThisBuild) := {
      /**
       * Roll up all the projects across the whole build.
       * When libraryDependencies change in any project, all lock.sbt files across all projects are potentially invalid.
       *
       * Consider:
       * {{{
       *   val thisProject = project.dependsOn(otherProject)
       * }}}
       *
       * Looking at thisProject.sbtLockIsUpToDate is not enough to determine
       * if "sbt ;unlock;reload;lock" would produce different hashes, since
       * libraryDependencies of otherProject may have changed.
       *
       * Hypothetically, we could do this more granularly over just the current project's
       * project dependency tree (including transitive ones),
       * but I don't know how to do that in sbt.
       */
      (sbtLockHashIsUpToDate in ThisBuild).value && sbtLockHashIsUpToDate.value
    },
    checkLockUpdate := {
      val lockFile = new File(baseDirectory.value, sbtLockLockFile.value)
      val currentHash =
        ModificationCheck.hash((libraryDependencies in Test).value)
      val logger = streams.value.log
      val ignoreOnStaleHash = sbtLockIgnoreOverridesOnStaleHash.value
      val projectName = name.value

      SbtLock.readDepsHash(lockFile) match {
        case Some(hashInFile) =>
          if (hashInFile != currentHash) {
            logger.debug(s"[$projectName] hashInFile: $hashInFile, currentHash: $currentHash")
            logger.warn(
              s"[$projectName] libraryDependencies is updated after ${lockFile.name} was created.")
            logger.warn(
              s"Run `;unlock ;reload ;lock` to re-create ${lockFile.name}.")
            logger.warn(
              s"Run just `lock` instead if you want to keep existing library versions.")
            if (ignoreOnStaleHash) {
              logger.warn(
                "Ignoring locked versions because sbtLockIgnoreOverridesOnStaleHash := true.")
            }
          } else {
            logger.info(s"${lockFile.name} is up to date.")
          }
        case None =>
          if (lockFile.isFile) {
            logger.warn(
              s"[$projectName] ${lockFile.name} seems to be created with old version of ${BuildInfo.name}.")
            logger.warn(
              s"Run `;unlock ;reload ;lock` to re-create ${lockFile.name}.")
            logger.warn(
              s"Run just `lock` instead if you want to keep existing library versions.")
          } else if (!lockFile.exists()) {
            logger.info(
              s"[$projectName] ${lockFile.name} doesn't exist. Run `lock` to create one.")
          }
      }
    })

  override val globalSettings = Seq(
    onLoad in Global ~= {
      _ compose SbtLock.checkDepUpdates
    },
    sbtLockLockFile := DEFAULT_LOCK_FILE_NAME,
    excludeDependencies in lock := Seq.empty,
    sbtLockScopes := SbtLock.DEFAULT_SCOPES)

}
