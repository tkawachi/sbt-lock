# sbt-lock

A sbt 0.13 plugin to create `lock.sbt` file which explicitly specifies
versions of all dependent libraries.

Your application or dependent libraries might contain loose version
dependencies, like `[1.0,)` (means version 1.0 or later),
`latest.release`, etc.
In this case, builds might become different by newer release of
dependent libraries.

This plugin strictly specifies versions by `dependencyOverrides`.

## Usage

Create `~/.sbt/0.13/plugins/sbt-lock.sbt` with following content.

    addSbtPlugin("com.github.tkawachi" % "sbt-lock" % "0.1.1")

* `lock` to create `lock.sbt` file.
  `lock.sbt` includes `dependencyOverrides` for all dependent library versions.
  Manage it with version control system.
* `unlock` to delete `lock.sbt` file.
