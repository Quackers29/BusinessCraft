#!/usr/bin/env bash
# Run every showcase mode once (Forge then Fabric). For CI / release smoke.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LOADER="${1:-forge}"
for mode in all town block all-views two-towns testbed; do
  echo "=== smoke $mode $LOADER ==="
  bash "$ROOT/tools/showcase.sh" "$mode" "$LOADER"
done
