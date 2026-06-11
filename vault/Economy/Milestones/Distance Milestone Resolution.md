---
tags:
  - detail
  - economy
---
# Distance Milestone Resolution

**Breadcrumb**: Economy > Milestones > Distance Milestone Resolution
**TL;DR**: On tourist batch arrival, if the average distance traveled meets or exceeds the highest configured milestone threshold (and milestones enabled), the destination town is awarded the associated item rewards (each stack count scaled by tourist count in the batch); rewards are delivered to the Payment Board under `RewardSource.MILESTONE` (or bundled into TOURIST_ARRIVAL).

## What it does
Long tourist journeys award bonus items (food, XP bottles, or custom) on top of the base fare payment. This gives players an incentive to build long-distance transport links. The system is fully configurable via `businesscraft.toml` under `[milestones]`, supports multiple thresholds, and silently ignores unresolvable reward items. The milestone check runs in the same visit processing pass that calculates fares (see Distance Payment Calculation), using the same captured average distance.

## How it works (process view)
- When a batch of tourists from one origin arrives and is processed, `VisitorProcessingHelper` captures the average travel distance, computes the fare, then calls `DistanceMilestoneHelper.checkMilestones(averageDistance, count)`.
- If `ConfigLoader.enableMilestones` is false, or no configured threshold is <= the distance, the result has `milestoneAchieved = -1` and empty rewards.
- Otherwise the *highest* qualifying threshold is selected; its reward string list (e.g. `["minecraft:bread:1", "minecraft:experience_bottle:2"]`) is parsed into ItemStacks, each stack's count multiplied by the tourist count in the batch, and returned in the result.
- If the result has rewards, they are either added as a standalone MILESTONE reward entry or bundled with the fare into a TOURIST_ARRIVAL entry on the destination town's Payment Board.
- Players see a gold "🏆 Distance Milestone | 123m journey | +4 Bread, 8 Experience Bottles" notification (if notify enabled).
- **Worked example (default config)**: milestone at 10 blocks with `["minecraft:bread:1", "minecraft:experience_bottle:2"]`. 4 tourists travel 15 blocks average → checkMilestones(15, 4) selects 10, yields two stacks: bread count=1*4=4, exp_bottle count=2*4=8. These items go to the Payment Board for claiming.
- Config is hot-reloadable; changing enabled or the rewards list affects the next arrival batch immediately.

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `DistanceMilestoneHelper.checkMilestones(double distance, int touristCount)` | `common/src/main/java/com/quackers29/businesscraft/town/data/DistanceMilestoneHelper.java` (line 54) | Public entry: guard on enable, selects highest milestone threshold <= distance, delegates to parse+scale for its rewards; returns MilestoneResult |
| `DistanceMilestoneHelper.MilestoneResult` (static inner) | same file (line 29) | Immutable-ish result holder: actualDistance, milestoneAchieved (-1=none), rewards (List<ItemStack>), touristCount; hasRewards() = !empty |
| `DistanceMilestoneHelper.parseRewards(List<String>, int)` (private) | same (line 99) | For each reward string, parse to ItemStack then mutate count *= touristCount (only non-empty added) |
| `DistanceMilestoneHelper.parseRewardString(String)` (private) | same (line 124) | "namespace:item" or "namespace:item:count" → ResourceLocation → registry.getItem → ItemStack (count default 1); all failures → ItemStack.EMPTY + log warn; outer try/catch |
| `DistanceMilestoneHelper.deliverRewards(Town, MilestoneResult)` (public) | same (line 180) | If hasRewards, copies list and calls town.getPaymentBoard().addReward(RewardSource.MILESTONE, list, "ALL"); returns true on non-null id |
| `DistanceMilestoneHelper.getMilestoneDistances()` / `getMilestoneRewards(int)` | same (lines 221, 233) | Return sorted list of configured distances; or the raw reward string list for a distance (or empty) |
| `ConfigLoader.enableMilestones` / `milestoneRewards` (Map<Integer, List<String>>) | `common/src/main/java/com/quackers29/businesscraft/config/ConfigLoader.java` (lines 47-48, 151-152, 209-227) | Source of truth; loaded from `[milestones]` section (or defaults to 10→[bread:1, exp_bottle:2] if no entries) |

## Rules & formulas (exact)
Selection (checkMilestones, ~lines 60-89):

```java
if (!ConfigLoader.enableMilestones) {
    return new MilestoneResult(distance, -1, new ArrayList<>(), touristCount);
}
int achievedMilestone = -1;
for (int milestoneDistance : ConfigLoader.milestoneRewards.keySet()) {
    if (distance >= milestoneDistance && milestoneDistance > achievedMilestone) {
        achievedMilestone = milestoneDistance;
    }
}
if (achievedMilestone == -1) {
    return new MilestoneResult(distance, -1, new ArrayList<>(), touristCount);
}
List<String> rewardStrings = ConfigLoader.milestoneRewards.get(achievedMilestone);
List<ItemStack> rewards = parseRewards(rewardStrings, touristCount);
return new MilestoneResult(distance, achievedMilestone, rewards, touristCount);
```

- Highest threshold wins: the loop is a max-over-keys-where-distance >= key (HashMap iteration order irrelevant because of the `> achieved` guard).
- Scaling: performed in parseRewards *after* single parse: `reward.setCount(reward.getCount() * touristCount);` — applies per reward line, so a milestone defining two items gives two scaled stacks.
- Parse details (parseRewardString):
  - Split on ":"; require >=2 parts else EMPTY + warn.
  - namespace=parts[0], itemName=parts[1]; ResourceLocation created directly (no validation beyond that).
  - count = 1; if >=3 parts, Integer.parseInt(parts[2]); if parse fails or result <=0 → count=1 + warn.
  - Lookup: `PlatformAccess.getRegistry().getItem(itemId)` — expected to return Item or something castable; null or non-Item → EMPTY + warn.
  - Final: `new ItemStack(item, count)` (the scaled mutation happens on this instance in caller).
