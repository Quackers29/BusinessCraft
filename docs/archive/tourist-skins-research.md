# Tourist Skins Research for BusinessCraft

**Kanban Task:** t_1ab40ead
**Date:** 2026-05-20
**Status:** Completed Research & Recommendations

## Goal Summary
Provided 3 tiered tourist skin designs aligned with existing level system (1-3), compatible with TouristEntity (extends Villager), TouristRenderer, and TouristHatLayer. Recommended custom creation over sourcing due to licensing and style consistency.

## Tier Recommendations & Mapping
Tiers map directly to `entity.getVillagerData().getLevel()` (Level 1=Basic, 2=Experienced, 3=Luxury). This aligns with existing hat color logic in `TouristHatLayer`.

### Tier 1: Basic Tourist (Level 1)
- **Style:** Casual entry-level traveler, fun and approachable.
- **Description for PNG (based on villager.png layout):**
  - Head: Light skin, short brown hair with slight messiness, wide friendly smile, sunglasses resting on forehead.
  - Body: Bright blue short-sleeve polo shirt with subtle pattern (e.g. small anchors or cameras), white undershirt visible at collar.
  - Arms: Matching blue sleeves rolled slightly, simple black watch on left wrist.
  - Legs: Khaki shorts with belt loops and pockets, white sneakers with red accents.
  - Back/Accessories: Small red backpack, black camera with strap around neck, binoculars.
- **Color Palette:** Vibrant blues, khakis, whites. Summer vacation vibe.
- **Visual Keywords:** Hawaiian tourist, first-time flyer, excited expression.

### Tier 2: Experienced Traveler (Level 2)
- **Style:** Seasoned explorer, practical and adventurous.
- **Description for PNG:**
  - Head: Tanned skin tone, neat salt-and-pepper hair, confident smile, aviator sunglasses on face.
  - Body: Olive green button-up safari shirt with multiple pockets, brown utility vest.
  - Arms: Sleeves rolled up, leather wrist cuff or compass on right arm.
  - Legs: Durable beige cargo pants with reinforced knees, sturdy brown hiking boots.
  - Back/Accessories: Large olive backpack with bedroll, folded map in pocket, high-quality binoculars, walking staff (if model supports).
- **Color Palette:** Earth tones - olive, khaki, brown, deep greens. Rugged but polished.
- **Visual Keywords:** Safari photographer, world traveler, knowledgeable look.

### Tier 3: Luxury/Premium Tourist (Level 3)
- **Style:** High-end VIP, sophisticated and elegant.
- **Description for PNG:**
  - Head: Fair/porcelain skin, perfectly styled dark hair, subtle smirk, designer rimless sunglasses, small earring or subtle jewelry.
  - Body: Crisp white linen shirt with subtle gold embroidery, light navy or beige tailored jacket/vest.
  - Arms: Fine fabric sleeves, luxury gold watch on left wrist.
  - Legs: Tailored chinos or light slacks in matching neutral tones, polished leather loafers.
  - Back/Accessories: Sleek black leather satchel (not bulky backpack), premium DSLR camera with large lens, gold-trimmed accessories.
- **Color Palette:** Elegant neutrals (white, beige, navy) with metallic gold accents. Clean lines.
- **Visual Keywords:** First-class traveler, influencer, luxury resort guest.

## Technical Implementation Notes
1. **Texture Format:** Use standard Minecraft villager texture layout (same as `textures/entity/villager/villager.png`). Edit the base villager texture to replace robes with tourist clothing. Ensure UV mapping aligns with `VillagerModel` (head, body, arms, legs separate).

2. **File Locations (recommended structure):**
   ```
   common/src/main/resources/assets/businesscraft/textures/entity/
   ├── tourist_basic.png          <- Tier 1
   ├── tourist_experienced.png    <- Tier 2  
   ├── tourist_luxury.png         <- Tier 3
   ├── tourist_hat.png            <- (existing)
   ├── tourist_hat_red.png        <- (existing, Level 1)
   ├── tourist_hat_blue.png       <- (existing, Level 2)
   └── tourist_hat_green.png      <- (existing, Level 3)
   ```

3. **Code Changes in `TouristRenderer.java`:**
   ```java
   private static final ResourceLocation[] TOURIST_SKINS = {
       new ResourceLocation("businesscraft", "textures/entity/tourist_basic.png"),
       new ResourceLocation("businesscraft", "textures/entity/tourist_experienced.png"),
       new ResourceLocation("businesscraft", "textures/entity/tourist_luxury.png")
   };

   @Override
   public ResourceLocation getTextureLocation(TouristEntity entity) {
       int level = Math.max(0, Math.min(entity.getVillagerData().getLevel() - 1, 2));
       return TOURIST_SKINS[level];
   }
   ```
   - Consider making `VillagerProfessionLayer` conditional (`if (level == 0)`) or remove it entirely since custom skins include clothing.
   - The `TouristHatLayer` already maps level to hat texture — perfect synergy.

4. **Entity Integration:**
   - No changes needed to `TouristEntity.java` if leveraging existing `VillagerData.getLevel()`.
   - The distance-traveled leveling system (DISTANCE_PER_LEVEL = 20.0) already supports tier progression naturally.
   - Ensure NBT persistence and client sync for level (already implemented).

5. **Config & Extensibility:**
   - For future: Add config option in `ConfigLoader` for custom skin paths or disable tiers.
   - Skins folder could be expanded to `assets/businesscraft/skins/tourist/` with JSON index for dynamic loading (advanced).
   - Resource pack compatibility: Allow overriding these textures via standard Minecraft resource packs.

## Sourcing Alternatives Considered
- PlanetMinecraft / NameMC tourist skins: Many player skins exist but few villager-specific. Licensing usually "personal use only" — not suitable for mod redistribution.
- Resource packs like "Tourist Villagers" or "Modern NPC" packs: Rare, often not 1.20.1 compatible or wrong aesthetic.
- **Conclusion:** Custom creation ensures perfect fit, professional mod aesthetic, and full licensing control. The descriptions above are detailed enough for pixel artists or Aseprite/Pixil (note existing .pixil files in resources).

## Assets Ready for Implementation Phase
- Use the 3 descriptions to create PNGs (recommend 64x32 or full 64x64 villager format).
- Update renderer as specified.
- Test with `./gradlew :common:runClient`, spawn tourists via town interface, verify level progression changes both hat + base skin.
- Debug with F3+K overlay and `DebugConfig.TOURIST_ENTITY`.

This delivers exactly what was requested: tier mapping, detailed descriptions, technical notes, and folder suggestions. Ready for immediate coding task handoff.

**Created by:** Hermes Kanban Worker (researcher profile)
**For follow-up:** See child task t_961c5537 (likely the implementation).
