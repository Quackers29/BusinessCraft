# BusinessCraft Knowledge Vault

Living documentation of BusinessCraft's systems, built incrementally alongside unit test coverage via the Test + Docs Loop (`tasks/test_doc_loop.md` in the repo).

## How this vault grows
Each loop iteration documents one unit of game logic (a calculation, a rule, a process) and covers it with unit tests. The note is written from what the **code actually does** — this vault is the ground truth reference for both humans and AI sessions.

## Structure
Folders mirror game systems, nesting from system → subsystem → process:

- **Economy/** — tourist payments, milestones, currency
- **Trade/** — contracts, bidding, courier delivery, town-to-town trades, global market
- **Town/** — town lifecycle, population, resources, boundaries, visit history
- **Tourists/** — spawning, AI, expiry, capacity, ride mechanics
- **Platforms/** — platform rules, paths, destinations
- **Production/** — production sites, upgrades, research
- **Config/** — configuration system and key settings
- **Network/** — packet flows (mostly DOC-ONLY material)
- **UI/** — screen/data flows (mostly DOC-ONLY material)

## Tracking
- [[_meta/Coverage Ledger|Coverage Ledger]] — what's covered, in progress, blocked
- [[_meta/Loop Log|Loop Log]] — one line per completed iteration
- [[_meta/Note Template|Note Template]] — required structure for every note
