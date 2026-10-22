#!/usr/bin/env bash
# Showcase runner. Starts a dev client in a throwaway superflat copy, lets the
# harness build a scene, take screenshots, and quit.
#
#   bash tools/showcase.sh                       # every shot (Forge)
#   bash tools/showcase.sh town                  # the four town tabs
#   bash tools/showcase.sh block                 # front and back of the block
#   bash tools/showcase.sh block fabric          # same scene on Fabric
#   bash tools/showcase.sh town_overview,town_settings forge
#   bash tools/showcase.sh all-views forge   # every registered UI route + HTML index
#   bash tools/showcase.sh all-views fabric
#   bash tools/showcase.sh two-towns forge   # boundary tests + all-views on town B
#   bash tools/showcase.sh two-towns fabric
#   bash tools/showcase.sh testbed forge                    # all registered testbed checks
#   bash tools/showcase.sh testbed fabric trade-contract    # single check
#
# -PquickPlay names the save. -Pshowcase selects scenes (block, town, all)
# or exact shot names and is what turns the harness on.
# Screenshots land in <loader>/run/screenshots/showcase_<shot>.png (warmup is
# not saved). The harness rewrites the world it is given, so this always copies
# a template world directory (must contain level.dat).
#
# Template path (first match wins):
#   1. SHOWCASE_TEMPLATE environment variable
#   2. showcaseTemplate in gradle.properties or -PshowcaseTemplate=... on Gradle
#   3. /opt/businesscraft/superflat-template
#
# A normal client launch does not pass -Pshowcase, so the harness stays off.
#
# Window and GUI scale are fixed for comparable Forge/Fabric PNGs (see
# tools/showcase-options.txt and -PshowcaseWidth / -PshowcaseHeight).

set -euo pipefail

SHOWCASE_WIDTH="${SHOWCASE_WIDTH:-1920}"
SHOWCASE_HEIGHT="${SHOWCASE_HEIGHT:-1080}"

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

SCENES="${1:-all}"
shift || true
LOADER="${1:-forge}"
shift || true
TESTBED_CHECKS="${*:-all}"

case "$LOADER" in
    forge | fabric) ;;
    *)
        echo "Unknown loader: $LOADER (use forge or fabric)" >&2
        exit 1
        ;;
esac

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

RUN_DIR="$ROOT/$LOADER/run"
WORLD="$RUN_DIR/saves/zz_showcase"
SHOTDIR="$RUN_DIR/screenshots"
LOG="$RUN_DIR/showcase.log"
OPTIONS="$RUN_DIR/options.txt"
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
rm -f "$SHOTDIR"/showcase_*.png "$SHOTDIR"/showcase_view_*.png
rm -f "$RUN_DIR/showcase-views-manifest.json"

mkdir -p "$RUN_DIR"
cp "$ROOT/tools/showcase-options.txt" "$OPTIONS"

GRADLE_PROPS=(
    -PquickPlay=zz_showcase
    -Pshowcase="$SCENES"
    -PshowcaseTemplate="$TEMPLATE"
    -PshowcaseWidth="$SHOWCASE_WIDTH"
    -PshowcaseHeight="$SHOWCASE_HEIGHT"
)
if [[ "$SCENES" == "all-views" || "$SCENES" == "two-towns" || "$SCENES" == "testbed" ]]; then
    GRADLE_PROPS+=(-PshowcaseLoader="$LOADER")
    export BUSINESSCRAFT_SHOWCASE_LOADER="$LOADER"
fi
if [[ "$SCENES" == "testbed" ]]; then
    GRADLE_PROPS+=(-PtestbedChecks="$TESTBED_CHECKS")
fi

set +e
bash ./gradlew --no-daemon "$GRADLE_TASK" "${GRADLE_PROPS[@]}" --console=plain >"$LOG" 2>&1
code=$?
set -e

