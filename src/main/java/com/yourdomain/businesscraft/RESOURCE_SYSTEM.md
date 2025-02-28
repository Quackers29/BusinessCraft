# BusinessCraft Resource System

## Overview

The resource system in BusinessCraft has been redesigned to support multiple types of resources beyond bread. This document explains how the resource system works and how to use it.

## Key Components

### TownResources

The `TownResources` class is the central storage mechanism for town resources. It maintains a mapping of Minecraft items to quantities and provides methods for adding, retrieving, and consuming resources.

### TownEconomyComponent

The `TownEconomyComponent` uses the `TownResources` class to manage a town's economy. It still maintains legacy support for bread-based population while allowing for expansion with other resource types.

### ITownDataProvider

The `ITownDataProvider` interface has been expanded to include methods for working with generic resources:

- `addResource(Item item, int count)` - Adds a resource to the town
- `getResourceCount(Item item)` - Gets the count of a specific resource
- `getAllResources()` - Gets all resources in the town

## How It Works

1. Any item can now be placed in the input slot of the town block.
2. When an item is processed, it is added to the town's resources.
3. Bread still has special behavior - it contributes to population growth based on the configured `breadPerPop` setting.
4. Other resources are stored and can be used for future features (crafting, trade, special buildings, etc.)

## Legacy Support

The system maintains backward compatibility:

- `getBreadCount()` still works and returns the count of bread resources
- `addBread(int count)` delegates to `addResource(Items.BREAD, count)`
- The UI display for bread and population remains unchanged

## Example Usage

To add a new item type as a resource:

```java
// Add 5 diamonds to the town
town.addResource(Items.DIAMOND, 5);

// Get the count of a specific resource
int diamondCount = town.getResourceCount(Items.DIAMOND);

// Get all resources
Map<Item, Integer> allResources = town.getAllResources();
```

## Future Expansions

This system can be expanded to support:

1. Different values for different resources
2. Resource conversion systems
3. Resource-specific buildings or features
4. Town trade systems between towns
5. Special effects based on resource combinations

## Technical Implementation

The resource data is serialized and deserialized using NBT tags to ensure persistence. Resource keys are stored as resource location strings to maintain compatibility across game launches. 