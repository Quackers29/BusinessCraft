# Tourist Behavior Enhancement

Add social behaviors using vanilla Minecraft animations/sounds only.

---

## Phase 1: Core Behaviors

### 1.1: Gossiping Between Tourists
**NEW FILE**: `TouristGossipGoal.java` (extends `Goal`)
- Priority 4 (after RandomLookAroundGoal)
- Every 40-80 ticks: search 4-block radius for another TouristEntity
- When found: `getLookControl().setLookAt(otherTourist)`
- Play `SoundEvents.VILLAGER_AMBIENT` (15% chance per tick when looking)
- Track partner for 80-150 ticks, then find new one
- Add field to TouristEntity: `@Nullable TouristEntity gossipPartner`

### 1.2: Window Gazing When Riding
**NEW FILE**: `TouristGazeGoal.java` (extends `Goal`)
- Priority 5 (only when NOT gossiping)
- When `isPassenger() == true`: look perpendicular to movement direction
- Calculate vehicle velocity, look 90° left/right
- Switch sides every 60-100 ticks
- Use `getLookControl().setLookAt(x, y, z)` for target position

### 1.3: UI Interaction Sounds
**MODIFY**: `TouristEntity.mobInteract()` + `stopTrading()`
- On UI open: `playSound(SoundEvents.VILLAGER_YES, 1.0f, 1.0f)`
- On UI close: `playSound(SoundEvents.VILLAGER_NO, 1.0f, 1.0f)`

---

## Phase 2: Speed Reactions

### 2.1: Celebrate When Fast
**MODIFY**: `TouristEntity.tick()` (lines 213-239, position update block)
- Calculate speed: `distanceMoved / (40 ticks / 20) = blocks/sec`
- If speed > 5 blocks/s:
  - `playSound(SoundEvents.VILLAGER_CELEBRATE, 1.0f, 1.0f)` (20% chance)
  - `swing(InteractionHand.MAIN_HAND)` - arm wave
- Cooldown: 100 ticks between reactions
- Add field: `int lastSpeedReactionTick`

---

## Phase 3: Enhanced Player Interaction

### 3.1: Closer Player Detection
**MODIFY**: `TouristEntity.registerGoals()`
- Change `LookAtPlayerGoal(this, Player.class, 5.0F)` → `3.0F` (closer range)
- Add occasional sound: in tick(), if looking at player, 5% chance play `VILLAGER_YES`

**Alternative** (if simple change insufficient):
**NEW FILE**: `TouristLookAtPlayerGoal.java` - custom version with sound triggers

---

## Phase 4: NBT Persistence

**MODIFY**: `TouristEntity.addAdditionalSaveData()` / `readAdditionalSaveData()`
- Save/load: `lastSpeedReactionTick` (int)
- Gossip partner not persisted (re-establish on load)

---

## Testing Checklist
- [ ] Spawn 2+ tourists - verify face each other, play ambient sounds
- [ ] Tourist in minecart - verify looks left/right periodically
- [ ] Boost minecart with powered rails - verify celebration + arm wave
- [ ] Right-click tourist - verify greeting sound on UI open
- [ ] Close UI - verify farewell sound
- [ ] Player near tourist - verify looks at player more often at close range
- [ ] Save/reload - verify behaviors persist

---

## Implementation Order
1. UI sounds (1.3) - 5 lines, immediate feedback
2. Speed reactions (2.1) - 15 lines, uses existing position tracking
3. Gossiping (1.1) - 40 lines, new goal class
4. Window gazing (1.2) - 35 lines, new goal class
5. Enhanced player look (3.1) - 10 lines or new class
6. NBT persistence (4) - 10 lines

**Total: ~115-150 lines new code**

---

## Files Changed
**New (2-3 files)**:
- `common/.../entity/ai/goal/TouristGossipGoal.java`
- `common/.../entity/ai/goal/TouristGazeGoal.java`
- `common/.../entity/ai/goal/TouristLookAtPlayerGoal.java` (optional)

**Modified (1 file)**:
- `TouristEntity.java` - fields, registerGoals(), tick(), mobInteract(), stopTrading(), NBT

---

## Vanilla Animations Used
- ✅ `swing(InteractionHand.MAIN_HAND)` - arm wave
- ✅ `getLookControl().setLookAt()` - head rotation
- ✅ `playSound(SoundEvents.VILLAGER_*)` - all sounds
- ❌ Head tilt/nod - NOT available in vanilla (requires custom model)

---

## Risks
- **Low**: UI sounds, speed reactions, player look changes
- **Medium**: Gossip entity search (test with 10+ tourists for performance)
- **Medium**: Window gaze velocity calculation (null checks for Create trains)
