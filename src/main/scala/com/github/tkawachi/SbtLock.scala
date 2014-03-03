package com.github.tkawachi

import sbt._
import sbt.Keys._
import java.io.PrintWriter

object SbtLock extends Plugin {
  val lock = taskKey[Unit]("Create a version locking file.")
  val lockFile = settingKey[File]("A version locking file.")

  val lockSettings = Seq(
    lockFile := baseDirectory {_ / "lock.sbt"}.value,
    lock := {
      val moduleLines = update.value.allModules.map { m =>
        val elem = m.configurations match {
          case Some(c) => Seq(m.organization, m.name, m.revision, c)
          case None => Seq(m.organization, m.name, m.revision)
        }
        elem.map("\"" + _ + "\"").mkString(" % ")
      }.sorted.mkString(",\n  ")
      val sbtString = "dependencyOverrides ++= Set(\n  " ++ moduleLines ++ "\n)"
      val writer = new PrintWriter(lockFile.value)
      try writer.write(sbtString) finally {
        writer.close()
      }
    }
  )
}
