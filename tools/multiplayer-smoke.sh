#!/usr/bin/env bash
# Two-client dedicated-server smoke for BusinessCraft.
#
#   bash tools/multiplayer-smoke.sh              # Forge on 25565, then Fabric on 25566
#   bash tools/multiplayer-smoke.sh forge
#   bash tools/multiplayer-smoke.sh --self-test  # observation comparator only
#
# One loader at a time. The dedicated dev server builds a short town line, plain
# minecart track, and a seeded AUTO route. Two offline clients (BC_Alpha, BC_Beta)
# join, screenshot the scene, and write multiplayer-smoke-observation.json.
# The pass fails when those observations disagree, or on a crash, disconnect, or timeout.
#
# Output: showcase-output/<timestamp>_mp-smoke_<loader>/
# See tools/MULTIPLAYER-SMOKE.md.

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
# shellcheck source=tools/showcase-output-lib.sh
source "$ROOT/tools/showcase-output-lib.sh"

if [[ "${1:-}" == "--self-test" ]]; then
    python3 "$ROOT/tools/mp-smoke-compare.py" --self-test
    exit 0
fi

TARGET="${1:-both}"
case "$TARGET" in
    forge | fabric | both) ;;
    *)
        echo "Unknown loader: $TARGET (use forge, fabric, or both)" >&2
        exit 2
        ;;
esac

if [[ -e /tmp/.X11-unix/X1 ]]; then
    export DISPLAY="${DISPLAY:-:1}"
fi

GAME_PIDS=()

kill_tree() {
    local pid="$1"
    local signal="${2:-TERM}"
    local child
    if [[ -z "$pid" || "$pid" -le 1 ]]; then
        return
    fi
    for child in $(ps -o pid= --ppid "$pid" 2>/dev/null || true); do
        kill_tree "$child" "$signal"
    done
    kill "-$signal" "$pid" 2>/dev/null || true
}

stop_pid() {
    local pid="$1"
    local i
    if [[ -z "$pid" ]]; then
        return
    fi
    kill_tree "$pid" TERM
    for i in 1 2 3 4 5 6 7 8 9 10 11 12; do
        if ! kill -0 "$pid" 2>/dev/null; then
            return
        fi
        sleep 1
    done
    kill_tree "$pid" KILL
}

cleanup() {
    local pid
    for pid in "${GAME_PIDS[@]}"; do
        stop_pid "$pid"
    done
}
trap cleanup EXIT

read_template() {
    if [[ -n "${SHOWCASE_TEMPLATE:-}" ]]; then
        printf '%s\n' "$SHOWCASE_TEMPLATE"
        return
    fi
    local from_props=""
    if [[ -f gradle.properties ]]; then
        from_props="$(grep -E '^showcaseTemplate=' gradle.properties 2>/dev/null | tail -1 | cut -d= -f2- | tr -d '\r' || true)"
    fi
    printf '%s\n' "${from_props:-/opt/businesscraft/superflat-template}"
}

offline_uuid() {
    python3 - "$1" <<'PY'
import hashlib, sys, uuid
name = "OfflinePlayer:" + sys.argv[1]
digest = bytearray(hashlib.md5(name.encode("utf-8")).digest())
digest[6] = (digest[6] & 0x0F) | 0x30
digest[8] = (digest[8] & 0x3F) | 0x80
print(uuid.UUID(bytes=bytes(digest)))
PY
}

port_busy() {
    local port="$1"
    if command -v ss >/dev/null 2>&1; then
        ss -ltn | grep -q ":${port} "
        return
    fi
    python3 - "$port" <<'PY'
import socket, sys
sock = socket.socket()
try:
    sock.bind(("127.0.0.1", int(sys.argv[1])))
except OSError:
    sys.exit(0)
sys.exit(1)
PY
}

dump_command() {
    local loader="$1"
    local task="$2"
    local dest="$3"
    shift 3
    local log="$SHOWCASE_RUN_DIR/logs/gradle-${task}.log"
    echo "Dumping :${loader}:${task} -> ${dest}"
    set +e
    bash ./gradlew --no-daemon ":${loader}:${task}" "$@" -PmpSmokeDumpCommand="$dest" --console=plain >"$log" 2>&1
    local code=$?
    set -e
    if [[ ! -s "$dest" ]]; then
        echo "Gradle did not write ${dest} (exit ${code})" >&2
        tail -n 60 "$log" >&2 || true
        return 1
    fi
    return 0
}

