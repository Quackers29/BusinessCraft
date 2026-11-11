# Jade tooltip integration

BusinessCraft registers an **optional** [Jade](https://www.curseforge.com/minecraft/mc-mods/jade) plugin on Forge and Fabric (Minecraft 1.20.1). The mod loads and runs normally without Jade installed.

## What tooltips show

| Target | Lines |
|--------|--------|
| **Platform Interface** | Platform name, destination mode (Any / Auto / Specific), enabled-town count (Specific), top learned towns by score % (Auto, capped by config), link/spawn enabled state |
| **Town Centre** | Town name, platform count, current tourist count |
| **Tourist** | Synced origin/destination labels (`DATA_ORIGIN_LABEL`, `DATA_DEST_LABEL`), trip status (riding / waiting / traveling) from server data |

Block tooltips use Jade **server data providers** so multiplayer tooltips match the authoritative world state.

## Configuration

`config/businesscraft.toml`:

```toml
[jade]
    platformInterface = true
    townInterface = true
    tourist = true
    maxLearnedRoutesInTooltip = 3
```

Jade’s own per-plugin toggles (`config.jade.plugin_businesscraft.*`) appear in Jade’s settings UI; both Jade and BusinessCraft toggles must allow a line to show.

## Development dependencies

Gradle compiles against Jade API artifacts via CurseMaven (see `gradle.properties` for pinned versions). Forge uses `@WailaPlugin` discovery; Fabric lists the `jade` entrypoint in `fabric.mod.json`.

## Code layout

- `common/.../integration/jade/` — plugin + providers + formatting helpers
- Loader modules declare optional Jade dependencies only (no hard dependency in `fabric.mod.json` / `mods.toml`)
