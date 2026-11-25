# Changelog

All notable changes to BusinessCraft are documented in this file.

Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added
- Sixteen more tourist looks. The builtin catalog is the list in `data/businesscraft/tourist_looks` (32 files). Each new look has its own skin and outfit colours. New hat shapes are a visor, bucket hat, turban, cycling helmet, and fedora. New held or worn items are binoculars, a briefcase, a canteen, a newspaper, and a tote. The other new hats and items are recolours of meshes that were already there. Spawn gives every catalog entry an equal chance and syncs the same packed int. The umbrella index stays empty. See `docs/TOURIST_MODELS.md`.
- `bash tools/showcase-play.sh start --production` starts the showcase server with `[production]` and `[research]` on (in that server's own config only) and four labelled towns to try them on: Starterton (biome kit only), Farmstead (full stores, recipes run), Highmark (mid-game, research AI active) and Hungerford (starving: low happiness, min-stock conditions fail). A plain `start` is unchanged.
- A fresh install now gets the shipped `items.csv`, `productions.csv`, `upgrades.csv` and `biomes.csv`. Before, each file was replaced by a smaller built-in version with different ids (`basic_farming`, `basic_taxes`) and no `tourism` recipe. Files that already exist are never overwritten.
- The four CSV files are checked after every load. The log gets one warning per problem with the file and line or entry: unknown resources in inputs, outputs or costs, unknown tokens in amount expressions (such as `1*popp`), effect targets that are not a recipe, upgrade or known stat, unknown prerequisites and biome starting upgrades, short rows, duplicate ids, and a cycle time of 0 or less. Nothing about how production or upgrades run changes.
- Unit tests for production and upgrades: CSV parsing, validation, recipe processing over whole cycles, upgrade modifiers and research, research AI choices, and a test that loads the shipped CSVs and expects no warnings.

### Fixed
- The Balanced Hamlet biome variant listed a starting upgrade (`border_basic`) that does not exist. It is removed.
- `upgrades.csv` rows with an empty effects column were silently dropped, and a comma inside a quoted description shifted every later column. `upgrades.csv` and `items.csv` now read rows the same way as `productions.csv` and `biomes.csv`: quotes are respected, empty trailing columns are kept, and `#` comment lines are skipped.
- `businesscraft.toml` comments for `[production]` and `[research]` now say what each key does, and the `[research]` comment sits above its key.

### Removed
- `production.tickInterval` is gone from `businesscraft.toml`. Nothing read it. An existing value in a config file is ignored.

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
