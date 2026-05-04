# UI Packet Skeleton Normalization Plan

## Objective
Normalize packet class skeletons in `common/src/main/java/com/quackers29/businesscraft/network/packets/ui` to remove repeated boilerplate and inconsistency while preserving runtime behavior.

This plan is intentionally scoped to skeleton normalization only:
- no feature changes
- no protocol changes
- no packet payload format changes
- no workflow changes in packet handlers

## In-Scope Files
All packet classes under:
- `common/src/main/java/com/quackers29/businesscraft/network/packets/ui/*.java`

Included helper/cache class for style-only checks:
- `ClientTownMapCache.java` (imports/comments/style only if touched)

## Out of Scope
- Removing legacy contract sync path
- Refactoring packet business logic
- Extracting shared domain utilities (direction math, UI-open helpers, etc.)
- Any behavior changes to packet handling order or side effects

## Normalization Targets

### 1) Handler Signature Consistency
Current package uses mixed signatures:
- `boolean handle(Object context)`
- `void handle(Object context)`

Plan:
- Standardize on one signature style used by `PacketRegistry` method references.
- Keep exact behavior inside handlers, including `enqueueWork` and `setPacketHandled`.

Constraint:
- Do not alter execution path, guard clauses, or side effects.

### 2) Encode/Serialize Method Shape
Current package has mixed patterns:
- `encode(...)` only
- `toBytes(...)` only
- both `encode(...)` and `toBytes(...)` where one delegates to the other

Plan:
- Pick one canonical internal serialization method per packet class.
- Keep static entrypoints required by `PacketRegistry` (`decode`, `encode`) stable.
- Remove redundant wrappers where safe and purely duplicative.

Constraint:
- Do not change field order, primitive types, nullability encoding flags, or string length limits.

### 3) Decode Entry Consistency
Current package has mixed decode styles:
- constructor decode (`new Packet(buf)`)
- static decode with local reads then constructor

Plan:
- Normalize decode pattern for readability and consistency.
- Keep wire compatibility exact.

Constraint:
- No changes to deserialization order or conditional reads.

### 4) Lifecycle Boilerplate Standardization
Most packets repeat:
- `PlatformAccess.getNetwork().enqueueWork(context, ...)`
- `PlatformAccess.getNetwork().setPacketHandled(context)`

Plan:
- Normalize placement and formatting of this lifecycle boilerplate.
- Remove duplicated explanatory comments that repeat obvious behavior.

Constraint:
- `setPacketHandled(context)` must remain on every packet path where it exists today.

### 5) Logging and Import Hygiene (Skeleton-Level Only)
Current package mixes logging APIs:
- `org.slf4j.*`
- `org.apache.logging.log4j.*`

Plan:
- Normalize logging API usage package-wide if this can be done with no behavior impact.
- Remove unused imports and repetitive scaffold comments.

Constraint:
- Do not change log levels/messages unless needed for consistency in a no-op way.

## Execution Sequence

1. Freeze packet wire contracts (reference snapshot by class).
2. Normalize handler signatures in smallest-risk packets first (stateless/simple packets).
3. Normalize encode/decode skeletons per class without touching payload layout.
4. Normalize lifecycle boilerplate formatting and redundant comments.
5. Normalize imports/logging API usage.
6. Verify `PacketRegistry` method references compile unchanged.
7. Run compile-only validation for `:common` module.

## File Grouping for Safe Rollout

### Group A: Low-risk simple packets
- `BoundarySyncRequestPacket`
- `BoundarySyncResponsePacket`
- `PlatformVisualizationPacket`
- `OpenPaymentBoardPacket`
- `OpenTownInterfacePacket`

### Group B: Medium packet payload complexity
- `RequestTownMapDataPacket`
- `TownMapDataResponsePacket`
- `RequestTownPlatformDataPacket`
- `TownPlatformDataResponsePacket`
- `TownOverviewSyncPacket`

### Group C: Higher logic density (touch skeleton only)
- `AcceptContractPacket`
- `BidContractPacket`
- `RequestContractListPacket`
- `ContractListSyncPacket`
- `RequestContractDetailPacket`
- `ContractDetailSyncPacket`
- `ContractSyncPacket`
- `OpenContractBoardPacket`
- `OpenDestinationsUIPacket`
- `RefreshDestinationsPacket`
- `PlayerExitUIPacket`
- `SetPathCreationModePacket`

## Verification Checklist
- [ ] Every packet in `network/packets/ui` still has a valid `PacketRegistry` registration path.
- [ ] No packet field order changed in `encode`/`toBytes`.
- [ ] No packet read order changed in `decode`/constructors.
- [ ] All handlers still enqueue work and mark packet handled.
- [ ] No new behavior branches added.
- [ ] `:common:compileJava` passes.
- [ ] Manual smoke check of one C2S and one S2C packet per major flow:
  - [ ] contract flow
  - [ ] town map flow
  - [ ] platform visualization flow

## Definition of Done
Packet class skeletons are consistent across `network/packets/ui`, duplicated boilerplate is reduced, and all packet behavior/wire format remains unchanged.
