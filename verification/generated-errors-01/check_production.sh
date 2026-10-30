#!/bin/bash
set -euo pipefail
project="$(cd "$(dirname "$0")/../.." && pwd)"
scratch="$project/build/tmp/java-build-tools"
mkdir -p "$scratch"
export TMPDIR="$scratch" TMP="$scratch" TEMP="$scratch"
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Djava.io.tmpdir=$scratch"
arguments="SerdeNativeOracle --profile generated-errors-01"
arguments+=" --root \"$project\""
for argument in "$@"; do
  argument="${argument//\\/\\\\}"
  argument="${argument//\"/\\\"}"
  arguments+=" \"$argument\""
done
exec "$project/gradlew" -p "$project/buildSrc" runBuildTool --no-daemon --no-configuration-cache --console=plain --max-workers=2 --args="$arguments"