write_server_properties() {
    local file="$1"
    local port="$2"
    cat >"$file" <<EOF
server-port=${port}
online-mode=false
enforce-secure-profile=false
prevent-proxy-connections=false
white-list=false
spawn-protection=0
gamemode=creative
force-gamemode=true
difficulty=peaceful
pvp=false
allow-flight=true
spawn-monsters=false
view-distance=6
simulation-distance=4
max-players=8
level-name=world
level-type=minecraft\:flat
enable-command-block=false
motd=BusinessCraft multiplayer smoke
EOF
}

prepare_client_dir() {
    local dir="$1"
    mkdir -p "$dir/screenshots"
    cp "$ROOT/tools/showcase-options.txt" "$dir/options.txt"
    sed -i \
        -e 's/^renderDistance:.*/renderDistance:4/' \
        -e 's/^simulationDistance:.*/simulationDistance:4/' \
        -e 's/^maxFps:.*/maxFps:10/' \
        -e 's/^pauseOnLostFocus:.*/pauseOnLostFocus:false/' \
        -e 's/^resourcePacks:.*/resourcePacks:["vanilla"]/' \
        "$dir/options.txt"
    grep -q '^onboardAccessibility:' "$dir/options.txt" \
        || echo 'onboardAccessibility:false' >>"$dir/options.txt"
    grep -q '^skipMultiplayerWarning:' "$dir/options.txt" \
        || echo 'skipMultiplayerWarning:true' >>"$dir/options.txt"
    rm -f "$dir/multiplayer-smoke-observation.json" "$dir/screenshots"/showcase_mp_*.png
}

launch_game() {
    local spec="$1"
    local work="$2"
    local log="$3"
    shift 3
    python3 "$ROOT/tools/mp-smoke-launch.py" "$spec" --working-dir "$work" "$@" >"$log" 2>&1 &
    LAST_PID=$!
    GAME_PIDS+=("$LAST_PID")
}

wait_ready() {
    local pid="$1"
    local ready="$2"
    local log="$3"
    local game_log="$4"
    local deadline=$((SECONDS + 420))
    while (( SECONDS < deadline )); do
        if [[ -f "$ready" ]]; then
            if python3 - "$ready" <<'PY'
import json, sys
doc = json.load(open(sys.argv[1]))
sys.exit(0 if doc.get("ok") is True else 1)
PY
            then
                echo "Server scene is ready"
                return 0
            fi
            echo "Server wrote a failed ready file" >&2
            cat "$ready" >&2 || true
            return 1
        fi
        if grep -q "MP_SMOKE_FAILED" "$log" "$game_log" 2>/dev/null; then
            echo "Server reported MP_SMOKE_FAILED" >&2
            return 1
        fi
        if grep -qE "Failed to start the minecraft server|Mod Loading has failed|Could not execute entrypoint" "$log" "$game_log" 2>/dev/null; then
            echo "Dedicated server failed during startup" >&2
            return 1
        fi
        if ! kill -0 "$pid" 2>/dev/null; then
            echo "Server process exited before the scene was ready" >&2
            return 1
        fi
        sleep 2
    done
    echo "Timed out waiting for the server scene" >&2
    return 1
}

wait_client() {
    local name="$1"
    local pid="$2"
    local observation="$3"
    local deadline=$((SECONDS + 900))
    while (( SECONDS < deadline )); do
        if ! kill -0 "$pid" 2>/dev/null; then
            if [[ -f "$observation" ]]; then
                echo "${name} exited after writing an observation"
                return 0
            fi
            echo "${name} exited without an observation (crash or disconnect)" >&2
            return 1
        fi
        sleep 2
    done
    echo "${name} timed out" >&2
    return 1
}

require_shots() {
    local dir="$1"
    local label="$2"
    local manifest="$dir/showcase-shots.json"
    if [[ ! -f "$manifest" ]]; then
        echo "${label} is missing showcase-shots.json" >&2
        return 1
    fi
    python3 - "$manifest" "$dir/screenshots" "$label" <<'PY'
import json, sys
from pathlib import Path
manifest = json.load(open(sys.argv[1]))
folder = Path(sys.argv[2])
label = sys.argv[3]
expected = ["mp_nametags", "mp_track", "mp_platform"]
by_name = {row.get("name"): row for row in manifest.get("shots") or []}
if manifest.get("expected") != expected or manifest.get("pending", 0) != 0 or manifest.get("ok") is not True:
    print(f"{label}: shot manifest is not the three saved views", file=sys.stderr)
    sys.exit(1)
missing = []
for name in expected:
    row = by_name.get(name)
    file_name = f"showcase_{name}.png"
    path = folder / file_name
    if row is None or not row.get("ok") or not path.is_file() or path.stat().st_size <= 0:
        missing.append(name)
if missing:
    print(f"{label}: shots failed or missing: {', '.join(missing)}", file=sys.stderr)
    sys.exit(1)
PY
}

