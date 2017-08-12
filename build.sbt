lazy val root = project
  .in(file("."))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    sbtPlugin := true,
    crossSbtVersions := Vector("0.13.16", "1.0.0"),
    name := "sbt-lock",
    organization := "com.github.tkawachi",
    licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT")),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/tkawachi/sbt-lock/"),
        "scm:git:github.com:tkawachi/sbt-lock.git"
      )),
    scalacOptions ++= Seq(
      "-deprecation"
    )
  )
