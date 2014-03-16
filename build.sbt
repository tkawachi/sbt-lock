sbtPlugin := true

name := "sbt-lock"

organization := "com.github.tkawachi"

version := "0.1-SNAPSHOT"

licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT"))

scmInfo := Some(ScmInfo(
  url("https://github.com/tkawachi/sbt-lock/"),
  "scm:git:github.com:tkawachi/sbt-lock.git"
))

scalacOptions ++= Seq(
  "-deprecation"
)
