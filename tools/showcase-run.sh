#!/usr/bin/env bash
# Front door for showcase screenshots. Runs only what the change needs, or everything.
#
#   bash tools/showcase-run.sh                       # targeted: scenes picked from the git diff vs origin/main
#   bash tools/showcase-run.sh --dry-run             # print the plan, run nothing
#   bash tools/showcase-run.sh --routes town/map,platform
#   bash tools/showcase-run.sh --regression          # all, town, block, all-views, two-towns, testbed
#   bash tools/showcase-run.sh --loader both         # forge (default), fabric, or both
#   bash tools/showcase-run.sh --base origin/main --files a.java b.java   # plan for a given file list
#
# Targeted mode maps changed paths to routes and scenes with tools/showcase-targets.json
# (tools/showcase-select.py). A harness or tooling change selects the regression set; a change
# that maps to nothing but is not docs/tests runs the 'town' sanity scene; docs/tests-only
# changes run nothing. Each run is tools/showcase.sh; the table at the end repeats its RESULT lines.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
# shellcheck source=tools/showcase-output-lib.sh
source "$ROOT/tools/showcase-output-lib.sh"

LOADER=forge
DRY=0
REGRESSION=0
ROUTES=""
SELECT_ARGS=()
while (( $# > 0 )); do
    case "$1" in
        --regression) REGRESSION=1 ;;
        --dry-run) DRY=1 ;;
        --routes) ROUTES="${2:?--routes needs a list}"; shift ;;
        --loader) LOADER="${2:?--loader needs forge, fabric or both}"; shift ;;
        --base) SELECT_ARGS+=(--base "${2:?--base needs a ref}"); shift ;;
        --files) shift; SELECT_ARGS+=(--files "$@"); break ;;
        -h | --help) sed -n '2,16p' "$0"; exit 0 ;;
        *) echo "Unknown option: $1 (see --help)" >&2; exit 2 ;;
    esac
    shift
done
case "$LOADER" in
    forge) LOADERS=(forge) ;;
    fabric) LOADERS=(fabric) ;;
    both) LOADERS=(forge fabric) ;;
    *) echo "Unknown loader: $LOADER" >&2; exit 2 ;;
esac

if [[ -n "$ROUTES" ]]; then
    PLAN="routes $ROUTES"
elif [[ "$REGRESSION" -eq 1 ]]; then
    PLAN="$(python3 "$ROOT/tools/showcase-select.py" --regression)"
else
    PLAN="$(python3 "$ROOT/tools/showcase-select.py" ${SELECT_ARGS[@]+"${SELECT_ARGS[@]}"})"
fi

if [[ -z "$PLAN" ]]; then
    echo "No showcase-relevant changes; nothing to run"
    echo "RESULT showcase-plan - SKIP 0s"
    exit 0
fi
echo "Showcase plan (${LOADERS[*]}):"
echo "$PLAN" | sed 's/^/  /'
if [[ "$DRY" -eq 1 ]]; then
    exit 0
fi

bc_ensure_display "$ROOT/tools/showcase-run.sh" "$@"
RESULTS=()
FAILED=0
for loader in "${LOADERS[@]}"; do
    while IFS= read -r line; do
        [[ -n "$line" ]] || continue
        mode="${line%% *}"
        extra=""
        [[ "$line" == *" "* ]] && extra="${line#* }"
        echo "=== showcase $mode $loader ${extra} ==="
        out="$(mktemp /tmp/showcase-run-XXXXXX.log)"
        # shellcheck disable=SC2086  # extra is "" or one routes/checks argument without spaces
        # </dev/null: Gradle and the game would otherwise swallow the rest of the plan from stdin.
        if bash "$ROOT/tools/showcase.sh" "$mode" "$loader" $extra </dev/null 2>&1 | tee "$out"; then
            :
        fi
        result="$(grep '^RESULT ' "$out" | tail -n 1 || true)"
        rm -f "$out"
        [[ -n "$result" ]] || result="RESULT $mode $loader FAIL 0s shots=0 failures=1 (no RESULT line)"
        RESULTS+=("$result")
        [[ "$result" == *" PASS "* ]] || FAILED=1
    done <<<"$PLAN"
done

echo
printf '%s\n' "${RESULTS[@]}"
[[ "$FAILED" -eq 0 ]] || { echo "Showcase failed" >&2; exit 1; }
