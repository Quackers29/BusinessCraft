---
tags:
  - detail
  - tourists
---
# Tourist Looks Names and Ranks

**Breadcrumb**: Tourists > Looks > Tourist Looks Names and Ranks
**TL;DR**: Thirty-two tourist appearances load from `data/businesscraft/tourist_looks`. A look is a skin, an outfit, a hat, and one carried or worn item. Each file is an equal chance at spawn. The choice is one number synced to every player. Each tourist also gets a personal name on the name tag, distance-based rank titles (Wanderer through Renowned), zero walking speed so they wait for minecarts or Create trains, and a scrollable trade list on the tourist screen.

## What it does
Tourists are visually distinct villagers, not generic clones. The mod ships thirty-two **looks** defined in data files under `data/businesscraft/tourist_looks`, loaded by `TouristLooks` (with reload listener support). The second sixteen keep the same four-part costume and add beach, business, student, retiree, photographer, cyclist, hiker, and other travellers, with a few new hat and item shapes plus new colours on shapes that already existed. At spawn, `TouristEntity` picks a look and a **personal name** from `TouristNames`; the name tag shows the personal name, not only the rank. As they accumulate **journey distance** (`JourneyProgress`), their rank title advances: Wanderer → Traveler → Voyager → Veteran → Renowned using configurable thresholds (`tourists.levelDistances`, defaults 500 / 1500 / 3000 / 6000 meters). Tourists do not walk to destinations: movement speed is set to effectively zero so they stand on platforms until they board minecarts or Create trains. Players can open the tourist screen to scroll through that tourist's trade offers.

## How it works (process view)
- **Looks** — On resource load, `TouristLookReloadListener` parses the tourist_looks pack; `TouristLooks.all()` exposes the set; spawn picks one index in that list, so every file has the same chance. The packed costume int is the value other players receive.
- **Names** — `TouristNames` supplies personal names; the entity stores and displays them on the name tag.
- **Ranks** — `JourneyProgress` maps total traveled distance to one of five rank labels; thresholds come from config (must be strictly rising ints).
- **Movement** — `TouristEntity` sets movement speed to `0.000001f` so pathfinding on foot is not part of the gameplay loop; ride mechanics extend lifetime and count distance while in vehicles (see [[Tourists/Lifecycle/Tourist Distance Tracking, Ride Extension and Expiry]]). Seated pose uses a lowered riding offset so humanoid models sit inside minecarts and Create seats.
- **Audio** — villager hurt/death/ambient/trade sounds as before; boarding plays a one-shot **note-block bell** (`TouristSounds.BOARDING`) instead of minecart rolling, with client-side loop muting when only tourists are aboard. Fast rides and short successful arrivals can play **experience orb pickup** at the tuned volumes from `TouristEntity` / `TouristEffects`.
- **Trades** — Trade catalog is tied to looks/catalog data; the tourist UI presents a scrollable list.
- **Worked example** — A new tourist spawns with look #7 and personal name "Elena"; rank shows Wanderer at 0 m traveled. After 600 m in a minecart, rank stays Wanderer; at 520 m total journey distance they become Traveler. They never path-walk off the platform — they wait for a cart.

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `TouristLooks` | `common/src/main/java/com/quackers29/businesscraft/entity/tourist/TouristLooks.java` | Index + JSON load; `all()`, `pick` |
| `TouristLookReloadListener` | same package | Datapack reload |
| `TouristNames` | entity/tourist package | Personal name pool |
| `JourneyProgress` | `common/src/main/java/com/quackers29/businesscraft/economy/JourneyProgress.java` | Rank from distance; default name array Wanderer…Renowned |
| `TouristEntity` | `common/src/main/java/com/quackers29/businesscraft/entity/TouristEntity.java` | Look pick, name tag, speed ≈ 0, distance accrual |
| `ConfigLoader.touristLevelDistances` | `common/src/main/java/com/quackers29/businesscraft/config/ConfigLoader.java` | Default 500, 1500, 3000, 6000 |

## Rules & formulas (exact)
- **Look count**: one entry per JSON file under `tourist_looks`. The mod ships 32 (the original sixteen plus beach day-tripper, business traveller, campus student, park retiree, field photographer, city cyclist, summit hiker, museum visitor, orchard picker, rain commuter, tea tourist, night painter, souk traveller, rail surveyor, spa guest, and winter scholar). A datapack adds more by dropping in another JSON file. There is no weight field and no fixed cap in code.
- **Sync**: the server writes `TouristLook` (packed int) and `TouristLookId`. Clients render the int. Hats and items the client does not know draw nothing, so the mesh has to be in the same mod jar as the texture.
- **Rank thresholds**: `tourists.levelDistances` — four boundaries for five tiers (0 → first threshold → …).
- **Walking**: `setMovementSpeed(0.000001f)` (or equivalent attribute) — not a gameplay transport mode.

## Edge cases & behaviors
- Reloading datapacks replaces looks via `TouristLooks.replace`.
- Rank display uses journey distance, aligned with payment distance tracking in lifecycle note.

## Test coverage
- `JourneyProgressTest` — threshold boundaries with default 500/1500/3000/6000.
- Entity look/name integration largely NEEDS-MC; math and config covered in unit tests.

## Open questions
- Gossip / window-gazing behaviors remain on the entity but are secondary to ride-based transport.

## Related
- [[Tourists/Tourists Overview]]
- [[Tourists/Lifecycle/Tourist Distance Tracking, Ride Extension and Expiry]]
- [[Tourists/Spawning/Tourist Spawning and Destination Selection]]
- [[Config/Config Overview]]
