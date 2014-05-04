buildInfoSettings

sourceGenerators in Compile <+= buildInfo

val _url = settingKey[URL]("_url")

_url := scmInfo.value.map(_.browseUrl).getOrElse(sys.error("Set scmInfo.browseUrl"))

buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion, _url)

buildInfoPackage := "com.github.tkawachi.sbtlock"
