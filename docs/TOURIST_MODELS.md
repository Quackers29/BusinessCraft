# Tourist models

This is how a tourist's costume is built, and how to add another one. The game does not sculpt a new body per look. A look is four numbers: a skin, an outfit, a hat, and an accessory. Those numbers pick textures, and (for hats and accessories) a mesh that already exists in code.

There is no generator script in the repo, and no separate "face mask" image. The faces are pixels inside the skin PNGs. `docs/reference/tourist_skins/*.pixil` is old reference art. Nothing in the game loads those files.

## What a look is

A look is one JSON file:

`common/src/main/resources/data/businesscraft/tourist_looks/<id>.json`

`safari_sightseer.json` is the whole format:

```json
{ "id": "safari_sightseer", "order": 0, "type": "SIGHTSEER", "skin": 1, "outfit": 0, "headwear": 1, "accessory": 1 }
```

| Field | What it does |
| --- | --- |
| `id` | Lowercase letters, digits, and underscores only (`[a-z0-9_]+`). This is the save id and the trade-list key. The screen title is this id with underscores turned into spaces and each word capitalised, so `alpine_walker` is shown as "Alpine Walker". There is no lang entry for a look. |
| `order` | Sort order in the catalog. Lower comes first. If you leave it out, the parser uses 1000. Ties break by `id`. |
| `type` | One of `SIGHTSEER`, `SHOPPER`, `SCHOLAR`, `PILGRIM`, `ADVENTURER` (`TouristType`). This is the flavour label on the screen ("Pilgrim · Desert Guide") and it picks the shared trade list when this look has no list of its own. It does not change the model. Leave it out and a platform spawn keeps the random type rolled just before the look is chosen. The synched default, before that roll, is Sightseer. Every builtin look sets it. |
| `skin` | Index of `skin_N.png`. The head, hair, and any skin that shows past the clothes. |
| `outfit` | Index of `outfit_N.png`. The jacket, sleeves, and trouser overlay. |
| `headwear` | Which hat mesh to show, and `headwear_N.png`. `0` means no hat. |
| `accessory` | Which held or worn item mesh to show, and `accessory_N.png`. `0` means nothing. |

The parser is a small pattern match in `TouristLook.parse`, not a general JSON library. Keep the file flat: one string or number per key. Do not nest objects.

The four numbers are packed into one synced int by `TouristAppearance.pack`:

```text
skin | (outfit << 8) | (headwear << 16) | (accessory << 24)
```

Each number must be 0 through 254. 255 is rejected so it cannot collide with "no look yet" (`TouristAppearance.UNSET`, which is -1). The client draws from this int. The id is saved next to it as `TouristLookId` but is not synced on its own.

## Where the files live

Builtin list, one id per line. Blank lines and `#` comments are ignored:

`common/src/main/resources/data/businesscraft/tourist_looks/index.txt`

The classpath loader (`TouristLooks.loadFromClasspath`) reads that list and then each `data/businesscraft/tourist_looks/<id>.json`. Tests use this path. A dedicated server replaces that list on data reload.

Reload listener: `TouristLookReloadListener`. Forge registers it in `BusinessCraftForge`. Fabric registers `FabricTouristLookReloadListener`. It loads every `data/<namespace>/tourist_looks/<id>.json` the resource manager can see, including the mod's own files. A datapack can add a look without editing `index.txt`. An empty reload keeps the builtin catalog. A duplicate id logs a warning and the later copy wins.

Textures, all 64×64 PNG:

`common/src/main/resources/assets/businesscraft/textures/entity/tourist/`

| File | Used when |
| --- | --- |
| `skin_N.png` | Always. `TouristTextures.skin` |
| `outfit_N.png` | Always. `TouristTextures.outfit` |
| `headwear_N.png` | Hat index N, and only if N is a known hat. There is no `headwear_0.png`. |
| `accessory_N.png` | Accessory index N, and only if N is a known accessory. There is no `accessory_0.png`. |

