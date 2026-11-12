#!/usr/bin/env bash
# Playable multiplayer showcase: bring up a dedicated dev server with a built-out BusinessCraft
# world, join it with one visible client, look around, then shut everything down.
#
#   bash tools/showcase-play.sh start [forge|fabric]   # default forge; leaves everything running
#   bash tools/showcase-play.sh status                 # running? loader, port, how to join, logs
#   bash tools/showcase-play.sh stop                   # save and stop what start launched
#
# start builds the world once (towns, ticket machines in every destination mode, a rail line
# with tourists, contracts, signs, a guide chest), waits for the server's "Done (" line, then
# opens a Minecraft window already connected. Nothing quits on its own.
# stop ends only the processes recorded in the session file. A client you started yourself is
# left alone. Both are safe to repeat.
#
# Environment: SHOWCASE_TEMPLATE (superflat template world), BC_PLAY_NAME (player name,
# default BC_Player), BC_PLAY_NO_CLIENT=1 (server only), BC_NO_DAEMON=1 (no Gradle daemon).
# Output: showcase-output/<timestamp>_play_<loader>/ (logs). See tools/SHOWCASE-TESTBED.md.

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
# shellcheck source=tools/showcase-output-lib.sh
source "$ROOT/tools/showcase-output-lib.sh"

SESSION_FILE="${SHOWCASE_OUTPUT_ROOT:-$ROOT/showcase-output}/play-session.env"
PLAY_NAME="${BC_PLAY_NAME:-BC_Player}"
READY_TIMEOUT=480

usage() {
    sed -n '2,13p' "$0" | sed 's/^# \{0,1\}//' >&2
    exit 2
}

# ---- process helpers ------------------------------------------------------------------------

# Field 22 of /proc/<pid>/stat: the process start time. A pid whose start time differs from the
# recorded one was reused by something else, and is never signalled.
pid_start() {
    local pid="$1" stat
    [[ -n "$pid" && -r "/proc/$pid/stat" ]] || return 1
    stat="$(cat "/proc/$pid/stat")"
    stat="${stat##*) }"
    set -- $stat
    echo "${20}"
}

# pid_alive <pid> <recorded-start-time>
pid_alive() {
    local pid="$1" want="$2" have
    [[ -n "$pid" && -n "$want" ]] || return 1
    kill -0 "$pid" 2>/dev/null || return 1
    have="$(pid_start "$pid" 2>/dev/null)" || return 1
    [[ "$have" == "$want" ]]
}

stop_pid() {
    local pid="$1" want="$2" i
    pid_alive "$pid" "$want" || return 0
    kill_tree "$pid" TERM
    for i in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15; do
        pid_alive "$pid" "$want" || return 0
        sleep 1
    done
    kill_tree "$pid" KILL
}

# ---- session file ---------------------------------------------------------------------------

save_session() {
    mkdir -p "$(dirname "$SESSION_FILE")"
    {
        for key in LOADER PORT RUN_DIR SERVER_DIR CLIENT_DIR SERVER_PID SERVER_START CLIENT_PID CLIENT_START \
                STARTED_AT READY_SECONDS DETAIL PLAYER; do
            printf '%s=%q\n' "$key" "${!key:-}"
        done
    } >"$SESSION_FILE.tmp"
    mv -f "$SESSION_FILE.tmp" "$SESSION_FILE"
}

load_session() {
    LOADER="" PORT="" RUN_DIR="" SERVER_DIR="" CLIENT_DIR="" SERVER_PID="" SERVER_START=""
    CLIENT_PID="" CLIENT_START="" STARTED_AT="" READY_SECONDS="" DETAIL="" PLAYER=""
    [[ -f "$SESSION_FILE" ]] || return 1
    # shellcheck disable=SC1090
    source "$SESSION_FILE"
}

server_running() { pid_alive "${SERVER_PID:-}" "${SERVER_START:-}"; }
client_running() { pid_alive "${CLIENT_PID:-}" "${CLIENT_START:-}"; }

# ---- join information -----------------------------------------------------------------------

