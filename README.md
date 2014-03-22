# sbt-lock

A sbt plugin to create `lock.sbt` file which explicitly specifies
versions of all dependent libraries.
It makes builds more stable and reproducible.

Create `~/.sbt/0.13/plugins/sbt-lock.sbt` with following content.

    addSbtPlugin("com.github.tkawachi" % "sbt-lock" % "0.1.1")

* `lock` to create `lock.sbt` file.
  `lock.sbt` includes `depnedencyOverrides` for all dependent library versions.
* `unlock` to delete `lock.sbt` file.