The path the game asks for is `businesscraft:textures/entity/tourist/<name>.png`. A missing file is the usual purple-and-black missing texture.

Code that turns the numbers into a person:

| Class | Job |
| --- | --- |
| `TouristParts` | The legal hat and accessory indices, and their names. |
| `TouristModel` | The body, every hat mesh, every accessory mesh, and which one is visible. |
| `TouristRenderer` | Draws the skin, then the three layers. Also draws the first name. |
| `TouristOutfitLayer`, `TouristHeadwearLayer`, `TouristAccessoryLayer` | One texture each. |
| `CommonClientSetup` and `FabricClientSetup` | Register the model layer `businesscraft:tourist`. |

## How the model is assembled

`TouristModel` starts from Minecraft's wide-arm player mesh (`PlayerModel.createMesh` with slim arms off). The body is the vanilla player: head, torso, two arms, two legs. You do not add a new body for a new look.

Drawing happens in four passes:

1. **Skin.** The base head, body, arms, and legs, plus the hair. The clothing overlay (jacket, sleeves, trouser layer) is hidden for this pass, so the skin texture is not also used as a jacket.
2. **Outfit.** `TouristOutfitLayer` draws only that clothing overlay, with `outfit_N.png`.
3. **Hat.** One child of the `headwear` part, copied onto the head every frame so it turns with the head.
4. **Accessory.** One mesh, copied onto the body or an arm (see the table below).

Hair is the vanilla second head layer (the player "hat" part). `TouristModel.prepare` shows it only for these headwear indices:

| Index | Hat | Hair visible? |
| --- | --- | --- |
| 0 | none | yes |
| 1 | pith helmet | no |
| 2 | cap | no |
| 3 | beret | no |
| 4 | sun hat | no |
| 5 | fez | yes |
| 6 | headband | yes |
| 7 | top hat | no |
| 8 | hood | no |
| 9 | boater | no |
| 10 | flower crown | yes |
| 11 | ushanka | no |
| 12 | beanie | no |
| 13 | navy cap (same mesh as 2) | no |
| 14 | dust cap (same mesh as 2) | no |
| 15 | plum fez (same mesh as 5) | yes |

Indices 13, 14, and 15 are recolours: `addCap` / `addFez` build the same boxes under a new name, and the new name has its own texture. Accessories 9, 10, and 11 are the same idea (red backpack, brown suitcase, brass camera).

### Skin and outfit UV

Skin and outfit use the vanilla 64×64 player layout. Paint them in any skin editor that understands that layout. The parts this mod actually shows:

**On `skin_N.png` (base layer, plus the hair overlay):**

| Region | Texture rectangle (x, y, width, height) |
| --- | --- |
| Head top | 8, 0, 8×8 |
| Head front (the face) | 8, 8, 8×8 |
| Head right / left / back | 0, 8, 8×8 and 16, 8, 8×8 and 24, 8, 8×8 |
| Head bottom | 16, 0, 8×8 |
| Hair top | 40, 0, 8×8 |
| Hair front | 40, 8, 8×8 |
| Body, arms, legs | the usual base-layer boxes (torso front is 20, 20, 8×12) |

**On `outfit_N.png` (clothing overlay only):**

| Region | Texture rectangle |
| --- | --- |
| Jacket front | 20, 36, 8×12 |
| Jacket and the rest of the torso overlay | around 16, 32 |
| Right sleeve | around 40, 32 |
| Left sleeve | around 48, 48 |
| Trouser overlay | right leg around 0, 32, left leg around 0, 48 |

`outfit_0.png` paints the jacket front solid and only part of each sleeve. Transparent overlay pixels let the skin show through. That is why a short sleeve still needs a sensible skin colour on the arm.

### Hat and accessory UV

Hats and accessories do **not** use the player layout. Each box in `TouristModel.createBodyLayer` has its own `texOffs(u, v)`, and the texture is still 64×64 (`LayerDefinition.create(mesh, 64, 64)`).

