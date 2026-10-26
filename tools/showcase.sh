#!/usr/bin/env bash
# Showcase runner. Starts a dev client in a throwaway superflat copy, lets the
# harness build a scene, take screenshots, and quit.
#
#   bash tools/showcase.sh                       # every shot (Forge)
#   bash tools/showcase.sh town                  # the four town tabs
#   bash tools/showcase.sh block                 # front and back of the block
#   bash tools/showcase.sh block fabric          # same scene on Fabric
#   bash tools/showcase.sh all-views forge
#   bash tools/showcase.sh two-towns forge
#   bash tools/showcase.sh testbed forge trade-contract
#   bash tools/showcase.sh map-sweep forge 1920x1080_gui4
#   bash tools/showcase.sh tourists forge          # every tourist look, front and side
#   bash tools/showcase.sh tourist-screen forge   # vanilla merchant screen, then the tourist screen
#   bash tools/showcase.sh tourist-screen-854-gui2 forge   # same shots at 854x480, gui scale 2
#   bash tools/showcase.sh tourist-screen-854-gui3 forge   # same shots at 854x480, gui scale 3
#   bash tools/showcase.sh tourist-screen-720-gui2 forge   # same shots at 1280x720, gui scale 2
#   bash tools/showcase-map-sweep.sh forge       # full resolution/gui matrix
#
# Each run writes:
#   showcase-output/<YYYY-MM-DD_HHMMSS>_<mode>_<loader>/
#     screenshots/  logs/client.log  logs/server.log  logs/gradle-console.log
#     run-meta.json  (+ mode-specific report JSON / steps.log / index.html)
#
# See tools/SHOWCASE-TESTBED.md and showcase-output/index.html (latest symlink).

set -euo pipefail

SHOWCASE_WIDTH="${SHOWCASE_WIDTH:-1920}"
SHOWCASE_HEIGHT="${SHOWCASE_HEIGHT:-1080}"

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
# shellcheck source=tools/showcase-output-lib.sh
source "$ROOT/tools/showcase-output-lib.sh"

SCENES="${1:-all}"
shift || true
LOADER="${1:-forge}"
shift || true

# Small-window aliases. Gradle still receives tourist-screen; the folder name keeps the size.
MODE_TAG=""
case "$SCENES" in
    tourist-screen-854-gui2)
        MODE_TAG="$SCENES"
        SCENES="tourist-screen"
        SHOWCASE_WIDTH=854
        SHOWCASE_HEIGHT=480
        SHOWCASE_GUI_SCALE="${SHOWCASE_GUI_SCALE:-2}"
        ;;
    tourist-screen-854-gui3)
        MODE_TAG="$SCENES"
        SCENES="tourist-screen"
        SHOWCASE_WIDTH=854
        SHOWCASE_HEIGHT=480
        SHOWCASE_GUI_SCALE="${SHOWCASE_GUI_SCALE:-3}"
        ;;
    tourist-screen-720-gui2)
        MODE_TAG="$SCENES"
        SCENES="tourist-screen"
        SHOWCASE_WIDTH=1280
        SHOWCASE_HEIGHT=720
        SHOWCASE_GUI_SCALE="${SHOWCASE_GUI_SCALE:-2}"
        ;;
esac
TESTBED_CHECKS="all"
MAP_SWEEP_TAG="map"
if [[ "$SCENES" == "testbed" ]]; then
    TESTBED_CHECKS="${*:-all}"
elif [[ "$SCENES" == "map-sweep" ]]; then
    MAP_SWEEP_TAG="${1:-${SHOWCASE_WIDTH}x${SHOWCASE_HEIGHT}_map}"
fi

case "$LOADER" in
    forge | fabric) ;;
    *)
        echo "Unknown loader: $LOADER (use forge or fabric)" >&2
        exit 1
        ;;
esac

MODE="${MODE_TAG:-$SCENES}"
showcase_output_init "$MODE" "$LOADER" "$ROOT"
echo "Output: $SHOWCASE_RUN_DIR"

read_gradle_showcase_template() {
    if [[ -f gradle.properties ]]; then
        grep -E '^showcaseTemplate=' gradle.properties 2>/dev/null | tail -1 | cut -d= -f2- | tr -d '\r' || true
    fi
}

