#!/usr/bin/env bash
# Capture town map PNGs across resolutions and GUI scales (Forge/Fabric).
#
#   bash tools/showcase-map-sweep.sh forge
#   bash tools/showcase-map-sweep.sh fabric
#
# Sweeps:
#   1920x1080 — guiScale 1,2,3,4,auto
#   854x480   — guiScale 4, auto
#   1280x720  — guiScale 4, auto
#
# Output: showcase-output/<timestamp>_map-sweep_<loader>/runs/...

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=tools/showcase-output-lib.sh
source "$ROOT/tools/showcase-output-lib.sh"

LOADER="${1:-forge}"
case "$LOADER" in
    forge | fabric) ;;
    *)
        echo "Usage: bash tools/showcase-map-sweep.sh [forge|fabric]" >&2
        exit 1
        ;;
esac

SWEEP_ROOT="$(showcase_output_init "map-sweep-batch" "$LOADER" "$ROOT")"
RUNS_DIR="$SWEEP_ROOT/runs"
mkdir -p "$RUNS_DIR"
BATCH_LOG="$SWEEP_ROOT/logs/batch.log"
: >"$BATCH_LOG"

run_one() {
    local w="$1"
    local h="$2"
    local scale="$3"
    local tag="${w}x${h}_gui${scale}"
    echo "=== map sweep $LOADER $tag ===" | tee -a "$BATCH_LOG"
    local run_sub
    run_sub="$(SHOWCASE_OUTPUT_ROOT="$RUNS_DIR" SHOWCASE_GUI_SCALE="$scale" SHOWCASE_WIDTH="$w" SHOWCASE_HEIGHT="$h" \
        bash "$ROOT/tools/showcase.sh" map-sweep "$LOADER" "$tag" 2>&1)" || true
    echo "$run_sub" >>"$BATCH_LOG"
    if echo "$run_sub" | grep -q "Showcase done"; then
        echo "PASS $tag" | tee -a "$BATCH_LOG"
        return 0
    fi
    echo "FAIL $tag" | tee -a "$BATCH_LOG"
    return 1
}

failures=0
for scale in 1 2 3 4 auto; do
    run_one 1920 1080 "$scale" || failures=$((failures + 1))
done
for scale in 4 auto; do
    run_one 854 480 "$scale" || failures=$((failures + 1))
    run_one 1280 720 "$scale" || failures=$((failures + 1))
done

python3 - "$SWEEP_ROOT" "$BATCH_LOG" <<'PY'
import json, re, sys
root, blog = sys.argv[1:3]
results = []
for line in open(blog):
    m = re.match(r"(PASS|FAIL) (\S+)", line.strip())
    if m:
        results.append({"case": m.group(2), "pass": m.group(1) == "PASS"})
json.dump({"results": results}, open(f"{root}/map-sweep-summary.json", "w"), indent=2)
PY

showcase_append_index "$ROOT/showcase-output" "$SWEEP_ROOT" "map-sweep-batch" "$LOADER" "$([[ $failures -eq 0 ]] && echo PASS || echo FAIL)"

if [[ "$failures" -ne 0 ]]; then
    echo "$failures map sweep case(s) failed — see $BATCH_LOG" >&2
    exit 1
fi
echo "Map sweep batch: $SWEEP_ROOT"