Minecraft unwraps a box of width `w`, height `h`, depth `d` starting at `(u, v)` like this:

| Face | Origin | Size |
| --- | --- | --- |
| Bottom | u+d, v | w × d |
| Top | u+d+w, v | w × d |
| West | u, v+d | d × h |
| North (the side you usually see from the front) | u+d, v+d | w × h |
| East | u+d+w, v+d | d × h |
| South | u+d+w+d, v+d | w × h |

Example: the pith crown is `texOffs(0, 0).addBox(-5, -14, -5, 10, 5, 10)`. Its front face is a 10×5 rectangle at texture (10, 10). The brim is a second box at `texOffs(0, 20)`. Paint only those rectangles. The rest of `headwear_1.png` is unused.

Box positions are in Minecraft model pixels. The head is 8×8×8 and sits with its top near local y = -8. A hat crown above the head uses a negative y (the pith crown starts at y = -14). An accessory on the body uses positive y (the camera body starts around y = 3).

### Which part an accessory follows

`TouristModel.setupAnim` copies the pose each frame:

| Follows | Indices | What they are |
| --- | --- | --- |
| Body | 1 camera, 3 backpack, 9 red backpack, 11 brass camera | Worn on the torso |
| Left arm | 2 suitcase, 5 umbrella, 7 basket, 10 brown suitcase | Held on the left |
| Right arm | 4 map, 6 book, 8 lantern | Held on the right |

A new accessory that is not added to one of those three lists stays at the model root and will not move with the arm or the body.

## How faces work

The face is the 8×8 head front at texture (8, 8) on `skin_N.png`. There is no second face texture and no code that composites face parts. Every builtin skin uses the same pixel positions and only changes colours, or switches to one of the variants below.

Rows are counted from the top of that 8×8. Columns are left to right, 0 through 7.

```text
row 0  skin skin skin skin skin skin skin skin
row 1  skin skin skin skin skin skin skin skin
row 2  skin BROW BROW skin skin BROW BROW skin
row 3  skin WHITE iris skin skin iris WHITE skin
row 4  skin skin ···· skin skin ···· skin skin
row 5  skin skin skin MOUTH MOUTH skin skin skin
row 6  chin chin chin chin chin chin chin chin
row 7  chin chin chin chin chin chin chin chin
```

- Eye white is `f8f8fc` on every face that has whites.
- The two mouth pixels are a muted red, not a bright cartoon mouth.
- Brows are a dark brown, a bit darker than the hair.
- Rows 6–7 are a darker skin shade (the chin). The body uses more flat shades of the same skin. Recolour those areas as solid colours. Do not draw a new eye shape.

Variants, and which skin file has each one:

| Skin file | Variant | What changes |
| --- | --- | --- |
| `skin_0.png` | Blush smile | Row 4, columns 2 and 5, are the same red as the mouth. Used by the market shopper. |
| `skin_1.png` | Stubble | Row 6, columns 2–5, are a darker brown. Safari sightseer. |
| `skin_2.png` | Glasses | No eye whites. Row 2 is a dark bar (skin only on the two outer pixels). Row 3 is dark, iris, dark, dark, dark, iris, dark, skin. Row 4 repeats the dark bar. Station scholar. |
| `skin_3.png` | Beard | Row 5 is hair except the two mouth pixels. Rows 6–7 are all hair. Trail hiker. |
| `skin_4.png` | Blush, deep skin | Same blush positions as skin 0, darker skin. One gold earring pixel on the left side of the head. Coast visitor. |
| `skin_5.png` | Plain, light streak | No blush. The hair overlay's front, rows 0–2, columns 6–7, are a light streak instead of the hair colour. Night luxury. |
| `skin_6.png` | Moustache | Row 4, columns 2–5, are the hair colour. Desert guide. |
| `skin_7.png` | Plain | No blush, no facial hair. One gold earring pixel, same place as skin 4. Festival guest. |

