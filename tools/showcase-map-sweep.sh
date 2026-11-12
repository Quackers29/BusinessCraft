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

bc_ensure_display "$ROOT/tools/showcase-map-sweep.sh" "$@"
START_SECONDS=$SECONDS
showcase_output_init "map-sweep-batch" "$LOADER" "$ROOT"
SWEEP_ROOT="$SHOWCASE_RUN_DIR"
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
    local run_log="$RUNS_DIR/${tag}.log"
    # PASS or FAIL comes from showcase.sh's exit code, never from its text.
    if SHOWCASE_OUTPUT_ROOT="$RUNS_DIR" SHOWCASE_GUI_SCALE="$scale" SHOWCASE_WIDTH="$w" SHOWCASE_HEIGHT="$h" \
        bash "$ROOT/tools/showcase.sh" map-sweep "$LOADER" "$tag" >"$run_log" 2>&1; then
        cat "$run_log" >>"$BATCH_LOG"
        echo "PASS $tag" | tee -a "$BATCH_LOG"
        return 0
    fi
    cat "$run_log" >>"$BATCH_LOG"
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

STATUS=PASS
[[ "$failures" -eq 0 ]] || STATUS=FAIL
SHOTS="$(find "$RUNS_DIR" -name 'showcase_map_*.png' | wc -l | tr -d ' ')"
SWEEP_FAILURES=()
[[ "$failures" -eq 0 ]] || SWEEP_FAILURES+=("$failures map sweep case(s) failed, see $BATCH_LOG")
showcase_write_result "$SWEEP_ROOT" "map-sweep-batch" "$LOADER" "$STATUS" "$((SECONDS - START_SECONDS))" "$SHOTS" \
    "${SWEEP_FAILURES[@]+"${SWEEP_FAILURES[@]}"}"
if [[ "$failures" -ne 0 ]]; then
    exit 1
fi
