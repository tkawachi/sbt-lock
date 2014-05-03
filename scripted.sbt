ScriptedPlugin.scriptedSettings

ScriptedPlugin.scriptedBufferLog := false

scriptedLaunchOpts += "-Dplugin.version=" + version.value

scriptedLaunchOpts ++= sys.process.javaVmArguments.filter(
  arg => Seq("-Xmx", "-Xms", "-XX").exists(arg.startsWith)
)
