# Task: Town Interface World Generation & Village Spawning

**Note**: See `tasks/ROADMAP_v1.md` for the overall v1.0 phased plan and priorities. This task is one important piece of Phase 1 (Onboarding & Accessibility).

**Goal**: Add configurable world generation options so Town Interface blocks can naturally appear in the world, improving accessibility and world integration without breaking the transport economy fantasy.

## Desired Modes (user specified)
Players should be able to combine generation styles. The main options are:

1. **No Generation** — World spawning completely disabled.
2. **Villager Villages** — Spawn Town Interfaces near or integrated with vanilla Minecraft villager villages.
3. **Random World Spawning** — Spawn at random valid locations across the world, respecting town boundary rules, with a rarity/frequency setting.
4. **Both** — Villages + Random spawning enabled together (user-requested combination).

## Proposed Config Design
Better UX than a single enum (supports combinations):

- `enableTownInterfaceWorldgen` (boolean) — Master switch. Default: `false`
  - When false → no world generation at all (regardless of other settings).
- `townInterfaceWorldgenVillages` (boolean) — Enable spawning near vanilla villages.
- `townInterfaceWorldgenRandom` (boolean) — Enable random world spawning.

When `townInterfaceWorldgenRandom` is enabled, additional settings:
- `townInterfaceRandomSpawnRarity` (or frequency/chance per chunk or region)
- Reuse or extend existing boundary distance logic

Future options could include:
- Per-biome weights
- Dimension blacklists/whitelists
- Separate rarity for villages vs random

This design gives players full flexibility (off, villages only, random only, or both).

## Current State
- Zero world generation or structure code exists for Town Interfaces.
- The block can only be obtained via creative or (when enabled) crafting.
- Strong existing boundary/distance validation system in `TownBoundaryService` + `TownManager`.

## Plan

