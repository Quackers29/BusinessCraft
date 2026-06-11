---
tags:
  - detail
  - core
---
# Time Display Formatting

**Breadcrumb**: Core > Time > Time Display Formatting
**TL;DR**: Shared utility that turns raw epoch millisecond values into consistent human-readable strings for durations ("2d 5h", "5m 30s", "0s"), time remaining ("Expired" or the duration), "time ago" labels, and calendar date/times in the configured timezone (default UTC); powers all contract and reward displays.

## What it does
BusinessCraft shows players remaining auction times, when rewards were earned, creation dates, and "X ago" labels in UIs (contract lists, payment board history, detail views). All of these displays are produced by one place so the format never drifts between screens. The formatting rules are deliberately simple integer math (no external libs beyond java.time) and always produce short strings suitable for tight UI space. Timezone is configurable so servers in different regions see local wall time for the date parts.

## How it works (process view)
- Callers (contract viewmodels, RewardEntry) pass an epoch millis timestamp plus the "current" server time (usually System.currentTimeMillis() captured at request time or passed down).
- `formatTimeRemaining(expiry, serverNow)` returns "Expired" if already past, otherwise the formatted positive duration.
- `formatDuration(millis)` breaks the positive milliseconds down by successive integer division (days, hours, minutes, seconds) and assembles the largest two adjacent units only.
- `formatTimeAgo(timestamp, now)` computes the difference and picks the largest unit that is >0, or "Just now".
- Date formatting methods (`formatDateTime`, `formatFullDateTime`, `formatTimeOnly`) convert the millis to a ZonedDateTime in the currently configured zone and apply a fixed pattern.
- `setTimezone` accepts "UTC", "SYSTEM", or any java ZoneId string; invalid values fall back to UTC. This affects only the three date/time formatters; pure duration strings are timezone-agnostic.
- **Worked example**: expiry = 1_704_000_000_000L (some future), serverNow = expiry - (2*86400 + 5*3600)*1000 → formatTimeRemaining returns "2d 5h". A 125_000 ms duration formats as "2m 5s". A timestamp 45 seconds in the past with now at call time yields "Just now" (diff < 60s). Setting zone to "America/New_York" makes formatDateTime produce the equivalent local clock time string.

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `BCTimeUtils.formatTimeRemaining(long expiryEpoch, long serverNow)` | `common/src/main/java/com/quackers29/businesscraft/util/BCTimeUtils.java:118` | Returns "Expired" or the positive duration string; the main entry point for countdowns in contracts and rewards. |
| `BCTimeUtils.formatDuration(long millis)` | same:143 | Core breakdown: converts ms > 0 into "Xd Yh", "Xh Ym", "Xm Ys", or "Zs" using successive integer division; <=0 yields "0s". |
| `BCTimeUtils.formatTimeAgo(long timestamp, long now)` | same:179 | "time ago" labels: floors diff into days/hours/minutes or "Just now"; negative diff (future ts) yields "In the future". |
| `BCTimeUtils.isExpired(long expiryEpoch, long serverNow)` | same:250 | Simple `serverNow > expiryEpoch` (strict). Used for gating bids, claims, etc. |
| `BCTimeUtils.getTimeRemaining(long expiryEpoch, long serverNow)` | same:261 | Returns max(0, expiry - now) in millis; the raw value behind the formatted remaining string. |
| `BCTimeUtils.formatDateTime(long epochMillis)` / `formatFullDateTime` / `formatTimeOnly` | same:208 | Apply the configured zone + fixed patterns ("MM/dd HH:mm", "MMM dd, yyyy HH:mm:ss", "HH:mm:ss"). |
| `BCTimeUtils.setTimezone(String)` / `getTimezone()` | same:74 | Mutator for the static configuredZone; "UTC", "SYSTEM", or ZoneId string; bad input → UTC. Affects only date/time formatters. |

## Rules & formulas (exact)
All duration logic uses plain integer division on milliseconds (no rounding, floors toward zero for positive values).

```java
// formatTimeRemaining
long remaining = expiryEpoch - serverNow;
if (remaining <= 0) return "Expired";
return formatDuration(remaining);
```

