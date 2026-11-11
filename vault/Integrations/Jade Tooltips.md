---
tags:
  - detail
  - integration
---
# Jade Tooltips

**Breadcrumb**: Integrations > Jade Tooltips  
**TL;DR**: Optional [Jade](https://www.curseforge.com/minecraft/mc-mods/jade) (WAILA-style) tooltips for Platform Interface, Town Centre, and tourists. BusinessCraft runs with or without Jade; server data providers keep block tooltips accurate in multiplayer.

## What it does
When Jade is installed, looking at a **Platform Interface** shows the platform name, a compact mode line (Specific includes enabled-town count), a single AUTO learned-route summary line (capped by `jade.maxLearnedRoutesInTooltip`), and either an inactive-link warning or spawns on/off. **Town Centre** blocks show town name plus platform and tourist counts. **Tourists** show server-synced origin/destination (including Any Town in multiplayer) plus riding / waiting / traveling status.

## Configuration
`businesscraft.toml` section `[jade]` — see [[Config/Configuration Loading]] and repo `docs/JADE_INTEGRATION.md`.

## Related
- [[Town/Platforms/Platform Interface]]
- [[Tourists/Tourists Overview]]
- [[Config/Config Overview]]