lan_addresses() {
    local out="" ip
    if command -v powershell.exe >/dev/null 2>&1; then
        out="$(timeout 8 powershell.exe -NoProfile -Command \
            "(Get-NetIPConfiguration | Where-Object { \$_.IPv4DefaultGateway -ne \$null } | ForEach-Object { \$_.IPv4Address.IPAddress }) -join ' '" \
            2>/dev/null | tr -d '\r' || true)"
        if [[ -n "${out// /}" ]]; then
            printf 'windows:%s\n' "$out"
        fi
    fi
    ip="$(hostname -I 2>/dev/null | awk '{print $1}' || true)"
    if [[ -n "$ip" ]]; then
        printf 'wsl:%s\n' "$ip"
    fi
}

print_join() {
    local host_ips
    echo "  Join:     localhost:${PORT}   (Minecraft 1.20.1, ${LOADER}, offline mode, any username)"
    host_ips="$(lan_addresses)"
    if [[ -n "$host_ips" ]]; then
        local line
        while IFS= read -r line; do
            case "$line" in
                windows:*)
                    local ip
                    for ip in ${line#windows:}; do
                        [[ "$ip" == 169.254.* ]] && continue
                        echo "  LAN:      ${ip}:${PORT}   (this PC's address for another machine; the Windows firewall must allow it)"
                    done
                    ;;
                wsl:*) echo "  WSL addr: ${line#wsl:}:${PORT}   (only reachable from this PC unless WSL uses mirrored networking)" ;;
            esac
        done <<<"$host_ips"
    else
        echo "  LAN:      address not found; use this machine's IP with port ${PORT}"
    fi
    echo "  Mods:     a client you join with must have BusinessCraft for ${LOADER} (1.20.1) installed"
    echo "  Stop:     bash tools/showcase-play.sh stop   (ends the server and the client this script opened,"
    echo "            not a client you started yourself)"
}

# ---- start ----------------------------------------------------------------------------------

dump_command() {
    local loader="$1" task="$2" dest="$3"
    shift 3
    local log="$RUN_DIR/logs/gradle-${task}.log" code
    echo "Resolving :${loader}:${task} launch command (Gradle)"
    set +e
    bc_gradle ":${loader}:${task}" "$@" -PmpSmokeDumpCommand="$dest" --console=plain >"$log" 2>&1
    code=$?
    set -e
    if [[ ! -s "$dest" ]]; then
        echo "Gradle did not write ${dest} (exit ${code})" >&2
        tail -n 40 "$log" >&2 || true
        return 1
    fi
}

write_server_properties() {
    local file="$1" port="$2"
    bc_write_server_properties "$file" "$port"
    # Walkable world: towns are 120+ blocks apart and the cart line must stay loaded.
    sed -i -e 's/^view-distance=.*/view-distance=10/' \
        -e 's/^simulation-distance=.*/simulation-distance=10/' \
        -e 's/^motd=.*/motd=BusinessCraft showcase tour/' "$file"
}

prepare_client_dir() {
    local dir="$1"
    mkdir -p "$dir"
    if [[ ! -f "$dir/options.txt" ]]; then
        cp "$ROOT/tools/showcase-options.txt" "$dir/options.txt"
        sed -i \
            -e 's/^renderDistance:.*/renderDistance:12/' \
            -e 's/^simulationDistance:.*/simulationDistance:10/' \
            -e 's/^maxFps:.*/maxFps:144/' \
            -e 's/^enableVsync:.*/enableVsync:false/' \
            -e 's/^guiScale:.*/guiScale:0/' \
            "$dir/options.txt"
    fi
    sed -i -e 's/^pauseOnLostFocus:.*/pauseOnLostFocus:false/' "$dir/options.txt"
    grep -q '^pauseOnLostFocus:' "$dir/options.txt" || echo 'pauseOnLostFocus:false' >>"$dir/options.txt"
    grep -q '^onboardAccessibility:' "$dir/options.txt" || echo 'onboardAccessibility:false' >>"$dir/options.txt"
    grep -q '^skipMultiplayerWarning:' "$dir/options.txt" || echo 'skipMultiplayerWarning:true' >>"$dir/options.txt"
    sed -i -e 's/^skipMultiplayerWarning:.*/skipMultiplayerWarning:true/' "$dir/options.txt"
}

