---
tags:
  - detail
  - trade
---
# Bid Selection and Clamping

**Breadcrumb**: Trade > Contracts > Bid Selection and Clamping
**TL;DR**: Bids are per-bidder personal maxima only (you can raise your offer but never lower it); the winner is whichever bidder currently holds the single highest of those maxima; time-based expiry and extensions always measure from the instant the check or extend call happens.

## What it does
Contracts (primarily sell auctions between towns) collect competing emerald offers from potential buyer towns. The base contract keeps a map of "best offer so far" per bidder. This guarantees fairness—no one can snipe by retracting a high bid—and gives a simple "current standing high bid" for the UI, for escrow calculations at bid time, and for picking a winner when the auction timer expires. All of this logic is pure data transformation on in-memory maps and wall-clock times; no world, items, or side effects.

## How it works (process view)
- A bidder submits an offer amount (emeralds). The contract stores max(what that bidder has offered before, or 0, new amount). If a name is supplied it is remembered for display.
- At any moment the "current high bid" is the largest of the per-bidder best-offers (or 0 if none). The winner UUID is the bidder who made that best offer.
- **Worked example**: TownA bids 18.0 emeralds → stored for A as 18.0. TownB bids 22.5 → stored for B as 22.5. TownA bids again at 15.0 → A still recorded at 18.0 (the lower offer is ignored). High bid = 22.5 by B.
- Expiry check (`isExpired`) is a simple "is wall clock past the stored expiry instant?". 
- `extendExpiry(millis)` does not add to the old expiry; it replaces the expiry with "right now + millis". This effectively resets the remaining window from the moment of the call.
- `expireNow()` forces the expiry into the past so the next isExpired check returns true (handy for tests and some shutdown paths).
- The full bid map (with names) round-trips through NBT as a list of compound entries so that saved auctions survive restarts with their bidding history intact.
- A convenience formatter turns the raw expiry instant into a short "MM/dd HH:mm" string for screens (JVM default timezone, no seconds).

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `Contract.addBid(UUID bidder, String name, float amount)` | `common/src/main/java/com/quackers29/businesscraft/contract/Contract.java` (line 76) | Stores `max(existingForBidder or 0, amount)` for that bidder only. Caches name if supplied. |
| `Contract.getHighestBid()` | same file (line 87) | Returns 0 when no bids, otherwise the maximum value among the per-bidder best offers. |
| `Contract.getHighestBidder()` | same file (line 91) | Returns the UUID that holds the current max value, or null when empty. On exact ties, returns whichever entry the stream max encounters first (HashMap iteration order). |
| `Contract.isExpired()` | same file (line 42) | `System.currentTimeMillis() > expiryTime` (strictly greater). |
| `Contract.extendExpiry(long additionalMillis)` | same file (line 62) | Sets `expiryTime = System.currentTimeMillis() + additionalMillis` (absolute reset from call instant, not relative extension of prior expiry). |
| `Contract.expireNow()` | same file (line 66) | Sets `expiryTime = System.currentTimeMillis() - 1` (guarantees next isExpired is true). |
| `Contract.getFullDateTimeDisplay()` | same file (line 156) | Formats `expiryTime` via `SimpleDateFormat("MM/dd HH:mm")` on a Date (local TZ of the process). |
| `Contract.save` / `load` (base portion) | same file (lines 97-150) | Serializes core fields + the entire bids map (as ListTag of {bidder, amount, optional name}) and bidderNames. Load clears then repopulates. |

## Rules & formulas (exact)
All taken directly from the code (not comments or prior docs).

**Per-bidder max clamp (addBid)**:
```java
bids.put(bidder, Math.max(bids.getOrDefault(bidder, 0f), amount));
if (name != null) bidderNames.put(bidder, name);
```
- A bidder can only improve (or equal) their own standing offer.
- Lower offers from the same bidder are silently ignored for the stored amount.
- First bid for a bidder with a negative amount will store the negative (no lower-bound guard here; callers are expected to pass sensible values).

**Highest selection**:
```java
return bids.isEmpty() ? 0f : bids.values().stream().max(Float::compare).orElse(0f);
```
```java
return bids.entrySet().stream()
    .max(Map.Entry.comparingByValue(Float::compare))
    .map(Map.Entry::getKey).orElse(null);
```
- Empty map → 0f / null.
- On value ties the `max` terminal operation returns the first element in encounter order (HashMap bucket order, which in practice follows insertion order for these puts but is not contractually guaranteed).

