#!/bin/bash
# 本机构建 EorzeaPhone：没有 wrapper，用独立 Gradle + AF_UNIX 补丁。
# 用法: bash szj_build.sh :app:compileDebugKotlin
export JAVA_HOME="/c/Users/Administrator/.gradle_tools/jdk-17-ms/jdk-17.0.20+8"
export JDK_JAVA_OPTIONS="--patch-module java.base=C:/Users/Administrator/.gradle_tools/afunix-patch"
export PATH="$JAVA_HOME/bin:/c/Users/Administrator/.gradle_tools/gradle-8.11.1/bin:$PATH"
export TMP="$(pwd)/.buildtmp"
export TEMP="$TMP"
mkdir -p "$TMP"
cd "$(dirname "$0")" || exit 1
TASK="${1:-:app:compileDebugKotlin}"
gradle "$TASK" --console=plain --no-daemon > szj_full.log 2>&1
CODE=$?
echo "exit=$CODE"
grep -E "^e: |error:|FAILURE|BUILD " szj_full.log | sort -u | head -40
exit $CODE
