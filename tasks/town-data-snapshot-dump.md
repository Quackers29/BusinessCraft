# Plan: Town Data Snapshot Dump (Option C)

**Status**: Plan — awaiting owner verification before implementation  
**Scope**: Op-only server command that writes a **full-picture** snapshot of all BusinessCraft persisted state. Not a live HUD; not F4 changes.  
**Permission**: Brigadier `.requires(source -> source.hasPermission(2))` (same as `/bc debug clear_contracts`)  
**Format**: SNBT (Minecraft `CompoundTag` text) — zero extra deps, matches save format, dynamic by construction  
**Out of scope for this task**: F4 overlay changes, in-game tree UI, dump-on-shutdown, CSV

---

## Goal

Give operators / developers a single command that materializes **everything the mod currently stores** into a readable file under the world directory, without hardcoding which fields to show. New `tag.put(...)` calls appear in future dumps automatically because the dump walks the same NBT trees the save path already builds.

---

## Command UX

```
/bc debug dump
```

- Registers under existing `/bc debug` tree (alongside `clear_contracts`)
- Permission level **2** (op)
- Iterates **all loaded `ServerLevel`s**
- Writes one snapshot file; chat feedback with absolute/relative path + town/contract counts
- Optional later (not v1 of this plan): `/bc debug dump <townName|uuid>` filter

Suggested output path (pick one at implement time; prefer A):

| Option | Path |
|--------|------|
| **A (preferred)** | `<world>/businesscraft/dumps/bc-dump-<yyyyMMdd-HHmmss>.snbt` |
| B | `<world>/data/businesscraft_dump_<timestamp>.snbt` |

Use `server.getWorldPath(LevelResource.ROOT)` (or platform equivalent) so dedicated servers and SP both land in the correct world folder.

---

## Dump file schema (refined from code)

This is the **target file shape**. Inner compounds are produced by calling existing `save` / `toNBT` methods wherever possible — do **not** hand-project fields.

```
CompoundTag                          # dump root (wrapper only; not a SavedData file)
├── dumpVersion : int                # dump-envelope version (= 1); independent of TownSavedData.dataVersion
├── dumpTimestamp : long             # System.currentTimeMillis()
├── dumpTimestampIso : string        # human-readable UTC/local for logs
├── minecraftTime : long             # overworld gameTime if available
├── levels : CompoundTag
│   └── <dimensionResourceLocation> : CompoundTag   # e.g. "minecraft:overworld"
│       ├── towns : CompoundTag      # same shape as TownSavedData.towns values
│       │   └── <townUUID> : CompoundTag
│       │       ├── dataVersion : int          # copy of TownSavedData.CURRENT / stamped value
│       │       ├── town : CompoundTag         # Town.save(tag) verbatim
│       │       ├── platforms : ListTag?       # from TownInterfaceEntity at town position (omit if BE missing)
│       │       ├── blockEntity : CompoundTag? # optional slim BE extras: TownId, name, inventory, Path*, searchRadius
│       │       └── contracts : ListTag        # contracts touching this town (see filter below)
│       └── contractsAll : ListTag             # full ContractSavedData list for this level (unfiltered)
└── market : CompoundTag             # GlobalMarket from overworld MarketSavedData (once at root)
    ├── prices : CompoundTag
    └── volume : CompoundTag
```

### Why this envelope

| Concern | Decision |
|---------|----------|
| Dynamic fields | Inner `town` / `contracts` / `market` / `platforms` come from existing serializers |
| Full picture | Towns + platforms (BE) + contracts + global market |
| Per-level | Towns and contracts are per-`ServerLevel`; market is overworld singleton |
| Per-town convenience | Duplicate filtered contracts under each town **and** keep `contractsAll` so nothing is lost |
| Schema bugs visible | Dump writes **what `save()` emits**, so escrow/happiness asymmetries show up as-is (document, don’t “fix” in dump) |

---

## Ground-truth schemas (from current `save` / `toNBT`)

Source of truth is the Java listed below. Dump implementation should **call these methods**, not re-encode fields.

### 1. TownSavedData (disk id: `businesscraft_towns`)

**File:** `common/.../data/TownSavedData.java`

```
{
  dataVersion : int          # CURRENT_DATA_VERSION = 1
  towns : {
    "<uuid>" : { ... Town.save ... }
  }
}
```

### 2. Town (`Town.save`)

**File:** `common/.../town/Town.java`

