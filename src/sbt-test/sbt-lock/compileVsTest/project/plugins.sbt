{
  val pluginVersion = System.getProperty("plugin.version")
  if(pluginVersion == null)
    sys.error("""|The system property 'plugin.version' is not defined.
                 |Specify this property using the scriptedLaunchOpts -D.""".stripMargin)
  else addSbtPlugin("com.github.tkawachi" % "sbt-lock" % pluginVersion)
}