```java
// formatDuration
if (millis <= 0) return "0s";
long seconds = millis / 1000;
long minutes = seconds / 60;
long hours   = minutes / 60;
long days    = hours / 24;

if (days > 0) {
    return days + "d " + (hours % 24) + "h";
} else if (hours > 0) {
    return hours + "h " + (minutes % 60) + "m";
} else if (minutes > 0) {
    return minutes + "m " + (seconds % 60) + "s";
} else {
    return seconds + "s";
}
```

```java
// formatTimeAgo (diff floors)
long diff = now - timestamp;
if (diff < 0) return "In the future";
long minutes = diff / (60 * 1000);
long hours   = diff / (60 * 60 * 1000);
long days    = diff / (24 * 60 * 60 * 1000);
if (days > 0) return days + "d ago";
if (hours > 0) return hours + "h ago";
if (minutes > 0) return minutes + "m ago";
return "Just now";
```

- `isExpired`: `serverNow > expiryEpoch` (already passed, not >=). Exact equality at the ms is still "not expired".
- `getTimeRemaining`: `remaining > 0 ? remaining : 0`
- Date formatters always use the live `configuredZone` at call time; the three `DateTimeFormatter` constants are created once at class load with the pattern only (zone comes from the ZonedDateTime).
- Config integration: `ConfigLoader` calls `BCTimeUtils.setTimezone(displayTimezone)` during load (the `[display] displayTimezone` key).

## Edge cases & behaviors
- `millis <= 0` (duration) or `remaining <= 0` → "0s" / "Expired".
- Exact unit boundaries: 86400000 ms (exactly 1 day) → "1d 0h"; 3600000 ms → "1h 0m"; 60000 ms → "1m 0s".
- 59999 ms → "59s" (not rounded up).
- Very large values: days component can grow arbitrarily (no cap); hours component is always %24 when days present.
- Future timestamp for "ago" → "In the future" (not a negative number).
- diff < 60000 ms for ago → "Just now" (even 59999 ms).
- Timezone: setTimezone(null) / "" / "UTC" / unknown string all result in "UTC". "SYSTEM" uses ZoneId.systemDefault().
- The static zone and formatters are process-global; concurrent tests or config reloads must save/restore to avoid cross-test pollution (exactly as done for ConfigLoader statics in other tests).
- No locale awareness in duration strings (always English abbreviations); date formatting inherits the JVM default locale for month names via the pattern.

## Test coverage
- Test file: `common/src/test/java/com/quackers29/businesscraft/util/BCTimeUtilsTest.java`
- Covered: all public formatting + predicate methods (formatDuration, formatTimeRemaining, formatTimeAgo, isExpired, getTimeRemaining, the three date formatters, set/getTimezone) with hand-computed expectations; zero/negative/future boundaries; unit rollover points (exact 1m/1h/1d); timezone set/get and effect on date output only; save/restore hygiene for the mutable static.
- 29 tests; full suite green.
- Intentionally not covered: actual wall-clock passage of time, JVM default zone differences (we force UTC in tests), locale-specific month names (patterns are fixed).

## Open questions
- The "In the future" string for negative diff in formatTimeAgo is defensive but has never been observed in normal play (timestamps come from past events or future expiries). If a UI ever shows it, consider whether a different treatment is wanted.
- Integer division means sub-second durations are lost (always whole seconds). This matches the UI needs but is worth noting if sub-second precision ever becomes relevant.
- Callers sometimes pass `System.currentTimeMillis()` directly inside the formatting call; that means two calls microseconds apart can theoretically differ. Production code captures "serverTime" once per request where possible (see Contract*ViewModelBuilder).
- No thread-safety hardening around the static zone/formatters (simple assignments). In a hot-reload scenario this is fine; documented for completeness.

## Related
- [[Trade/Trade Overview]]
- [[Trade/Contracts/Sell Contract Lifecycle]]
- [[Town/Payment Board/Reward Claims]]
- [[Config/Config Overview]] (the displayTimezone setting that feeds setTimezone)
- [[Economy/Economy Overview]] (rewards use the time-ago labels)
