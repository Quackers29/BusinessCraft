#!/usr/bin/env bash
# Run the showcase modes that assert real output for one loader.
# `all` checks showcase-shots.json (every non-warmup shot), not a fixed count.
# Pass forge or fabric. Run it twice to cover both loaders.
# tourists, tourist-screen, and map-sweep stay separate: they are long and
# already have their own manifests.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LOADER="${1:-forge}"
for mode in all town block all-views two-towns testbed; do
  echo "=== smoke $mode $LOADER ==="
  bash "$ROOT/tools/showcase.sh" "$mode" "$LOADER"
done
