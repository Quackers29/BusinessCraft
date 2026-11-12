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
#   bash tools/showcase.sh jade forge              # Jade tooltips (Town Centre, platforms, tourist)
#   bash tools/showcase.sh tourist-screen forge   # vanilla merchant screen, then the tourist screen
#   bash tools/showcase.sh production-tooltip forge   # upgrades list with the first tooltip open
#   bash tools/showcase.sh tourist-screen-854-gui2 forge   # same shots at 854x480, gui scale 2
#   bash tools/showcase.sh tourist-screen-854-gui3 forge   # same shots at 854x480, gui scale 3
#   bash tools/showcase.sh tourist-screen-720-gui2 forge   # same shots at 1280x720, gui scale 2
#   bash tools/showcase.sh routes forge town/map,platform   # all-views limited to these routes
#   bash tools/showcase-map-sweep.sh forge       # full resolution/gui matrix
#
# This runs ONE scene. To pick scenes from what changed, or to run the whole regression
# set, use tools/showcase-run.sh (see tools/SHOWCASE-TESTBED.md).
#
# Each run writes:
#   showcase-output/<YYYY-MM-DD_HHMMSS>_<mode>_<loader>/
#     screenshots/  logs/client.log  logs/server.log  logs/gradle-console.log
#     run-meta.json  result.json  (+ mode-specific report JSON / steps.log / index.html)
# and the last stdout line is  RESULT <mode> <loader> PASS|FAIL <n>s shots=<n> dir=<run dir>
#
# See tools/SHOWCASE-TESTBED.md and showcase-output/index.html (latest symlink).

set -euo pipefail

SHOWCASE_WIDTH="${SHOWCASE_WIDTH:-1920}"
SHOWCASE_HEIGHT="${SHOWCASE_HEIGHT:-1080}"

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
# shellcheck source=tools/showcase-output-lib.sh
source "$ROOT/tools/showcase-output-lib.sh"
bc_ensure_display "$ROOT/tools/showcase.sh" "$@"
START_SECONDS=$SECONDS
FAILURES=()
code=0
fail_run() {
    echo "$1" >&2
    FAILURES+=("$1")
    code=1
}

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
ROUTES_SPEC=""
if [[ "$SCENES" == "routes" ]]; then
    ROUTES_SPEC="${1:-}"
    if [[ -z "$ROUTES_SPEC" ]]; then
        echo "Usage: bash tools/showcase.sh routes <loader> <route[,route...]>  (e.g. town/map,platform)" >&2
        exit 1
    fi
    MODE_TAG="routes"
    SCENES="all-views"
elif [[ "$SCENES" == "testbed" ]]; then
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

TEMPLATE="$(bc_ensure_template)" || {
    echo "Showcase template could not be built" >&2
    exit 1
}

MC_RUN="$ROOT/$LOADER/run"
WORLD="$MC_RUN/saves/zz_showcase"
SHOTDIR="$MC_RUN/screenshots"
LOG="$SHOWCASE_RUN_DIR/logs/gradle-console.log"
OPTIONS="$MC_RUN/options.txt"
GRADLE_TASK=":$LOADER:runClient"

mkdir -p "$(dirname "$WORLD")"
rm -rf "$WORLD"
cp -a "$TEMPLATE" "$WORLD"
rm -f "$WORLD/session.lock"
mkdir -p "$SHOTDIR"
rm -f "$SHOTDIR"/showcase_*.png "$SHOTDIR"/showcase_view_*.png "$SHOTDIR"/showcase_map_*.png "$SHOTDIR"/showcase_testbed_*.png
rm -f "$MC_RUN/showcase-views-manifest.json" "$MC_RUN/showcase-two-towns-report.json" "$MC_RUN/showcase-testbed-report.json" \
    "$MC_RUN/showcase-shots.json"

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
if [[ -n "$ROUTES_SPEC" ]]; then
    GRADLE_PROPS+=(-PshowcaseRoutes="$ROUTES_SPEC")
fi

set +e
bc_gradle "$GRADLE_TASK" "${GRADLE_PROPS[@]}" --console=plain >"$LOG" 2>&1
code=$?
set -e

showcase_collect_logs "$MC_RUN" "$LOG" "$SHOWCASE_RUN_DIR"
showcase_copy_screenshots "$SHOTDIR" "$SHOWCASE_RUN_DIR"

count="$(find "$SHOWCASE_RUN_DIR/screenshots" -maxdepth 1 -name 'showcase_*.png' 2>/dev/null | wc -l | tr -d ' ')"
view_count="$(find "$SHOWCASE_RUN_DIR/screenshots" -maxdepth 1 -name 'showcase_view_*.png' 2>/dev/null | wc -l | tr -d ' ')"
map_count="$(find "$SHOWCASE_RUN_DIR/screenshots" -maxdepth 1 -name 'showcase_map_*.png' 2>/dev/null | wc -l | tr -d ' ')"
echo "Showcase ($LOADER) shots: $count views: $view_count map: $map_count (gradle exit $code)"

