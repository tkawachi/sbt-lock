package com.github.tkawachi.sbtlock

import sbt._
import java.nio.charset.Charset

object SbtLock {
  private[sbtlock] val DEFAULT_LOCK_FILE_NAME = "lock.sbt"
  private[sbtlock] val DEPS_HASH_PREFIX = "// LIBRARY_DEPENDENCIES_HASH "

  case class Artifact(organization: String, name: String) {
    def sbtString(revision: String) =
      Seq(organization, name, revision).map("\"" + _ + "\"").mkString(" % ")
  }

  def doLock(
    allModules: Seq[ModuleID],
    depsHash: String,
    lockFile: File,
    log: Logger): File = {

    val moduleLines = allModules
      .filter(m => m.organization != "org.scala-lang")
      .map(mod =>
        s""""${mod.organization}" % "${mod.name}" % "${mod.extraAttributes.getOrElse("version", mod.revision)}"""")
      .distinct
      .sorted

    val dependencyOverrides =
      s"""// DON'T EDIT THIS FILE.
         |// This file is auto generated by ${BuildInfo.name} ${BuildInfo.version}.
         |// ${BuildInfo._url}
         |dependencyOverrides in Compile ++= {
         |  if (!(sbtLockHashIsUpToDate in ThisBuild).value && sbtLockIgnoreOverridesOnStaleHash.value) {
         |    ${Compat.dependencyOverridesType}.empty
         |  } else {
         |    ${Compat.dependencyOverridesType}(
         |      ${moduleLines.mkString(",\n      ")}
         |    )
         |  }
         |}
         |${SbtLock.DEPS_HASH_PREFIX}${depsHash}
         |""".stripMargin

    IO.write(lockFile, dependencyOverrides)
    log.info(s"$lockFile was created. Commit it to version control system.")
    lockFile
  }

  def readDepsHash(lockFile: File): Option[String] =
    if (lockFile.isFile) {
      val charset = Charset.forName("UTF-8")
      val lines = IO.read(lockFile, charset).split("\n")
      lines
        .find(_.startsWith(DEPS_HASH_PREFIX))
        .map(_.drop(DEPS_HASH_PREFIX.length))
    } else {
      None
    }

  def lockFile(state: State): File = {
    val extracted = Project.extract(state)
    val buildStruct = extracted.structure
    val buildUnit = buildStruct.units(buildStruct.root)

    val lockFileName = EvaluateTask.getSetting(
      SbtLockKeys.sbtLockLockFile,
      DEFAULT_LOCK_FILE_NAME,
      extracted,
      buildStruct)
    new File(buildUnit.localBase, lockFileName)
  }

  val checkDepUpdates = (state: State) => {
    // TODO run for all projects
    val (nextState, _) =
      Project.extract(state).runTask(SbtLockKeys.checkLockUpdate, state)
    nextState
  }
}
