# Changelog

All notable changes to BusinessCraft are documented in this file.

Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Fixed
- Biome kit CSV loading accepts both column orders. The bundled file puts the description before the weight, and the generated stub used to put the weight first, so starting values such as `food:140` were unlocked as upgrade nodes. New stub files use the bundled column order.

### Added
- Production debug event log. One listener writes a CSV when a recipe completes or changes stall, research starts or finishes, an upgrade level is gained, population, border, or work units change, a contract changes stage, or a town is created. Per-tick progress is not logged. The file is `<run>/businesscraft-debug/production-events-<stamp>.csv` unless `[debug] productionEventLogDir` is set, and a per-town summary is written beside it when the server stops. `[debug] productionEventLog` defaults to false. `/bc debug production [town]` adds one snapshot row while the log is open. `bash tools/showcase-play.sh start --production` turns production, research, contracts, and the log on for that run and writes into `showcase-output/<run>/production/`. The same log runs headless: `./gradlew test --tests '*ProductionSimulation*'` (1000 ticks) or `./gradlew productionSim` (6000 ticks) into `common/build/production-sim/`.
- Sixteen more tourist looks. The builtin catalog is the list in `data/businesscraft/tourist_looks` (32 files). Each new look has its own skin and outfit colours. New hat shapes are a visor, bucket hat, turban, cycling helmet, and fedora. New held or worn items are binoculars, a briefcase, a canteen, a newspaper, and a tote. The other new hats and items are recolours of meshes that were already there. Spawn gives every catalog entry an equal chance and syncs the same packed int. The umbrella index stays empty. See `docs/TOURIST_MODELS.md`.

### Changed
- Right-clicking a tourist opens a tourist screen instead of the villager trade window. The title is the tourist's personal name and rank, and the screen shows the origin town, destination, distance, and progress to the next rank.
- The old barrier "info" row is gone. Locked deals say which rank unlocks them. The Travel Ticket tooltip says it is a paper souvenir and what Any Town means.
- Tourist deals are a wider, look-themed set (still records in code, not a config file). The distance fare is unchanged. Nothing a tourist sells is worth more than the goods they ask for.
- A tourist shows their first name above their head, from a few blocks away, including through walls. The full name stays on the tourist screen. The name is chosen when they spawn and kept in the save.
- The eight newer tourist looks use the same face layout as the original eight: the same brow, eye, and mouth placement, and the same blush, stubble, glasses, beard, and moustache masks.
- The tourist screen times the visit in minutes. The trade list scrolls when the window is short.
- Builds are numbered `0.9.0-alphaN`. `tools/bump-alpha.sh` adds one to N on every merge to main.

## [0.9.0-beta] — 2026-07-16

First public beta. Focus: polished core tourism loop on Forge and Fabric 1.20.1.

**Note:** This is a beta. Saved worlds and config may not carry forward to v1.0. Report issues on [GitHub Issues](https://github.com/Quackers29/BusinessCraft/issues).

### Added
- Dual-loader packaging (Forge + Fabric) with shared common module
- Town founding, platforms, tourist travel, distance-based emerald payments, and milestone rewards
- Payment board claims with transactional buffer placement
- Subsystem feature toggles (`tourists`, `production`, `research`, `trading`, `contracts`) with server-side honesty gates
- Tourism recipe ownership (Option C): tourist generation can run with production disabled
- Distance anti-cheat clamp and configurable minimum payout distance
- Obsidian knowledge vault (`vault/`) documenting game logic

### Changed
- Version renumbered from `1.0.0` to `0.9.0-beta` (1.0 reserved for contracts / prestige / worldgen)
- Forge release jar now includes `assets/`, `data/`, and `pack.mcmeta` (was incorrectly excluded)

### Fixed
- Payment board partial-placement claim bugs (T-012)
- Duplicate `OpenPaymentBoardPacket` registration on Forge

### Removed
- Orphaned Fabric stub packets / placeholder API interfaces
- Unused UI demo screens from main sources
]
