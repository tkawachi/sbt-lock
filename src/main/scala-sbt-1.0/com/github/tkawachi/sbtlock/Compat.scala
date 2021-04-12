package com.github.tkawachi.sbtlock

object Compat {
  val dependencyOverridesType: String = "Seq"
  def syntax(config: String, key: String) = s"$config / $key"
}
