#!/usr/bin/env bash
# One command for the whole local test pass.
#
#   bash tools/ci.sh                 # targeted: unit tests, server boot, showcase for the changed
#                                    # screens (Forge), two-client smoke on both loaders
#   bash tools/ci.sh --full          # same, but the showcase is the full regression set on both loaders
#   bash tools/ci.sh --no-mp         # skip the two-client smoke
#   bash tools/ci.sh --no-showcase   # skip screenshots (unit tests + server boot + mp smoke)
#   bash tools/ci.sh --only test,boot,showcase,mp
#
# Every stage runs even if an earlier one failed. The table at the end lists each stage's
# result; the exit code is non-zero if any stage failed. Set CI=true to keep --no-daemon.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
# shellcheck source=tools/common-lib.sh
source "$ROOT/tools/common-lib.sh"

FULL=0
STAGES="test,boot,showcase,mp"
while (( $# > 0 )); do
    case "$1" in
        --full) FULL=1 ;;
        --targeted) FULL=0 ;;
        --no-mp) STAGES="${STAGES/,mp/}" ;;
        --no-showcase) STAGES="${STAGES/,showcase/}" ;;
        --only) STAGES="${2:?--only needs a list}"; shift ;;
        -h | --help) sed -n '2,13p' "$0"; exit 0 ;;
        *) echo "Unknown option: $1 (see --help)" >&2; exit 2 ;;
    esac
    shift
done

bc_ensure_display "$ROOT/tools/ci.sh" "$@"
NAMES=()
CODES=()
SECS=()

run_stage() {
    local name="$1"
    shift
    [[ ",$STAGES," == *",$name,"* ]] || return 0
    echo "=== ci stage: $name ==="
    local started=$SECONDS code=0
    "$@" || code=$?
    NAMES+=("$name")
    CODES+=("$code")
    SECS+=("$((SECONDS - started))")
}

run_stage test bc_gradle test --console=plain
run_stage boot bash "$ROOT/tools/server-boot-smoke.sh"
if [[ "$FULL" -eq 1 ]]; then
    run_stage showcase bash "$ROOT/tools/showcase-run.sh" --regression --loader both
else
    run_stage showcase bash "$ROOT/tools/showcase-run.sh"
fi
run_stage mp bash "$ROOT/tools/multiplayer-smoke.sh" both

echo
echo "ci results ($([[ "$FULL" -eq 1 ]] && echo full || echo targeted)):"
FAILED=0
for i in "${!NAMES[@]}"; do
    status=PASS
    [[ "${CODES[$i]}" -eq 0 ]] || { status=FAIL; FAILED=1; }
    printf '  %-9s %-4s %ss\n' "${NAMES[$i]}" "$status" "${SECS[$i]}"
    echo "RESULT ci ${NAMES[$i]} $status ${SECS[$i]}s"
done
[[ "$FAILED" -eq 0 ]] || exit 1
