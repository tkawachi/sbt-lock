package com.github.tkawachi.sbtlock

import sbt._
import sbt.Keys.libraryDependencies

object ModificationCheck {
  val CHARSET = "UTF-8"

  /**
   * libraryDependencies in all projects and configurations
   */
  def allLibraryDependencies(state: State): Seq[ModuleID] = {
    val extracted = Project.extract(state)
    val buildStruct: BuildStructure = extracted.structure
    val buildUnit = buildStruct.units(buildStruct.root)

    buildUnit.defined.flatMap {
      case (id, _) =>
        val projectRef = ProjectRef(extracted.currentProject.base, id)
        libraryDependencies.in(projectRef).get(buildStruct.data)
    }.flatten.toList
  }

  /**
   * Hash of ModuleIDs.
   * It uses only organization, name and revision, ignores others.
   */
  def hash(deps: Seq[ModuleID]): String = {
    val depsString = deps
      .map(formatID)
      .distinct
      .sorted
      .mkString("\n")
    sha1(depsString, CHARSET)
  }

  def hashLibraryDependencies(state: State): String = hash(allLibraryDependencies(state))

  private def formatID(m: ModuleID) = s"${m.organization}:${m.name}:${m.revision}"

  def sha1(s: String, charset: String): String = {
    val md = java.security.MessageDigest.getInstance("SHA-1")
    md.digest(s.getBytes(charset)).map(_ & 0xFF).map("%02x".format(_)).mkString
  }
}
