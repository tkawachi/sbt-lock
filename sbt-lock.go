package main

import "bytes"
import "fmt"
import "log"
import "io/ioutil"
import "os/exec"
import "regexp"
import "sort"
import "strings"

var LockFileName = "lock.sbt"

type LibraryVersion struct {
	group    string
	artifact string
	revision string
}

func (version *LibraryVersion) format() string {
	return fmt.Sprintf("\"%s\" %% \"%s\" %% \"%s\"",
		version.group, version.artifact, version.revision)
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

	// map を使って unique な LibraryVersion 一覧を得るよ
	versionsMap := make(map[LibraryVersion]bool)
	lines := strings.Split(updateOutput, "\n")
	for _, line := range lines {
		match := libRegexp.FindStringSubmatch(line)
		if match != nil {
			version := LibraryVersion{match[1], match[2], match[3]}
			versionsMap[version] = true
		}
	}

	// % で区切られた文字列にしてからソート
	versionLines := make([]string, len(versionsMap))
	i := 0
	for version, _ := range versionsMap {
		versionLines[i] = version.format()
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