run_loader() {
    local loader="$1"
    local port="$2"
    local server_dir="$ROOT/$loader/run"
    local alpha_dir="$ROOT/$loader/run-mp-alpha"
    local beta_dir="$ROOT/$loader/run-mp-beta"
    local template
    template="$(read_template)"

    showcase_output_init "mp-smoke" "$loader" "$ROOT"
    echo "Output: $SHOWCASE_RUN_DIR"
    mkdir -p "$SHOWCASE_RUN_DIR/screenshots/alpha" "$SHOWCASE_RUN_DIR/screenshots/beta" "$SHOWCASE_RUN_DIR/logs"

    if [[ ! -f "$template/level.dat" ]]; then
        echo "Showcase template not found: $template" >&2
        showcase_append_index "$SHOWCASE_OUTPUT_ROOT" "$SHOWCASE_RUN_DIR" "mp-smoke" "$loader" "FAIL"
        return 1
    fi
    if port_busy "$port"; then
        echo "Port ${port} is already in use" >&2
        showcase_append_index "$SHOWCASE_OUTPUT_ROOT" "$SHOWCASE_RUN_DIR" "mp-smoke" "$loader" "FAIL"
        return 1
    fi

    local client_spec="$SHOWCASE_RUN_DIR/client-command.json"
    local server_spec="$SHOWCASE_RUN_DIR/server-command.json"
    if ! dump_command "$loader" runClient "$client_spec" \
            -PmpSmoke=observe -PmpSmokePort="$port" -PshowcaseWidth=960 -PshowcaseHeight=540; then
        showcase_append_index "$SHOWCASE_OUTPUT_ROOT" "$SHOWCASE_RUN_DIR" "mp-smoke" "$loader" "FAIL"
        return 1
    fi
    if ! dump_command "$loader" runServer "$server_spec" -PmpSmoke=setup; then
        showcase_append_index "$SHOWCASE_OUTPUT_ROOT" "$SHOWCASE_RUN_DIR" "mp-smoke" "$loader" "FAIL"
        return 1
    fi

    mkdir -p "$server_dir"
    printf 'eula=true\n' >"$server_dir/eula.txt"
    write_server_properties "$server_dir/server.properties" "$port"
    rm -rf "$server_dir/world"
    cp -a "$template" "$server_dir/world"
    rm -f "$server_dir/world/session.lock" "$server_dir/mp-smoke-ready.json"
    rm -f "$server_dir/logs/latest.log"

    prepare_client_dir "$alpha_dir"
    prepare_client_dir "$beta_dir"

    local server_console="$SHOWCASE_RUN_DIR/logs/server-console.log"
    launch_game "$server_spec" "$server_dir" "$server_console" --xmx 1536m --append-arg nogui
    local server_pid="$LAST_PID"
    echo "Server pid ${server_pid} on port ${port}"

    local status="FAIL"
    local server_log="$server_dir/logs/latest.log"
    if wait_ready "$server_pid" "$server_dir/mp-smoke-ready.json" "$server_console" "$server_log"; then
        local alpha_uuid beta_uuid
        alpha_uuid="$(offline_uuid BC_Alpha)"
        beta_uuid="$(offline_uuid BC_Beta)"
        local alpha_console="$SHOWCASE_RUN_DIR/logs/alpha-console.log"
        local beta_console="$SHOWCASE_RUN_DIR/logs/beta-console.log"
        launch_game "$client_spec" "$alpha_dir" "$alpha_console" \
            --xmx 1280m --username BC_Alpha --uuid "$alpha_uuid" --game-dir "$alpha_dir"
        local alpha_pid="$LAST_PID"
        echo "BC_Alpha pid ${alpha_pid}"
        sleep 20
        launch_game "$client_spec" "$beta_dir" "$beta_console" \
            --xmx 1280m --username BC_Beta --uuid "$beta_uuid" --game-dir "$beta_dir"
        local beta_pid="$LAST_PID"
        echo "BC_Beta pid ${beta_pid}"

        local alpha_ok=0 beta_ok=0
        if wait_client BC_Alpha "$alpha_pid" "$alpha_dir/multiplayer-smoke-observation.json"; then
            alpha_ok=1
        fi
        if wait_client BC_Beta "$beta_pid" "$beta_dir/multiplayer-smoke-observation.json"; then
            beta_ok=1
        fi
        stop_pid "$alpha_pid"
        stop_pid "$beta_pid"

        cp -f "$alpha_dir/multiplayer-smoke-observation.json" "$SHOWCASE_RUN_DIR/observation-alpha.json" 2>/dev/null || true
        cp -f "$beta_dir/multiplayer-smoke-observation.json" "$SHOWCASE_RUN_DIR/observation-beta.json" 2>/dev/null || true
        cp -f "$alpha_dir/showcase-shots.json" "$SHOWCASE_RUN_DIR/shots-alpha.json" 2>/dev/null || true
        cp -f "$beta_dir/showcase-shots.json" "$SHOWCASE_RUN_DIR/shots-beta.json" 2>/dev/null || true
        if [[ -d "$alpha_dir/screenshots" ]]; then
            cp -f "$alpha_dir/screenshots"/showcase_mp_*.png "$SHOWCASE_RUN_DIR/screenshots/alpha/" 2>/dev/null || true
        fi
        if [[ -d "$beta_dir/screenshots" ]]; then
            cp -f "$beta_dir/screenshots"/showcase_mp_*.png "$SHOWCASE_RUN_DIR/screenshots/beta/" 2>/dev/null || true
        fi

        if [[ "$alpha_ok" -eq 1 && "$beta_ok" -eq 1 ]] \
            && require_shots "$alpha_dir" BC_Alpha \
            && require_shots "$beta_dir" BC_Beta; then
            set +e
            python3 "$ROOT/tools/mp-smoke-compare.py" \
                "$SHOWCASE_RUN_DIR/observation-alpha.json" \
                "$SHOWCASE_RUN_DIR/observation-beta.json" \
                >"$SHOWCASE_RUN_DIR/diff.txt" 2>"$SHOWCASE_RUN_DIR/compare-errors.txt"
            local compare_code=$?
            set -e
            if [[ "$compare_code" -eq 0 ]]; then
                status="PASS"
            else
                echo "Observations disagree or the seeded scene was missing" >&2
                cat "$SHOWCASE_RUN_DIR/compare-errors.txt" >&2 || true
                cat "$SHOWCASE_RUN_DIR/diff.txt" >&2 || true
            fi
        else
            echo "${loader} smoke did not get two complete client observations" >&2
            tail -n 40 "$alpha_console" >&2 || true
            tail -n 40 "$beta_console" >&2 || true
        fi
    else
        tail -n 80 "$server_console" >&2 || true
        tail -n 40 "$server_log" >&2 || true
    fi

    stop_pid "$server_pid"
    cp -f "$server_console" "$SHOWCASE_RUN_DIR/logs/server-console.log" 2>/dev/null || true
    cp -f "$server_log" "$SHOWCASE_RUN_DIR/logs/server.log" 2>/dev/null || true
    cp -f "$alpha_dir/logs/latest.log" "$SHOWCASE_RUN_DIR/logs/alpha-client.log" 2>/dev/null || true
    cp -f "$beta_dir/logs/latest.log" "$SHOWCASE_RUN_DIR/logs/beta-client.log" 2>/dev/null || true
    cp -f "$server_dir/mp-smoke-ready.json" "$SHOWCASE_RUN_DIR/mp-smoke-ready.json" 2>/dev/null || true

    {
        echo "loader=${loader}"
        echo "port=${port}"
        echo "status=${status}"
        echo "server_pid=${server_pid}"
        echo "output=${SHOWCASE_RUN_DIR}"
    } >"$SHOWCASE_RUN_DIR/summary.txt"

    showcase_append_index "$SHOWCASE_OUTPUT_ROOT" "$SHOWCASE_RUN_DIR" "mp-smoke" "$loader" "$status"
    echo "${loader} multiplayer smoke: ${status}"
    [[ "$status" == "PASS" ]]
}

FAILED=0
# Keep these aligned with MultiplayerSmokePlan (Forge 25565, Fabric 25566).
FORGE_PORT=25565
FABRIC_PORT=25566
if [[ "$TARGET" == "forge" || "$TARGET" == "both" ]]; then
    run_loader forge "$FORGE_PORT" || FAILED=1
fi
if [[ "$TARGET" == "fabric" || "$TARGET" == "both" ]]; then
    run_loader fabric "$FABRIC_PORT" || FAILED=1
fi

if [[ "$FAILED" -ne 0 ]]; then
    echo "Multiplayer smoke failed" >&2
    exit 1
fi
echo "Multiplayer smoke passed"
