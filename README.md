# sbt-lock [![Build Status](https://secure.travis-ci.org/tkawachi/sbt-lock.png?branch=master)](http://travis-ci.org/tkawachi/sbt-lock) [![Stories in Ready](https://badge.waffle.io/tkawachi/sbt-lock.png?label=ready&title=Ready)](https://waffle.io/tkawachi/sbt-lock)
[![Codacy Badge](https://www.codacy.com/project/badge/6f20cdcdf27d4e8a9cbbe47385382c44)](https://www.codacy.com/app/tkawachi/sbt-lock)

A sbt 0.13 plugin to create `lock.sbt` file which explicitly specifies
versions of all dependent libraries.

Your application or dependent libraries might contain loose version
dependencies, like `[1.0,)` (means version 1.0 or later),
`latest.release`, etc.
In this case, builds might become different by a newer release of
dependent libraries.

This plugin strictly specifies versions by `dependencyOverrides`.

## Usage

Add the following line to `~/.sbt/0.13/plugins/sbt-lock.sbt` for sbt 0.13.x,
`~/.sbt/1.0/plugins/sbt-lock.sbt` for sbt 1.0.x or `project/plugins.sbt`.

    addSbtPlugin("com.github.tkawachi" % "sbt-lock" % "0.4.2")

* `lock` to create `lock.sbt` file.
  `lock.sbt` includes `dependencyOverrides` for all dependent library versions.
  Manage it with version control system.
* `unlock` to delete `lock.sbt` file.
