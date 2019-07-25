# sbt-lock [![Build Status](https://secure.travis-ci.org/tkawachi/sbt-lock.png?branch=master)](http://travis-ci.org/tkawachi/sbt-lock) [![Stories in Ready](https://badge.waffle.io/tkawachi/sbt-lock.png?label=ready&title=Ready)](https://waffle.io/tkawachi/sbt-lock)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/6f20cdcdf27d4e8a9cbbe47385382c44)](https://www.codacy.com/app/tkawachi/sbt-lock?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=tkawachi/sbt-lock&amp;utm_campaign=Badge_Grade)

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

```
addSbtPlugin("com.github.tkawachi" % "sbt-lock" % "0.6.1")
```

* `lock` to create `lock.sbt` file.
  `lock.sbt` includes `dependencyOverrides` for all dependent library versions.
  Manage it with version control system.
* `unlock` to delete `lock.sbt` file.
* `checkLockUpdate` to print whether the lock file needs an update.

## Settings

* `excludeDependencies` could be used to exclude some dependencies from locking. This could be required for platform-specific dependencies (e.g. Netty native)

    ```
    import com.github.tkawachi.sbtlock._
    
    val settings: Seq[Setting[_]] = Seq(
      excludeDependencies in SbtLockKeys.lock := Seq(
        "org.reactivemongo" % "reactivemongo-shaded-native"
      )
    )
    ```

* `sbtLockIgnoreOverridesOnStaleHash := true` (default: `false`) makes `libraryDependencies` 
    changes to take effect on reload, even without a `;unlock;reload;lock` cycle.
    
    Enabling is useful to match expectations of 1. update library dependencies, 2. reload, 3. see changes immediately.
