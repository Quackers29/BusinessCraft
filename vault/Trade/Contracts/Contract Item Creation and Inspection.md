---
tags:
  - detail
  - trade
---
# Contract Item Creation and Inspection

**Breadcrumb**: Trade > Contracts > Contract Item Creation and Inspection
**TL;DR**: createContractItem packages a resource delivery job into an enchanted ItemStack (vanishing curse for purple glow) carrying full contract metadata in nested NBT plus a boolean flag; inspectors (isContractItem, getContractId, matchesContract, getContractData) let delivery code recognize and extract the job without parsing the whole stack; unknown resource types fall back to paper while still preserving metadata and lore.

## What it does
When a town wins a sell contract auction or a courier contract is issued, the logistics layer turns the abstract contract into a physical "package" the courier (player or villager) can carry in inventory. The item visually stands out (enchanted glow), displays human-readable pickup/delivery/cargo details in its lore, and secretly holds the contract UUID, town IDs/names, quantity, resource type, and creation timestamp so the receiving town interface can validate "this is for me" and credit the correct contract on arrival.

## How it works (process view)
1. A contract (Sell or Courier) reaches the point where a deliverable item is needed (e.g. on courier assignment or manual handoff).
2. ContractItemHelper.createContractItem is called with the resource type string (e.g. "iron"), quantity, the contract's UUID, destination town UUID+name, and source town name.
3. It asks ResourceRegistry for the canonical Minecraft item for that resource (via getBaseItemForResource). If the registry has no mapping or the resolved item is AIR, it falls back to PAPER so the package is still carryable.
4. An ItemStack of that base item + quantity is created, then cursed with VANISHING_CURSE (level 1) purely for the visual purple enchantment glint.
5. A "contractData" compound is built with the six fields and attached under the stack's root tag; "isContractItem" boolean true is also set at root for cheap detection.
6. Human lore is generated (six lines: header, blank, Pickup, Deliver, blank, Cargo, blank, warning) and written into the display.Lore list as JSON-serialized Components.
7. Later, at a Town Interface (hopper, player insert, or tourist arrival), isContractItem(stack) does a cheap flag check; getContractId and getContractData pull the payload for matching against open contracts and for reward/escrow processing.

**Worked example**: createContractItem("iron", 64, contract-uuid-1234, dest-uuid-5678, "Ironville", "Woodtown") produces an iron_ingot ×64 stack whose tag contains:
- contractData = {contractId:1234, resourceType:"iron", quantity:64, destinationTownId:5678, destinationTownName:"Ironville", sourceTownName:"Woodtown", creationTime:1718000000000}
- isContractItem = true
- display.Lore = [ "§6§l═══ Contract Delivery ═══", "", "Pickup From: §fWoodtown", "Deliver To: §fIronville", "", "Cargo: §e64x Iron", "", "§5§o⚠ Special Delivery Item" ]
The stack can be placed in a chest, given to a player, or carried by a tourist; only stacks with the boolean flag are treated as contract packages.

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `ContractItemHelper.createContractItem(...)` | `common/src/main/java/com/quackers29/businesscraft/util/ContractItemHelper.java` | Main entry: builds the enchanted, NBT-tagged, lore-annotated delivery package ItemStack |
| `ContractItemHelper.getBaseItemForResource(String)` | same | Resolves resourceType via ResourceRegistry → canonical item id → Platform registry lookup; PAPER fallback |
| `ContractItemHelper.isContractItem(ItemStack)` | same | Fast predicate: root tag has boolean "isContractItem" == true |
| `ContractItemHelper.getContractData(ItemStack)` | same | Returns the inner "contractData" CompoundTag or null if not a contract item |
| `ContractItemHelper.getContractId(ItemStack)` / `matchesContract(ItemStack, UUID)` | same | Extractor + equality guard used by delivery arrival code in TownInterfaceEntity |
| `ContractItemHelper.addContractLore(...)` (private) | same | Builds the 6-line gold/gray/yellow/dark-purple lore array and writes JSON Components under display.Lore |

## Rules & formulas (exact)
- Base item resolution: `ResourceRegistry.get(resourceType.toLowerCase())`; if found take `type.getCanonicalItemId()`, then `PlatformAccess.getRegistry().getItem(id)`. Return that Item only if non-null and != Items.AIR; otherwise Items.PAPER. The string type name is still stored verbatim in contractData.resourceType even on fallback.
- Stack construction: `new ItemStack(baseItem, quantity)` — quantity is taken as-is (upstream callers are responsible for clamping; see SellContract clamps).
- Enchant: always `stack.enchant(Enchantments.VANISHING_CURSE, 1)` — used solely for the glint effect; vanishing curse has no functional "vanish on death" intent here.
- contractData CompoundTag keys (all written unconditionally):
  - "contractId": UUID
  - "resourceType": String (original case preserved from caller)
  - "quantity": int
  - "destinationTownId": UUID
  - "destinationTownName": String
  - "sourceTownName": String
  - "creationTime": long (System.currentTimeMillis() at create instant)