**Time controls** (all use wall clock at the moment of the call):
- `isExpired()`: strictly after the stored instant.
- `extendExpiry(m)`: expiry becomes "now + m" (remaining time is reset to exactly the supplied window).
- `expireNow()`: expiry becomes "now - 1 ms".

**Display formatting**:
```java
java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MM/dd HH:mm");
return sdf.format(new java.util.Date(this.expiryTime));
```
- Two-digit month/day, 24h HH:mm, no year, no seconds, no timezone indicator. Uses the JVM's default timezone at format time.

**NBT bids roundtrip** (save):
- Emits a ListTag "bids" under the root tag; each entry is a Compound with "bidder" (UUID), "amount" (float), and optional "name" (string).
- Load clears the maps then re-inserts in list order.

## Edge cases & behaviors
- No bids at all: `getHighestBid()` == 0f, `getHighestBidder()` == null, `getBids()` returns the (empty) live map.
- Single bidder, one or more raises: highest reflects only that bidder's personal max.
- Same bidder submits a lower amount: stored value is unchanged (the "clamp" behavior).
- Negative or zero amount on first bid for a bidder: the value is stored as-is (no `Math.max(0, ...)` in addBid itself).
- Exact tie between two different bidders: which one is returned by getHighestBidder depends on encounter order in the stream (pinned by test as current HashMap behavior).
- `extendExpiry` always measures the new window from the call's System.currentTimeMillis(), even if called multiple times in rapid succession or after some time has already passed.
- `isExpired()` uses the instant of the call; two calls microseconds apart can straddle the boundary.
- `getFullDateTimeDisplay()` result changes only when the stored expiryTime changes; it does not depend on "now".
- Roundtrip through save/load (with bids present) restores exact float values, UUIDs, the per-bidder names, creation/expiry times, and the completed flag. Order of bids list in the tag is insertion order of the map at save time.
- `getBids()` returns the internal mutable map (no defensive copy). Callers in production treat it as read-only.

## Test coverage
- Test file: `common/src/test/java/com/quackers29/businesscraft/contract/ContractTest.java`
- Covered (all with hand-computed expectations in comments): empty-bids paths, single/multi-bidder happy paths, same-bidder raise vs. ignored-lower, negative-bid storage, tie resolution (current encounter order), isExpired true/false boundaries, extendExpiry absolute reset semantics, expireNow, getFullDateTimeDisplay format, full base + bids NBT save/load roundtrips (via a concrete subclass), bidder name caching and fallback.
- Not covered: actual auction orchestration / escrow movement / market side-effects (those live in ContractBoard and are covered by T-003), Sell/Courier subclass fields (covered by their own tests), real wall-clock races (inherently non-deterministic; we use the helper mutators for determinism).
- A minimal concrete TestContract subclass is used inside the test so only base logic is exercised (no Sell-specific clamps or courier fields).

## Open questions
- **Negative bid amounts**: addBid will happily store a negative if it is the first (or highest) offer for that bidder. In normal play callers never pass <= 0, but nothing in the base class prevents it. A pinning test asserts the current "store whatever" behavior.
- **Tie breaker is not stable by design**: on exact equal high bids the winner chosen is whichever entry the HashMap stream yields first. This is acceptable for the game (rare, and money amounts are floats but usually whole emeralds after ceiling at call sites) but worth noting if deterministic "earliest bidder wins" is ever desired.
- **extendExpiry is a "set remaining" not "add extra"**: calling it replaces the deadline from the call instant. This is used intentionally for courier-acceptance windows after resolution, but callers must not assume it lengthens whatever was left.
- **getBids() mutability**: returns the live internal map. Production code does not mutate through it after construction, but a future defensive copy would be a behavior change worth a note.
- **Date formatting uses default TZ**: the "MM/dd HH:mm" string is JVM-local. In a server with players in many timezones this display is only cosmetic; the underlying long instant is what matters for logic.

## Related
- [[Trade/Contracts/Auction Resolution]] (T-003 — the board that calls addBid / getHighest* and uses the winner)
- [[Trade/Contracts/Sell Contract Lifecycle]] (T-004 — subclass that adds resource/quantity/price + delivery state on top of these base mechanisms)
- [[Trade/Contracts/Courier Delivery Rewards]] (T-005)
- [[Trade/Global Market/Price Calculation]] (T-006 — receives transaction price derived from the winning bid)
- [[Trade/Trade Overview]]