offline_uuid() {
    python3 - "$1" <<'PY'
import hashlib, sys, uuid
digest = bytearray(hashlib.md5(("OfflinePlayer:" + sys.argv[1]).encode("utf-8")).digest())
digest[6] = (digest[6] & 0x0F) | 0x30
digest[8] = (digest[8] & 0x3F) | 0x80
print(uuid.UUID(bytes=bytes(digest)))
PY
}

have_real_display() {
    [[ -n "${DISPLAY:-}" || -e /tmp/.X11-unix/X1 || -e /tmp/.X11-unix/X0 || -n "${WAYLAND_DISPLAY:-}" ]]
}

wait_server_ready() {
    local pid="$1" start="$2" server_log="$3" console="$4" ready="$5"
    local deadline=$((SECONDS + READY_TIMEOUT))
    local done_seen=0
    while (( SECONDS < deadline )); do
        if [[ -f "$server_log" ]] && grep -q 'Done (' "$server_log" 2>/dev/null; then
            done_seen=1
        fi
        if [[ -f "$ready" ]]; then
            if python3 -c 'import json,sys; sys.exit(0 if json.load(open(sys.argv[1])).get("ok") is True else 1)' "$ready"; then
                if [[ "$done_seen" -eq 1 ]]; then
                    return 0
                fi
            else
                echo "The world build reported a failure:" >&2
                cat "$ready" >&2 || true
                return 1
            fi
        fi
        if grep -q "PLAY_FAILED" "$server_log" "$console" 2>/dev/null; then
            echo "Server reported PLAY_FAILED" >&2
            return 1
        fi
        if grep -qE "Failed to start the minecraft server|Mod Loading has failed|Could not execute entrypoint" "$server_log" "$console" 2>/dev/null; then
            echo "Dedicated server failed during startup" >&2
            return 1
        fi
        if ! pid_alive "$pid" "$start"; then
            echo "Server process exited before it was ready" >&2
            return 1
        fi
        sleep 2
    done
    echo "Timed out after ${READY_TIMEOUT}s waiting for the server" >&2
    return 1
}

START_BEGUN=0
start_abort() {
    if [[ "$START_BEGUN" -eq 1 ]]; then
        echo "Start did not finish; shutting down what was launched" >&2
        do_stop || true
    fi
}

