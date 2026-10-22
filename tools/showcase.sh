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
LOADER="${2:-forge}"

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
if [[ "$SCENES" == "all-views" ]]; then
    GRADLE_PROPS+=(-PshowcaseLoader="$LOADER")
    export BUSINESSCRAFT_SHOWCASE_LOADER="$LOADER"
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
ALL_VIEWS_EXPECT=10

case "$SCENES" in
    "" | all | ALL) expect=6 ;;
    town) expect=4 ;;
    block) expect=2 ;;
    all-views) expect=0 ;;
    *) expect=0 ;;
esac

if grep -n "Showcase failed" "$LOG"; then
    echo "Showcase reported a failure" >&2
    code=1
fi

grep -nE "ERROR|Exception" "$LOG" | grep -v "Unable to delete file" | grep -v "ALSA" | head -n 30 || true

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