if [[ -n "${SHOWCASE_TEMPLATE:-}" ]]; then
    TEMPLATE="$SHOWCASE_TEMPLATE"
else
    TEMPLATE="$(read_gradle_showcase_template)"
    TEMPLATE="${TEMPLATE:-/opt/businesscraft/superflat-template}"
fi

MC_RUN="$ROOT/$LOADER/run"
WORLD="$MC_RUN/saves/zz_showcase"
SHOTDIR="$MC_RUN/screenshots"
LOG="$SHOWCASE_RUN_DIR/logs/gradle-console.log"
OPTIONS="$MC_RUN/options.txt"
GRADLE_TASK=":$LOADER:runClient"

if [[ ! -f "$TEMPLATE/level.dat" ]]; then
    echo "Showcase template not found: $TEMPLATE" >&2
    exit 1
fi

if [[ -e /tmp/.X11-unix/X1 ]]; then
    export DISPLAY="${DISPLAY:-:1}"
fi

mkdir -p "$(dirname "$WORLD")"
rm -rf "$WORLD"
cp -a "$TEMPLATE" "$WORLD"
rm -f "$WORLD/session.lock"
mkdir -p "$SHOTDIR"
rm -f "$SHOTDIR"/showcase_*.png "$SHOTDIR"/showcase_view_*.png "$SHOTDIR"/showcase_map_*.png "$SHOTDIR"/showcase_testbed_*.png
rm -f "$MC_RUN/showcase-views-manifest.json" "$MC_RUN/showcase-two-towns-report.json" "$MC_RUN/showcase-testbed-report.json"

mkdir -p "$MC_RUN"
cp "$ROOT/tools/showcase-options.txt" "$OPTIONS"
if [[ -n "${SHOWCASE_GUI_SCALE:-}" ]]; then
    if [[ "$SHOWCASE_GUI_SCALE" == "auto" ]]; then
        sed -i 's/^guiScale:.*/guiScale:0/' "$OPTIONS"
    else
        sed -i "s/^guiScale:.*/guiScale:${SHOWCASE_GUI_SCALE}/" "$OPTIONS"
    fi
fi

GRADLE_PROPS=(
    -PquickPlay=zz_showcase
    -Pshowcase="$SCENES"
    -PshowcaseTemplate="$TEMPLATE"
    -PshowcaseWidth="$SHOWCASE_WIDTH"
    -PshowcaseHeight="$SHOWCASE_HEIGHT"
)
if [[ "$SCENES" == "all-views" || "$SCENES" == "two-towns" || "$SCENES" == "testbed" || "$SCENES" == "map-sweep" ]]; then
    GRADLE_PROPS+=(-PshowcaseLoader="$LOADER")
    export BUSINESSCRAFT_SHOWCASE_LOADER="$LOADER"
fi
if [[ "$SCENES" == "testbed" ]]; then
    GRADLE_PROPS+=(-PtestbedChecks="$TESTBED_CHECKS")
fi
if [[ "$SCENES" == "map-sweep" ]]; then
    GRADLE_PROPS+=(-PshowcaseMapTag="$MAP_SWEEP_TAG")
fi

set +e
bash ./gradlew --no-daemon "$GRADLE_TASK" "${GRADLE_PROPS[@]}" --console=plain >"$LOG" 2>&1
code=$?
set -e

showcase_collect_logs "$MC_RUN" "$LOG" "$SHOWCASE_RUN_DIR"
showcase_copy_screenshots "$SHOTDIR" "$SHOWCASE_RUN_DIR"

count="$(find "$SHOWCASE_RUN_DIR/screenshots" -maxdepth 1 -name 'showcase_*.png' 2>/dev/null | wc -l | tr -d ' ')"
view_count="$(find "$SHOWCASE_RUN_DIR/screenshots" -maxdepth 1 -name 'showcase_view_*.png' 2>/dev/null | wc -l | tr -d ' ')"
map_count="$(find "$SHOWCASE_RUN_DIR/screenshots" -maxdepth 1 -name 'showcase_map_*.png' 2>/dev/null | wc -l | tr -d ' ')"
echo "Showcase ($LOADER) shots: $count views: $view_count map: $map_count (gradle exit $code)"

