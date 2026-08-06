# BusinessCraft Dump Viewer

Standalone browser tool (not part of the mod) for inspecting `/bc debug dump` SNBT files.

## Open

1. Open `index.html` in a browser (double-click, or drag into Chrome/Firefox/Edge).
2. **Open file** or drop a `bc-dump-*.snbt` from:

   `forge/run/saves/<world>/businesscraft/dumps/`  
   (or the Fabric run saves equivalent)

No install, no server, no build.

## Features

- **Split view**: left outline (levels → towns → sections, market), right detail tree
- **Collapsible** nodes (double-click row or click ▶/▼)
- **Collapse heavy**: contracts, visitHistory, storage, bids, platforms, etc.
- Filter box for keys/values
- Minecraft int-array UUIDs (`[I; …]`) shown as dashed UUID strings

## From WSL / terminal

```bash
# Linux / WSL with a browser available:
xdg-open tools/bc-dump-viewer/index.html

# Or on Windows from the repo (PowerShell):
# start tools/bc-dump-viewer/index.html
```
