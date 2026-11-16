#!/bin/bash
set -euo pipefail
project="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$project"
exec "$project/../tools/java-build-tool.sh" SerdeNativeOracle --profile generated-errors-01 "$@"