MANIFEST="$MC_RUN/showcase-views-manifest.json"
ALL_VIEWS_EXPECT=19

case "$SCENES" in
    "" | all | ALL) expect=6 ;;
    town) expect=4 ;;
    tourists) expect=1 ;;
    block) expect=2 ;;
    all-views | two-towns | testbed | map-sweep | tourist-screen) expect=0 ;;
    *) expect=0 ;;
esac

if grep -n "Showcase failed" "$LOG"; then
    echo "Showcase reported a failure" >&2
    code=1
fi

grep -nE "ERROR|Exception" "$LOG" | grep -v "Unable to delete file" | grep -v "ALSA" | head -n 30 || true

TWO_TOWNS_REPORT="$MC_RUN/showcase-two-towns-report.json"
TESTBED_REPORT="$MC_RUN/showcase-testbed-report.json"

write_testbed_checks_log() {
    local report="$1"
    local log_out="$SHOWCASE_RUN_DIR/logs/checks.log"
    if [[ ! -f "$report" ]]; then
        echo "Missing testbed report: $report" >&2
        return 1
    fi
    cp -f "$report" "$SHOWCASE_RUN_DIR/showcase-testbed-report.json"
    {
        echo "# testbed ($LOADER) checks=$TESTBED_CHECKS"
        python3 - "$report" <<'PY'
import json, sys
report = json.load(open(sys.argv[1]))
for row in report.get("results", []):
    mark = "PASS" if row.get("pass") else "FAIL"
    print(f"  {row.get('check')}: {mark}")
sys.exit(1 if report.get("exitCode", 0) != 0 else 0)
PY
    } >"$log_out" || return 1
    return 0
}

write_two_towns_steps_log() {
    local report="$1"
    local log_out="$SHOWCASE_RUN_DIR/logs/steps.log"
    if [[ ! -f "$report" ]]; then
        echo "Missing two-towns report: $report" >&2
        return 1
    fi
    cp -f "$report" "$SHOWCASE_RUN_DIR/showcase-two-towns-report.json"
    if [[ -f "$MANIFEST" ]]; then
        cp -f "$MANIFEST" "$SHOWCASE_RUN_DIR/showcase-views-manifest.json"
        bash "$ROOT/tools/showcase-generate-index.sh" "$SHOWCASE_RUN_DIR/index.html" "$SHOWCASE_RUN_DIR/showcase-views-manifest.json"
    fi
    {
        echo "# two-towns ($LOADER)"
        python3 - "$report" <<'PY'
import json, sys
report = json.load(open(sys.argv[1]))
for step in report.get("steps", []):
    mark = "PASS" if step.get("pass") else "FAIL"
    print(f"  {step.get('step')}: {mark}")
sys.exit(1 if report.get("exitCode", 0) != 0 else 0)
PY
    } >"$log_out" || return 1
    return 0
}

if [[ "$SCENES" == "all-views" ]]; then
    if [[ ! -f "$MANIFEST" ]]; then
        echo "Missing showcase manifest: $MANIFEST" >&2
        code=1
    fi
    if [[ "$view_count" -lt "$ALL_VIEWS_EXPECT" ]]; then
        echo "Expected at least $ALL_VIEWS_EXPECT view PNGs, found $view_count" >&2
        code=1
    fi
    if grep -q '"ok": false' "$MANIFEST" 2>/dev/null; then
        echo "One or more routes failed (see manifest)" >&2
        code=1
    fi
    cp -f "$MANIFEST" "$SHOWCASE_RUN_DIR/showcase-views-manifest.json"
    bash "$ROOT/tools/showcase-generate-index.sh" "$SHOWCASE_RUN_DIR/index.html" "$SHOWCASE_RUN_DIR/showcase-views-manifest.json"
elif [[ "$SCENES" == "two-towns" ]]; then
    [[ -f "$TWO_TOWNS_REPORT" ]] || code=1
    if [[ "$view_count" -lt "$ALL_VIEWS_EXPECT" ]]; then
        code=1
    fi
    if [[ -f "$MANIFEST" ]] && grep -q '"ok": false' "$MANIFEST"; then
        code=1
    fi
    if [[ -f "$TWO_TOWNS_REPORT" ]]; then
        write_two_towns_steps_log "$TWO_TOWNS_REPORT" || code=1
    fi