do_start() {
    local loader="${1:-forge}"
    case "$loader" in
        forge | fabric) ;;
        *)
            echo "Unknown loader: $loader (use forge or fabric)" >&2
            exit 2
            ;;
    esac

    if load_session && { server_running || client_running; }; then
        echo "A showcase session is already running (loader ${LOADER}, port ${PORT})."
        echo "Run 'bash tools/showcase-play.sh stop' first, or 'status' for the join details."
        return 0
    fi
    rm -f "$SESSION_FILE"

    local began=$SECONDS
    showcase_output_init "play" "$loader" "$ROOT"
    RUN_DIR="$SHOWCASE_RUN_DIR"
    LOADER="$loader"
    PLAYER="$PLAY_NAME"
    echo "Output: $RUN_DIR"

    local template wanted
    if ! template="$(bc_ensure_template)"; then
        echo "Showcase template could not be built" >&2
        return 1
    fi
    wanted=25565
    [[ "$loader" == "fabric" ]] && wanted=25566
    if ! PORT="$(bc_free_port "$wanted")"; then
        echo "No free port from ${wanted}" >&2
        return 1
    fi
    [[ "$PORT" != "$wanted" ]] && echo "Port ${wanted} is busy; using ${PORT}"

    local want_client=1
    if [[ "${BC_PLAY_NO_CLIENT:-}" == "1" ]]; then
        want_client=0
    elif have_real_display; then
        export BC_IN_XVFB=1 # a window for a person: never re-run under xvfb, which would end with this script
        bc_ensure_display "$0" "$@"
    else
        want_client=0
        echo "No display found (DISPLAY, WSLg and X sockets are all absent): starting the server only." >&2
    fi

    START_BEGUN=1
    trap start_abort EXIT
    trap 'exit 130' INT TERM

    SERVER_DIR="$ROOT/$loader/run-play-server"
    CLIENT_DIR="$ROOT/$loader/run-play-client"
    local server_spec="$RUN_DIR/server-command.json"
    local client_spec="$RUN_DIR/client-command.json"
    dump_command "$loader" runServer "$server_spec" -PmpSmoke=play

    mkdir -p "$SERVER_DIR"
    printf 'eula=true\n' >"$SERVER_DIR/eula.txt"
    write_server_properties "$SERVER_DIR/server.properties" "$PORT"
    rm -rf "$SERVER_DIR/world"
    cp -a "$template" "$SERVER_DIR/world"
    rm -f "$SERVER_DIR/world/session.lock" "$SERVER_DIR/showcase-play-ready.json" "$SERVER_DIR/logs/latest.log"

    local fifo="$RUN_DIR/server-stdin"
    mkfifo "$fifo"
    local server_console="$RUN_DIR/logs/server-console.log"
    setsid python3 "$ROOT/tools/mp-smoke-launch.py" "$server_spec" --working-dir "$SERVER_DIR" \
        --xmx 2048m --append-arg nogui <>"$fifo" >"$server_console" 2>&1 &
    SERVER_PID=$!
    sleep 1
    SERVER_START="$(pid_start "$SERVER_PID" || true)"
    STARTED_AT="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    save_session
    echo "Server pid ${SERVER_PID} on port ${PORT}; building the world as soon as it is up"

    if [[ "$want_client" -eq 1 ]]; then
        # Resolved while the server boots. The client run has no smoke property: nothing is scripted.
        if ! dump_command "$loader" runClient "$client_spec"; then
            want_client=0
            echo "Client command could not be resolved; continuing with the server only" >&2
        fi
    fi

    if ! wait_server_ready "$SERVER_PID" "$SERVER_START" "$SERVER_DIR/logs/latest.log" "$server_console" \
            "$SERVER_DIR/showcase-play-ready.json"; then
        tail -n 60 "$server_console" >&2 || true
        tail -n 30 "$SERVER_DIR/logs/latest.log" >&2 || true
        return 1
    fi
    DETAIL="$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1])).get("detail",""))' \
        "$SERVER_DIR/showcase-play-ready.json" 2>/dev/null || true)"
    READY_SECONDS=$((SECONDS - began))
    echo "Server is ready (${READY_SECONDS}s): ${DETAIL%% layout=*}"

    if [[ "$want_client" -eq 1 ]]; then
        prepare_client_dir "$CLIENT_DIR"
        local uuid
        uuid="$(offline_uuid "$PLAYER")"
        local client_console="$RUN_DIR/logs/client-console.log"
        setsid python3 "$ROOT/tools/mp-smoke-launch.py" "$client_spec" --working-dir "$CLIENT_DIR" \
            --xmx 3072m --username "$PLAYER" --uuid "$uuid" --game-dir "$CLIENT_DIR" \
            --append-arg=--quickPlayMultiplayer --append-arg="localhost:${PORT}" \
            --append-arg=--width --append-arg=1280 --append-arg=--height --append-arg=720 \
            </dev/null >"$client_console" 2>&1 &
        CLIENT_PID=$!
        sleep 1
        CLIENT_START="$(pid_start "$CLIENT_PID" || true)"
        save_session
        echo "Client pid ${CLIENT_PID} (player ${PLAYER}) is opening and will connect to localhost:${PORT}"
        wait_client_joined "$CLIENT_PID" "$CLIENT_START" "$SERVER_DIR/logs/latest.log" || true
    else
        save_session
    fi

    START_BEGUN=0
    trap - EXIT
    echo
    echo "Showcase is running ($(( SECONDS - began ))s since start)."
    print_status_body
}

