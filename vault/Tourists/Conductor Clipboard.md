# Conductor's Clipboard

**TL;DR:** Craftable item `conductor_clipboard`. While held (main or off hand), tourist name tags within 5 blocks show a second solid light-yellow line (`#FFFFAA`) `Origin -> Destination` using synced entity labels (includes Any Town).

## Related

- `item/ConductorClipboardItem.java`, `client/renderer/TouristRenderer.java`, `entity/TouristDisplayFormat.java`
- Recipe: `data/businesscraft/recipes/conductor_clipboard.json`
- Showcase: `tools/showcase.sh testbed forge conductor-clipboard`

## Recipe

Shaped: paper + iron ingot + sticks (cheap vanilla ingredients). Same datapack recipe for Forge and Fabric via common resources.

## Tests

- `TouristDisplayFormatTest.conductorRouteLine_formatsOriginAndDestination`
