# Changelog

All notable changes to BusinessCraft are documented in this file.

Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Changed
- Right-clicking a tourist opens a tourist screen instead of the villager trade window. The title is the tourist's personal name and rank, and the screen shows the origin town, destination, distance, and progress to the next rank.
- The old barrier "info" row is gone. Locked deals say which rank unlocks them. The Travel Ticket tooltip says it is a paper souvenir and what Any Town means.
- Tourist deals are a wider, look-themed set (still records in code, not a config file). The distance fare is unchanged. Nothing a tourist sells is worth more than the goods they ask for.
- A tourist shows only their personal name, and only from a few blocks away. The name is chosen when they spawn and kept in the save.
- The tourist screen times the visit in minutes. The trade list scrolls when the window is short.

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
