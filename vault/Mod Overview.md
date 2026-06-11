---
tags:
  - overview
---
# BusinessCraft — Mod Overview

**TL;DR**: Town management + tourism economy mod (Forge & Fabric 1.20.1) — players found towns, build transport platforms, and earn emeralds when tourists travel real distances between towns.

## The core loop
1. **Found a town** by placing a Town Interface block (minimum distance from other towns enforced).
2. **Build platforms** (up to 10 per town) — designated arrival/departure areas that can target specific destination towns or "any town".
3. **Tourists spawn** at towns (population-driven) and travel via minecarts, Create trains, or on foot.
4. **Arrivals pay**: the destination town earns emeralds based on the distance each tourist actually traveled (see [[Economy/Tourist Payments/Distance Payment Calculation]]). Long journeys also trigger milestone rewards.
5. **Towns grow**: tourism increases population, which raises tourist capacity, enabling more traffic — and feeds town upgrades, production, and research.

## System areas (one overview note each)
- [[Economy/Economy Overview]] — tourist payments, milestones, global market, currency
- [[Trade/Trade Overview|Trade]] — sell contracts (towns auction surplus), courier delivery contracts, town-to-town trading
- [[Town/Town Overview|Town]] — lifecycle, population, multi-tier storage (resources + escrow + personal), boundaries, visit history, payment board
- [[Tourists/Tourists Overview|Tourists]] — spawning, allocation, capacity, AI behaviors, expiry, ride mechanics (see Tourist Allocation T-009)
- [[Town/Platforms/Platform Data Model|Platforms]] — paths, destinations, enable/disable (data model T-015), visualization *(full Platforms overview pending)*
- Production — production sites, upgrades, research *(overview pending)*
- [[Config/Config Overview|Config]] — TOML config system, hot reload, key settings (T-014)

## Reading paths
- **Human, "how does the mod work?"**: this note → area overview notes. Stop there; that's the full picture in plain language.
- **Developer/AI, working on a specific system**: same path, then follow links into `#detail` notes for exact formulas, classes, and edge cases.
- **Hide the deep stuff in Obsidian**: detail notes are tagged `#detail` — exclude them with `-tag:#detail` in search, or filter them out of graph view. Inside a detail note, everything below the "Deep reference" callout is skippable.