- Defaults (when toml has no `[[milestones.rewards]]` or empty): milestone 10 with `["minecraft:bread:1", "minecraft:experience_bottle:2"]`.
- deliverRewards only acts if `hasRewards()`; it does a shallow copy of the list before posting.
- All config values read live from the public statics at call time (hot-reload friendly).

## Edge cases & behaviors
- `enableMilestones = false` → always milestoneAchieved=-1, empty rewards (even for 10000-block trips).
- Empty `milestoneRewards` map (or no keys) → -1, empty.
- `distance <` every configured threshold (including distance=0 or negative) → -1, empty.
- `distance ==` a threshold → that milestone is achieved (uses `>=`).
- Multiple thresholds (e.g. 10, 50, 200): distance=120 selects 50 (highest <=120); distance=200 selects 200; distance=199 selects 50.
- `touristCount = 0` on an achieved milestone → parse produces stacks, then count *=0 → stacks with count 0 (likely treated as empty later); result still carries milestoneAchieved and touristCount=0.
- `touristCount < 0` → would produce negative counts (bad for ItemStack); unreachable in normal flow.
- Bad reward strings (wrong part count, non-numeric count, count<=0, unknown item id, bad namespace) → that entry contributes 0 items; other entries for the same milestone may still succeed. No exception propagates.
- ResourceLocation constructor or registry lookup throws → caught, that string → EMPTY.
- `deliverRewards` with empty result → immediate false, no board interaction.
- `deliverRewards` failure (addReward returns null or exception) → logs warning/error, returns false; rewards are lost for that batch.
- The achieved milestone id (the distance number) is recorded in the result even if all parsed rewards became empty (e.g. misconfigured items) — callers that only look at hasRewards() will not see a "milestone" effect.
- Debug logging (when enabled) emits at every decision point but does not affect behavior.

## Test coverage
- Test file: `common/src/test/java/com/quackers29/businesscraft/town/data/DistanceMilestoneHelperTest.java`
- Covered (8 tests, direct calls only): the three public config/UI helpers (`getMilestoneDistances` sorted, `getMilestoneRewards`); `checkMilestones` guard paths that return `milestoneAchieved=-1` (disabled, empty map, distance below/==0/negative all thresholds). Also basic `MilestoneResult.hasRewards()` on the empty-rewards case.
- Not covered: any path that selects a positive `milestoneAchieved` (the threshold matching + "reward selection" for an achieved milestone) — those paths always invoke `parseRewards`/`parseRewardString` which reference `ItemStack.EMPTY` (and transitively other net.minecraft classes). Their static initializers throw `IllegalArgumentException` (or cause `NoClassDefFoundError` / `ExceptionInInitializerError`) in a pure JUnit context with no MC bootstrap. `deliverRewards`, notifications, and hot-reload also require game state.
- Note: because the positive-resolution logic is entangled with the parse call, the core of "Distance Milestone Resolution" is not unit-testable as written. 8 guard/getter tests + full suite stay green. See Open questions and protocol (NEEDS-MC).

## Open questions
- **Core resolution not unit-testable (root cause of NEEDS-MC)**: The loop that computes `achievedMilestone` (the actual "threshold matching") is followed immediately by the parseRewards call when a match is found. Executing any test that would observe `milestoneAchieved >= 0` forces `parseRewardString` to run, which does `new ResourceLocation` + registry call + `ItemStack.EMPTY` (in the null-item case) or `new ItemStack`. Either path initializes MC classes whose `<clinit>` throws `IllegalArgumentException` (or EIIE/NoClassDef) in the Gradle `:common:test` environment. Only the early-return `-1` + empty branches + the two getters can be safely executed. This was discovered during T-002; a pure helper method for "int findHighestMilestone(double, Collection<Integer>)" would have allowed full coverage of the selection rule.
- **Registry coupling in reward path**: `parseRewardString` unconditionally calls `PlatformAccess.getRegistry().getItem(...)`. Even with a test double, successful non-null Items trigger enough MC statics to crash the test JVM. The string parsing, count defaulting, and scaling (`* touristCount`) logic therefore cannot be asserted with real expected ItemStacks here.
- **Silent drop on bad rewards**: if a milestone's items are all unresolvable at runtime, the batch still "achieves" the milestone number (the distance value is recorded) but players receive nothing extra and no error is surfaced to the player. Is this desired, or should there be a fallback or admin-visible log?
- **Scaling to count=0**: would (if parse succeeded) produce zero-count ItemStacks in the result list. The current guard tests never reach that; harmless today because callers check `hasRewards()`, but worth a note.
- **Distance units**: code and javadoc say "blocks"; toml comment says "meters". In Minecraft horizontal travel they are equivalent for this purpose, but worth noting if vertical or other metrics ever appear.
- `deliverRewards` mutates the town's payment board as a side effect and is the only way milestone items reach players outside the bundled path; its success depends on implementation details of RewardEntry/PaymentBoard (see T-012).
- No validation on milestone distances (e.g. duplicates ignored by Map, negative distances would be unachievable since dist>=0).

## Related
- [[Economy/Economy Overview]]
- [[Economy/Tourist Payments/Distance Payment Calculation]] (T-001 — supplies the `averageDistance` and calls this immediately after fare calc)
- [[Town/Visits/Visit Buffer]] (T-010)
- [[Town/Payment Board/Reward Claims]] (T-012 — final destination of milestone items)
- [[Town/Data/Visitor Processing]] (internal)