Skins 8–15 are the same masks with new colours. They were made by copying one of the files above and replacing each flat colour. Nothing in the repo rebuilds them:

| New skin | Copied from |
| --- | --- |
| 8 alpine | 7 plain |
| 9 rain | 1 stubble |
| 10 garden | 0 blush |
| 11 library | 2 glasses |
| 12 harbour | 6 moustache |
| 13 bazaar | 3 beard |
| 14 canyon | 4 blush |
| 15 conductor | 5 plain, including the light streak |

Hair overlay, on every skin, at (40, 8):

```text
row 0  hair hair hair hair hair hair hair hair
row 1  hair hair hair hair hair hair hair hair
row 2  hair hair hair hair hair hair hair hair
row 3  hair ···· ···· ···· ···· ···· ···· hair
row 4–7 empty (transparent)
```

The head top (8, 0) and the hair top (40, 0) are solid hair. The head sides are hair on the top three rows and skin below. The back of the head is a darker hair colour. Do not paint bangs down over the eyes. A wide hat hides this overlay anyway (see the table above), so a moustache, beard, blush, stubble, or glasses has to live on the base face if that hat covers the hair.

To make a new face, copy the skin whose mask you want, change the skin colours and the hair colours, and leave the feature pixels where they are. You can reuse an iris colour the originals already use (blue, brown, or green). Keep the eye whites at `f8f8fc`.

## How to add a look that reuses a hat

This example is not in the game. It reuses the sun hat (4) and the basket (7), and a new outfit index.

1. Copy a skin whose face you want. For a blush smile, copy `skin_0.png` to `skin_16.png` and recolour it. Do not edit `skin_0.png` if you still want the market shopper unchanged.
2. Copy an outfit, for example `outfit_0.png` to `outfit_16.png`, and recolour the jacket, sleeves, and trousers.
3. You do not need a new `headwear_4.png` or `accessory_7.png`. Those already exist. You also do not need a Java change, because 4 and 7 are already in `TouristParts` and `TouristModel`.
4. Add `common/src/main/resources/data/businesscraft/tourist_looks/pond_skater.json`:

```json
{ "id": "pond_skater", "order": 16, "type": "SIGHTSEER", "skin": 16, "outfit": 16, "headwear": 4, "accessory": 7 }
```

5. Add a line `pond_skater` to `index.txt`. The reload listener would find the JSON without that line, but the classpath loader and `TouristLooksTest` only see ids listed there.
6. No lang file. The screen will say "Pond Skater". With `type` set to `SIGHTSEER` and no private trade list, the tourist sells the shared sightseer deals (`TouristTradeCatalog.forLook`). A private list is a `lists.put("pond_skater", ...)` entry in `lookLists()`, which is Java, not JSON.

Preview every loaded look, front and side, plus a lineup:

```bash
bash tools/showcase.sh tourists forge
```

Screenshots land in `showcase-output/<timestamp>_tourists_forge/screenshots/`. The new look is `showcase_tourist_pond_skater_front.png` and `showcase_tourist_pond_skater_side.png`. The lineup shots are `showcase_tourists_lineup_front.png` and `showcase_tourists_lineup_side.png`. The harness loads `TouristLooks.all()` and places one tourist per look, so there is no switch for a single id. `bash tools/showcase.sh tourists fabric` is the same scene on Fabric.

`./gradlew test` runs `TouristLooksTest`. A builtin look must have a unique id, a unique packed int, a known hat index, a known accessory index, and a `type`. Adding `pond_skater` as above satisfies that. Changing an existing look's numbers fails the test that pins the original sixteen.

## How to add a new hat or accessory shape

Recolouring an existing mesh stops at the steps above. A new silhouette needs code, because the mesh is not in the JSON.