MANIFEST="$MC_RUN/showcase-views-manifest.json"
SHOT_MANIFEST="$MC_RUN/showcase-shots.json"
ALL_VIEWS_EXPECT=19 # two-towns only; all-views reads the manifest

if [[ "$code" -ne 0 ]]; then
    FAILURES+=("gradle exit $code")
fi
if grep -q "Showcase failed" "$LOG"; then
    fail_run "Showcase reported a failure: $(grep -m1 'Showcase failed' "$LOG" | cut -c1-200)"
fi

# Unexpected ERROR lines from the Minecraft log (not the Gradle console), minus known noise.
PROBLEMS="$(showcase_log_problems "$SHOWCASE_RUN_DIR/logs/client.log")"
if [[ -n "$PROBLEMS" ]]; then
    echo "Client log ERROR lines (known noise hidden, see $SHOWCASE_RUN_DIR/logs/client.log):" >&2
    echo "$PROBLEMS" >&2
fi

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

check_views_manifest() {
    # Every selected route must have an ok manifest row and its PNG. A run without a route
    # filter must have selected the whole registry.
    python3 - "$MANIFEST" "$SHOWCASE_RUN_DIR/screenshots" "$ROUTES_SPEC" <<'PY'
import json, sys
from pathlib import Path
manifest = json.load(open(sys.argv[1]))
folder, spec = Path(sys.argv[2]), sys.argv[3]
selected = manifest.get("selected") or []
rows = {r.get("route"): r for r in manifest.get("routes") or []}
problems = []
if not manifest.get("complete"):
    problems.append("manifest is not complete")
if not selected:
    problems.append("manifest selected no routes")
if not spec and selected != manifest.get("registry"):
    problems.append("full run did not select every registered route")
for name in selected:
    row = rows.get(name)
    if row is None or not row.get("ok") or not (folder / str(row.get("screenshot", ""))).is_file():
        problems.append(f"route {name} failed or has no PNG")
if problems:
    print("; ".join(problems), file=sys.stderr)
    sys.exit(1)
print(f"views ok: {len(selected)}")
PY
}

if [[ "$SCENES" == "all-views" ]]; then
    if [[ ! -f "$MANIFEST" ]]; then
        fail_run "Missing showcase manifest: $MANIFEST"
    else
        check_views_manifest || fail_run "View manifest check failed"
        cp -f "$MANIFEST" "$SHOWCASE_RUN_DIR/showcase-views-manifest.json"
        bash "$ROOT/tools/showcase-generate-index.sh" "$SHOWCASE_RUN_DIR/index.html" "$SHOWCASE_RUN_DIR/showcase-views-manifest.json"
    fi
elif [[ "$SCENES" == "two-towns" ]]; then
    [[ -f "$TWO_TOWNS_REPORT" ]] || fail_run "Missing two-towns report"
    if [[ "$view_count" -lt "$ALL_VIEWS_EXPECT" ]]; then
        fail_run "Expected at least $ALL_VIEWS_EXPECT view PNGs, found $view_count"
    fi
    if [[ -f "$MANIFEST" ]] && grep -q '"ok": false' "$MANIFEST"; then
        fail_run "One or more routes failed (see manifest)"
    fi
    if [[ -f "$TWO_TOWNS_REPORT" ]]; then
        write_two_towns_steps_log "$TWO_TOWNS_REPORT" || fail_run "Two-towns steps failed"
    fi
elif [[ "$SCENES" == "testbed" ]]; then
    [[ -f "$TESTBED_REPORT" ]] || fail_run "Missing testbed report"
    if [[ -f "$TESTBED_REPORT" ]]; then
        write_testbed_checks_log "$TESTBED_REPORT" || fail_run "A testbed check failed"
    fi
elif [[ "$SCENES" == "map-sweep" ]]; then
    if [[ "$map_count" -lt 1 ]]; then
        fail_run "Expected map sweep PNG"
    fi
    if ! grep -q "SHOWCASE_MAP_SWEEP_CAPTURED" "$LOG"; then
        fail_run "Map sweep did not capture TownMapModal"
    fi
elif [[ "$SCENES" == "tourist-screen" ]]; then
    if [[ ! -f "$MANIFEST" ]]; then
        fail_run "Missing showcase manifest: $MANIFEST"
    fi
    if [[ "$view_count" -lt 5 ]]; then
        fail_run "Expected at least 5 tourist screen views, found $view_count"
    fi
    if grep -q '"ok": false' "$MANIFEST" 2>/dev/null; then
        fail_run "One or more tourist screen routes failed (see manifest)"
    fi
    if [[ -f "$MANIFEST" ]]; then
        cp -f "$MANIFEST" "$SHOWCASE_RUN_DIR/showcase-views-manifest.json"
        bash "$ROOT/tools/showcase-generate-index.sh" "$SHOWCASE_RUN_DIR/index.html" "$SHOWCASE_RUN_DIR/showcase-views-manifest.json"
    fi
