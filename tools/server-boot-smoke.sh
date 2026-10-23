#!/usr/bin/env bash
# Boots each loader's dedicated dev server and fails unless the world reaches
# Minecraft's "Done (" line. A green Gradle runServer is not enough: the game
# can crash during mod construction and the task can still exit 0.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

kill_tree() {
    local pid="$1"
    local child
    if [[ -z "$pid" ]]; then
        return
    fi
    for child in $(ps -o pid= --ppid "$pid" 2>/dev/null || true); do
        kill_tree "$child"
    done
    kill "$pid" 2>/dev/null || true
}

smoke_one() {
    local loader="$1"
    local log="/tmp/businesscraft-${loader}-server-smoke.log"
    local game_log="$ROOT/$loader/run/logs/latest.log"
    mkdir -p "$ROOT/$loader/run"
    printf 'eula=true\n' > "$ROOT/$loader/run/eula.txt"
    rm -f "$game_log" "$log"

    echo "Starting :${loader}:runServer"
    bash ./gradlew --no-daemon ":${loader}:runServer" --console=plain >"$log" 2>&1 &
    local pid=$!
    local deadline=$((SECONDS + 420))

    while (( SECONDS < deadline )); do
        if grep -q "Done (" "$log" 2>/dev/null || { [[ -f "$game_log" ]] && grep -q "Done (" "$game_log"; }; then
            echo "${loader} dedicated server reached Done"
            kill_tree "$pid"
            wait "$pid" 2>/dev/null || true
            return 0
        fi
        if grep -qE "Failed to start the minecraft server|Failed to complete lifecycle|Mod Loading has failed|Could not execute entrypoint" "$log" 2>/dev/null; then
            echo "${loader} dedicated server failed to start" >&2
            tail -n 80 "$log" >&2 || true
            kill_tree "$pid"
            wait "$pid" 2>/dev/null || true
            return 1
        fi
        if ! kill -0 "$pid" 2>/dev/null; then
            echo "${loader} server process exited before Done" >&2
            tail -n 80 "$log" >&2 || true
            return 1
        fi
        sleep 2
    done

    echo "${loader} dedicated server timed out waiting for Done" >&2
    tail -n 80 "$log" >&2 || true
    kill_tree "$pid"
    wait "$pid" 2>/dev/null || true
    return 1
}

smoke_one forge
smoke_one fabric
echo "Dedicated server smoke passed"
