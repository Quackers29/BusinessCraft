#!/usr/bin/env bash
# Build showcase-views-index.html from one or more showcase-views-manifest.json files.
set -euo pipefail

OUT="${1:?output html path}"
shift

python3 - "$OUT" "$@" <<'PY'
import html
import json
import sys
from pathlib import Path

out_path = Path(sys.argv[1])
manifests = [Path(p) for p in sys.argv[2:]]

entries = []
for manifest in manifests:
    if not manifest.is_file():
        continue
    data = json.loads(manifest.read_text(encoding="utf-8"))
    loader = data.get("loader", manifest.parent.name)
    shot_dir = manifest.parent / "screenshots"
    for route in data.get("routes", []):
        route = dict(route)
        route["_loader"] = loader
        route["_shot_dir"] = str(shot_dir)
        entries.append(route)

rows = []
for e in entries:
    route = html.escape(str(e.get("route", "")))
    loader = html.escape(str(e.get("_loader", "")))
    steps = html.escape(", ".join(e.get("steps") or []))
    ok = e.get("ok", False)
    status = "ok" if ok else html.escape(str(e.get("error", "failed")))
    thumb = ""
    shot = e.get("screenshot")
    if shot and ok:
        img_path = Path(e["_shot_dir"]) / shot
        rel = html.escape(shot)
        if img_path.is_file():
            thumb = f'<img src="{rel}" alt="{route}" loading="lazy" />'
        else:
            thumb = f'<span class="missing">{rel}</span>'
    rows.append(
        f"<tr><td>{thumb}</td><td><code>{route}</code></td><td>{loader}</td>"
        f"<td><pre>{steps}</pre></td><td>{status}</td></tr>"
    )

doc = f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8"/>
<title>BusinessCraft UI showcase index</title>
<style>
body {{ font-family: system-ui, sans-serif; margin: 1.5rem; background: #1a1a1a; color: #eee; }}
table {{ border-collapse: collapse; width: 100%; }}
th, td {{ border: 1px solid #444; padding: 0.5rem; vertical-align: top; }}
th {{ background: #333; }}
img {{ max-width: 320px; height: auto; border: 1px solid #555; }}
pre {{ white-space: pre-wrap; font-size: 0.75rem; margin: 0; }}
code {{ color: #9cf; }}
.missing {{ color: #f88; }}
</style>
</head>
<body>
<h1>BusinessCraft UI views</h1>
<p>{len(entries)} capture(s) from {len(manifests)} manifest(s).</p>
<table>
<thead><tr><th>Preview</th><th>Route</th><th>Loader</th><th>Steps</th><th>Status</th></tr></thead>
<tbody>
{"".join(rows)}
</tbody>
</table>
</body>
</html>
"""
out_path.parent.mkdir(parents=True, exist_ok=True)
out_path.write_text(doc, encoding="utf-8")
print(out_path)
PY
