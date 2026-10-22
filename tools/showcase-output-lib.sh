# Sourced by showcase runners — do not execute directly.
# shellcheck shell=bash

showcase_output_init() {
    local mode="$1"
    local loader="$2"
    local root="$3"
    export SHOWCASE_OUTPUT_ROOT="${SHOWCASE_OUTPUT_ROOT:-$root/showcase-output}"
    SHOWCASE_RUN_TS="$(date -u +%Y-%m-%d_%H%M%S)"
    SHOWCASE_RUN_DIR="$SHOWCASE_OUTPUT_ROOT/${SHOWCASE_RUN_TS}_${mode}_${loader}"
    mkdir -p "$SHOWCASE_RUN_DIR/screenshots" "$SHOWCASE_RUN_DIR/logs"
    mkdir -p "$SHOWCASE_OUTPUT_ROOT"
    cat >"$SHOWCASE_RUN_DIR/run-meta.json" <<EOF
{"timestampUtc":"${SHOWCASE_RUN_TS}","mode":"${mode}","loader":"${loader}"}
EOF
    ln -sfn "$SHOWCASE_RUN_DIR" "$SHOWCASE_OUTPUT_ROOT/latest"
    ln -sfn "$SHOWCASE_RUN_DIR" "$SHOWCASE_OUTPUT_ROOT/latest_${mode}_${loader}"
    echo "$SHOWCASE_RUN_DIR"
}

showcase_collect_logs() {
    local mc_run_dir="$1"
    local gradle_log="$2"
    local out_dir="$3"
    cp -f "$gradle_log" "$out_dir/logs/gradle-console.log" 2>/dev/null || true
    if [[ -f "$mc_run_dir/logs/latest.log" ]]; then
        cp -f "$mc_run_dir/logs/latest.log" "$out_dir/logs/client.log"
    fi
    if [[ -f "$mc_run_dir/logs/debug.log" ]]; then
        cp -f "$mc_run_dir/logs/debug.log" "$out_dir/logs/debug.log"
    fi
    local world_log
    world_log="$(find "$mc_run_dir/saves" -path '*/logs/latest.log' 2>/dev/null | head -1 || true)"
    if [[ -n "$world_log" && -f "$world_log" ]]; then
        cp -f "$world_log" "$out_dir/logs/server.log"
    elif [[ -f "$out_dir/logs/client.log" ]]; then
        cp -f "$out_dir/logs/client.log" "$out_dir/logs/server.log"
        echo "(integrated server — same as client.log)" >"$out_dir/logs/server.log.note"
    fi
}

showcase_copy_screenshots() {
    local shotdir="$1"
    local out_dir="$2"
    shopt -s nullglob
    for f in "$shotdir"/showcase_*.png "$shotdir"/showcase_map_*.png; do
        cp -f "$f" "$out_dir/screenshots/"
    done
    shopt -u nullglob
}

showcase_append_index() {
    local root="$1"
    local run_dir="$2"
    local mode="$3"
    local loader="$4"
    local status="$5"
    local index="$root/index.json"
    python3 - "$index" "$run_dir" "$mode" "$loader" "$status" <<'PY'
import json, sys, os
index, run_dir, mode, loader, status = sys.argv[1:6]
entries = []
if os.path.isfile(index):
    try:
        entries = json.load(open(index))
    except Exception:
        entries = []
entries.insert(0, {
    "dir": run_dir,
    "mode": mode,
    "loader": loader,
    "status": status,
    "timestampUtc": os.path.basename(run_dir).split("_")[0] + "T" + os.path.basename(run_dir).split("_")[1].replace("-", ":") if False else os.path.basename(run_dir),
})
entries = entries[:200]
json.dump(entries, open(index, "w"), indent=2)
PY
    {
        echo '<!DOCTYPE html><html><head><meta charset="utf-8"><title>Showcase runs</title></head><body>'
        echo '<h1>Showcase output</h1><p>Latest: <a href="latest/">latest</a></p><ul>'
        python3 - "$index" <<'PY'
import json, sys, os
path = sys.argv[1]
if not os.path.isfile(path):
    sys.exit(0)
for e in json.load(open(path))[:30]:
    name = os.path.basename(e["dir"])
    print(f'<li><a href="{name}/">{name}</a> — {e.get("mode")} / {e.get("loader")} — {e.get("status")}</li>')
PY
        echo '</ul></body></html>'
    } >"$root/index.html"
}
