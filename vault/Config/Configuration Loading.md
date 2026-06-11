---
tags:
  - detail
  - config
---
# Configuration Loading

**Breadcrumb**: Config > Configuration Loading
**TL;DR**: ConfigLoader holds ~30 public static fields as the single source of live configuration (Java field defaults + TOML overrides via NightConfig); loadConfig() ensures the businesscraft.toml file exists (copying a resource default or synthesizing via save) then populates fields by sectioned getOrElse lookups, with special clear+fallback logic for milestones and townNames; saveConfig() writes current memory values back with comments; hot reload is provided by ConfigurationService's WatchService + debounce.

## What it does
The mod's entire behavior (town spacing, tourist spawning thresholds, payment rates, milestone rewards, contract timings, trading/production toggles, timezone, etc.) is driven by these statics. Players edit businesscraft.toml (or let the default be generated) and changes take effect immediately on next load or via hot-reload when the file is edited while the game is running. No in-game settings UI exists; the TOML is the authority.

## How it works (process view)
- On first access to ConfigLoader (via the public INSTANCE singleton or any static field read), the ctor calls loadConfig().
- loadConfig() asks the current PlatformHelper for the config directory (e.g. .minecraft/config), creates the dir if needed, and if no businesscraft.toml exists it copies the embedded default from the jar resources (or falls back to saveConfig() for a minimal file).
- It then opens the file with NightConfig's CommentedFileConfig, calls load(), and for every known key does `config.getIntOrElse("section.key", DEFAULT)` or `getOrElse(...)` (the defaults exactly match the Java field initializers and the shipped toml).
- Special cases:
  - `general.townNames` falls back to an internal list of 14 themed names via private getDefaultTownNames().
  - `milestones.rewards` (a list of tables) is handled by loadMilestoneRewards: if the list is absent or empty the map is populated with a single default entry (distance 10 → bread×1 + experience_bottle×2); otherwise each entry is parsed (distance as Number, items as list of "modid:item:count" strings) and inserted only if distance > 0 and items non-empty.
- After the TOML block, loadRegistries() is called (Resource/Production/Upgrade/Biome registries, which themselves may consult the platform config dir).
- Hot reload: registerWithHotReload() registers the main config with ConfigurationService, which sets up a JDK WatchService on the parent directory and a 250 ms debounced executor callback that re-invokes loadConfig() on detected modify/create.
- saveConfig() (also used by the in-game "save" path if needed) rebuilds a fresh CommentedFileConfig, sets every current static value plus a descriptive comment, handles milestone serialization symmetrically, and writes it.
- All values are live: changing a static (in tests, via commands, or after reload) immediately affects every reader (e.g. TouristEntity.expiry, payment math, boundary checks, name picker).

**Worked example**: A fresh install has no businesscraft.toml. loadConfig copies the resource default (metersPerEmerald = 50, one 10 m milestone, 14 town names, all systems enabled). A player edits the file to add `metersPerEmerald = 100` under [economy] and `[[milestones.rewards]] distance=50 items=["minecraft:emerald:1"]`, saves, and (with hot reload) the next tourist arrival batch uses the 100 m/emerald rate and the 50 m milestone becomes available. If the player sets `townNames = []`, name generation falls back to the literal "DefaultTown" in the two call sites.

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `ConfigLoader` (all public statics + `loadConfig()`, `saveConfig()`) | `common/src/main/java/com/quackers29/businesscraft/config/ConfigLoader.java` | Holds every config value as mutable public static; orchestrates file ensure + NightConfig parse + milestone special case + registry side-loads |
| `ConfigLoader.getDefaultTownNames()` (private static) | same | Returns the hardcoded 14-name list used only as the getOrElse fallback for a missing `general.townNames` key |
| `ConfigLoader.loadMilestoneRewards(CommentedFileConfig)` (private static) | same (~209) | Clears the map, applies the "empty list → default milestone" rule, then filters/parses valid distance+items entries |
| `ConfigLoader.copyDefaultConfig(Path)` / `saveConfig()` | same | Resource copy or full round-trip serialization with comments |
| `ConfigurationService` (registerConfiguration, reload*, watch loop, debounce) | `common/src/main/java/com/quackers29/businesscraft/config/ConfigurationService.java` | Singleton WatchService + ScheduledExecutor hot-reload dispatcher; returns Result<Void, BCError.ConfigError> with explicit null/empty/file checks |
| `ConfigLoader.INSTANCE` | same | Eager singleton whose ctor triggers the first load + hot-reload registration |
| `PlatformAccess.platform.getConfigDirectory()` | api + loader impls | Supplies the physical path; null platform → NPE caught inside loadConfig → Java defaults remain |

