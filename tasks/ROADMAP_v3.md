# BusinessCraft v3 Roadmap — Multi-Server Federation (Cross-Server Economy)

**Status**: Initial High-Level Vision (Subject to Significant Change)  
**Target**: Long-term Flagship Release  
**Focus**: Turning individual BusinessCraft servers into nodes of a living, interconnected global Minecraft economy.

## Vision for v3
Every BusinessCraft server becomes a living node in a global economic network. Resources, people (tourists and workers), and reputation flow between servers. What was once a self-contained town economy becomes part of something much larger — a persistent, player-driven multiverse economy where the actions on one server can meaningfully affect another.

This is the feature that could make BusinessCraft unique in the Minecraft modding space.

## Core Federation Features

- **Global Marketplace**: Each server publishes its available resources, prices, and surpluses/shortages to a shared, visible marketplace visible across all connected servers. Players and towns can initiate cross-server trade.
- **Tourist & Worker Migration**: Tourists and workers can physically travel between servers, carrying their history, preferences, and economic impact with them.
- **Shared Global Scoreboards & Leaderboards**: Global rankings for richest servers, most visited destinations, top-performing towns across the network, most reliable transport operators, etc.
- **Persistent Cross-Server Identity**: Tourist history, reputation, rewards, and contracts persist across server boundaries. A tourist who had a great experience on Server A can later seek out the same company or route on Server C.

## Inter-Server Travel System

Two distinct methods are proposed for moving people between servers. Both are designed to be fully optional and compatible with proxy solutions (Velocity, BungeeCord, etc.).

### Method 1: Standard Teleport Station (Proposed MVP)

- Players build a named "Teleport Station" block/platform on each connected server (e.g., "Station → Server B").
- When a Create-mod train stops at the station, all seated occupants (tourists + passengers) are detected.
- Passengers (except optionally the driver) are teleported to the matching station on the destination server.
- People spawn on the arrival platform ready to board local trains or explore.
- Extremely clean implementation with minimal timing or queuing complexity.
- Excellent redstone and automation integration.
- Reuses existing Create train stopping and passenger detection logic.

### Method 2: Express Teleport – "Phantom Train" Portal (Deluxe Experience)

- A glowing Portal Barrier arch is built directly on the tracks.
- A loaded train drives straight through the barrier on the origin server. The physical train never leaves its original server.
- The driver is automatically excluded from teleport (detected via Create locomotive/driver seat).
- Tourists enter a normal processing queue.
- Seated players are instantly teleported.

**On the Destination Server**:
- If a train is currently passing through the receiving portal, passengers intelligently snap into empty seats (with smart grouping priority).
- If no train is present, passengers appear on a small invisible "Arrival Platform" beside the tracks with cinematic effects (portal glow + train whoosh sounds).

**Quality of Life & Automation**:
- Players have a prominent "Abort / Return to Origin" button while in transit.
- The Portal Barrier Block provides rich redstone output:
  - Comparator signal (0–7 = queued tourists, 8–15 = queued players)
  - Digital pulses for train passing, per-second countdown, "player arrived", "tourist arrived"
- Designed to feel magical — a train disappears into glowing light and passengers reappear already riding a moving train on the other side.

## Technical & Design Considerations (Initial Thoughts)

- Both travel methods must be proxy-aware and should not require players to be online on multiple servers simultaneously.
- Cross-server data synchronization (tourist state, reputation, contract history, resource listings) will be one of the largest technical challenges.
- Security, anti-cheat, and griefing prevention become significantly more complex in a federated environment.
- Performance implications of global leaderboards and marketplace queries must be carefully managed.
- Economic balance across servers of different sizes and populations will require thoughtful design (supply/demand curves, travel costs, reputation weighting).
- Persistence of cross-server tourist memory and reputation is essential to making the federation feel alive rather than just a fast travel system.

## Relationship to Previous Releases

- v1.0 establishes the core tourist experience and basic contract system.
- v2 deepens the single-server economy through specialization, reputation, and player production.
- v3 then "lifts" that economy into a multi-server context, allowing everything built in v1 and v2 to have greater meaning and reach.

## Current Stance

This remains a long-term, high-ambition vision. Significant technical, design, and community work will be required before this becomes feasible. The two proposed travel methods represent different points on the complexity vs. magic spectrum and will likely be refined or replaced during actual design phases.

**Note**: This document represents early conceptual planning only. Every aspect — scope, technical approach, timeline, and even whether this release happens at all — is subject to major revision based on the success of v1.0, v2 development realities, and community feedback.