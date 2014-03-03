package main

import "bytes"
import "fmt"
import "log"
import "io/ioutil"
import "os/exec"
import "regexp"
import "sort"
import "strings"

var LockFileName = "sbt-lock.sbt"

type Artifact struct {
	group    string
	artifact string
}

func (version *Artifact) format(revision string) string {
	return fmt.Sprintf("\"%s\" %% \"%s\" %% \"%s\"",
		version.group, version.artifact, revision)
}

// sbt 'show update' を実行するよ
func sbtUpdate() string {
	out, err := exec.Command("sbt", "-Dsbt.log.noformat=true", "show update").Output()
	if err != nil {
		log.Fatal(err)
	}
	return string(out)
}

// dependencyOverrides の設定文字列を生成するよ
func formatDeps(updateOutput string) string {
	// 次のような行をひっかけるよ
	// [info]          com.github.nscala-time:nscala-time_2.10:0.6.0: (Artifact(...
	libRegexp := regexp.MustCompile(`^\[info\]\s+([^:\s]+):([^:\s]+):([^:\s]+):`)

	// Artifact -> revision の map を作るよ
	revisionMap := make(map[Artifact]string)
	lines := strings.Split(updateOutput, "\n")
	for _, line := range lines {
		match := libRegexp.FindStringSubmatch(line)
		if match != nil {
			artifact := Artifact{match[1], match[2]}
			revision := match[3]
			if revisionMap[artifact] != "" && revisionMap[artifact] != revision {
				// 複数の revision に依存がある場合にここにくる。
				// compile と test (configuration って呼ぶの？)で
				// 違うバージョンに依存している場合などに起こるみたい?
				//
				// dependencyOverrides では configuration 毎の指定は
				// 効かないような...
				// とりあえず新しい revision を指定することにするけど、
				// 元のビルドと異なる部分が出てくるので注意。
				rev1 := revisionMap[artifact]
				rev2 := revision

				// 本当は文字列の大小比較じゃだめだよね。
				if rev1 > rev2 {
					revision = rev1
				} else {
					revision = rev2
				}
				log.Printf(
					"%v: multiple revision exists: %s, %s. %s is selected.\n",
					artifact, rev1, rev2, revision)
			}
			revisionMap[artifact] = revision
		}
	}

	// % で区切られた文字列にしてからソート
	versionLines := make([]string, len(revisionMap))
	i := 0
	for artifact, revision := range revisionMap {
		versionLines[i] = artifact.format(revision)
		i += 1
	}
	sort.Strings(versionLines)

	// sbt で読める文字列にするよ
	var buf bytes.Buffer
	buf.WriteString("dependencyOverrides ++= Set(\n")
	for i, versionLine := range versionLines {
		buf.WriteString("  ")
		buf.WriteString(versionLine)
		if i != len(versionLines)-1 {
			buf.WriteString(",")
		}
		buf.WriteString("\n")
	}
	buf.WriteString(")\n")
	return buf.String()
}

func main() {
	updateOutput := sbtUpdate()
	depsString := formatDeps(updateOutput)
	ioutil.WriteFile(LockFileName, []byte(depsString), 0644)
}