```
{
  id : UUID
  name : string
  posX, posY, posZ : int
  touristCount : int
  biome, biomeVariant : string
  workUnits : long
  totalTouristsArrived : long
  totalTouristDistance : double
  visitors : { "<originTownUUID>" : long }
  economy : { population : int, resources : { "<itemId>" : long } }
  trading : { tradingStocks : [ { id, current, min, max } ] }
  production : { recipeProgress : { "<recipeId>" : float } }
  upgrades : {
    upgradeLevels : [ { id, lvl } ]
    currentResearch? : string
    researchProgress? : float
    idleTicks? : long
    flatModifiers : { "<key>" : float }
  }
  contracts : { lastContractCheckTime : long }    # meta only — real contracts elsewhere
  PathStart? / PathEnd? : { x, y, z }
  searchRadius : int
  pendingTouristSpawns : int
  touristSpawningEnabled : boolean
  visitHistory? : [ { timestamp, townId?, count, pos? } ]
  paymentBoard : {
    rewards : [ RewardEntry NBT ]
    bufferStorage : { SlotCount : int, Slots : [ { Slot, + ItemStack NBT? } ] }  # 18 slots
  }
  personalStorage? : { "<playerUUID>" : { "<itemId>" : long } }
  wantedResources? : { "<itemId>" : long }
  escrowedResources? : { "<itemId>" : long }      # ⚠ load looks for "escrow" — dump shows save key
}
```

**Empty-omit (as coded today):** `PathStart`/`PathEnd` if null; `visitHistory` / `personalStorage` / `wantedResources` / `escrowedResources` if empty. Components + `paymentBoard` always written.

**Not in Town.save (known gaps):**
- `happiness` — loaded, never saved (runtime-only today)
- Active sell/courier contracts — in `ContractSavedData`
- Platforms — on `TownInterfaceEntity`
- Global market prices — in `MarketSavedData`

**Component files:**
- `town/components/TownEconomyComponent.java`, `TownResources.java`
- `town/components/TownTradingComponent.java`
- `town/components/TownProductionComponent.java`
- `town/components/TownUpgradeComponent.java`
- `town/components/TownContractComponent.java`
- `town/data/TownPaymentBoard.java`, `RewardEntry.java`, `SlotBasedStorage.java`

### 3. Platforms + block entity

**Files:** `platform/Platform.java`, `town/data/PlatformManager.java`, `town/data/NBTDataHelper.java`, `block/entity/TownInterfaceEntity.java`

Locate BE: `level.getBlockEntity(town.getPosition())` as `TownInterfaceEntity` (position set at town registration). Fallback: scan loaded chunks for BE whose `TownId` equals town id if position miss.

```
platforms : [ {
  Id : UUID
  Name : string
  Enabled : boolean
  StartX/Y/Z?, EndX/Y/Z? : int
  Destinations : { Count : int, Dest0..N-1 : UUID }
} ]

blockEntity extras (optional compound):
  TownId?, name, inventory (1-slot handler), PathStart?, PathEnd?, searchRadius
```

Do **not** dump `TownBufferManager` separately — it mirrors `paymentBoard.bufferStorage`.

### 4. Contracts (`ContractSavedData`, disk id: `businesscraft_contracts`)

**Files:** `data/ContractSavedData.java`, `contract/Contract.java`, `SellContract.java`, `CourierContract.java`, `ContractBoard.java`

```
contracts : [ {
  type : "sell" | "courier"
  # common:
  id, issuerTownId : UUID
  issuerTownName? : string
  creationTime, expiryTime : long
  isCompleted : boolean
  bids : [ { bidder : UUID, amount : float, name? : string } ]
  # sell extras:
  resourceId, quantity, pricePerUnit, buyerTownId?, winningTownId?,
  winningTownName?, acceptedBid, isDelivered, courierId?, courierReward,
  courierAcceptedTime, deliveredAmount
  # courier extras:
  resourceId, quantity, deliveredAmount, destinationTownId,
  destinationTownName?, reward, courierId?, acceptedTime,
  sourceX/Y/Z?, sourceRadius
} ]
```

**Per-town filter** (for `levels.*.towns.*.contracts`): include contract if any of  
`issuerTownId`, `buyerTownId`, `winningTownId`, `destinationTownId` equals town id, **or** any bid `bidder` equals… (bidder is player UUID — skip unless we also want player-centric; **v1: town UUID fields only**).

**Note:** `ContractSavedData` historically could load `marketPrices` but **never saves** them; ignore that path. Market comes from `MarketSavedData`.

### 5. Market (`MarketSavedData`, disk id: `businesscraft_global_market`)

**Files:** `economy/MarketSavedData.java`, `economy/GlobalMarket.java`  
Attached to **overworld only**.

