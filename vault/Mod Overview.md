---
tags:
  - overview
---
# BusinessCraft — Mod Overview

**TL;DR**: Town management + tourism economy mod (Forge & Fabric 1.20.1) — players place a Town Centre block to found a town, add up to ten Platform Interface blocks inside the boundary, and earn emeralds when tourists ride real distances between towns.

## The core loop
1. **Found a town** by placing a **Town Centre** block (`town_interface`; no town owner). Placement must respect each town's border radius (`general.defaultBorderRadius`, default 50) and stay at least `newRadius + existingRadius` away from other towns.
2. **Build platforms** by placing **Platform Interface** blocks inside the boundary (up to 10 per town) — each block is one platform with path, destinations, search radius, and spread edited on the block screen ([[Town/Platforms/Platform Interface]]).
3. **Tourists spawn** at enabled platforms (population-driven) and **wait for minecarts or Create trains** — they do not walk ([[Tourists/Looks/Tourist Looks Names and Ranks]]).
4. **Arrivals pay**: the destination town earns emeralds based on the distance each tourist actually traveled (see [[Economy/Tourist Payments/Distance Payment Calculation]]). Long journeys also trigger milestone rewards.
5. **Towns grow**: tourism increases population, which raises tourist capacity, enabling more traffic — and feeds town upgrades, production, and research.

## System areas (one overview note each)
- [[Economy/Economy Overview]] — tourist payments, milestones, global market, currency
- [[Trade/Trade Overview|Trade]] — sell contracts (towns auction surplus), courier delivery contracts, town-to-town trading
- [[Town/Town Overview|Town]] — lifecycle, population, multi-tier storage (resources + escrow + personal), boundaries, visit history, payment board
- [[Tourists/Tourists Overview|Tourists]] — spawning, allocation, capacity, AI behaviors, expiry, ride mechanics
- [[Production/Production Overview|Production]] — production recipes (farming, population, crafting) and upgrade nodes that scale town stats by level
- [[Config/Config Overview|Config]] — settings file, hot reload, key settings
- [[Core/Core Overview|Core]] — cross-cutting utilities (time formatting for all UIs, success/failure handling)

Platforms are documented under Town: [[Town/Platforms/Platform Interface]], [[Town/Platforms/Platform Data Model|Platform Data Model]], and [[Town/Platforms/Platform Management|Platform Management]]. Town/product choices: [[Town/Decisions/Town Centre and Platform Decisions]].

## Reading paths
- **Human, "how does the mod work?"**: this note → area overview notes. Stop there; that's the full picture in plain language.
- **Developer/AI, working on a specific system**: same path, then follow links into `#detail` notes for exact formulas, classes, and edge cases.
- **Hide the deep stuff in Obsidian**: detail notes are tagged `#detail` — exclude them with `-tag:#detail` in search, or filter them out of graph view. Inside a detail note, everything below the "Deep reference" callout is skippable.
