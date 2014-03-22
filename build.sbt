sbtPlugin := true

name := "sbt-lock"

organization := "com.github.tkawachi"

licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT"))

scmInfo := Some(ScmInfo(
  url("https://github.com/tkawachi/sbt-lock/"),
  "scm:git:github.com:tkawachi/sbt-lock.git"
))

libraryDependencies += "org.apache.maven" % "maven-artifact" % "latest.release" exclude("org.codehaus.plexus", "plexus-utils")

scalacOptions ++= Seq(
  "-deprecation"
)