### Exploration Phase
- [x] Research Minecraft 1.20.1 structure generation systems (StructureSets, Jigsaw structures, StructureTemplates, ChunkGenerator hooks)
- [x] Study how vanilla villages are generated and how mods commonly inject structures into or near them
- [x] Investigate best practices for "rare random structure" spawning that respects modded distance rules
- [ ] Review performance implications and existing worldgen patterns in the mod (if any)
- [ ] Explore compatibility concerns (Terralith, Biomes O' Plenty, Create, other structure mods, etc.)

### Exploration Phase - Findings (as of 2026-05-25)

**exp-1: 1.20.1 Structure System**
- Modern datapack system (stable since 1.19): Structures defined in `data/modid/worldgen/structure/` + `worldgen/structure_set/`.
- **StructureSet** controls rarity and placement via `random_spread` (spacing + separation in chunks). This is the primary way to make rare structures.
- Strong support for `exclusion_zone` to prevent spawning near other structure sets — very promising for enforcing our town boundary rules.
- Jigsaw structures + Template Pools are the standard for multi-piece buildings.
- Custom logic (e.g., calling our `TownBoundaryService` at generation time) would require a custom `StructurePlacement` class extending the vanilla system.

**exp-2: Vanilla Village Integration**
- Common and well-documented pattern: Use `DatapackBuiltinEntriesProvider` (Forge) or equivalent registry injection (Fabric) to add custom entries to vanilla village `StructureTemplatePool`s (e.g., `minecraft:village/plains/houses`).
- TelepathicGrunt's StructureTutorialMod is repeatedly cited as the best reference implementation.
- Alternative approaches:
  - Add a small standalone structure near villages using exclusion zones around vanilla village structure sets.
  - Full custom village replacement (higher complexity, more compatibility risk).
- Important detail: Custom pieces added to village pools need proper jigsaw connections and height offsets to fit correctly.

**Implications for our design so far**:
- "Villages" mode is very achievable.
- "Random" mode can leverage `random_spread` + `exclusion_zone` for basic boundary respect.
- Supporting "both" at the same time looks feasible without major conflicts.
- We will likely need a small amount of platform-specific code for pool injection on Forge vs Fabric.

**exp-3: Rare Random Spawning + Distance Rules**
- Community pattern for rarity: Use `random_spread` placement in StructureSet JSON with `spacing` (average distance between attempts) and `separation` (minimum distance between two instances of the same structure).
- Tools like **Structurify** and **Structure Control** are popular in modpacks specifically to tweak structure spacing — this validates that the vanilla system is the expected way to control rarity.
- For *our specific need* (respecting dynamic boundaries of already-placed player towns via `TownBoundaryService`), the standard JSON `exclusion_zone` is insufficient because it is static and defined at datapack load time.
- Most advanced mods that need custom distance logic between structures end up implementing a **custom `StructurePlacement`** class. This allows calling into existing game state (in our case, `TownManager` / `TownBoundaryService`) during generation.
- Trade-off: Custom placement gives us full power but adds complexity and some platform differences.

**Updated design implication**: For full "respect player town boundaries" behavior in Random mode, we will probably need at least one custom `StructurePlacement` class. Pure datapack approach will give us good rarity control but weaker dynamic boundary enforcement.

**exp-4: Performance Implications**
- Chunk generation is one of the heaviest operations in Minecraft. Heavy logic inside `findGenerationPoint()` or during Jigsaw assembly can cause noticeable lag spikes or generation stutters.
- There is a dedicated optimization mod: **Structure Layout Optimizer** (by TelepathicGrunt) specifically for making Jigsaw structures generate faster without changing their appearance.
- Best practices from community:
  - Keep placement checks as lightweight as possible.
  - Avoid loading large amounts of NBT, querying complex systems, or doing heavy calculations during the generation phase.
  - Use static exclusion zones in JSON where possible instead of runtime checks.
  - For our case: Any custom `StructurePlacement` that calls `TownManager.get(level).canPlaceTownAt(...)` on every attempt needs to be extremely fast and ideally cached.
- Pre-generating the world with tools like Chunky is a common recommendation for large modded worlds to avoid player-experienced generation lag.
- Our existing `TownBoundaryService` and `TownManager` are already reasonably optimized, but repeated calls during worldgen could still add up if the rarity is not tuned conservatively.

**exp-5: Compatibility Concerns**
- Large worldgen mods (Terralith, Biomes O' Plenty, Oh The Biomes You'll Go, etc.) heavily modify biome distribution and often include their own structures with custom spacing rules.
- Common issues reported:
  - Structures from one mod failing to spawn or spawning in incorrect biomes when combined with others.
  - Overlapping or conflicting spacing rules leading to either too many or too few structures.
- Popular solution in modpacks: **Structurify** — allows per-structure control of spacing, separation, biome restrictions, and even disabling structures entirely. Highly recommended for any mod adding structures.
- Create mod: Has some structure-related addons (e.g. Create: Structures). Chunk loading/contraption interactions with newly generated structures can occasionally cause issues, but generally manageable.
- General takeaway: Our system should be designed to be "polite" — use conservative spacing by default and provide config options for players to adjust rarity when using heavy worldgen mods.
- No major red flags, but we should expect that players using Terralith + our mod will likely want to tune the random spawn rarity significantly.

**Exploration Phase Complete** (2026-05-25)
All five exploration items finished. Key takeaways have been recorded above. We now have a good understanding of:
- How to implement the three generation modes.
- The need for a custom StructurePlacement for proper dynamic boundary respect.
- Performance risks and mitigation strategies.
- Expected compatibility friction with popular worldgen mods and how to handle it gracefully via config.

### Design Phase
- [ ] Finalize config design (master enable + independent toggles for villages/random)
- [ ] Define exact behavior for "villages" mode (inside village bounds? nearby? replaces a house? adds a platform?)
- [ ] Define rarity model for "random" mode (chunks, regions, per-biome weight, etc.)
- [ ] Decide how generated towns should handle initial population, naming, and default platform
- [ ] Document edge cases (world border, superflat, void worlds, modded dimensions)

### Implementation Phase
- [ ] Add new config options to `ConfigLoader` + default TOML + run configs (master enable + villages + random toggles + rarity)
- [ ] Create structure registration infrastructure (if using structures)
- [ ] Implement generation logic gated by the master `enableTownInterfaceWorldgen` setting
- [ ] Implement "Villages" mode integration
- [ ] Implement "Random" spawning with boundary respect
- [ ] Ensure generated towns still go through proper registration, boundary checks, and platform creation
- [ ] Add debug logging / visualization for generated town locations
- [ ] Write clear in-game tooltips or config comments explaining the modes and combinations

### Testing Phase
- [ ] Test all combinations (off, villages only, random only, both)
- [ ] Verify boundary rules are never violated by worldgen
- [ ] Test with various world types (default, amplified, superflat, large biomes)
- [ ] Test mod compatibility (at least one major worldgen mod)
- [ ] Verify on both Forge and Fabric
- [ ] Performance testing in newly generated chunks

## Notes
- Village / worldgen spawning was previously marked as deferred during the Town Interface Acquisition task.
- This feature directly supports the "Build Towns" part of the core player loop from ARCHITECTURE.md.
- Quality bar should match the rest of the mod (good config UX, debuggability via F3+K, clean separation of concerns).

**Status**: Previous acquisition task completed. New task: World Generation & Village Spawning — currently in planning/exploration phase. User requested initial plan.