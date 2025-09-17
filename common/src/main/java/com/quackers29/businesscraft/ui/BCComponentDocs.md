# BusinessCraft UI Component Library Documentation

This document provides detailed information on the UI components available in the BusinessCraft mod, their usage, and examples.

## Core Architecture

The BusinessCraft UI system is built on the following key concepts:

1. **Component Architecture** - All UI elements extend the base `BCComponent` class or implement the `UIComponent` interface.
2. **Theme System** - The `BCTheme` class provides centralized styling for consistent appearance.
3. **Layout Management** - Layout managers like `BCFlowLayout` and `BCGridLayout` handle component positioning.
4. **Factory Pattern** - The `BCComponentFactory` simplifies component creation with preconfigured styles.

## Base Components

### BCComponent

The base component class implements the `UIComponent` interface and provides common functionality for all UI components.

```java
// Create a basic component
BCComponent component = new BCComponent(100, 50);
component.withBackgroundColor(0x80000000);
component.withBorderColor(0xFFFFFFFF);
```

### BCPanel

A container component that holds and arranges other components using layout managers.

```java
// Create a panel with a flow layout
BCPanel panel = BCComponentFactory.createFlowPanel(
    200, 300, BCFlowLayout.Direction.VERTICAL, 5
);

// Add components to the panel
panel.addChild(BCComponentFactory.createHeaderLabel("gui.title", 190));
panel.addChild(BCComponentFactory.createBodyLabel("gui.description", 190));
```

### BCButton

A button component with various styles and states.

```java
// Create different types of buttons
BCButton primaryButton = BCComponentFactory.createPrimaryButton(
    "gui.mymod.save", button -> saveChanges(), 100
);

BCButton dangerButton = BCComponentFactory.createDangerButton(
    "gui.mymod.delete", button -> deleteItem(), 100
);

// Button with a tooltip
BCButton iconButton = BCComponentFactory.createIconButton(
    Component.translatable("gui.mymod.info_tooltip"), 
    button -> showInfo()
);
```

### BCLabel

A text display component with various formatting options.

```java
// Create a header label
BCLabel headerLabel = BCComponentFactory.createHeaderLabel(
    "gui.mymod.title", 200
);

// Create a body text label
BCLabel bodyLabel = BCComponentFactory.createBodyLabel(
    "gui.mymod.description", 200
);

// Create a dynamic label that updates automatically
BCLabel dynamicLabel = BCComponentFactory.createDynamicLabel(
    () -> Component.literal("Count: " + count), 100
);
```

## Layout Managers

### BCFlowLayout

Arranges components in a flow, either horizontally or vertically, with consistent spacing.

```java
// Create a vertical flow layout
BCFlowLayout verticalLayout = new BCFlowLayout(
    BCFlowLayout.Direction.VERTICAL, 5
);

// Create a horizontal flow layout
BCFlowLayout horizontalLayout = new BCFlowLayout(
    BCFlowLayout.Direction.HORIZONTAL, 8
);

// Apply the layout to a panel
panel.withLayout(verticalLayout);
```

### BCGridLayout

Arranges components in a grid with specified columns, rows are created automatically.

```java
// Create a grid layout with 3 columns
BCGridLayout gridLayout = new BCGridLayout(3, 5, 5);

// Apply the layout to a panel
panel.withLayout(gridLayout);

// Add components (will be arranged in the grid)
for (int i = 0; i < 9; i++) {
    panel.addChild(BCComponentFactory.createPrimaryButton(
        "Button " + i, button -> {}, 60
    ));
}
```

## Advanced Components

### BCTabPanel

A tabbed panel component that shows different content based on the selected tab.

```java
// Create a tab panel
BCTabPanel tabPanel = new BCTabPanel(300, 200);

// Add tabs with different content
tabPanel.addTab("info", Component.literal("Info"), panel -> {
    panel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 5));
    panel.addChild(BCComponentFactory.createHeaderLabel("Info Tab", 290));
    panel.addChild(BCComponentFactory.createBodyLabel("Information content...", 290));
});

tabPanel.addTab("settings", Component.literal("Settings"), panel -> {
    panel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 5));
    panel.addChild(BCComponentFactory.createHeaderLabel("Settings Tab", 290));
    panel.addChild(BCComponentFactory.createPrimaryButton(
        "Save Settings", button -> saveSettings(), 150
    ));
});
```

### EditBoxComponent

A text input component for user input.

