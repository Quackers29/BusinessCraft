---
tags:
  - overview
  - tourists
---
# Tourists Overview

**TL;DR**: Tourists are AI villagers that spawn at towns once the population is big enough, travel via platforms (on foot, minecarts, or trains), and on arrival pay the destination town emeralds based on the distance actually traveled, plus optional milestone bonuses. Towns cap how many of their tourists can be out at once, and destinations are chosen fairly so bigger towns receive proportionally more visitors.

## Processes in this area
- **[[Tourists/Capacity/Tourist Allocation|Tourist Allocation]]** (T-009) — When a tourist could go to several destinations, the choice is designed to favor towns in proportion to their population so bigger towns get proportionally more visitors. (In the current code the balancing memory is never actually fed, so selection effectively follows list order — quirk documented in the note.)
- **[[Tourists/Capacity/Tourist Capacity Calculation|Tourist Capacity Calculation]]** (T-027) — A town can only send tourists once its population reaches a minimum (default 5), and the number of its tourists out in the world at once is capped — the cap grows through town upgrades, with a global server safety limit on top.
- **[[Tourists/Spawning/Tourist Spawning and Destination Selection|Tourist Spawning and Destination Selection]]** (T-037) — When a town passes its capacity checks, a tourist appears at a random spot along one of its platforms (trying a few spots to avoid crowding). Its destination is one of the platform's chosen target towns — picked fairly by population — or left open as "any town". Its lifespan is set from config at spawn.
- **[[Tourists/Lifecycle/Tourist Distance Tracking, Ride Extension and Expiry|Tourist Distance Tracking, Ride Extension and Expiry]]** (T-038) — Tourists measure the real distance they travel (the value that actually pays destinations); their lifetime only counts down while they stand still, gets extended when they board minecarts or trains, and their appearance upgrades as they accumulate travel distance.
- AI behaviors (gossip, window gazing, Create train integration) live on the tourist entity itself and need a running game to test; they are covered descriptively in the detail notes only.
- **Where new tourists come from** (July 2026, Option C) — Tourist generation is still defined by the tourism recipe in the productions config (cycle time, optional costs/conditions, output). The tourist system owns ticking that recipe; the production toggle only runs economy recipes and skips tourism so both toggles on never double-grant. Cap stall counts active plus pending tourists against tourist capacity. 0.9 ships a free tourism row; full-mod servers can restore food/money costs in their config copy.
- **Research off still allows tourism** (July 2026, toggle honesty) — New towns always keep the basic settlement starting unlock even when research is disabled, because that unlock is what grants tourist generation and a base tourist capacity. Advanced research starting nodes and production stock are skipped instead. See [[Config/Subsystem Toggle Honesty]].

## How it connects
Allocation and capacity sit between the Town population/economy systems and the actual spawning + payment pipelines. Fair destination choice uses live town populations at spawn time. On a successful spawn the origin town's outstanding-tourist count goes up; on arrival it goes down again, the destination earns the distance payment (see Economy area), and the visit is recorded in the destination's history.
