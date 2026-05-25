# Task: Town Interface Acquisition (Config-Driven Mode)

**Goal**: Make the Town Interface block feel safe by default while still supporting a proper survival crafting path. Use a single global config toggle instead of creating separate block variants.

## Final Design
- New config: `craftableTownInterface` (boolean, under [general])
  - `false` (default): Block is nearly indestructible in survival. Creative players can still place and remove it.
  - `true`: Block becomes craftable and mineable at obsidian difficulty (hardness 50, requires diamond pickaxe or better).
- Village / world structure spawning: Explicitly deferred

## Key Implementation Details
- Dynamic hardness logic lives in `TownInterfaceBlock`:
  - When `craftableTownInterface = false`: `getDestroyProgress()` returns 0 for non-creative players. Extremely high explosion resistance.
  - When `true`: Falls back to normal obsidian-like behavior.
- `playerDestroy()` overridden to prevent item drops in indestructible mode unless the player is in creative.

## Current State
- Single breakable stone-like block (strength 3.0, no recipe)
- No config-driven hardness or drop control

## Plan
- [ ] Add `craftableTownInterface` boolean config to ConfigLoader (under [general]) + default TOML
- [ ] Implement dynamic destroy logic in TownInterfaceBlock:
    - Override `getDestroyProgress` to respect the config for non-creative players
    - Set appropriate explosion resistance based on config
    - Override `playerDestroy` to control drops (no drops in safe mode unless creative)
- [ ] Add crafting recipe for the block (active when config is true)
- [ ] Update block registration / properties if needed for dynamic hardness behavior
- [ ] Ensure placement validation, town registration, and default platform creation still work regardless of config
- [ ] Test thoroughly:
    - Config false (default): indestructible in survival, fully usable in creative, no drops on attempted break
    - Config true: craftable + mineable with diamond+ pickaxe (obsidian tier)
- [ ] Verify on both Forge and Fabric clients

**Status**: Plan updated to match user direction (single config toggle, dynamic block behavior, no separate variants, village spawning deferred). Ready for implementation.