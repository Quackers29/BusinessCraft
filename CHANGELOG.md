# Changelog

All notable changes to BusinessCraft are documented in this file.

Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

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
