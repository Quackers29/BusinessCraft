# BusinessCraft Screen Builder System

The Screen Builder System provides a powerful and flexible way to create screens in the BusinessCraft mod. It simplifies screen creation with a fluent API, animation support, layout management, and pre-configured templates.

## Table of Contents

1. [Overview](#overview)
2. [Basic Usage](#basic-usage)
3. [Layouts](#layouts)
4. [Tabs](#tabs)
5. [Animations](#animations)
6. [Themes](#themes)
7. [Templates](#templates)
8. [Examples](#examples)

## Overview

The Screen Builder System is built around the `BCScreenBuilder` class, which provides a fluent API for creating screens. It includes:

- Builder pattern for easy screen creation
- Built-in layout managers for automatic component placement
- Support for tabbed interfaces
- Animation and transition effects
- Theme integration
- Pre-configured templates for common screen types

## Basic Usage

### Creating a Simple Screen

```java
AbstractContainerScreen<MyMenu> screen = BCScreenBuilder.create(
    menu,
    playerInventory,
    Component.translatable("screen.mymod.title"),
    176, 166
)
.withPadding(8)
.withBackgroundColor(0xE0000000)
.withBorderColor(0xFF666666)
.addComponent(new BCButton(10, 10, 80, 20, Component.literal("Click Me"), button -> {
    // Handle click
}))
.build();
```

### Using Themes

```java
BCTheme theme = BCTheme.get();

AbstractContainerScreen<MyMenu> screen = BCScreenBuilder.create(
    menu,
    playerInventory,
    Component.translatable("screen.mymod.title"),
    176, 166
)
.withPadding(theme.getMediumPadding())
.withBackgroundColor(theme.getBackgroundColor())
.withBorderColor(theme.getBorderColor())
.addComponent(new BCButton(10, 10, 80, 20, Component.literal("Click Me"), button -> {
    // Handle click
}))
.build();
```

## Layouts

The Screen Builder System provides several layout managers for automatic component placement:

### Flow Layout

The `BCFlowLayout` arranges components in a horizontal or vertical flow:

```java
BCScreenBuilder<MyMenu> builder = BCScreenBuilder.create(/* ... */)
    .withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 8));

// Components will be arranged vertically with 8 pixels spacing
builder.addComponent(component1);
builder.addComponent(component2);
builder.addComponent(component3);
```

### Grid Layout

The `BCGridLayout` arranges components in a grid:

```java
BCScreenBuilder<MyMenu> builder = BCScreenBuilder.create(/* ... */)
    .withLayout(new BCGridLayout(2, 3, 5)); // 2 columns, 3 rows, 5 pixels spacing

// Components will be arranged in a 2x3 grid
builder.addComponent(component1); // Cell (0,0)
builder.addComponent(component2); // Cell (1,0)
builder.addComponent(component3); // Cell (0,1)
builder.addComponent(component4); // Cell (1,1)
builder.addComponent(component5); // Cell (0,2)
builder.addComponent(component6); // Cell (1,2)
```

## Tabs

The Screen Builder System supports tabbed interfaces:

```java
BCScreenBuilder<MyMenu> builder = BCScreenBuilder.create(/* ... */)
    .withTabs(20) // Set the tab height
    .addTab("tab1", Component.literal("First Tab"), panel -> {
        panel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 5));
        panel.addChild(new BCButton(0, 0, 80, 20, Component.literal("Button 1"), button -> {}));
    })
    .addTab("tab2", Component.literal("Second Tab"), panel -> {
        panel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 5));
        panel.addChild(new BCButton(0, 0, 80, 20, Component.literal("Button 2"), button -> {}));
    });
```

## Animations

The Screen Builder System supports various animations for screen transitions:

```java
BCScreenBuilder<MyMenu> builder = BCScreenBuilder.create(/* ... */)
    .withEnterAnimation(BCAnimation.AnimationType.FADE, BCAnimation.EasingFunction.EASE_OUT, 300)
    .withExitAnimation(BCAnimation.AnimationType.SCALE, BCAnimation.EasingFunction.EASE_IN, 200)
    .withTabAnimation(BCAnimation.AnimationType.SLIDE_LEFT, BCAnimation.EasingFunction.EASE_IN_OUT, 250);
```

Available animation types:
- `FADE` - Fade in/out
- `SLIDE_LEFT` - Slide from/to left
- `SLIDE_RIGHT` - Slide from/to right
- `SLIDE_UP` - Slide from/to top
- `SLIDE_DOWN` - Slide from/to bottom
- `SCALE` - Scale up/down
- `NONE` - No animation

Available easing functions:
- `LINEAR` - Constant speed
- `EASE_IN` - Start slow, end fast
- `EASE_OUT` - Start fast, end slow
- `EASE_IN_OUT` - Start slow, middle fast, end slow
- `BOUNCE` - Bounce effect at the end
- `ELASTIC` - Elastic/spring effect

## Themes

The Screen Builder System integrates with the `BCTheme` system to provide consistent styling:

```java
BCTheme theme = BCTheme.get(); // Get the current theme

// Or select a predefined theme
BCTheme.set(BCTheme.darkTheme());
BCTheme.set(BCTheme.lightTheme());
BCTheme.set(BCTheme.fantasyTheme());

// Or create a custom theme
BCTheme customTheme = BCTheme.builder()
    .primaryColor(0xFF1976D2)
    .secondaryColor(0xFF388E3C)
    .backgroundColor(0xE0101010)
    .surfaceColor(0xF0303030)
    .borderColor(0xFF555555)
    .textColor(0xFFFFFFFF)
    .textSecondaryColor(0xFFCCCCCC)
    .build();

BCTheme.set(customTheme);
```

## Templates

The Screen Builder System provides pre-configured templates for common screen types:

```java
// Town Management Screen
AbstractContainerScreen<MyMenu> townScreen = BCScreenTemplates.createTownManagementScreen(
    menu,
    playerInventory,
    Component.translatable("screen.mymod.town"),
    256, 240
);

// Dialog Screen
AbstractContainerScreen<MyMenu> dialogScreen = BCScreenTemplates.createDialogScreen(
    menu,
    playerInventory,
    Component.translatable("screen.mymod.dialog"),
    200, 150
);

// Resource Management Screen
AbstractContainerScreen<MyMenu> resourceScreen = BCScreenTemplates.createResourceScreen(
    menu,
    playerInventory,
    Component.translatable("screen.mymod.resources"),
    256, 240
);

// Quest/Mission Screen
AbstractContainerScreen<MyMenu> questScreen = BCScreenTemplates.createQuestScreen(
    menu,
    playerInventory,
    Component.translatable("screen.mymod.quests"),
    256, 240
);
```

## Examples

### Complete Town Management Screen Example

```java
public class TownBlockScreen extends AbstractContainerScreen<TownBlockMenu> {
    public TownBlockScreen(TownBlockMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        
        // Use the template to create the screen
        AbstractContainerScreen<TownBlockMenu> templatedScreen = BCScreenTemplates.createTownManagementScreen(
            menu,
            playerInventory,
            title,
            256, 240
        );
        
        // Copy properties from templated screen
        this.width = templatedScreen.width;
        this.height = templatedScreen.height;
    }
    
    @Override
    protected void init() {
        super.init();
        // Initialize the screen
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
    
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // Render background
    }
}
```

### Custom Screen with Complex Layout

```java
public class CustomScreen extends AbstractContainerScreen<CustomMenu> {
    public CustomScreen(CustomMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        
        BCTheme theme = BCTheme.get();
        
        // Create screen with the builder
        AbstractContainerScreen<CustomMenu> builtScreen = BCScreenBuilder.create(
            menu,
            playerInventory,
            title,
            256, 240
        )
        .withPadding(theme.getMediumPadding())
        .withBackgroundColor(theme.getBackgroundColor())
        .withBorderColor(theme.getBorderColor())
        .withEnterAnimation(BCAnimation.AnimationType.FADE, BCAnimation.EasingFunction.EASE_OUT, 300)
        .withTabs(24)
        .withTabAnimation(BCAnimation.AnimationType.SLIDE_LEFT, BCAnimation.EasingFunction.EASE_OUT, 200)
        .addTab("info", Component.translatable("custom.tab.info"), panel -> {
            panel.withLayout(new BCGridLayout(2, 2, 8));
            
            // Add components to the panel
            BCPanel leftPanel = BCComponentFactory.createPanel(panel.getWidth() / 2 - 4, 200);
            leftPanel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 5));
            leftPanel.withBorderColor(theme.getPrimaryColor());
            leftPanel.withBackgroundColor(theme.getSurfaceColor());
            
            BCPanel rightPanel = BCComponentFactory.createPanel(panel.getWidth() / 2 - 4, 200);
            rightPanel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 5));
            rightPanel.withBorderColor(theme.getSecondaryColor());
            rightPanel.withBackgroundColor(theme.getSurfaceColor());
            
            // Add components to the panels
            leftPanel.addChild(new BCButton(0, 0, 100, 20, Component.literal("Button 1"), button -> {}));
            rightPanel.addChild(new BCButton(0, 0, 100, 20, Component.literal("Button 2"), button -> {}));
            
            // Add panels to the tab
            panel.addChild(leftPanel);
            panel.addChild(rightPanel);
        })
        .addTab("settings", Component.translatable("custom.tab.settings"), panel -> {
            panel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 10));
            
            // Add components to the settings tab
            panel.addChild(new BCButton(0, 0, 120, 20, Component.literal("Setting 1"), button -> {}));
            panel.addChild(new BCButton(0, 0, 120, 20, Component.literal("Setting 2"), button -> {}));
            panel.addChild(new BCButton(0, 0, 120, 20, Component.literal("Setting 3"), button -> {}));
        })
        .build();
        
        // Copy properties from built screen
        this.width = builtScreen.width;
        this.height = builtScreen.height;
    }
}
```

## Best Practices

1. **Use themes for consistent styling** - Leverage the `BCTheme` system to ensure consistent colors and styling across your screens.

2. **Prefer layout managers over manual positioning** - Use `BCFlowLayout` and `BCGridLayout` to automatically position components instead of hard-coding coordinates.

3. **Break complex screens into tabs** - Use tabs to organize complex screens and improve user experience.

4. **Use animation judiciously** - Animations can enhance the user experience, but don't overuse them. Keep animations short (200-300ms) to avoid frustrating users.

5. **Consider screen templates** - For common screen types, use the pre-configured templates to save time and ensure consistency.

6. **Structure components hierarchically** - Use panels to group related components for better organization and easier layout management.

7. **Add tooltips for important controls** - Use `BCComponentFactory.createTooltip()` to add tooltips to important controls to improve usability.

8. **Test different screen resolutions** - Make sure your screens look good at different resolutions and scale factors. 