count="$(find "$SHOTDIR" -maxdepth 1 -name 'showcase_*.png' 2>/dev/null | wc -l | tr -d ' ')"
view_count="$(find "$SHOTDIR" -maxdepth 1 -name 'showcase_view_*.png' 2>/dev/null | wc -l | tr -d ' ')"
echo "Showcase ($LOADER) shots: $count views: $view_count (gradle exit $code)"
echo "Log: $LOG"

MANIFEST="$RUN_DIR/showcase-views-manifest.json"
ALL_VIEWS_EXPECT=14

case "$SCENES" in
    "" | all | ALL) expect=6 ;;
    town) expect=4 ;;
    block) expect=2 ;;
    all-views) expect=0 ;;
    two-towns) expect=0 ;;
    testbed) expect=0 ;;
    *) expect=0 ;;
esac

if grep -n "Showcase failed" "$LOG"; then
    echo "Showcase reported a failure" >&2
    code=1
fi

grep -nE "ERROR|Exception" "$LOG" | grep -v "Unable to delete file" | grep -v "ALSA" | head -n 30 || true

TWO_TOWNS_REPORT="$RUN_DIR/showcase-two-towns-report.json"
TESTBED_REPORT="$RUN_DIR/showcase-testbed-report.json"

write_testbed_step_log() {
    local artifact_dir="$1"
    local report="$2"
    local log_out="$artifact_dir/checks.log"
    mkdir -p "$artifact_dir"
    if [[ ! -f "$report" ]]; then
        echo "Missing testbed report: $report" >&2
        return 1
    fi
    cp -f "$report" "$artifact_dir/"
    cp -f "$LOG" "$artifact_dir/showcase.log" 2>/dev/null || true
    mkdir -p "$artifact_dir/screenshots"
    cp -f "$SHOTDIR"/showcase_testbed_*.png "$artifact_dir/screenshots/" 2>/dev/null || true
    {
        echo "# testbed showcase ($LOADER) checks=$TESTBED_CHECKS — $(date -u +%Y-%m-%dT%H:%M:%SZ)"
        python3 - "$report" <<'PY'
import json, sys
report = json.load(open(sys.argv[1]))
print(f"loader: {report.get('loader')}")
print(f"checksRequested: {report.get('checksRequested')}")
print("results:")
failed = False
for row in report.get("results", []):
    ok = row.get("pass")
    name = row.get("check")
    mark = "PASS" if ok else "FAIL"
    print(f"  {name}: {mark}")
    if not ok:
        failed = True
print(f"exitCode: {report.get('exitCode')}")
sys.exit(1 if failed or report.get("exitCode", 0) != 0 else 0)
PY
    } >"$log_out" || return 1
    echo "Artifacts: $artifact_dir"
    echo "Check log: $log_out"
    return 0
}

write_two_towns_step_log() {
    local artifact_dir="$1"
    local report="$2"
    local log_out="$artifact_dir/steps.log"
    mkdir -p "$artifact_dir"
    if [[ ! -f "$report" ]]; then
        echo "Missing two-towns report: $report" >&2
        return 1
    fi
    cp -f "$report" "$artifact_dir/"
    cp -f "$LOG" "$artifact_dir/showcase.log" 2>/dev/null || true
    cp -f "$MANIFEST" "$artifact_dir/showcase-views-manifest.json" 2>/dev/null || true
    mkdir -p "$artifact_dir/screenshots"
    cp -f "$SHOTDIR"/showcase_view_*.png "$artifact_dir/screenshots/" 2>/dev/null || true
    {
        echo "# two-towns showcase ($LOADER) — $(date -u +%Y-%m-%dT%H:%M:%SZ)"
        python3 - "$report" <<'PY'
import json, sys
report = json.load(open(sys.argv[1]))
print(f"loader: {report.get('loader')}")
print(f"townA: {report.get('townA')} townB: {report.get('townB')}")
print(f"requiredCentreDistance: {report.get('requiredCentreDistance')}")
print(f"defaultBorderRadius: {report.get('defaultBorderRadius')}")
print(f"rejectionMessage: {report.get('rejectionMessage')}")
print("steps:")
failed = False
for step in report.get("steps", []):
    ok = step.get("pass")
    name = step.get("step")
    mark = "PASS" if ok else "FAIL"
    print(f"  {name}: {mark}")
    if not ok:
        failed = True
print(f"exitCode: {report.get('exitCode')}")
sys.exit(1 if failed or report.get("exitCode", 0) != 0 else 0)
PY
    } >"$log_out" || return 1
    echo "Artifacts: $artifact_dir"
    echo "Step log: $log_out"
    return 0
}