```
market : {
  prices : { "<resourceId>" : float }
  volume : { "<resourceId>" : long }
}
```

### 6. Optional (defer unless cheap)

| Data | Persist location | v1 dump? |
|------|------------------|----------|
| Live `TouristEntity` (origin/dest UUIDs, distance, expiry) | Entity NBT in chunks | **Defer** — world-entity scan; not SavedData |
| Client caches / view-model packets | RAM | No |
| Leaderboard | Not SavedData | No |

---

## Implementation plan

### Approach

1. Add a small dump builder in `common` that:
   - Creates envelope `CompoundTag`
   - For each `ServerLevel`: pulls `TownManager.get(level).getAllTowns()`, calls `town.save` into nested tag; looks up BE for platforms; pulls `ContractBoard` / `ContractSavedData` list and filters
   - Pulls `GlobalMarket` / `MarketSavedData` once into root `market`
2. Serialize with vanilla SNBT (`tag.toString()` or `NbtUtils.structureToSnbt` / equivalent available on 1.20.1)
3. Write UTF-8 file under world `businesscraft/dumps/`
4. Wire command `/bc debug dump` permission 2; register on Forge + Fabric next to `ClearContractsCommand`

Prefer **reusing** `town.save`, `paymentBoard.toNBT`, `Platform.save`, `Contract.save` + type tag, `GlobalMarket.save` — no parallel DTO.

### Suggested files (to create/touch when implementing)

| Action | File |
|--------|------|
| Create | `common/.../command/DumpTownDataCommand.java` (or `debug/TownDataDumpCommand.java`) |
| Create | `common/.../debug/TownDataSnapshotBuilder.java` (pure builder; easier to unit-test) |
| Touch | `forge/.../BusinessCraftForge.java` — register command |
| Touch | `fabric/.../event/FabricEventCallbackHandler.java` — register command |
| Optional test | `common/.../debug/TownDataSnapshotBuilderTest.java` — build tag from fixture towns, assert keys exist |

Keep command class thin (permission + IO); builder owns schema assembly.

### Checklist

- [ ] `TownDataSnapshotBuilder` assembles envelope from `Town.save` + BE platforms + contracts + market
- [ ] SNBT written under `<world>/businesscraft/dumps/bc-dump-<timestamp>.snbt`
- [ ] `/bc debug dump` registered, `hasPermission(2)`, works on Forge + Fabric
- [ ] Chat success: path + counts (towns, contracts, whether market present)
- [ ] Chat failure paths: IO error, no server
- [ ] Document in command feedback that dump reflects **save()** output (including known asymmetries)
- [ ] Unit test: builder includes nested `economy` / `paymentBoard` without hardcoding every leaf
- [ ] Manual smoke: create 2 towns + platform + reward → dump → open file, confirm sections present
- [ ] Do **not** change F4, SavedData formats, or “fix” escrow/happiness in this task

### Non-goals / explicit exclusions

- No CSV
- No dump-on-shutdown (can be a follow-up one-liner calling the same builder)
- No client packets / F4 changes
- No restore/import command
- No fixing escrow key / happiness persistence here (file a note if dump makes them obvious)

### Known save asymmetries (document in dump header comment or chat tip)

1. Escrow saved as `escrowedResources`, loaded as `escrow` → currently broken across restarts  
2. `happiness` never written by `Town.save`  
3. Town `contracts` compound is only `lastContractCheckTime`; real contracts are under `contractsAll` / per-town filtered list  
4. Platforms only appear if the Town Interface chunk/BE is loaded

---

## Acceptance criteria

1. Op runs `/bc debug dump` → file appears under world folder  
2. Non-op cannot run it  
3. File contains, for a populated test world: town identity + economy resources + payment board + at least one platform (if BE loaded) + contracts (if any) + market prices/volume compounds  
4. Adding a new key inside `Town.save` appears in the next dump **without** changing the dump command  
5. Both loaders can register/run the command

---

## Relation to 0.9

Dev/ops tooling — **not a 0.9 beta blocker**. Can land anytime; useful for playtest diagnosis and AI-assisted debugging of live worlds.

---

## Open decisions (owner)

Confirm before coding:

1. Output path **A** (`businesscraft/dumps/…`) vs B?  
2. Include slim `blockEntity` compound, or **platforms list only**?  
3. Include live tourist entity scan in v1, or defer? (plan defers)  
4. Filename timestamp timezone: local vs UTC?

**Defaults if no reply:** A; platforms + slim BE; defer tourists; UTC in filename + `dumpTimestampIso`.
