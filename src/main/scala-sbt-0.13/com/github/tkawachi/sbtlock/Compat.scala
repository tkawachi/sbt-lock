package com.github.tkawachi.sbtlock

object Compat {
  val dependencyOverridesType: String = "Set"
  def syntax(config: String, key: String) = s"$key in $config"
}
