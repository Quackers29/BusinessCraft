# Current Task: Implement tiered tourist skins (manual creation approach)

**Goal**: Add support for 3 distinct full-body tourist skins (Basic / Experienced / Luxury) that map to villager level. Skins are chosen once at spawn (and updated on level-up) rather than checked repeatedly.

## Current State (verified)
- TouristRenderer currently always returns vanilla villager.png
- TouristHatLayer already works well: uses level to choose red/blue/green hat overlays at render time
- Existing placeholder files: tourist_basic.png + colored hat textures
- No skin selection system currently present

## Refined Design (per user feedback)
- Skin tier is determined once when a tourist is spawned, based on its starting level.
- If the tourist levels up later, the skin updates at that moment.
- No repeated "check every tick or every render" logic.
- Implementation: Add a synced EntityDataAccessor<Integer> on TouristEntity to store the current skin tier.
- TouristRenderer simply reads this stored value — very cheap.
- Mapping: level 1 → basic, level 2 → experienced, level 3+ → luxury
- Skins are placed in mod resources (user will create the actual PNGs):
  - textures/entity/tourist_basic.png
  - textures/entity/tourist_experienced.png
  - textures/entity/tourist_luxury.png
- Hat layer continues to work cleanly on top.

## Plan
- [ ] Review current TouristEntity (especially synced data and spawning logic)
- [ ] Add new EntityDataAccessor for skin tier in TouristEntity
- [ ] Set skin tier once during tourist creation/spawning based on initial level
- [ ] Hook into level-up so skin updates when the tourist gains a level
- [ ] Update TouristRenderer.getTextureLocation() to read the stored skin tier
- [ ] Ensure TouristHatLayer still renders correctly on top of the new base skins
- [ ] Verify texture paths and add any necessary resource registration
- [ ] Build and launch client for testing
- [ ] Spawn tourists at different levels and confirm correct skins + hats
- [ ] Test level-up scenario (make sure skin changes when tourist levels)
- [ ] Check in with user for plan approval before coding
- [ ] After approval: implement changes, test thoroughly, mark items done
- [ ] Move task to done.md when complete

**Status**: Updated plan written. Awaiting user verification before starting implementation.