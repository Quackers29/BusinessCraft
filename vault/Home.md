# BusinessCraft Knowledge Vault

Living documentation of BusinessCraft's systems, built incrementally alongside unit test coverage via the Test + Docs Loop (`tasks/test_doc_loop.md` in the repo).

## Start here
**[[Mod Overview]]** — the executive summary and reading paths.

The vault is layered so you only read as deep as you need:
1. **[[Mod Overview]]** (1 page) → 2. **Area overview notes** (`#overview`, 10–20 lines each, plain language) → 3. **Detail notes** (`#detail` — exact formulas, classes, edge cases; reference material, not reading material).

Humans reading "how do the processes work?" stop at layer 2. To hide detail notes in Obsidian: search with `-tag:#detail`, or filter `#detail` out of graph view. Within a detail note, everything below the collapsible "Deep reference" callout is skippable. Every note starts with a `TL;DR:` line — `grep -r "TL;DR:" vault/` summarizes the entire vault.

## How this vault grows
Each loop iteration documents one unit of game logic (a calculation, a rule, a process) and covers it with unit tests. The note is written from what the **code actually does** — this vault is the ground truth reference for both humans and AI sessions.

## Structure
Folders mirror game systems, nesting from system → subsystem → process:

- **Economy/** — tourist payments, milestones, currency
- **Trade/** — contracts, bidding, courier delivery, town-to-town trades, global market
- **Town/** — town lifecycle, population, resources, boundaries, visit history
- **Tourists/** — spawning, AI, expiry, capacity, ride mechanics
- **Platforms/** — platform rules, paths, destinations
- **[[Production/Production Overview|Production/]]** — production sites, upgrades, research (Effect Value Calculation T-016)
- **Config/** — configuration system and key settings
- **Network/** — packet flows (mostly DOC-ONLY material)
- **UI/** — screen/data flows (mostly DOC-ONLY material)

## Tracking
- [[_meta/Coverage Ledger|Coverage Ledger]] — what's covered, in progress, blocked
- [[_meta/Loop Log|Loop Log]] — one line per completed iteration
- [[_meta/Note Template|Note Template]] — required structure for every note
