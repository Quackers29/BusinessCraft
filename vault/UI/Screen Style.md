---
tags:
  - detail
  - ui
---
# Screen Style

**Breadcrumb**: UI > Screen Style
**TL;DR**: Every BusinessCraft screen reads one palette and one control size from `UiTheme`: opaque dark panels, a gold frame, 20px buttons, and toggles that are light green when On and light red when Off.

## What it does
Town Centre, payment, contracts, the platform list, the platform block screen, the tourist conversation, the map, and the trade window share one look. Text is drawn without a shadow so it stays sharp on the dark panel. A list with nothing in it says so in one muted line. Counts that used to be fixed in the UI (contracts per page, history length, bids shown, radius click step) are settings, and the defaults are the old numbers.

## How it works (process view)
A screen fills its panel with the opaque dark colour, draws a gold border, and puts a single gold title inside the frame. Tab names are the tabs themselves, so Overview, Resources, and Settings do not repeat that name as a second heading. Production keeps its heading because the same tab switches between production and upgrades.

Buttons are 20 pixels tall (16 on a short panel) and wide enough for their label plus 16 pixels. The back control is a 20×20 chevron. A toggle is 64×18: light green with dark text when On, light red with dark text when Off. Claim, View, and Courier stay on the neutral button colour with white text, because light green behind white text is hard to read.

Spacing steps are 4, 8, 12, and 16. A scrollbar is a 3px gold thumb. Tooltips are clamped to the scaled screen. Town names on the map step downward when they would otherwise stack. The ranking, visitor history, town detail, and contract detail windows shrink to their rows instead of leaving a tall empty panel.

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `UiTheme` | `common/src/main/java/com/quackers29/businesscraft/ui/theme/UiTheme.java` | Palette, spacing, button, toggle, and scrollbar sizes |
| `UiTooltip.place` | `common/src/main/java/com/quackers29/businesscraft/ui/theme/UiTooltip.java` | Keeps a tooltip box inside the scaled screen |
| `UIGridBuilder` | `common/src/main/java/com/quackers29/businesscraft/ui/grid/UIGridBuilder.java` | Public grid API. Painting, input, cells, and list filling are package helpers |
| `GridPainter` | `common/src/main/java/com/quackers29/businesscraft/ui/grid/GridPainter.java` | Draws cells, scrollbars, and vanilla grid tooltips |
| `UiTextWrap.ellipsize` | `common/src/main/java/com/quackers29/businesscraft/ui/util/UiTextWrap.java` | Cuts a label to a pixel width |
| `UiLabelLayout.placeY` | `common/src/main/java/com/quackers29/businesscraft/ui/util/UiLabelLayout.java` | Nudges overlapping map labels down |
| `TownInterfaceTheme` | `common/src/main/java/com/quackers29/businesscraft/ui/templates/TownInterfaceTheme.java` | Town colour names, now aliases of `UiTheme` |
| `ConfigLoader.contractPageSize` | `common/src/main/java/com/quackers29/businesscraft/config/ConfigLoader.java` | Contracts per page, default 20 |
| `ConfigLoader.contractHistoryLimit` | same | History rows before Show All, default 50 |
| `ConfigLoader.contractVisibleBids` | same | Bid rows on the detail screen, default 2 |
| `ConfigLoader.platformRadiusStep` | same | Radius or spread change per click, default 1 |
| `ConfigLoader.platformRadiusStepFast` | same | Same change while Shift is held, default 5 |

## Rules & formulas (exact)
Panel `0xF014161C`, border `0xFFD7B56D`, section `0xFF1A1F2A`, text `0xFFFFFFFF`, muted `0xFFC2C6CE`, gold `0xFFFFD56A`, aqua `0xFF8FDBFF`. On fill `0xFF8FDB9A` with text `0xFF102816`. Off fill `0xFFF0A3A3` with text `0xFF3A1216`. Button `0xFF2C3342`, hover `0xFF3C4A62`.

`buttonWidth` is `max(20, textWidth + 16)`. `buttonHeight` is 16 when the panel height is below 250, otherwise 20. Scrollbar thickness is 3.

`contracts.pageSize`, `contracts.historyLimit`, and `contracts.visibleBids` load with `getIntOrElse` and are clamped to at least 1. A missing key keeps 20, 50, and 2. `platforms.radiusStep` defaults to 1 and `platforms.radiusStepFast` defaults to 5, and the fast step is at least the normal step. The history tab sends the normal page size, and the server replaces it with `contractHistoryLimit` unless Show All is set.

## Edge cases & behaviors
- A page size or history limit of 0 or less in the file becomes 1, so the board still requests a row.
- Light green and light red are status and toggle colours. Claim and other actions use the neutral button so white text stays readable.
- The shared Town Centre panel stays 256×204. Settings shrinks only the button well, so switching tabs does not resize the window.
- Storage slot grids stay 2×9 because that is the menu layout, not a display cap.
- Trade and storage open as `BCModalInventoryScreen`. `TradeMenu` and `StorageMenu` stay registered; there is no separate trade or storage screen.

## Test coverage
- Test file: `common/src/test/java/com/quackers29/businesscraft/ui/theme/UiTooltipTest.java` covers the scaled-screen inset.
- Test file: `common/src/test/java/com/quackers29/businesscraft/ui/util/UiTextWrapTest.java` covers ellipsis width and overlapping label nudge.
- Test file: `common/src/test/java/com/quackers29/businesscraft/config/ConfigLoaderTest.java` covers the new defaults when keys are absent, and a save/load round trip for page size and the fast radius step.
- `ContractSummaryViewModelBuilderTest.build_pageSizeZeroOrNegative_clampsToDefault20` still expects 20, which is the config default.

## Open questions
- The edit-name popup is still 300×150. That size is chrome, not a gameplay cap.

## Related
- [[Town/Town Overview]]
- [[Town/Platforms/Platform Interface]]
- [[Config/Config Overview]]
- [[UI/Showcase Harness]]
- `docs/UI_STYLE.md`
