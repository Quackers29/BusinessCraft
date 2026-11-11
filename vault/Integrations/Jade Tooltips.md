---
tags:
  - detail
  - integration
---
# Jade Tooltips

**Breadcrumb**: Integrations > Jade Tooltips  
**TL;DR**: Optional [Jade](https://www.curseforge.com/minecraft/mc-mods/jade) (WAILA-style) tooltips for Platform Interface, Town Centre, and tourists. BusinessCraft runs with or without Jade; server data providers keep block tooltips accurate in multiplayer.

## What it does
When Jade is installed, looking at a **Platform Interface** shows the platform name, destination mode (Any / Auto / Specific), Specific enabled-town count, Auto learned-route shares (capped by `jade.maxLearnedRoutesInTooltip`), and spawn/link state. **Town Centre** blocks show town name plus platform and tourist counts. **Tourists** show synced origin/destination labels and a riding / waiting / traveling status line.

## Configuration
`businesscraft.toml` section `[jade]` — see [[Config/Configuration Loading]] and repo `docs/JADE_INTEGRATION.md`.

## Related
- [[Town/Platforms/Platform Interface]]
- [[Tourists/Tourists Overview]]
- [[Config/Config Overview]]
