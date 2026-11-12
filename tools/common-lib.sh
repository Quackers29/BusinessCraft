# Sourced by the tools/*.sh runners — do not execute directly.
# shellcheck shell=bash
# Gradle, display, port, template and result helpers shared by every runner.

BC_LIB_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# OpenAL Soft's null backend: no sound device is needed, so the "Failed to open OpenAL device"
# stack trace does not appear on machines without audio (WSL2, CI, containers).
export ALSOFT_DRIVERS="${ALSOFT_DRIVERS:-null}"

# ---- Gradle ---------------------------------------------------------------

# Local runs reuse the Gradle daemon (saves ~15-20 s of configuration per call).
# CI (CI=true) and BC_NO_DAEMON=1 keep --no-daemon so nothing outlives the job.
bc_gradle() {
    local flag=()
    if [[ "${CI:-}" == "true" || "${BC_NO_DAEMON:-}" == "1" ]]; then
        flag=(--no-daemon)
    fi
    (cd "$BC_LIB_ROOT" && bash ./gradlew ${flag[@]+"${flag[@]}"} "$@")
}

# ---- Display --------------------------------------------------------------

# Call first in a script that opens game windows: bc_ensure_display "$0" "$@".
# WSLg, X11 and the cloud VM's :1 display are used as they are. With no display at all and
# xvfb-run installed, the script re-runs itself once under xvfb-run so the server and both
# clients of a smoke share one virtual screen.
bc_ensure_display() {
    if [[ -n "${DISPLAY:-}" ]]; then
        return 0
    fi
    if [[ -e /tmp/.X11-unix/X1 ]]; then
        export DISPLAY=:1
        return 0
    fi
    if [[ -e /tmp/.X11-unix/X0 ]]; then
        export DISPLAY=:0
        return 0
    fi
    if [[ -z "${BC_IN_XVFB:-}" ]] && command -v xvfb-run >/dev/null 2>&1; then
        echo "No display found; re-running under xvfb-run" >&2
        exec env BC_IN_XVFB=1 xvfb-run -a -s "-screen 0 1920x1080x24" bash "$@"
    fi
    echo "No DISPLAY and no xvfb-run: the game window cannot open (install xvfb or use WSLg)" >&2
    return 0
}

# ---- Processes and ports --------------------------------------------------

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

# First free TCP port at or above $1 (up to 50 tries). Prints it.
bc_free_port() {
    local port="$1"
    local tries=0
    while port_busy "$port"; do
        port=$((port + 1))
        tries=$((tries + 1))
        if (( tries > 50 )); then
            echo "no free port found from $1" >&2
            return 1
        fi
    done
    echo "$port"
}

# ---- Dedicated server properties and the showcase template ----------------

# Flat world with explicit layers: an empty generator-settings makes Minecraft log
# "No key layers in MapLike" on every boot. Same ground as the default flat preset
# (bedrock, 2 dirt, grass) without villages.
bc_write_server_properties() {
    local file="$1"
    local port="$2"
    cat >"$file" <<PROPS
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
generator-settings={"biome"\:"minecraft\:plains","layers"\:[{"block"\:"minecraft\:bedrock","height"\:1},{"block"\:"minecraft\:dirt","height"\:2},{"block"\:"minecraft\:grass_block","height"\:1}],"structure_overrides"\:[]}
enable-command-block=false
motd=BusinessCraft test server
PROPS
}

bc_default_template() {
    printf '%s\n' "${XDG_CACHE_HOME:-$HOME/.cache}/businesscraft/superflat-template"
}

# Where the superflat template lives. Order:
#   1. SHOWCASE_TEMPLATE (environment)
#   2. showcaseTemplate= in gradle.properties
#   3. /opt/businesscraft/superflat-template, when it exists (the cloud VM image)
#   4. ~/.cache/businesscraft/superflat-template (built on first use)
bc_template_path() {
    if [[ -n "${SHOWCASE_TEMPLATE:-}" ]]; then
        printf '%s\n' "$SHOWCASE_TEMPLATE"
        return
    fi
    local from_props=""
    if [[ -f "$BC_LIB_ROOT/gradle.properties" ]]; then
        from_props="$(grep -E '^showcaseTemplate=' "$BC_LIB_ROOT/gradle.properties" 2>/dev/null | tail -1 | cut -d= -f2- | tr -d '\r' || true)"
    fi
    if [[ -n "$from_props" ]]; then
        printf '%s\n' "$from_props"
    elif [[ -f /opt/businesscraft/superflat-template/level.dat ]]; then
        printf '%s\n' /opt/businesscraft/superflat-template
    else
        bc_default_template
    fi
}

# Prints the template path, building it first when level.dat is missing.
bc_ensure_template() {
    local path
    path="$(bc_template_path)"
    if [[ ! -f "$path/level.dat" ]]; then
        echo "Showcase template missing at $path; building it from a dedicated server run" >&2
        bash "$BC_LIB_ROOT/tools/showcase-template.sh" "$path" >&2 || return 1
    fi
    printf '%s\n' "$path"
}

# ---- Log problems ---------------------------------------------------------

# Prints the ERROR lines of one Minecraft log that are not listed in tools/known-noise.txt.
# At most 10 lines, each cut to 200 characters. Never reads the Gradle console log.
showcase_log_problems() {
    local log="$1"
    [[ -f "$log" ]] || return 0
    { grep -E '/ERROR[]\)]' "$log" 2>/dev/null || true; } \
        | { grep -vEf "$BC_LIB_ROOT/tools/known-noise.txt" || true; } \
        | cut -c1-200 | head -n 10
}

# ---- Results --------------------------------------------------------------

# showcase_write_result <run-dir> <mode> <loader> <PASS|FAIL> <seconds> <shots> [failure...]
# Writes <run-dir>/result.json and prints the one line agents can grep:
#   RESULT <mode> <loader> <PASS|FAIL> <seconds>s shots=<n> [failures=<n>] dir=<run-dir>
showcase_write_result() {
    local dir="$1" mode="$2" loader="$3" status="$4" seconds="$5" shots="$6"
    shift 6
    python3 - "$dir" "$mode" "$loader" "$status" "$seconds" "$shots" "$@" <<'PY'
import json, sys
dir_, mode, loader, status, seconds, shots, *failures = sys.argv[1:]
doc = {"status": status, "mode": mode, "loader": loader, "seconds": int(seconds),
       "shots": int(shots), "failures": failures, "dir": dir_}
json.dump(doc, open(f"{dir_}/result.json", "w"), indent=2)
line = f"RESULT {mode} {loader} {status} {seconds}s shots={shots}"
if failures:
    line += f" failures={len(failures)}"
print(line + f" dir={dir_}")
PY
}