if [[ "$SCENES" == "all-views" ]]; then
    if [[ ! -f "$MANIFEST" ]]; then
        echo "Missing showcase manifest: $MANIFEST" >&2
        exit 1
    fi
    if [[ "$view_count" -lt "$ALL_VIEWS_EXPECT" ]]; then
        echo "Expected at least $ALL_VIEWS_EXPECT view PNGs, found $view_count" >&2
        code=1
    fi
    if grep -q '"ok": false' "$MANIFEST"; then
        echo "One or more routes failed (see manifest)" >&2
        code=1
    fi
    ARTIFACT_DIR="${SHOWCASE_ARTIFACT_DIR:-/opt/cursor/artifacts/showcase-all-views/$LOADER}"
    mkdir -p "$ARTIFACT_DIR/screenshots"
    cp -f "$MANIFEST" "$ARTIFACT_DIR/"
    cp -f "$SHOTDIR"/showcase_view_*.png "$ARTIFACT_DIR/screenshots/" 2>/dev/null || true
    bash "$ROOT/tools/showcase-generate-index.sh" "$ARTIFACT_DIR/showcase-views-index.html" "$MANIFEST"
    echo "Artifacts: $ARTIFACT_DIR"
elif [[ "$SCENES" == "two-towns" ]]; then
    if [[ ! -f "$TWO_TOWNS_REPORT" ]]; then
        echo "Missing two-towns report: $TWO_TOWNS_REPORT" >&2
        code=1
    fi
    if [[ "$view_count" -lt "$ALL_VIEWS_EXPECT" ]]; then
        echo "Expected at least $ALL_VIEWS_EXPECT view PNGs for town B all-views, found $view_count" >&2
        code=1
    fi
    if [[ -f "$MANIFEST" ]] && grep -q '"ok": false' "$MANIFEST"; then
        echo "One or more all-views routes failed (see manifest)" >&2
        code=1
    fi
    ARTIFACT_DIR="${SHOWCASE_ARTIFACT_DIR:-/opt/cursor/artifacts/showcase-two-towns/$LOADER}"
    if [[ -f "$TWO_TOWNS_REPORT" ]]; then
        if ! write_two_towns_step_log "$ARTIFACT_DIR" "$TWO_TOWNS_REPORT"; then
            code=1
        fi
    fi
elif [[ "$SCENES" == "testbed" ]]; then
    testbed_shots="$(find "$SHOTDIR" -maxdepth 1 -name 'showcase_testbed_*.png' 2>/dev/null | wc -l | tr -d ' ')"
    echo "Testbed screenshots: $testbed_shots"
    if [[ ! -f "$TESTBED_REPORT" ]]; then
        echo "Missing testbed report: $TESTBED_REPORT" >&2
        code=1
    fi
    ARTIFACT_DIR="${SHOWCASE_ARTIFACT_DIR:-/opt/cursor/artifacts/showcase-testbed/$LOADER}"
    if [[ -f "$TESTBED_REPORT" ]]; then
        if ! write_testbed_step_log "$ARTIFACT_DIR" "$TESTBED_REPORT"; then
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
        exit 1
    fi
fi
if [[ "$code" -ne 0 ]]; then
    echo "Client exit code $code" >&2
    exit "$code"
fi

rm -rf "$WORLD"
echo "Showcase done"
