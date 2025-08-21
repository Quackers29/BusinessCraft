# BCModalGridScreen Component

## Overview

The `BCModalGridScreen` component is a flexible, customizable modal screen that displays data in a grid format with support for:
- Multiple columns
- Custom data types
- Scrolling for large data sets
- Customizable appearance (colors, dimensions, etc.)
- Row click handling
- Back button with customizable text
- Header titles

## Basic Usage

### Simple String List

```java
// Create a list of string items
List<String> items = Arrays.asList("Item 1", "Item 2", "Item 3");

// Use the factory to create a simple string list
BCModalGridScreen<String> screen = BCModalGridFactory.createStringListScreen(
    Component.literal("My List"),      // Title
    parentScreen,                      // Parent screen
    items,                             // Data
    selectedItem -> {                  // Click handler
        // Handle item selection
    },
    BCModalGridFactory.Themes.BC_DEFAULT // Color theme
);

// Show the screen
Minecraft.getInstance().setScreen(screen);
```

### Custom Data Class

```java
// Create sample data with a custom class
List<MyDataClass> dataItems = getMyData();

// Create a custom modal grid screen
BCModalGridScreen<MyDataClass> screen = new BCModalGridScreen<>(
    Component.literal("My Custom Data"),
    parentScreen,
    null // No close callback
);

// Configure the columns
screen.addColumn("Column 1", item -> item.getProperty1())
      .addColumn("Column 2", item -> item.getProperty2())
      .addColumn("Column 3", item -> item.getProperty3())
      .withData(dataItems)
      .withBackButtonText("Return");

// Show the screen
Minecraft.getInstance().setScreen(screen);
```

## Customization Options

### Panel Size and Appearance

```java
screen.withPanelSize(0.6f, 0.5f)    // 60% width, 50% height
      .withRowHeight(24)            // Taller rows
      .withTitleScale(1.5f)         // Larger title
      .withColors(
          0xFF000000,               // Background color
          0xFFDDDDDD,               // Border color
          0xFFFFFFFF,               // Title color
          0xFFDDFFFF,               // Header color
          0xFFFFFFFF                // Text color
      )
      .withAlternatingRowColors(0x30FFFFFF, 0x20FFFFFF)
      .withScrollbarColors(0x40FFFFFF, 0xA0CCDDFF, 0xFFCCDDFF);
```

### Row Click Handler

```java
screen.withRowClickHandler(item -> {
    // Handle item click
    // 'item' is of the generic type T provided to BCModalGridScreen<T>
});
```

### Back Button Text

```java
screen.withBackButtonText("Go Back");
```

## Factory Methods

The `BCModalGridFactory` class provides convenient methods for creating common types of grid screens:

### Visitor History Screen

```java
BCModalGridScreen<VisitHistoryRecord> screen = BCModalGridFactory.createVisitorHistoryScreen(
    Component.literal("Visitor History"),
    parentScreen,
    visitHistory,
    onCloseCallback
);
```

### Resource List Screen

```java
Map<Item, Integer> resources = getResourceMap();

BCModalGridScreen<Map.Entry<Item, Integer>> screen = BCModalGridFactory.createResourceListScreen(
    Component.literal("Resources"),
    parentScreen,
    resources,
    onCloseCallback
);
```

### String List Screen

```java
List<String> items = getStringList();

BCModalGridScreen<String> screen = BCModalGridFactory.createStringListScreen(
    Component.literal("String List"),
    parentScreen,
    items,
    onItemClickHandler,
    BCModalGridFactory.Themes.LIGHT // Optional theme
);
```

## Color Themes

The `BCModalGridFactory.Themes` class provides several predefined color themes:

- `BC_DEFAULT` - Business Craft default theme (Bluish)
- `DARK` - Dark theme
- `LIGHT` - Light theme
- `SUCCESS` - Success theme (Greenish)
- `DANGER` - Danger theme (Reddish)

Example:
```java
screen.withColors(
    BCModalGridFactory.Themes.SUCCESS[0],  // Background
    BCModalGridFactory.Themes.SUCCESS[1],  // Border
    BCModalGridFactory.Themes.SUCCESS[2],  // Title
    BCModalGridFactory.Themes.SUCCESS[3],  // Header
    BCModalGridFactory.Themes.SUCCESS[4]   // Text
);
```

## Complete Example

For full examples of usage, see the `BCModalGridExample.java` class. 