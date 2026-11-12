#!/usr/bin/env bash
# The regression set for one loader: all, town, block, all-views, two-towns, testbed.
# Same as: bash tools/showcase-run.sh --regression --loader <forge|fabric>
# tourists, tourist-screen, jade and map-sweep stay separate: they are long, and
# tools/showcase-run.sh picks them when a change touches them.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
exec bash "$ROOT/tools/showcase-run.sh" --regression --loader "${1:-forge}"