## Rules & formulas (exact)
All configuration is side-effectful mutation of public static fields. There are no "calculate X from config" methods inside ConfigLoader itself — the values are the config.

Defaults (source of truth = the Java field initializers; must match the shipped toml and every getOrElse second argument):

- Vehicles: enableCreateTrains=true, enableMinecarts=true, vehicleSearchRadius=3, minecartStopThreshold=0.001
- Town: minDistanceBetweenTowns=100, defaultStartingPopulation=5, craftableTownInterface=false, townNames=<14 names via getDefault...>, maxTouristsPerTown=1000, populationPerTourist=5, maxPopBasedTourists=20, minPopForTourists=5
- Tourist: touristExpiryMinutes=120.0, enableTouristExpiry=true, notifyOnTouristDeparture=true, touristSystemEnabled=true
- Economy: metersPerEmerald=50, currencyItem="minecraft:emerald"
- Milestones: enableMilestones=true, milestoneRewards populated by loadMilestoneRewards
- Player: playerTracking=true, townBoundaryMessages=true
- Trading: tradingEnabled=true, tradingTickInterval=60, tradingRestockRate=0.5f, tradingDefaultMaxStock=1000.0f
- Production: productionEnabled=true, productionTickInterval=100, dailyTickInterval=24000, minStockPercent=60, excessStockPercent=80
- Contracts: contractAuctionDurationMinutes=1.0, contractCourierAcceptanceMinutes=2.0, contractCourierDeliveryMinutesPerMeter=0.05, contractSnailMailDeliveryMinutesPerMeter=0.1, contractsEnabled=true
- Display: displayTimezone="UTC" (also pushed to BCTimeUtils on load)
- Research/phase11: researchEnabled=true

Parsing rules (loadConfig, lines ~124-182):
- Uses `getIntOrElse`, `getOrElse`, and for floats: `config.<Number>getOrElse("key", d).floatValue()` (or .doubleValue() in save path).
- `townNames` uses plain `getOrElse` returning List<String>.
- `milestones.rewards` is read as `List<CommentedConfig>`; the sub-table shape is `distance` (Number) + `items` (List<String>).
- After every successful NightConfig load the registries are (re)loaded as a side effect.

Milestone load logic (exact, from loadMilestoneRewards):
```java
milestoneRewards.clear();
List<CommentedConfig> rewards = config.getOrElse("milestones.rewards", new ArrayList<>());
if (rewards.isEmpty()) {
    List<String> defaultRewards = List.of("minecraft:bread:1", "minecraft:experience_bottle:2");
    milestoneRewards.put(10, new ArrayList<>(defaultRewards));
    return;
}
for (CommentedConfig reward : rewards) {
    int distance = reward.<Number>getOrElse("distance", 0).intValue();
    List<String> items = reward.getOrElse("items", new ArrayList<>());
    if (distance > 0 && !items.isEmpty()) {
        milestoneRewards.put(distance, new ArrayList<>(items));
    }
}
```
- The default is only installed on the empty-list (or absent) path.
- Later entries with the same distance overwrite earlier ones (HashMap).

Save is the inverse: every static is written with set(...) + setComment(...); milestones are turned into a list of sub-configs via saveMilestoneRewards.

Hot-reload debounce (ConfigurationService.handleFileChange):
- Ignores events if <1000 ms since our own lastModified.
- Schedules the reload callback 250 ms later on the executor.

Error handling: every I/O or platform failure path in loadConfig / saveConfig / copy is caught at high level; loadConfig leaves the Java field defaults in place and continues. No values are ever "invalidated".