wait_client_joined() {
    local pid="$1" start="$2" server_log="$3"
    local deadline=$((SECONDS + 240))
    while (( SECONDS < deadline )); do
        if grep -q "PLAY_WELCOME ${PLAYER}" "$server_log" 2>/dev/null; then
            echo "${PLAYER} joined the server"
            return 0
        fi
        pid_alive "$pid" "$start" || {
            echo "The client exited before joining; see ${RUN_DIR}/logs/client-console.log" >&2
            return 1
        }
        sleep 2
    done
    echo "The client has not joined after 240s; it may still be loading" >&2
    return 1
}

# ---- status ---------------------------------------------------------------------------------

print_status_body() {
    local client_note="none (server only)"
    if [[ -n "${CLIENT_PID:-}" ]]; then
        if client_running; then client_note="running (pid ${CLIENT_PID}, player ${PLAYER})"; else client_note="not running"; fi
    fi
    echo "  Loader:   ${LOADER}   Port: ${PORT}"
    echo "  Server:   running (pid ${SERVER_PID})   Client: ${client_note}"
    [[ -n "${STARTED_AT:-}" ]] && echo "  Started:  ${STARTED_AT}   ready after ${READY_SECONDS:-?}s"
    print_join
    echo "  World:    ${DETAIL%% layout=*}"
    echo "  Layout:   ${DETAIL#*layout=}"
    echo "  Logs:     ${RUN_DIR}/logs  (server: ${SERVER_DIR}/logs/latest.log)"
    local problems
    problems="$(showcase_log_problems "$SERVER_DIR/logs/latest.log" || true)"
    if [[ -n "$problems" ]]; then
        echo "  Server log problems (known noise hidden):"
        echo "$problems" | sed 's/^/    /'
    fi
}

do_status() {
    if ! load_session; then
        echo "Showcase play session: not running (no session file)."
        return 1
    fi
    if ! server_running; then
        echo "Showcase play session: not running (server pid ${SERVER_PID:-?} is gone)."
        echo "  Logs of the last session: ${RUN_DIR}/logs"
        echo "  Run 'bash tools/showcase-play.sh stop' to clear the record, or 'start' to begin again."
        return 1
    fi
    echo "Showcase play session: RUNNING"
    print_status_body
}

# ---- stop -----------------------------------------------------------------------------------

collect_logs() {
    [[ -n "${RUN_DIR:-}" && -d "$RUN_DIR/logs" ]] || return 0
    cp -f "$SERVER_DIR/logs/latest.log" "$RUN_DIR/logs/server.log" 2>/dev/null || true
    cp -f "$CLIENT_DIR/logs/latest.log" "$RUN_DIR/logs/client.log" 2>/dev/null || true
}

do_stop() {
    if ! load_session; then
        echo "Nothing to stop: no showcase play session is recorded."
        return 0
    fi
    local was_running=0
    if client_running; then
        was_running=1
        echo "Stopping the client (pid ${CLIENT_PID})"
        stop_pid "$CLIENT_PID" "$CLIENT_START"
    fi
    if server_running; then
        was_running=1
        echo "Saving the world and stopping the server (pid ${SERVER_PID})"
        # The stdin pipe is held open by the server; RDWR never blocks even if it is gone.
        if [[ -p "$RUN_DIR/server-stdin" ]]; then
            { printf 'save-all flush\nstop\n' >&3; } 3<>"$RUN_DIR/server-stdin" 2>/dev/null || true
        fi
        local i
        for i in $(seq 1 60); do
            server_running || break
            sleep 1
        done
        if server_running; then
            echo "The server did not stop on its own; sending SIGTERM"
            stop_pid "$SERVER_PID" "$SERVER_START"
        fi
    fi
    collect_logs
    rm -f "$SESSION_FILE" "$RUN_DIR/server-stdin" 2>/dev/null || true
    if [[ "$was_running" -eq 1 ]]; then
        echo "Stopped."
    else
        echo "Nothing was running (cleared a stale session record)."
    fi
    echo "Logs: ${RUN_DIR}/logs"
}

# ---- main -----------------------------------------------------------------------------------

case "${1:-}" in
    start) do_start "${2:-forge}" ;;
    status) do_status ;;
    stop) do_stop ;;
    *) usage ;;
esac
