#!/usr/bin/env bash
# Showcase runner. Starts the Forge client in a throwaway superflat copy,
# lets the harness build a scene, take screenshots, and quit.
#
#   bash tools/showcase.sh                 # every shot
#   bash tools/showcase.sh town            # the four town tabs
#   bash tools/showcase.sh block           # front and back of the block
#   bash tools/showcase.sh town_overview,town_settings
#
# -PquickPlay names the save. -Pshowcase selects scenes (block, town, all)
# or exact shot names and is what turns the harness on.
# Screenshots land in forge/run/screenshots/showcase_<shot>.png.
# The harness rewrites the world it is given, so this always copies a template
# ($SHOWCASE_TEMPLATE, or /opt/businesscraft/superflat-template).
# A normal client launch does not pass -Pshowcase, so this stays off.

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

SCENES="${1:-all}"
TEMPLATE="${SHOWCASE_TEMPLATE:-/opt/businesscraft/superflat-template}"
WORLD="$ROOT/forge/run/saves/zz_showcase"
SHOTDIR="$ROOT/forge/run/screenshots"
LOG="$ROOT/forge/run/showcase.log"
OPTIONS="$ROOT/forge/run/options.txt"

if [[ ! -f "$TEMPLATE/level.dat" ]]; then
    echo "Showcase template not found: $TEMPLATE" >&2
    exit 1
fi

if [[ -e /tmp/.X11-unix/X1 ]]; then
    export DISPLAY="${DISPLAY:-:1}"
fi

rm -rf "$WORLD"
cp -a "$TEMPLATE" "$WORLD"
rm -f "$WORLD/session.lock"
mkdir -p "$SHOTDIR"
rm -f "$SHOTDIR"/showcase_*.png

# The game must keep ticking when the window is not focused, or the harness
# never reaches its first shot.
if [[ -f "$OPTIONS" ]]; then
    sed -i 's/^pauseOnLostFocus:.*/pauseOnLostFocus:false/' "$OPTIONS"
fi

set +e
bash ./gradlew --no-daemon :forge:runClient -PquickPlay=zz_showcase -Pshowcase="$SCENES" --console=plain >"$LOG" 2>&1
code=$?
set -e

count="$(find "$SHOTDIR" -maxdepth 1 -name 'showcase_*.png' | wc -l | tr -d ' ')"
echo "Showcase shots: $count (gradle exit $code)"
echo "Log: $LOG"

case "$SCENES" in
    "" | all | ALL) expect=7 ;;
    town) expect=5 ;;
    block) expect=3 ;;
    *) expect=0 ;;
esac

if grep -n "Showcase failed" "$LOG"; then
    echo "Showcase reported a failure" >&2
    code=1
fi

grep -nE "ERROR|Exception" "$LOG" | grep -v "Unable to delete file" | grep -v "ALSA" | head -n 30 || true

if [[ "$count" -eq 0 ]]; then
    echo "No showcase screenshots were written" >&2
    exit 1
fi
if [[ "$expect" -gt 0 && "$count" -ne "$expect" ]]; then
    echo "Expected $expect shots, found $count" >&2
    exit 1
fi
if [[ "$code" -ne 0 ]]; then
    echo "Client exit code $code" >&2
    exit "$code"
fi

rm -rf "$WORLD"
echo "Showcase done"
