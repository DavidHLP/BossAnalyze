#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
: "${JAVA_HOME:?Use a Java 17 JDK}"
mvn -q -f pom.xml dependency:build-classpath -Dmdep.outputFile=target/classpath.txt
mkdir -p target/classes
cp="$(cat target/classpath.txt)"
"$JAVA_HOME/bin/javac" --release 17 -encoding UTF-8 -cp "$cp" -d target/classes StorageCheck.java StorageCheckTest.java
if [[ "${1:-}" == self-test ]]; then
  shift
  "$JAVA_HOME/bin/java" -cp "target/classes:$cp" StorageCheckTest "$@"
else
  "$JAVA_HOME/bin/java" -cp "target/classes:$cp" StorageCheck "$@"
fi
