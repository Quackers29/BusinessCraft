# BusinessCraft Screen Builder System

This documentation provides an overview of the Screen Builder System, which simplifies the creation of UI screens for the BusinessCraft mod.

## Overview

The Screen Builder System provides:

1. A builder pattern for creating screens
2. Layout managers for automatic component placement
3. Screen templates for common BusinessCraft interfaces

## Using the Screen Builder

The `BCScreenBuilder` class provides a fluent API for creating screens:

```java
// Create a basic screen
AbstractContainerScreen<MyMenu> screen = BCScreenBuilder.create(menu, inventory, title, 256, 204)
    .withPadding(10)
    .withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 10))
    .addComponent(BCComponentFactory.createHeaderLabel("My Screen", 236))
    .addComponent(someComponent)
    .build();
```

### Creating a Tabbed Screen

```java
// Create a tabbed screen
BCScreenBuilder<MyMenu> builder = BCScreenBuilder.create(menu, inventory, title, 256, 204)
    .withPadding(10)
    .withTabs(20); // Set tab height

// Add first tab
builder.addTab("tab1", Component.literal("First Tab"), panel -> {
    panel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 5));
    panel.addChild(BCComponentFactory.createHeaderLabel("Tab 1 Content", 210));
    // Add more components to the tab
});

// Add second tab
builder.addTab("tab2", Component.literal("Second Tab"), panel -> {
    panel.withLayout(new BCGridLayout(2, 5, 5));
    // Add grid-based components
});

// Build the screen
AbstractContainerScreen<MyMenu> screen = builder.build();
```

## Layout Managers

The Screen Builder System includes the following layout managers:

### Flow Layout

`BCFlowLayout` arranges components sequentially, either horizontally or vertically:

```java
// Vertical layout with 10px spacing
BCFlowLayout verticalLayout = new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 10);

// Horizontal layout with 5px spacing
BCFlowLayout horizontalLayout = new BCFlowLayout(BCFlowLayout.Direction.HORIZONTAL, 5);
```

### Grid Layout

`BCGridLayout` arranges components in a grid pattern:

```java
// 2-column grid with 10px horizontal spacing and 5px vertical spacing
BCGridLayout gridLayout = new BCGridLayout(2, 10, 5);

// 3-column grid
BCGridLayout threeColGrid = new BCGridLayout(3, 5, 5);
```

## Screen Templates

The `BCScreenTemplates` class provides ready-to-use templates for common screen types:

### Information Screen

```java
// Create an information screen
AbstractContainerScreen<MyMenu> infoScreen = BCScreenTemplates.createInfoScreen(
    menu,
    inventory,
    Component.literal("Information"),
    Component.literal("Header Text"),
    Component.literal("Content text goes here with important information for the player."),
    Arrays.asList(
        BCScreenTemplates.ButtonConfig.of(
            Component.literal("Button 1"),
            () -> System.out.println("Button 1 clicked")
        ),
        BCScreenTemplates.ButtonConfig.of(
            Component.literal("Button 2"),
            () -> System.out.println("Button 2 clicked")
        )
    )
);
```

### Tabbed Management Screen

```java
// Create a tabbed screen
AbstractContainerScreen<MyMenu> tabbedScreen = BCScreenTemplates.createTabbedScreen(
    menu,
    inventory,
    Component.literal("Management"),
    Arrays.asList(
        BCScreenTemplates.TabConfig.of(
            "tab1",
            Component.literal("Overview"),
            panel -> {
                // Configure overview tab
                panel.addChild(BCComponentFactory.createHeaderLabel("Overview", 210));
                // Add more components
            }
        ),
        BCScreenTemplates.TabConfig.of(
            "tab2",
            Component.literal("Details"),
            panel -> {
                // Configure details tab
                panel.addChild(BCComponentFactory.createHeaderLabel("Details", 210));
                // Add more components
            }
        )
    )
);
```

### Resource Management Screen

```java
// Create a resource management screen
AbstractContainerScreen<MyMenu> resourceScreen = BCScreenTemplates.createResourceScreen(
    menu,
    inventory,
    Component.literal("Resources"),
    Component.literal("Available Resources"),
    Arrays.asList(
        BCScreenTemplates.ResourceConfig.of(
            Component.literal("Wood"),
            () -> 50, // Dynamic resource amount
            new ItemStack(Items.OAK_LOG)
        ),
        BCScreenTemplates.ResourceConfig.of(
            Component.literal("Stone"),
            () -> 35,
            new ItemStack(Items.STONE)
        )
    ),
    Arrays.asList(
        BCScreenTemplates.ButtonConfig.of(
            Component.literal("Buy"),
            () -> System.out.println("Buy clicked")
        ),
        BCScreenTemplates.ButtonConfig.of(
            Component.literal("Sell"),
            () -> System.out.println("Sell clicked")
        )
    )
);
```