```java
// Create an edit box
EditBoxComponent editBox = BCComponentFactory.createEditBox(
    200,                     // width
    () -> "Default Text",    // initial text supplier
    text -> updateText(text), // text change handler
    50                       // max length
);

// Set focus on the edit box
editBox.setFocused(true);
```

### DataBoundButtonComponent and DataLabelComponent

Components that automatically update based on data suppliers.

```java
// Create a button that updates its text based on state
DataBoundButtonComponent toggleButton = BCComponentFactory.createDataBoundButton(
    () -> Component.literal(isEnabled ? "Disable" : "Enable"),
    button -> toggleState(),
    100
);

// Create a label that updates based on data
DataLabelComponent countLabel = BCComponentFactory.createDataLabel(
    () -> "Count: " + count,
    0xFFFFFF,
    100
);
```

## Theme System

The theme system provides consistent styling across all components.

```java
// Use the default theme
BCButton button = BCComponentFactory.createPrimaryButton(...);

// Create a custom theme
BCTheme customTheme = BCTheme.builder()
    .primaryColor(0x00AA00)
    .secondaryColor(0xAA0000)
    .textLight(0xEEEEEE)
    .build();

// Apply the custom theme
BCTheme.setActiveTheme(customTheme);

// All new components will use the custom theme
BCButton greenButton = BCComponentFactory.createPrimaryButton(...);

// Reset to default theme when done
BCTheme.resetToDefault();
```

## Screen Builder

The `BCScreenBuilder` simplifies screen creation with a fluent API.

```java
// Create a screen
AbstractContainerScreen<MyMenu> screen = BCScreenBuilder.create(menu, inventory, title, 256, 204)
    .withPadding(8)
    .withTabs(20)
    .addTab("main", Component.translatable("gui.mymod.main"), mainPanel -> {
        mainPanel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 5));
        mainPanel.addChild(BCComponentFactory.createHeaderLabel("gui.mymod.title", 200));
        // Add more components to the main tab...
    })
    .addTab("settings", Component.translatable("gui.mymod.settings"), settingsPanel -> {
        // Configure settings tab...
    })
    .build();
```

## Best Practices

1. **Use the Component Factory** - Always use `BCComponentFactory` methods to create components when possible.
2. **Use Layout Managers** - Avoid manual positioning and use layout managers to arrange components.
3. **Follow Theme Guidelines** - Use the theme system for colors and styling instead of hardcoding values.
4. **Group Related Components** - Use panels to group related components for better organization.
5. **Provide Clear Tooltips** - Add tooltips to interactive components to explain their function.
6. **Make Responsive UIs** - Design UIs that adapt to different screen sizes and resolutions.

## Example: Complete UI Screen

```java
public class MyScreen extends AbstractContainerScreen<MyMenu> {
    public MyScreen(MyMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        
        // Use the screen builder to create the UI
        return BCScreenBuilder.create(menu, inventory, title, 256, 204)
            .withPadding(8)
            .withTabs(20)
            .addTab("info", Component.translatable("gui.mymod.info"), infoPanel -> {
                infoPanel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 5));
                
                // Add a header
                infoPanel.addChild(BCComponentFactory.createHeaderLabel(
                    "gui.mymod.info_title", 240
                ));
                
                // Add some text
                infoPanel.addChild(BCComponentFactory.createBodyLabel(
                    "gui.mymod.info_desc", 240
                ));
                
                // Add a button
                infoPanel.addChild(BCComponentFactory.createPrimaryButton(
                    "gui.mymod.action_button", 
                    button -> performAction(), 
                    120
                ));
            })
            .addTab("settings", Component.translatable("gui.mymod.settings"), settingsPanel -> {
                settingsPanel.withLayout(new BCGridLayout(2, 5, 10));
                
                // Add settings controls in a grid layout
                settingsPanel.addChild(BCComponentFactory.createBodyLabel(
                    "gui.mymod.setting1", 110
                ));
                settingsPanel.addChild(BCComponentFactory.createToggleButton(
                    Component.literal("Enabled"), 
                    button -> toggleSetting1(), 
                    110
                ));
                
                settingsPanel.addChild(BCComponentFactory.createBodyLabel(
                    "gui.mymod.setting2", 110
                ));
                settingsPanel.addChild(BCComponentFactory.createEditBox(
                    110,
                    () -> "Default",
                    text -> updateSetting2(text),
                    20
                ));
            })
            .build();
    }
}
``` 