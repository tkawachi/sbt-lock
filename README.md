# sbt-lock

## やりたいこと

アプリケーションを何度ビルドしても同じ結果が得られるようにライブラリのバージョンを固定したい。
Ruby的に言うところの `Gemfile.lock` が欲しい。

## 問題意識

sbt で `libraryDependencies` を書く際には ivy 由来の緩い書き方ができる。
たとえば次のようなもの。

* `latest.integration` -- 最新のもの
*  `[1.0,2.0]` -- 1.0 以上、2.0 以下
*  `[1.0,)` -- 1.0 以上

自アプリケーション内で緩い指定をしていた場合は、ビルドのたびに違う結果になっても、それは自業自得だろうなと思う。

問題は、自分が利用しているライブラリで上のような指定がされていたとき。

    自アプリ --> libA --> libB

という依存関係があって、`libA --> libB` の依存関係が緩い場合、`libB` の新リリースに
よって参照するバージョンが違ってくることがある。

実際のところ[metrics-scala](https://github.com/erikvanoosten/metrics-scala)を
使っていて、akka への依存が`[2.2,)`という形で書かれており「あれー、おかしいな」となったことがある。
（[修正が行われたコミット](https://github.com/erikvanoosten/metrics-scala/commit/3ef0db723b35b97691b0abbe1ca16b1ddb10b7bb#diff-fdc3abdfd754eeb24090dbd90aeec2ceL30)）

## アイデア

sbtドキュメントの[Library Management](http://www.scala-sbt.org/release/docs/Detailed-Topics/Library-Management.html)
によれば、

    dependencyOverrides += "log4j" % "log4j" % "1.2.16"

などと書くことで、バージョンを固定できるらしい。

sbt で `show update` すると現在利用中のリビジョンが出るので、
上の例でいう `libA`, `libB` について `dependencyOverrides` を指定する。

幸い `.sbt` という拡張子がついてさえいれば sbt は読み込んでくれるので、
`sbt-lock.sbt` といった別ファイルで `dependencyOverrides` を指定する。

`sbt-lock.sbt` は VCS に入れて管理する。

これでうまくいくのではなかろうか。


## プロトタイプ実装

まずはコンセプトをテキトーに実装する。
やっていることは次の通り。

* `sbt "show update"` を実行
* 実行結果から現在利用中のリビジョンを抜き出す
* `sbt-lock.sbt` に `dependencyOverrides` として書き出す


`go run sbt-lock.go` で `sbt-lock.sbt` が作られる。

なんとなく動いてそう。

## 課題

* `libraryDependencies` に書かれた revision を上書きするため、新しいバージョンを使うときには `libraryDependencies` だけではなく、`dependencyOverrides`も更新する必要がある。
* sbt の plugin にしたいが書き方がわからない。だれか教えてください…