## Edge cases & behaviors
- Platform not initialized (common in pure unit tests): `PlatformAccess.platform` is null → NPE on .getConfigDirectory() inside the try → caught → all fields retain their declared = defaults. (This is why tests can safely read/mutate ConfigLoader.* without a full mod bootstrap.)
- Missing businesscraft.toml: copy of the jar resource (or saveConfig() minimal if resource absent).
- Key present but value missing/wrong type in toml: NightConfig getOrElse supplies the documented default.
- `milestones.rewards = []` (empty list in file) → default (10 m) milestone is installed.
- `milestones.rewards` present with entries but every entry has distance <= 0 or empty items → map ends up empty (no default is retroactively installed).
- `general.townNames = []` in toml → field becomes empty list; both name-picking sites return the literal "DefaultTown".
- Overriding `townNames` to a custom list replaces the 14-name set entirely for that run.
- Negative or zero rates (e.g. metersPerEmerald=0 or negative) are accepted by the loader; downstream math (distance / rate) will produce Infinity/NaN or divide-by-zero effects in VisitorProcessingHelper etc. (no guard here).
- Float fields are stored as double in the toml layer on save but read back via Number.floatValue().
- Multiple rapid file edits: only the first after the 1 s quiet window triggers; the 250 ms schedule can coalesce.
- loadConfig / saveConfig are static and can be called directly (used by the reload callback and by tests that want to force a re-read from a temp dir).
- saveConfig always rewrites the entire file with the current memory state + the exact comments defined in source; any extra keys or user comments in the original toml are lost.
- displayTimezone change side-effects BCTimeUtils.setTimezone immediately during load.
- The eager INSTANCE creation means simply referencing any ConfigLoader field in a test forces the load attempt (and the platform NPE path if un-stubbed).

## Test coverage
- Test file: `common/src/test/java/com/quackers29/businesscraft/config/ConfigLoaderTest.java`
- 16 tests. Covered (via field defaults, reflection on private getDefaultTownNames, save/restore of platform + many statics, temp-dir + TestPlatformHelper stub + real toml writes + explicit loadConfig calls): all declared defaults, getOrElse behavior for missing keys, townNames fallback list, milestone empty-list default insertion, milestone invalid-entry skipping (distance<=0 or empty items), custom toml values overriding (including float, list, sub-table), empty townNames → "DefaultTown" path exercised indirectly via loader, platform-null fallback to defaults, save roundtrip for a subset of fields, ConfigurationService register validation (null/empty name/path/callback, non-existent file).
- Not covered (by design for this pure-logic iteration): actual WatchService file events (would require real FS timing + threads), full end-to-end hot-reload, the loadRegistries() side effects, BCTimeUtils timezone mutation, any consumer behavior that reads the values.
- Pattern: identical @BeforeEach/@AfterEach save+restore of ConfigLoader statics + PlatformAccess.platform as seen in T-001, T-004, T-009 etc. Private method tested via reflection. No MC Item/Block/Entity construction.

## Open questions
- No value-range or semantic validation lives in the loader (e.g. minDistanceBetweenTowns=0, metersPerEmerald=0, negative expiry minutes, tickInterval=0 are all accepted and can produce bad game behavior downstream). Consumers (TownValidationService, payment math, etc.) are responsible; this is a latent foot-gun.
- saveConfig destroys any keys/comments not explicitly known to it (no "preserve unknown" mode).
- The default milestone is baked into loadMilestoneRewards rather than the default toml resource or a constant; changing the default requires editing code + the example toml.
- loadRegistries() is called unconditionally on every loadConfig, even when only one scalar changed; the registries themselves do their own platform.getConfigDirectory() work.
- ConfigurationService always starts its watcher thread and executor on first getInstance(), even if the mod never registers anything.
- getDefaultTownNames is private; tests (and any future "reset to factory" feature) must use reflection or duplicate the list.
- The two call sites that guard empty townNames both hardcode the same "DefaultTown" literal — if that string ever needs to change it must be updated in two places (or centralized).
- NightConfig CommentedConfig / CommentedFileConfig are used directly; their exact getOrElse null-vs-absent semantics are relied upon but not unit-tested here beyond the loader's usage.

## Related
- [[Economy/Economy Overview]] (metersPerEmerald and currencyItem feed T-001 payment calc and T-006 GlobalMarket)
- [[Town/Town Overview]] (most town sizing, pop, tourist capacity, and boundary numbers come from here)
- [[Tourists/Tourists Overview]] (minPopForTourists, maxTouristsPerTown, expiry, enabled flags)
- [[Mod Overview]] (top-level config area)
- T-001, T-002, T-008, T-009 and contract tests (they all mutate and restore ConfigLoader fields)