- Root tag also gets `"isContractItem": true` (boolean) and the whole contractData under key "contractData".
- Lore construction (addContractLore): exactly six Component lines in order (with intervening blanks):
  1. "═══ Contract Delivery ═══" (GOLD + BOLD)
  2. "" (empty)
  3. "Pickup From: " + gray + source (white)
  4. "Deliver To: " + gray + dest (white)
  5. "" (empty)
  6. "Cargo: " + gray + (qty + "x " + capitalizeFirst(resourceType)) (yellow)
  7. "" (empty)
  8. "⚠ Special Delivery Item" (DARK_PURPLE + ITALIC)
  Written as a ListTag of StringTag (Component.Serializer.toJson) under display.Lore.
- capitalizeFirst(str): if null or empty return as-is; else str.substring(0,1).toUpperCase() + substring(1).toLowerCase() (ASCII only; no locale).
- isContractItem: `stack.hasTag() && stack.getTag().getBoolean("isContractItem")` — false for null/empty stacks or stacks lacking the exact boolean.
- getContractData: short-circuits to null unless isContractItem; then returns the compound (may be empty compound if key absent after flag).
- getContractId: from data compound, `hasUUID("contractId") ? getUUID(...) : null`.
- matchesContract: `getContractId(stack) != null && getContractId(stack).equals(contractId)`.

## Edge cases & behaviors
- Unknown / unregistered resourceType (e.g. "mithril", "foo"): falls back to PAPER stack while still writing full contractData + lore (the lore will show "64x Mithril" via the passed string + capitalize). Delivery code can still match by contractId.
- quantity == 0 or negative: ItemStack allows it (count will be 0 or clamped by MC later); metadata is stored faithfully.
- null names or UUIDs: stored as-is (lore will render "null" or empty visually); callers are expected to supply real values.
- capitalizeFirst on already-mixed or all-caps: forces Title Case for the cargo line only (e.g. "IRON_INGOT" -> "Iron_ingot" — note underscore stays).
- Multiple calls with same params produce different creationTime (wall clock).
- isContractItem / getters on a normal player iron stack: all return false / null (no false positives).
- Enchant + lore are applied even to the PAPER fallback, so every contract package looks "special".
- No size or value validation inside this helper; clamps live in SellContract ctor/load and ContractBoard.

## Test coverage
- Test file: `common/src/test/java/com/quackers29/businesscraft/util/ContractItemHelperTest.java`
- Covered (12 tests): create for known resources (iron/wood resolve to real Items via seeded registry), unknown falls back to PAPER while preserving metadata/lore, all 7 contractData keys + types + creationTime freshness, root isContractItem flag, enchant presence, isContractItem positive/negative/EMPTY, getContractData/getContractId/matchesContract roundtrips and nulls, capitalizeFirst (reflection) on mixed/edge inputs, lore list structure present. NPE on isContractItem(null) pinned with assertThrows as current behavior.
- Not covered: actual courier handoff flow in TownInterfaceEntity (MC entity + menu + TouristEntity), visual glint (client render), integration with payment board claims on delivery, full ResourceRegistry csv load path (minimal map seeding used for isolation).
- Uses McBootstrap + TestRegistryHelper (BuiltInRegistries delegation) + TestPlatformHelper + PlatformAccess + ResourceRegistry private map snapshot/restore (exact pattern from T-019) so ItemStacks and getBaseItemForResource are realistic. Full suite green.

## Open questions
- capitalizeFirst is a very small private util; if more string formatting appears it could be hoisted, but currently only used for lore cargo line.
- The "isContractItem" boolean + nested compound is a simple convention. Any other code that manually constructs fake contract stacks could spoof the flag; delivery sites should still cross-check contractId against the real board.
- creationTime is written but never read by the current inspectors (only stored for potential future audit / expiry of carried packages). If that changes, add a getter test.
- Resource type casing: stored as passed, looked up lower-cased. Inconsistent casing from callers would produce different lore text vs. registry match.

## Related
- [[Trade/Trade Overview]]
- [[Trade/Contracts/Sell Contract Lifecycle]] (T-004 — the contracts that become items)
- [[Trade/Contracts/Courier Delivery Rewards]] (T-005 — fixed-reward delivery path that also produces items)
- [[Trade/Contracts/Auction Resolution]] (T-003 — when a sell contract resolves, a courier item may be created for the winner)
- [[Town/Payment Board/Reward Claims]] (T-012 — delivered contract cargo often surfaces as claimable rewards)