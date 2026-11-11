# UI style

One palette, one spacing scale, one control size. Screens read `UiTheme` (`common/src/main/java/com/quackers29/businesscraft/ui/theme/UiTheme.java`). They do not copy hex values.

## Palette

| Role | Value | Use |
|---|---|---|
| Panel | `#14161C` at 94% opacity (`0xF014161C`) | Screen fill. Opaque so text stays readable over the world. |
| Border | `#D7B56D` | Frame and scrollbar thumb. |
| Section | `#1A1F2A` | Inset wells and inactive tabs. |
| Text | `#FFFFFF` | Body. |
| Muted | `#C2C6CE` | Secondary lines and empty states. |
| Gold | `#FFD56A` | The one title inside the frame. |
| Aqua | `#8FDBFF` | Highlight values. |
| On | `#8FDB9A` fill, `#102816` text | Toggles that are on, and enabled status. |
| Off | `#F0A3A3` fill, `#3A1216` text | Toggles that are off, and disabled status. |
| Button | `#2C3342`, hover `#3C4A62` | Actions. White text. Light green and light red are not action fills. |
| Line | `#3A404C` | Rules and tracks. |

## Spacing and controls

Spacing is 4, 8, 12, 16. Buttons are 20 tall, or 16 when the panel is shorter than 250. Width is the label width plus 16, and at least 20. The back chevron is 20×20. Toggles are 64×18 and read On or Off. Scrollbars are 3px with a gold thumb. Tooltips stay inside the scaled screen (`UiTooltip.place`). An empty list is one muted sentence.

Titles are scale 1, gold, with no shadow, and they do not repeat the tab name. Page sizes, history length, visible bids, and platform radius steps come from `businesscraft.toml`. Defaults match the previous hardcoded numbers (20, 50, 2, 1, and 5).

## Drawing text

Use `drawString(..., false)`. `drawCenteredString` always draws a shadow. Shorten labels with `UiTextWrap.ellipsize` measured by the font, not by a character count.
