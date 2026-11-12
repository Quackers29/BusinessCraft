---
tags:
  - detail
  - town
---
# Packet Reach Checks

**Breadcrumb**: Town > Platforms > Packet Reach Checks
**TL;DR**: A button press in a Town Centre or platform screen only changes anything if the sender stands within 8 blocks of that Town Centre (or, for platform actions, of that platform's own kiosk); the server refuses everything else, logs `BC_REACH_REJECT`, and never loads a chunk to find out.

## What it does
Screens send their button presses to the server as packets that name a block position. Without a distance rule, any connected player could rename a town, switch its tourist spawning off, claim its payments, or reconfigure a platform from across the world, by sending the packet by hand. The rule keeps every one of those actions tied to where the player is actually standing.

## How it works (process view)
When such a packet arrives, the server first checks that the Town Centre's chunk is already loaded; if not, the packet is dropped without loading it. It then measures the sender's distance to the middle of the Town Centre block. Within 8 blocks, the action goes ahead. Platform actions (enable or disable, destination mode, destinations, area, path, learned-route reset) also pass when the sender is within 8 blocks of that platform's own kiosk, since the platform screen is opened from the kiosk and may be far from the Town Centre. Anything else is refused and one warning line is written to the server log naming the player, the town position, the distance and the reason.

Leaving path-drawing mode is always allowed, because it only clears the sender's own session.

## Where it applies
Rename town, toggle tourist spawning, claim a payment, open the town screen, contract board, payment board or destinations list, and every platform packet. The three storage packets (personal, communal, buffer) already used it.

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `LoadedTownAccess.getIfLoadedInReach(player, pos)` | `common/src/main/java/com/quackers29/businesscraft/network/LoadedTownAccess.java` | Town Centre only: loaded chunk, 8 blocks, right block entity |
| `LoadedTownAccess.getForPlatform(player, townPos, platformId)` | same | Town Centre reach, or the platform's `getInterfacePos()` reach |
| `LoadedTownAccess.withinReach` / `withinPlatformReach` | same | Pure distance rules, unit tested |
| `BaseBlockEntityPacket.handlePacketInReach` | `network/packets/misc/BaseBlockEntityPacket.java` | Reach-checked variant used by `SetTownNamePacket` and `ToggleTouristSpawningPacket` |

## Rules & formulas (exact)
- Reach is distance squared from the player's position to the block centre (`pos + 0.5`) of at most 64 (8 blocks), the same limit as `TownInterfaceMenu.stillValid` and `PlatformInterfaceMenu.stillValid`.
- An unknown platform id has no kiosk, so it needs Town Centre reach.
- Refusals log `BC_REACH_REJECT player=<name> town=x,y,z distance=<blocks> reason=<text>` from the `BusinessCraft/Reach` logger. The two-client smoke counts these lines.
- `BaseBlockEntityPacket.handlePacket` (no reach check) is still used by packets that only send data back.

## Edge cases & behaviors
- `PaymentBoardClaimPacket` answers a refused claim with "Town Centre is out of reach". Storage packets stay silent as before.
- The menus for the contract board and storage return `stillValid = true`; their open packets are where the distance is enforced.

## Test coverage
- Test file: `common/src/test/java/com/quackers29/businesscraft/network/LoadedTownAccessTest.java` (8-block boundary, height, diagonals, 120 blocks, town-or-kiosk rule, missing kiosk).
- Live check: the reach stage of `tools/multiplayer-smoke.sh` ([[Tourists/Spawning/Multiplayer Client Agreement]]) sends two platform packets from 120 blocks away and requires the server to refuse both.

## Open questions
- `RefreshPlatformsPacket`, `PlatformVisualizationPacket` and the other data-request packets have no reach check; they only send data to the requester.

## Related
- [[Town/Platforms/Platform Interface]]
- [[Town/Platforms/Platform Management]]
- [[Town/Payment Board/Reward Claims]]
- [[Tourists/Spawning/Multiplayer Client Agreement]]
