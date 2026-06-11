---
tags:
  - detail
  - town
---
# Reward Claims

**Breadcrumb**: Town > Payment Board > Reward Claims
**TL;DR**: A TownPaymentBoard holds up to 100 RewardEntry rows; each carries source (TOURIST_ARRIVAL, MILESTONE, COURIER_*, etc.), a list of ItemStacks, eligibility ("ALL" or a specific player UUID string), status (UNCLAIMED/CLAIMED/EXPIRED), and timestamps. addReward forces 7-day expiry (overriding RewardEntry's 24 h ctor default). canBeClaimed(claimer) returns true only for UNCLAIMED + !expired + ("ALL" or exact eligibility match). claimReward sets CLAIMED on success (buffer path requires 18-slot space); cleanupExpiredRewards marks expired non-claimed as EXPIRED and prunes EXPIRED entries whose creation timestamp is >30 days old; getUnclaimedRewards always cleans first then returns newest-first UNCLAIMED non-expired.

## What it does
The payment board replaced a simple shared "communal chest" with a structured, auditable reward log. Tourist fares, milestone bonuses, trade profits, job payouts, and courier handoffs all land here as claimable entries. Some are town-wide (eligibility "ALL" — any resident or visitor can claim) while others are personal (eligibility = the acting player's UUID string) so that courier rewards go only to the player who accepted the contract. Every entry has a hard 7-day claim window and the board self-prunes to the most recent 100 to prevent unbounded growth.

## How it works (process view)
- Production code (VisitorProcessingHelper for arrivals/milestones, ContractBoard for couriers) calls `town.getPaymentBoard().addReward(source, listOfItemStacks, eligibilityString)`.
- If the list is null or empty, addReward returns null and logs a warning; nothing is added.
- Otherwise a RewardEntry is created (its ctor defaults expiration to +24 h) and addReward immediately overwrites it to `now + 7 days`. The entry is appended, then if size > MAX_REWARDS (100) a cleanup + oldest-first trim brings it back to 100.
- Players open the Payment Board UI (PaymentBoardScreen + menu). The screen requests unclaimed via `getUnclaimedRewards()`, which runs `cleanupExpiredRewards()` then filters to status==UNCLAIMED && !isExpired(), sorted by timestamp descending (newest first).
- For each visible row the screen calls `reward.canBeClaimed(player.getStringUUID())`. If true and UNCLAIMED, a "Claim" button appears (always to buffer in current UI).
- Claim button sends PaymentBoardClaimPacket → `claimReward(uuid, playerUUID, toBuffer)`.
  - If no entry or !canBeClaimed, returns failure with a reason string ("already claimed", "expired", "not eligible").
  - If toBuffer: tries to add the entry's items into the board's internal 18-slot SlotBasedStorage via smart stacking; on full failure the entry stays UNCLAIMED and result says "Buffer storage is full".
  - If not toBuffer (inventory path, used by some contract flows): the entry is marked CLAIMED immediately and the caller is given the ItemStack list to place in the player's inventory (overflow falls back to buffer).
- On every add/getUnclaimed/stats, and explicitly in the UI tick, `cleanupExpiredRewards()` runs: any entry whose `System.currentTimeMillis() > expirationTime` and status != CLAIMED gets status=EXPIRED. Then any EXPIRED whose original `timestamp < (now - 30 days)` is removed from the list entirely.
- `getStats()` also cleans then counts by current status (UNCLAIMED/CLAIMED/EXPIRED) plus total.
- NBT (toNBT/fromNBT) round-trips the whole rewards list (each entry serializes id, ts, expiry, source, status, eligibility, rewards as ItemStack saves, and metadata map) plus the buffer SlotBasedStorage. fromNBT is defensive: unknown source/status default to OTHER/UNCLAIMED; bad entries are skipped; empty reward stacks inside an entry are dropped on load.

**Worked example (times illustrative, rate irrelevant here)**:
- At t=1_000_000_000 ms a tourist batch arrives: `addReward(TOURIST_ARRIVAL, [1x emerald×12, 1x diamond], "ALL")` → entry E1 with 7-day expiry, eligibility "ALL".
- At t=1_000_000_100 ms a courier delivery for player P (uuid "p-uuid-123") completes: `addReward(COURIER_DELIVERY, [4x iron], "p-uuid-123")` → entry E2, 7-day expiry, personal eligibility.
- Player Q (different uuid) opens board: getUnclaimedRewards returns [E2, E1] (newest first, both still valid). Q's canBeClaimed("q-uuid") on E1 → true ("ALL"); on E2 → false (specific mismatch).
- Player P claims E2 with toBuffer=true: canBeClaimed succeeds, buffer add succeeds → E2 status becomes CLAIMED; P sees the iron in the 2×9 buffer grid.
- 8 days later (t ≈ 1_000_000_000 + 8*86400000): any call to getUnclaimedRewards or stats runs cleanup. E1 (if never claimed) has now > its expirationTime and status != CLAIMED → status set to EXPIRED. It remains visible in getAllRewards but not in unclaimed. If it stays EXPIRED and its creation ts is now >30 days old, the next cleanup prunes it from the list.
- Board never exceeds 100 rows: on the 101st addReward the oldest (by timestamp) non-protected rows are dropped after a cleanup pass.

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `TownPaymentBoard.addReward(RewardSource, List<ItemStack>, String eligibility)` | `common/src/main/java/com/quackers29/businesscraft/town/data/TownPaymentBoard.java` | Guarded insert; forces 7-day expiry; trims to MAX_REWARDS=100 oldest-first after cleanup if needed; returns the new entry's UUID or null |
| `TownPaymentBoard.getUnclaimedRewards()` | same | Runs cleanup; returns only UNCLAIMED && !isExpired(), newest-first by timestamp |
| `TownPaymentBoard.claimReward(UUID, String claimerEligibility, boolean toBuffer)` | same | Finds by id; delegates to entry.canBeClaimed; on success marks CLAIMED; buffer path requires SlotBasedStorage space (18 slots) or fails without claiming; inventory path trusts caller |
| `TownPaymentBoard.cleanupExpiredRewards()` | same | Two-phase: mark qualifying expired→EXPIRED (if status != CLAIMED), then remove any EXPIRED whose creation timestamp < now-30d |
| `TownPaymentBoard.getStats()` / `getAllRewards()` / `getRewardsBySource(...)` / `getRewardById(UUID)` | same | Stats counts by status after cleanup; the getters return sorted views (newest first) or filtered; getRewardById is the public Optional wrapper over private find |
| `RewardEntry.canBeClaimed(String claimerEligibility)` | `common/src/main/java/com/quackers29/businesscraft/town/data/RewardEntry.java` | `status == UNCLAIMED && !isExpired() && ("ALL".equals(eligibility) \|\| eligibility.equals(claimerEligibility))` |
| `RewardEntry.isExpired()` | same | `System.currentTimeMillis() > expirationTime` (strict greater) |
| `RewardEntry` (ctor + fromNBT/fromNetwork + metadata + getters/setters) | same | Holds immutable id/timestamp/source/rewards + mutable expiration/status + eligibility + metadata map; equals/hash by id only |
| `ClaimStatus` | `common/src/main/java/com/quackers29/businesscraft/town/data/ClaimStatus.java` | Enum: UNCLAIMED, CLAIMED, EXPIRED |
| `TownPaymentBoard.ClaimResult` / `PaymentBoardStats` | TownPaymentBoard.java (inner) | Simple value objects for claim outcome (success + message + items) and counts (unclaimed/claimed/expired/total) |

## Rules & formulas (exact)
From the code (write-from-implementation):

- **Default vs forced expiry**:
  - `RewardEntry` public ctor: `expirationTime = timestamp + (24 * 60 * 60 * 1000L)` (24 h).
  - `TownPaymentBoard.addReward` (the only normal creation path): immediately `entry.setExpirationTime(System.currentTimeMillis() + DEFAULT_EXPIRATION_TIME)` where `DEFAULT_EXPIRATION_TIME = 7 * 24 * 60 * 60 * 1000L`.
  - Result: board-added rewards always get 7 calendar days from the moment of add, regardless of the 24 h written in the entry ctor. The 24 h default is only observable for entries constructed directly or loaded via the private ctor before any set.

- **addReward guard**:
  ```java
  if (rewardItems == null || rewardItems.isEmpty()) {
      LOGGER.warn(...);
      return null;
  }
  ```
  No entry is created; caller receives null.

- **Eligibility & claim gate (RewardEntry.canBeClaimed)**:
  ```java
  if (status != ClaimStatus.UNCLAIMED) return false;
  if (isExpired()) return false;
  return "ALL".equals(eligibility) || eligibility.equals(claimerEligibility);
  ```
  Exact string match (case-sensitive). "ALL" is special and matches any claimer string. A specific eligibility (UUID string) matches only that exact value.

- **claimReward decision** (after find):
  - Not found → `ClaimResult(false, "Reward not found")`
  - !canBeClaimed → reason derived from status (CLAIMED/EXPIRED) or `isExpired()` or generic "not eligible"
  - toBuffer path success: `addToBufferStorage(entry.getRewards())` must return true for all items (uses SlotBasedStorage smart add); then set CLAIMED; return success + items. Failure (any item wouldn't fit) → `false, "Buffer storage is full"` and status unchanged.
  - !toBuffer path: unconditionally `setStatus(CLAIMED)` then return success + items (caller responsible for giving them to the player or overflowing).

- **getUnclaimedRewards**:
  - Always `cleanupExpiredRewards()` first.
  - Stream filter: `status == UNCLAIMED && !entry.isExpired()`
  - Then `.sorted(Comparator.comparing(RewardEntry::getTimestamp).reversed())`
  - So newest (largest ts) first.

- **cleanupExpiredRewards** (per-entry):
  - If `entry.isExpired() && entry.getStatus() != ClaimStatus.CLAIMED` → `setStatus(EXPIRED)`
  - Independently: if `entry.getTimestamp() < (now - 30d) && status == EXPIRED` → iterator.remove()
  - Note: prune uses the *creation timestamp*, not the expirationTime. A reward created long ago but with a manually extended expiration could still be pruned once 30 d old.

- **MAX_REWARDS trim (in addReward after insert)**:
  - Hard cap 100.
  - If over: first `cleanupExpiredRewards()`.
  - If still over: `rewards.sort(Comparator.comparing(RewardEntry::getTimestamp))` (oldest first), then `while (size > 100) remove(0)`.
  - Debug logs the removals.

- **Stats**:
  - After cleanup: counts current UNCLAIMED / CLAIMED / EXPIRED + total size.

- **NBT roundtrip (RewardEntry)**:
  - toNBT writes all scalar fields + rewards as stack.save() compounds + metadata as string map.
  - fromNBT is try/catch: unknown RewardSource → OTHER with warn; unknown ClaimStatus → UNCLAIMED with warn; empty eligibility → "ALL"; only non-empty stacks after `ItemStack.of` are kept in the rewards list; metadata loaded only if present.
  - TownPaymentBoard.fromNBT clears then repopulates rewards + delegates bufferStorage.fromNBT.

- **Buffer storage**:
  - 18 slots (hard-coded `new SlotBasedStorage(18)`).
  - addToBuffer / removeFromBuffer / getBufferStorage are legacy shims that chunk counts to Integer.MAX_VALUE because ItemStack count is int; they delegate to the slot storage.
  - getBufferStorageSlots exposes the real SlotBasedStorage for new code.

- **Equality**:
  - RewardEntry equals/hashCode based solely on id (UUID). Two entries with same id are considered the same even if other fields differ.

## Edge cases & behaviors
- Add empty/null list → immediate null return, no side effects, warning logged.
- Eligibility "ALL" (from tourist arrival, milestone, some trade paths) is claimable by any player string.
- Personal eligibility (courier pickup/delivery, some contract resolutions) uses the relevant player's/courier's UUID string; only exact match (or the owner) can claim.
- Claim by wrong player for a personal reward: canBeClaimed false → "not eligible".
- Claim after status moved to CLAIMED or EXPIRED, or after wall time passes expiry: canBeClaimed false with appropriate reason.
- toBuffer claim when the 18-slot buffer cannot accept every stack in the reward (considering stacking rules): fails, entry remains UNCLAIMED.
- Non-toBuffer claim always marks CLAIMED (even if the returned items later cannot be given to the player — overflow handling lives in the packet handler).
- `isExpired()` uses strict `>` on wall clock vs expirationTime. Setting expirationTime to exactly now still counts as not-expired until the next millisecond tick.
- Cleanup is opportunistic (called from many read paths); an entry can sit past its expiry until the next cleanup-touching call.
- 30-day prune uses creation timestamp, so a very old entry that was CLAIMED long ago is never pruned by this rule (only EXPIRED ones are candidates).
- MAX trim happens only on add when crossing 100; it may drop recently added but very old (by ts) entries after cleaning.
- getAllRewards and getRewardsBySource do **not** run cleanup and include expired/claimed rows (sorted newest first).
- RewardEntry constructed directly (tests or future code) gets 24 h unless setExpirationTime is called afterward.
- getRewardsDisplay uses LinkedHashMap + hover names to produce stable "3x Emerald, Diamond" style strings; it is side-effect free on the data but depends on ItemStack hover text (not pure-logic in a headless test).
- fromNBT with a reward stack tag that has no "id" key produces an EMPTY stack which is filtered out → the loaded entry may have fewer (or zero) reward items than when it was saved. This is a potential roundtrip fidelity quirk for entries that only ever contained air/empty stacks.
- ClaimResult and PaymentBoardStats are simple immutable snapshots; ClaimResult always defensively copies the item list.

## Test coverage
- Test file: none (NEEDS-MC)
- The core eligibility, expiry, claim-decision, cleanup, and trim logic lives in RewardEntry.canBeClaimed / isExpired and TownPaymentBoard methods, but both classes have hard dependencies that prevent pure JUnit instantiation:
  - TownPaymentBoard eagerly `new SlotBasedStorage(18)` in its field initializer → ItemStack[] + ItemStack.EMPTY → triggers ItemStack codec/registry static init (same "Not bootstrapped" crash seen in T-007 / T-013).
  - RewardEntry public ctor and fromNBT/fromNetwork all take or produce List<ItemStack>; even referencing ItemStack.EMPTY in a test that loads these classes initializes the failing ItemStack <clinit>.
- Only the trivial ClaimStatus enum values could be referenced in isolation. No meaningful guard-only surface existed that would justify a test class under the loop rules (cf. T-007 which got 4 population/null guards before hitting the wall).
- If MC bootstrap or a GameTest harness with real registries becomes available, the positive paths (add non-empty real stacks, buffer space behavior, full item NBT fidelity, claim to real inventories) would be covered by the scenarios described in the "Rules & formulas" and "Edge cases" sections above.
- The vault note itself was written first per protocol; it captures the exact formulas and multiplayer fairness rules (ALL vs UUID eligibility) from code inspection.

## Open questions
- **Testability blocker (ItemStack bootstrap)**: Even the pure decision methods (canBeClaimed, claimReward logic, cleanup rules, eligibility string matching) cannot be exercised in the current JUnit setup because TownPaymentBoard and RewardEntry construction unconditionally touch ItemStack (eager buffer + ctor param). This is the same fundamental limit that caused T-002/T-007/T-008/T-013 to be marked NEEDS-MC. No production bug; just an architectural coupling of data objects to MC item types. Future improvement: factor the pure claim/eligibility/expiry state into a POJO that the board wraps, or provide a test seam.
- **24 h vs 7 d discrepancy**: RewardEntry public ctor hard-codes +24 h, but the only caller (addReward) always overwrites it to +7 d. The 24 h value is effectively dead for all rewards that go through the payment board. If direct construction of RewardEntry becomes common, this default will surprise callers. Consider removing the default or making the board's value a constant visible to the entry.
- **Prune key is creation timestamp, not expiry**: An entry created 40 days ago with a manually far-future expirationTime will still be hard-pruned once it is EXPIRED, because the 30 d check keys off `getTimestamp()`. This may be intentional (age of the reward record) but is not the same as "30 days after it became unclaimable."
- **NBT fidelity for empty-reward entries**: Saving an entry whose reward list contained only ItemStack.EMPTY (or became empty after filtering) will reload with 0 items. If any production path can create such an entry and later expects the count or content, this is a silent data loss on reload. (Current production paths always put at least one real stack, so probably harmless today.)
- **Buffer storage size hard-coded**: 18 slots is magic in TownPaymentBoard; SlotBasedStorage itself is generic. If the UI ever wants a larger claim staging area this constant must move in sync.
- **No per-player claim tracking beyond the eligibility string**: Once claimed the entry just becomes CLAIMED with no record of *who* claimed it. For audit or "only the claimer can un-claim" features this would need to be extended (metadata already exists as an escape hatch).
- `getRewardsDisplay`, `getTimeAgoDisplay`, etc. on RewardEntry are presentation helpers that pull from the contained ItemStacks and BCTimeUtils; they are not exercised in pure unit tests.

## Related
- [[Town/Town Overview]]
- [[Economy/Tourist Payments/Distance Payment Calculation]] (T-001 — produces many of the "ALL" TOURIST_ARRIVAL rewards)
- [[Economy/Milestones/Distance Milestone Resolution]] (T-002 — also feeds MILESTONE rewards with "ALL")
- [[Trade/Contracts/Courier Delivery Rewards]] (T-005 — produces the personal COURIER_* rewards using actor UUID as eligibility)
- [[Town/Visits/Visit Buffer]] (T-010 — upstream batching that leads to the bundled rewards)
