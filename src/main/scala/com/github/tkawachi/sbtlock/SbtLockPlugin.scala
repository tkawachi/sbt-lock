package com.github.tkawachi.sbtlock

import sbt._
import sbt.Keys._
import SbtLock.DEFAULT_LOCK_FILE_NAME

object SbtLockPlugin extends AutoPlugin {

  override def trigger: PluginTrigger = AllRequirements

  object autoImport {
    val sbtLockLockFile: SettingKey[String] = SbtLockKeys.sbtLockLockFile
    val lock: TaskKey[File] = SbtLockKeys.lock
    val unlock: TaskKey[Unit] = SbtLockKeys.unlock
    val checkLockUpdate: TaskKey[Unit] = SbtLockKeys.checkLockUpdate
    val sbtLockHashIsUpToDate: SettingKey[Boolean] = SbtLockKeys.sbtLockHashIsUpToDate
    val sbtLockIgnoreOverridesOnStaleHash: SettingKey[Boolean] = SbtLockKeys.sbtLockIgnoreOverridesOnStaleHash
  }

  import autoImport._

  override def buildSettings: Seq[Def.Setting[_]] = {
    Seq(
      sbtLockHashIsUpToDate := true, // Initially assume all projects are up to date. projectSettings may flip this to false.
      sbtLockIgnoreOverridesOnStaleHash := false)
  }

  override lazy val projectSettings = Seq(
    SbtLockKeys.collectLockModuleIDs := {
      val compileClasspath: Seq[Attributed[File]] =
        Classpaths.managedJars(Compile, classpathTypes.value, update.value)
      val testClasspath: Seq[Attributed[File]] =
        Classpaths.managedJars(Test, classpathTypes.value, update.value)
      val logger = streams.value.log

      def filesToModules(classpath: Seq[Attributed[File]]): Seq[ModuleID] = classpath.flatMap { entry =>
        for {
          art: Artifact <- entry.get(artifact.key)
          mod: ModuleID <- entry.get(moduleID.key)
          if !(excludeDependencies in lock).value.exists { exclude =>
            exclude.organization == mod.organization && exclude.name == "*" && { logger.debug(s"""[Skipped] "${mod.organization}" % "${mod.name}" % "${mod.revision}" because of exclude by organization"""); true } ||
              exclude.organization == mod.organization && exclude.name == mod.name && { logger.debug(s"""[Skipped] "${mod.organization}" % "${mod.name}" % "${mod.revision}" because of exclude by organization/name"""); true }
          }
        } yield {
          logger.debug(
            s"""[Lock] "${mod.organization}" % "${mod.name}" % "${mod.revision}"""")
          mod
        }
      }

      val compileModules = filesToModules(compileClasspath)
      //only add a module with test configuration if they exist in compileModules but with a different version
      val testModules = filesToModules(testClasspath).filter {
        testModule =>
          compileModules.exists(compileModule =>
            compileModule.organization == testModule.organization &&
              compileModule.name == testModule.name &&
              compileModule.revision != testModule.revision)
      }.map(_.withConfigurations(Some(Test.name)))
      compileModules ++ testModules
    },
    lock := {
      val lockFile = new File(baseDirectory.value, sbtLockLockFile.value)
      val allModules = SbtLockKeys.collectLockModuleIDs.value
      val depsHash = ModificationCheck.hash(moduleListToHash.value)
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
      def currentHash: String = ModificationCheck.hash(moduleListToHash.value)
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
        ModificationCheck.hash(moduleListToHash.value)
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
    onLoad in Global ~= { _ compose SbtLock.checkDepUpdates },
    sbtLockLockFile := DEFAULT_LOCK_FILE_NAME,
    excludeDependencies in lock := Seq.empty)

  def moduleListToHash = Def.setting {
    val excludes = (excludeDependencies in lock).value
    (libraryDependencies in Compile).value
      .filterNot(dependency =>
        excludes.exists(exclude =>
          exclude.organization == dependency.organization && exclude.name == "*" ||
            exclude.organization == dependency.organization && exclude.name == dependency.name))
  }
}