### Settings Screen

```java
// Create a settings screen
AbstractContainerScreen<MyMenu> settingsScreen = BCScreenTemplates.createSettingsScreen(
    menu,
    inventory,
    Component.literal("Settings"),
    Arrays.asList(
        BCScreenTemplates.SettingConfig.of(
            Component.literal("Setting 1:"),
            new BCToggleButton(
                100, 20,
                Component.literal("Enabled"),
                Component.literal("Disabled"),
                true,
                button -> System.out.println("Setting 1 toggled: " + button.isToggled())
            )
        ),
        BCScreenTemplates.SettingConfig.of(
            Component.literal("Setting 2:"),
            new BCEditBoxComponent(
                100, 20,
                Component.literal(""),
                text -> System.out.println("Setting 2 changed: " + text)
            )
        )
    )
);
```

## Best Practices

1. **Consistent Screen Sizes** - Use standard screen sizes (e.g., 256x204) for consistency.
2. **Proper Padding** - Always use padding to prevent components from touching the screen edges.
3. **Layout Managers** - Use layout managers rather than manually positioning components.
4. **Screen Templates** - Use the provided templates for common screen types to maintain UI consistency.
5. **Tab Organization** - When using tabs, group related functionality and provide clear tab labels.
6. **Responsive Design** - Consider different screen resolutions by using relative sizing when possible.
7. **Theming** - Utilize the BCTheme class to maintain consistent colors and styles across screens.

## Example: Complete BusinessCraft Screen

```java
public class MyBusinessScreen extends AbstractContainerScreen<MyBusinessMenu> {
    private static final int SCREEN_WIDTH = 256;
    private static final int SCREEN_HEIGHT = 204;

    public MyBusinessScreen(MyBusinessMenu menu, Inventory inventory, Component title) {
        // Use the screen builder to create the screen
        AbstractContainerScreen<MyBusinessMenu> screen = BCScreenTemplates.createTabbedScreen(
            menu,
            inventory,
            title,
            Arrays.asList(
                BCScreenTemplates.TabConfig.of(
                    "overview",
                    Component.literal("Overview"),
                    panel -> {
                        // Overview tab configuration
                        panel.addChild(BCComponentFactory.createHeaderLabel("Business Overview", 210));
                        
                        BCPanel infoPanel = new BCPanel(210, 130);
                        infoPanel.withBackgroundColor(0x40000000);
                        infoPanel.withBorderColor(0x80FFFFFF);
                        infoPanel.withPadding(5);
                        infoPanel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 5));
                        
                        infoPanel.addChild(BCComponentFactory.createDynamicLabel(
                            () -> Component.literal("Business Level: " + getBusinessLevel()),
                            200
                        ));
                        infoPanel.addChild(BCComponentFactory.createDynamicLabel(
                            () -> Component.literal("Weekly Income: " + getWeeklyIncome() + " coins"),
                            200
                        ));
                        
                        panel.addChild(infoPanel);
                    }
                ),
                BCScreenTemplates.TabConfig.of(
                    "employees",
                    Component.literal("Employees"),
                    panel -> {
                        // Employees tab configuration
                        panel.addChild(BCComponentFactory.createHeaderLabel("Employees", 210));
                        
                        // Add employee list and management buttons
                    }
                ),
                BCScreenTemplates.TabConfig.of(
                    "finances",
                    Component.literal("Finances"),
                    panel -> {
                        // Finances tab configuration
                        panel.addChild(BCComponentFactory.createHeaderLabel("Financial Report", 210));
                        
                        // Add financial charts and information
                    }
                )
            )
        );
        
        // Copy properties from the built screen
        this.leftPos = screen.leftPos;
        this.topPos = screen.topPos;
        this.width = screen.width;
        this.height = screen.height;
        // Copy other necessary properties
    }
    
    // Methods to get business data
    private int getBusinessLevel() {
        return 3; // Replace with actual implementation
    }
    
    private int getWeeklyIncome() {
        return 1250; // Replace with actual implementation
    }
}
```

## Conclusion

The Screen Builder System simplifies the creation of UI screens for the BusinessCraft mod, providing a consistent and maintainable approach to UI development. By using the builder pattern, layout managers, and screen templates, developers can quickly create professional-looking interfaces without having to manually position components. 