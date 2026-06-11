---
tags:
  - detail
  - town
---
# Slot-Based Storage

**Breadcrumb**: Town > Storage > Slot-Based Storage
**TL;DR**: Exact-position slot storage (variable N slots) that preserves chest-like indexes across UI sessions; add uses stack-first then first-empty allocation with partial success, remove aggregates by item type in slot order, NBT roundtrips with exact slot-count guard and "id" presence check for items; primary consumer is the 18-slot payment board buffer.

## What it does
Replaces simple `Map<Item, Integer>` bags for systems that need stable slot positions (payment buffer claims, trade UIs, future hopper/automation). The storage keeps N independent slots; items stack by exact item+tags match up to max stack size; adds never reorder existing items; gets/sets by numeric index enable UI grids and packet sync to show the same physical "chest" layout every time a player opens the screen. Platform adapters (Forge ItemStackHandler, Fabric SimpleContainer) wrap it so existing menus and automation see a normal inventory.

## How it works (process view)
- Created with a fixed slot count (e.g. `new SlotBasedStorage(18)` for the payment board buffer, 2 for some trade views).
- `addItem(stack)` is the smart "insert" used by reward claiming: it first walks all slots trying to top up any existing same-item+tags stacks that have room, then walks again placing remainders into the first empty slots it finds (splitting across empties if needed). Returns true only if at least one item from the input was placed.
- `removeItem(item, amount)` removes up to `amount` of that exact Item (ignoring tags for the match in current impl), in ascending slot order, aggregating the removed pieces into a single returned ItemStack (or EMPTY if nothing removed). Slots that hit zero are set to EMPTY.
- `getSlot(i)` / `setSlot(i, stack)` / `clearSlot(i)` are the direct accessors used by menus and sync packets; they defensively copy on read and on write (EMPTY is normalized).
- `findEmptySlot` / `findStackableSlot` are helpers for the allocation logic and for UI "where would this go?" previews.
- Serialization (`toNBT` / `fromNBT`) writes the declared slotCount plus a list of per-slot `{Slot: i, ...item save fields...}`. On load it requires the exact slotCount to match (throws otherwise) and uses presence of the "id" key to decide `ItemStack.of(tag)` vs EMPTY. The packet `BufferSlotStorageResponsePacket` sends count + NBT so the client can reconstruct an identical layout.
- `copy()` / `copyFrom(other)` produce deep clones (used before network send and for defensive server copies).

**Worked example** (3-slot storage, default max stacks 64):
1. `addItem(30x emerald)` → slot 0 becomes 30 emerald; returns true.
2. `addItem(40x emerald)` → slot 0 tops up to 64, then slot 1 gets 6 emerald; returns true (all 40 placed).
3. `addItem(5x diamond)` → no matching stack → slot 2 gets 5 diamond.
4. `getTotalCount(emerald)` → 70.
5. `removeItem(emerald, 10)` → returns 10x emerald; slot 0 now 54, slot 1 now 6.
6. `isEmpty()` → false.
7. `toNBT()` → SlotCount=3 + Slots list with entries for 0,1,2 (only non-empties have id/Count data).
8. Fresh `new SlotBasedStorage(3); fromNBT(...)` restores the exact three stacks in their slots.

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `SlotBasedStorage(int slotCount)` | `common/src/main/java/com/quackers29/businesscraft/town/data/SlotBasedStorage.java` (33-41) | Allocates the array and fills every slot with `ItemStack.EMPTY`. |
| `getSlotCount()` / `getSlot(i)` / `setSlot(i, stack)` / `clearSlot(i)` | same (47-80) | Core indexed access. get returns a defensive `.copy()`; set normalizes empty and copies the input; out-of-range get returns EMPTY, set/clear are no-ops. |
| `findEmptySlot()` | same (86-93) | First index whose slot `.isEmpty()`, else -1. |
| `findStackableSlot(ItemStack)` | same (100-119) | First index that is non-empty, `isSameItemSameTags`, and has remaining space (>0), else -1. Empty input → -1 immediately. |
| `addItem(ItemStack)` | same (127-164) | Two-pass smart insert (stack pass across all, then empty pass). Mutates in place; returns `remaining.getCount() < originalCount` (true if anything was placed, including partial). Empty input → false. |
| `removeItem(Item, int amount)` | same (172-202) | Slot-order removal of exact Item (==); aggregates into one result stack; shrinks and blanks emptied slots; amount≤0 or nothing available → EMPTY. May return partial. |
| `getTotalCount(Item)` | same (209-218) | Sum of counts for exact item matches across all slots. |
| `isEmpty()` / `clear()` | same (224-240) | All-slots scan or blanket EMPTY assignment. |
| `toNBT()` / `fromNBT(CompoundTag)` | same (261-309) | Writes SlotCount + full Slots list (every slot index gets an entry; only non-empty carry item payload via `stack.save`). fromNBT clears, validates slot count (throws on mismatch), then repopulates using `contains("id") ? ItemStack.of(tag) : EMPTY`. |
| `copy()` / `copyFrom(SlotBasedStorage)` | same (315-335) | Deep copy via per-slot `.copy()`; copyFrom handles size mismatch by copying min prefix and blanking the tail. |
| `createItemHandler()` | same (247-249) | Delegates to `PlatformAccess.getItemHandlers().createStorageWrapper(this)` — returns the platform (Forge/Fabric) adapter object. |
| `SlotBasedStorageAccess` (interface) | `common/src/main/java/com/quackers29/businesscraft/api/SlotBasedStorageAccess.java` | Minimal surface (getSlotCount + get/setSlot + onContentsChanged hook) implemented by this class and consumed by platform wrappers and some menus. |