elif [[ "$SCENES" == "testbed" ]]; then
    [[ -f "$TESTBED_REPORT" ]] || code=1
    if [[ -f "$TESTBED_REPORT" ]]; then
        write_testbed_checks_log "$TESTBED_REPORT" || code=1
    fi
elif [[ "$SCENES" == "map-sweep" ]]; then
    if [[ "$map_count" -lt 1 ]]; then
        echo "Expected map sweep PNG" >&2
        code=1
    fi
    if ! grep -q "SHOWCASE_MAP_SWEEP_CAPTURED" "$LOG"; then
        echo "Map sweep did not capture TownMapModal" >&2
        code=1
    fi
elif [[ "$SCENES" == "tourist-screen" ]]; then
    if [[ ! -f "$MANIFEST" ]]; then
        echo "Missing showcase manifest: $MANIFEST" >&2
        code=1
    fi
    if [[ "$view_count" -lt 5 ]]; then
        echo "Expected at least 5 tourist screen views, found $view_count" >&2
        code=1
    fi
    if grep -q '"ok": false' "$MANIFEST" 2>/dev/null; then
        echo "One or more tourist screen routes failed (see manifest)" >&2
        code=1
    fi
    if [[ -f "$MANIFEST" ]]; then
        cp -f "$MANIFEST" "$SHOWCASE_RUN_DIR/showcase-views-manifest.json"
        bash "$ROOT/tools/showcase-generate-index.sh" "$SHOWCASE_RUN_DIR/index.html" "$SHOWCASE_RUN_DIR/showcase-views-manifest.json"
    fi
elif [[ "$SCENES" == "tourists" ]]; then
    MANIFEST_T="$MC_RUN/showcase-tourists.json"
    if [[ ! -f "$MANIFEST_T" ]]; then
        echo "Missing tourist showcase manifest: $MANIFEST_T" >&2
        code=1
    else
        cp -f "$MANIFEST_T" "$SHOWCASE_RUN_DIR/showcase-tourists.json"
        if ! python3 - "$MANIFEST_T" "$SHOWCASE_RUN_DIR/screenshots" <<'PY'
import json, sys
from pathlib import Path
manifest = json.load(open(sys.argv[1]))
shots = manifest.get("shots", [])
folder = Path(sys.argv[2])
missing = [s for s in shots if not (folder / f"showcase_{s}.png").is_file()]
front = [s for s in shots if s.endswith("_front")]
side = [s for s in shots if s.endswith("_side")]
if len(shots) < 2 or not front or not side:
    print("expected front and side tourist shots", file=sys.stderr)
    sys.exit(1)
if missing:
    print("missing shots: " + ", ".join(missing), file=sys.stderr)
    sys.exit(1)
print(f"tourist shots ok: {len(shots)}")
PY
        then
            code=1
        fi
    fi
else
    if [[ "$count" -eq 0 ]]; then
        echo "No showcase screenshots were written" >&2
        exit 1
    fi
    if [[ "$expect" -gt 0 && "$count" -ne "$expect" ]]; then
        echo "Expected $expect shots, found $count" >&2
        code=1
    fi
fi

RUN_STATUS="PASS"
[[ "$code" -eq 0 ]] || RUN_STATUS="FAIL"
showcase_append_index "${SHOWCASE_OUTPUT_ROOT}" "$SHOWCASE_RUN_DIR" "$MODE" "$LOADER" "$RUN_STATUS"

if [[ -n "${SHOWCASE_ARTIFACT_DIR:-}" ]]; then
    mkdir -p "$SHOWCASE_ARTIFACT_DIR"
    # cp -a preserves ownership and fails on artifact dirs the agent cannot chown.
    if ! cp -a "$SHOWCASE_RUN_DIR/." "$SHOWCASE_ARTIFACT_DIR/"; then
        cp -R --no-preserve=mode,ownership "$SHOWCASE_RUN_DIR/." "$SHOWCASE_ARTIFACT_DIR/"
    fi
fi

if [[ "$code" -ne 0 ]]; then
    echo "Client exit code $code" >&2
    exit "$code"
fi

rm -rf "$WORLD"
echo "Showcase done"