elif [[ "$SCENES" == "production-tooltip" ]]; then
    if [[ ! -f "$MANIFEST" ]]; then
        fail_run "Missing showcase manifest: $MANIFEST"
    fi
    if [[ "$view_count" -lt 1 ]]; then
        fail_run "Expected a production tooltip view, found $view_count"
    fi
    if grep -q '"ok": false' "$MANIFEST" 2>/dev/null; then
        fail_run "Production tooltip route failed (see manifest)"
    fi
    if [[ -f "$MANIFEST" ]]; then
        cp -f "$MANIFEST" "$SHOWCASE_RUN_DIR/showcase-views-manifest.json"
        bash "$ROOT/tools/showcase-generate-index.sh" "$SHOWCASE_RUN_DIR/index.html" "$SHOWCASE_RUN_DIR/showcase-views-manifest.json"
    fi
elif [[ "$SCENES" == "jade" ]]; then
    MANIFEST_J="$MC_RUN/showcase-jade.json"
    if [[ ! -f "$MANIFEST_J" ]]; then
        fail_run "Missing jade showcase manifest: $MANIFEST_J"
    else
        cp -f "$MANIFEST_J" "$SHOWCASE_RUN_DIR/showcase-jade.json"
        if ! python3 - "$MANIFEST_J" "$SHOWCASE_RUN_DIR/screenshots" <<'PY'
import json, sys
from pathlib import Path
manifest = json.load(open(sys.argv[1]))
shots = manifest.get("shots", [])
folder = Path(sys.argv[2])
if not manifest.get("jadePresent", False):
    print("Jade was not loaded on the client", file=sys.stderr)
    sys.exit(1)
missing = [s for s in shots if not (folder / f"showcase_{s}.png").is_file()]
if missing:
    print("missing jade shots: " + ", ".join(missing), file=sys.stderr)
    sys.exit(1)
print(f"jade shots ok: {len(shots)}")
PY
        then
            fail_run "Shot manifest check failed (see messages above)"
        fi
    fi
elif [[ "$SCENES" == "tourists" ]]; then
    MANIFEST_T="$MC_RUN/showcase-tourists.json"
    if [[ ! -f "$MANIFEST_T" ]]; then
        fail_run "Missing tourist showcase manifest: $MANIFEST_T"
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
            fail_run "Shot manifest check failed (see messages above)"
        fi
    fi
else
    if [[ ! -f "$SHOT_MANIFEST" ]]; then
        fail_run "Missing shot manifest: $SHOT_MANIFEST"
    else
        cp -f "$SHOT_MANIFEST" "$SHOWCASE_RUN_DIR/showcase-shots.json"
        if ! python3 - "$SHOT_MANIFEST" "$SHOWCASE_RUN_DIR/screenshots" <<'PY'
import json, sys
from pathlib import Path
manifest = json.load(open(sys.argv[1]))
folder = Path(sys.argv[2])
expected = manifest.get("expected") or []
by_name = {row.get("name"): row for row in manifest.get("shots") or []}
if not expected:
    print("shot manifest has no expected shots", file=sys.stderr)
    sys.exit(1)
if manifest.get("pending", 0) != 0 or manifest.get("ok") is not True:
    print("shot manifest is not ok", file=sys.stderr)
    sys.exit(1)
missing = []
for name in expected:
    row = by_name.get(name)
    file_name = f"showcase_{name}.png"
    if row is None or not row.get("ok") or not (folder / file_name).is_file():
        missing.append(name)
if missing:
    print("shots failed or missing: " + ", ".join(missing), file=sys.stderr)
    sys.exit(1)
print(f"planned shots ok: {len(expected)}")
PY
        then
            fail_run "Shot manifest check failed (see messages above)"
        fi
    fi
fi

RUN_STATUS="PASS"
[[ "$code" -eq 0 ]] || RUN_STATUS="FAIL"
SHOT_TOTAL="$count"
RESULT_LINE="$(showcase_write_result "$SHOWCASE_RUN_DIR" "$MODE" "$LOADER" "$RUN_STATUS" "$((SECONDS - START_SECONDS))" "$SHOT_TOTAL" "${FAILURES[@]+"${FAILURES[@]}"}")"
showcase_append_index "${SHOWCASE_OUTPUT_ROOT}" "$SHOWCASE_RUN_DIR" "$MODE" "$LOADER" "$RUN_STATUS"

if [[ -n "${SHOWCASE_ARTIFACT_DIR:-}" ]]; then
    mkdir -p "$SHOWCASE_ARTIFACT_DIR"
    # cp -a preserves ownership and fails on artifact dirs the agent cannot chown.
    if ! cp -a "$SHOWCASE_RUN_DIR/." "$SHOWCASE_ARTIFACT_DIR/"; then
        cp -R --no-preserve=mode,ownership "$SHOWCASE_RUN_DIR/." "$SHOWCASE_ARTIFACT_DIR/"
    fi
fi

if [[ "$code" -ne 0 ]]; then
    echo "$RESULT_LINE"
    exit "$code"
fi

rm -rf "$WORLD"
echo "Showcase done"
echo "$RESULT_LINE"