## Rules & formulas (exact)
Allocation in `addItem` (two passes on the live `slots` array):

1. Stacking pass (for `i = 0..slotCount-1` while remaining not empty):
   - If slot non-empty and `ItemStack.isSameItemSameTags(slotStack, remaining)` and `space = max - current > 0`:
     - `toAdd = min(space, remaining.count)`
     - `slotStack.setCount(current + toAdd)`
     - `remaining.shrink(toAdd)`

2. Empty pass (second walk):
   - If slot is empty:
     - `toPlace = min(remaining.getMaxStackSize(), remaining.count)`
     - `slots[i] = remaining.copy(); slots[i].setCount(toPlace)`
     - `remaining.shrink(toPlace)`

Return value: `remaining.getCount() < stack.getCount()` (i.e., at least one item from the caller's stack was accepted).

`removeItem` walks once from low index, greedily takes `min(avail, stillNeeded)`, builds/accumulates a single result `ItemStack(item, ...)` (new on first piece), shrinks source, blanks when zero, and stops when target amount or end of slots.

NBT contract:
- toNBT always emits exactly `slotCount` entries under "Slots", each with "Slot":i; payload only when `!isEmpty()`.
- fromNBT: `clear()` first, then `if (nbt.getInt("SlotCount") != this.slotCount) throw ...`; for each list entry, if the tag contains key "id" then `ItemStack.of(slotTag)` else EMPTY at that index.

Index safety: all public mutators and `getSlot` treat negative or ≥slotCount as no-op / EMPTY. Internal helpers assume valid indices.

`copyFrom` copies the overlapping prefix by direct slot-to-slot `.copy()` assignment (bypassing setSlot), then forces EMPTY on any slots this instance has that source did not.

## Edge cases & behaviors
- `new SlotBasedStorage(0)`: valid (0-length array); all operations degenerate gracefully (finds return -1, isEmpty true, add always false, etc.). Not used in production today.
- `addItem(ItemStack.EMPTY)` or null? (code checks `.isEmpty()`) → immediate false, no mutation.
- `removeItem(item, ≤0)` → immediate EMPTY.
- `findStackableSlot(EMPTY)` → -1.
- Stacking requires both item and tags to match (`isSameItemSameTags`); two emeralds with different custom NBT are distinct.
- Partial add across stack + empty: e.g. 10 space left in a 64-stack + 2 empties, input 80 → fills the 10, then 64 into first empty, 6 into second; returns true; 0 remain.
- `removeItem` can return a stack whose count is < requested (when stock insufficient); caller must check.
- `getTotalCount` and remove match on raw `slotStack.getItem() == item` (no tags consideration in removal path).
- fromNBT with wrong SlotCount throws `IllegalArgumentException` (defensive, not silent repair).
- fromNBT on a slot tag without "id" key → treated as EMPTY (even if other keys present).
- `toNBT` / `fromNBT` roundtrip for a storage that never held anything still writes SlotCount + N empty slot entries.
- `copy()` of a storage with mixed stacks produces an independent array whose ItemStacks are equal but not `==`.
- `createItemHandler()` and `onContentsChanged(int)` exist only for the platform wrapper contract; the default impl does nothing.
- The 18-slot usage is hard-coded in `TownPaymentBoard` (`new SlotBasedStorage(18)` as instance field); changing buffer size requires coordinated change there and in any UI that assumes 2×9.

## Test coverage
- Test file: N/A (ItemStack bootstrap)
- No unit tests committed. The entire useful surface (add/remove/find/allocate, NBT fidelity, copy, index guards) requires constructing `SlotBasedStorage` (which unconditionally assigns `ItemStack.EMPTY` into its array in the ctor) and passing real or at least non-empty `ItemStack` / `Item` values into the methods. Both trigger the Minecraft item registry / codec bootstrap ("Not bootstrapped" / registry access during class or ItemStack init), exactly as seen for `TownPaymentBoard` (T-012) and `TownResources` (T-007).
- The handful of pure-integer behaviors (getSlotCount, out-of-range getSlot returns EMPTY, ctor size) are too trivial to justify a test class under the loop rules when the class cannot even be instantiated.
- If a GameTest or bootstrapped test harness becomes available, the scenarios in "How it works", "Rules & formulas", and "Edge cases" (partial fills, same-tags stacking, remove aggregation order, NBT slot-count guard, cross-size copyFrom) should be exercised with real ItemStacks.
- Documentation (this note) was written from direct code inspection before any test attempt, per protocol.

## Open questions
- **Same ItemStack bootstrap blocker as T-007/T-012**: Even though the allocation and serialization rules are pure (no world, no entities), the type system couples them to `net.minecraft.world.item.ItemStack` and `Item`. The architecture intentionally kept the common module free of loader-specific item handlers by using the real MC types + a thin `SlotBasedStorageAccess` + platform `createStorageWrapper`. Future: a pure data model (e.g. record of slot contents as (item id string, count, tag compound)) with a separate mapper could allow unit tests today.
- `removeItem` matches only on `getItem() == item` (raw Item identity), while `addItem` / `findStackable` use the stricter `isSameItemSameTags`. This asymmetry means you can add two differently-tagged variants of the same item and later `removeItem` will pull from both indiscriminately. Likely acceptable for the payment buffer use case (rewards are usually plain stacks), but worth noting for any future "exact match" requirements.
- `getTotalCount` and `removeItem` do not consider tags; if the payment buffer ever holds the same item with distinct tags that should be tracked separately for claims, these methods would need a tags-aware overload or the callers would have to iterate slots themselves.
- 0-slot storage is accepted by the API but never instantiated in current code; if a caller ever does `new SlotBasedStorage(0)`, many methods become no-ops — harmless but possibly surprising.
- The `onContentsChanged` callback on the interface is never overridden in SlotBasedStorage and the platform wrappers may or may not call it; its contract is effectively unused today.
- Hard-coded 18 in TownPaymentBoard is the only capacity that matters for the current payment flow; the generic SlotBasedStorage could support other sizes (e.g. full double-chest 54) if UI or automation needs grow.

## Related
- [[Town/Town Overview]]
- [[Town/Payment Board/Reward Claims]] (T-012 — owns the canonical 18-slot instance as `bufferStorage`; claim-to-buffer path uses `addItem` and fails the claim if not all stacks fit)
- [[Town/Resources/Resource Storage Operations]] (T-007 — the older map-based long-count bag for on-hand production/trade stock; SlotBasedStorage is the slot-exact, UI-preserving sibling used for claim staging)
- [[Town/Visits/Visit Buffer]] (T-010 — upstream of payments that land in the buffer)
