# BusinessCraft Technical Architecture Summary (Updated 2025 Code Analysis)

## Project Overview

BusinessCraft is a Minecraft mod implementing town-building simulation with interconnected systems for economy management, AI tourists, auction contracts, resource trading, transport platforms, and production chains. Designed for multi-platform deployment (Fabric/Forge) using a shared `common` module and [`PlatformAccess`](common/src/main/java/com/quackers29/businesscraft/api/PlatformAccess.java) service locator pattern for platform-agnostic API access via helpers (RegistryHelper, NetworkHelper, etc.).

## Core Entities/Components

- [`Town`](common/src/main/java/com/quackers29/businesscraft/town/Town.java) (`implements ITownDataProvider`): Core town data - position, name, population (grown from bread/resources via [`TownEconomyComponent`](common/src/main/java/com/quackers29/businesscraft/town/components/TownEconomyComponent.java)), tourist count/spawning flag/path start/end/search radius, modular components (Economy, Production, Trading, Contract), payment board (rewards via [`TownPaymentBoard`](common/src/main/java/com/quackers29/businesscraft/town/data/TownPaymentBoard.java)), personal storage (player UUID → Item→count), visit history.
- [`TownManager`](common/src/main/java/com/quackers29/businesscraft/town/TownManager.java): Singleton per `ServerLevel`, manages UUID→Town map, registration/placement validation, ticking/saving via [`TownSavedData`](common/src/main/java/com/quackers29/businesscraft/data/TownSavedData.java).
- [`TouristEntity`](common/src/main/java/com/quackers29/businesscraft/entity/TouristEntity.java): Extends `Villager` with slow movement (`RandomStrollGoal`), expiry timer (pauses during movement/riding), origin/destination tracking, notifies origin town on quit/death.
- [`Platform`](common/src/main/java/com/quackers29/businesscraft/platform/Platform.java): Transport line with start/end `BlockPos`, enabled destination towns (UUID set), managed per-town by [`PlatformManager`](common/src/main/java/com/quackers29/businesscraft/town/data/PlatformManager.java).
- Contracts: Abstract [`Contract`](common/src/main/java/com/quackers29/businesscraft/contract/Contract.java) (issuer, expiry, bids), [`SellContract`](common/src/main/java/com/quackers29/businesscraft/contract/SellContract.java) (resource/quantity/price, buyer/winning/delivered), [`CourierContract`](common/src/main/java/com/quackers29/businesscraft/contract/CourierContract.java) (resource/quantity/dest/reward/courier), global [`ContractBoard`](common/src/main/java/com/quackers29/businesscraft/contract/ContractBoard.java) singleton (file NBT).
- Economy: [`TownResources`](common/src/main/java/com/quackers29/businesscraft/town/components/TownResources.java) Item→int, [`GlobalMarket`](common/src/main/java/com/quackers29/businesscraft/economy/GlobalMarket.java) singleton resourceId→price (trade-weighted update), [`ResourceRegistry`](common/src/main/java/com/quackers29/businesscraft/economy/ResourceRegistry.java)/[`ResourceType`](common/src/main/java/com/quackers29/businesscraft/economy/ResourceType.java) CSV canonical+equivalents (heuristics).

## Systems Breakdown

### Towns
Created via [`TownInterfaceBlock`](common/src/main/java/com/quackers29/businesscraft/block/TownInterfaceBlock.java) placement → `TownManager.registerTown` validates no boundary overlap (radius=population, `distSqr >= (r1 + r2)^2` via [`TownBoundaryService`](common/src/main/java/com/quackers29/businesscraft/town/service/TownBoundaryService.java)). Soft boundaries for validation/viz (client 3D [`TownBoundaryVisualizationRenderer`](common/src/main/java/com/quackers29/businesscraft/client/render/world/TownBoundaryVisualizationRenderer.java), sync packets).

### Economy
Town resources processed in [`TownInterfaceEntity`](common/src/main/java/com/quackers29/businesscraft/block/entity/TownInterfaceEntity.java) tick (hopper input), bread grows pop. Trading stock auto-restock [`TownTradingComponent`](common/src/main/java/com/quackers29/businesscraft/town/components/TownTradingComponent.java). Global market unused in core flows.

### Tourists
Spawn in `TownInterfaceEntity.tick` if enabled/can/platforms via `TouristSpawningHelper` (every 10s), dest "any"/specific town. Wander slow goals, vehicle mount `TouristVehicleManager`, visitor detection `VisitorProcessingHelper` increments touristCount (threshold→pop growth), expiry stationary (pause moving), notify origin quit/death `TownNotificationUtils`.

### Contracts
Global `ContractBoard` file-NBT, tick expires/completes, broadcast sync packet. `TownContractComponent.tick` auto-sell excess/bid low (hardcoded wood/iron/coal >200/<100).

### Platforms
UI mode right-click path [`TownEventHandler`](common/src/main/java/com/quackers29/businesscraft/event/TownEventHandler.java), client 3D viz `PlatformVisualizationRenderer`, search radius tourist/vehicle detect.

### Production
`TownProductionComponent.tick` pop-unlocks [`Upgrade`](common/src/main/java/com/quackers29/businesscraft/production/Upgrade.java) (CSV registry), consume/produce trading stock rates.

### Networking
`PacketRegistry` registers extensive packets:
- Town: setName/toggleTourist.
- Storage: personal/communal/buffer/payment claim/request/response.
- Platform: add/del/refresh/reset/setDest/enabled/path/mode/radius.
- UI: open boards/menus/map/platformviz/boundary sync/request/response/contract sync.

### Client Features
Debug keybinds/overlays [`TownDebugKeyHandler`](common/src/main/java/com/quackers29/businesscraft/client/TownDebugKeyHandler.java)/[`TownDebugOverlay`](common/src/main/java/com/quackers29/businesscraft/client/TownDebugOverlay.java), 3D world renderers boundary/path/platform [`VisualizationManager`](common/src/main/java/com/quackers29/businesscraft/client/render/world/VisualizationManager.java).

### Data Persistence
`TownSavedData` world NBT map UUID→Town, `ContractSavedData` config file list.

## Key Data Flows

```
Place TownInterfaceBlock
↓ (boundary check)
Register Town + default Platform
↓ tick (TownInterfaceEntity)
Process hopper resources → pop growth
If platforms + enabled + can → spawn Tourist (helper)
Tourist wander/ride vehicle → visitor detect → touristCount++ → threshold pop++
Production tick: upgrade consume/produce trading stock
Contract tick: auto sell excess / bid low
UI packets sync live data (resources/platforms/boundary/pop/tourists)
```

## Potential Improvements/Gaps

- **Deprecated Code**: Direct `Town` tourist methods → services.
- **Hardcoding**: Contract resources/thresholds, upgrade configs CSV good but extend.
- **Incomplete**: Contract fulfillment (delivery/payment), platform pathfinding (events partial), GlobalMarket trade integration.
- **Scalability**: Visitor/tourist multiplayer sync, configurable limits.
- **Platform**: Helpers robust, some Fabric extensions (e.g. entity packet).

Analysis complete: Confirmed town/boundary/economy/tourist/contract/platform systems; updated ARCHITECTURE.md with code-derived insights.