1. Add a constant at the end of `TouristParts` and move `HEADWEAR_MAX` or `ACCESSORY_MAX` to it. Indices have to stay contiguous. The arrays in `TouristModel` are indexed by these numbers, with an empty name at index 0.
2. Add the part name to `HAT_NAMES` or `ACCESSORY_NAMES` at that same index.
3. Build it in `createBodyLayer` with `addBox` and `texOffs`. Copy `addCap` if you only wanted another cap colour: that is how `cap_navy` and `cap_dust` exist.
4. For a hat, decide whether the hair overlay should stay visible. If it should, add the new index next to the fez, headband, and flower checks in `prepare`.
5. For an accessory, add the index to `BODY_ACCESSORIES`, `LEFT_ACCESSORIES`, or `RIGHT_ACCESSORIES`.
6. Add `headwear_N.png` or `accessory_N.png` and paint the rectangles from the box unwrap above.
7. Point a look JSON at the new index.

`TouristHeadwearLayer` and `TouristAccessoryLayer` draw nothing for index 0 or for any index `TouristParts` does not know. A JSON can still pack the number (it only has to be 0–254), but the tourist will be bare-headed until the constant and the mesh exist.

## Names, and what is chosen at spawn

The costume and the personal name are chosen separately. A look does not have a name pool.

On the server, the destination constructor calls `assignLook` immediately. The plain constructor waits until the first server tick, which assigns a look if `DATA_LOOK` is still unset. `TouristLooks.pick` takes one entry at random from the catalog. The id is stored on the entity and the packed int is synced. NBT keys are `TouristLook` (the int) and `TouristLookId` (the string). A tourist already saved keeps those numbers if you later add more looks.

A platform spawn rolls a random type in `assignSpawnVariety`, then `assignLook` replaces it when the JSON has a `type`. The type is display and trades, not the mesh.

`ensurePersonalName` runs on the server tick until the tourist has a real name. The pool is `TouristNames`: 160 first names and 48 surnames. About one roll in four (`nextInt(4) == 0`) appends a surname. Tourists within 32 blocks (`NEARBY_BLOCKS`) avoid a full name that is already nearby, and avoid a repeated first name for the first 24 tries. The result is saved as `PersonalName` and copied to the vanilla custom name.

The floating text is only the first word (`TouristNames.firstOf`), in a light grey, with no shadow and no dark plate. It is drawn twice (see-through, then depth-tested) and only inside 5 blocks (`TouristDisplayFormat.NAME_TAG_MAX_DISTANCE`). The tourist screen title is the full name. `setCustomNameVisible(true)` is what allows the tag; the showcase lineup turns it off so the portraits stay clean.

`DATA_SKIN_TIER` tracks journey rank. The renderer does not read it. Rank does not change the model.

## Common pitfalls

- Editing `docs/reference/tourist_skins` does nothing. Edit `textures/entity/tourist`.
- A new face that invents its own eye grid will not match the others. Copy a skin and recolour it.
- Painting hair down over the eyes is lost under most hats, and it is not how the original faces are built. Put facial hair on the base face.
- Forgetting `index.txt` hides the look from tests and from the classpath catalog. The packed int can still render on a client that has the textures, because drawing uses the int, not the id.
- A datapack JSON can reuse texture indices that already ship in the mod. A new index needs the PNG on the client (the mod jar, or a resource pack). Server data packs do not deliver those PNGs by themselves.
- Hat or accessory index past `HEADWEAR_MAX` / `ACCESSORY_MAX` packs successfully and then draws nothing.
- Two looks with the same four numbers fail `TouristLooksTest`, and `getLookId` returns the first catalog match for that packed int.
- An accessory that is not in one of the three follow-lists will not move with the arm.
- The outfit file is the clothing overlay, not a second copy of the body. Paint the jacket and sleeves, or the arms stay the skin colour.
- `type` left out is legal in the parser and then fails the builtin test. A platform spawn then keeps the random type from `assignSpawnVariety` instead of one that matches the costume.
- A private trade list is optional and lives in `TouristTradeCatalog`, not in the look JSON. Market shopper, station scholar, trail hiker, rain caller, garden pilgrim, and library clerk use their type's shared list. The other looks have an override keyed by id.
