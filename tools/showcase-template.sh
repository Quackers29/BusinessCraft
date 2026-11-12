#!/usr/bin/env bash
# Builds the superflat template world that showcase.sh and multiplayer-smoke.sh copy.
#
#   bash tools/showcase-template.sh                 # to the default location
#   bash tools/showcase-template.sh /some/dir       # to a chosen directory
#   SHOWCASE_TEMPLATE=/some/dir bash tools/showcase-template.sh
#
# Boots the Forge dedicated dev server once on a flat level (same flat ground as the cloud
# VM's /opt/businesscraft/superflat-template), waits for "Done (", stops it so the level is
# saved, and copies the world. The other scripts call this on their own when the template
# is missing; run it by hand to rebuild one.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=tools/common-lib.sh
source "$ROOT/tools/common-lib.sh"

DEST="${1:-$(bc_template_path)}"
RUN="$ROOT/forge/run"
LOG="$(mktemp /tmp/businesscraft-template-XXXXXX.log)"

mkdir -p "$RUN"
printf 'eula=true\n' >"$RUN/eula.txt"
bc_write_server_properties "$RUN/server.properties" "$(bc_free_port 25565)"
rm -rf "$RUN/world" "$RUN/logs/latest.log"

echo "Building template at $DEST (log: $LOG)"
bc_gradle :forge:runServer --console=plain >"$LOG" 2>&1 &
pid=$!
deadline=$((SECONDS + 420))
done_seen=0
while (( SECONDS < deadline )); do
    if grep -q 'Done (' "$LOG" 2>/dev/null || { [[ -f "$RUN/logs/latest.log" ]] && grep -q 'Done (' "$RUN/logs/latest.log"; }; then
        done_seen=1
        break
    fi
    if ! kill -0 "$pid" 2>/dev/null; then
        break
    fi
    sleep 2
done
kill_tree "$pid"
for _ in $(seq 1 30); do
    kill -0 "$pid" 2>/dev/null || break
    sleep 1
done
kill_tree "$pid" KILL
wait "$pid" 2>/dev/null || true

if [[ "$done_seen" -ne 1 || ! -f "$RUN/world/level.dat" ]]; then
    echo "Template build failed (no Done line or no level.dat); see $LOG" >&2
    tail -n 30 "$LOG" >&2 || true
    exit 1
fi

rm -f "$RUN/world/session.lock"
mkdir -p "$(dirname "$DEST")"
rm -rf "$DEST.tmp"
cp -a "$RUN/world" "$DEST.tmp"
rm -rf "$DEST"
mv "$DEST.tmp" "$DEST"
rm -rf "$RUN/world"
echo "Template ready: $DEST"
