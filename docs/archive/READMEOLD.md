# BusinessCraft

A Minecraft mod that adds town management and tourism-based economy systems to the game.

## Features

### Town Management
- Create and manage towns with customizable names
- Town blocks serve as the center of town operations
- Population growth based on tourist visits
- Resource management and communal storage

### Platform System
- Build platforms for tourist spawning and transportation
- Connect platforms to destinations
- Customizable platform paths
- Enable/disable individual platforms

### Tourist System
- Population-based tourist allocation
- Tourists travel between towns via platforms
- Distance and direction indicators for destinations
- Fair distribution of tourists based on town populations

## Tourist Allocation System

The mod now includes a sophisticated allocation system for tourists that ensures fair distribution based on town populations:

- **Proportional Distribution**: Tourists are allocated proportionally based on town populations. A town with 75% of the total population will receive approximately 75% of tourists over time.

- **Fairness Gap Tracking**: The system tracks the current allocation of tourists compared to the target allocation, always selecting the most under-allocated town as the next destination.

- **Dynamic Adjustment**: As tourists arrive at or depart from towns, the system automatically adjusts its allocation targets to maintain proportional distribution.

- **Fallback System**: In cases where no population data exists, the system defaults to equal distribution among available towns.

## Configuration

The mod includes several configurable options:
- Minimum distance between towns
- Tourist capacity limits
- Population requirements for tourist spawning
- Tourist-to-population ratios

## Integration

BusinessCraft integrates with other mods:
- Compatible with Create mod train systems for tourist transportation
- Designed to work alongside economy mods

## Getting Started

1. Place a Town Block to establish a new town
2. Configure your town name and settings
3. Build platforms for tourist spawning
4. Connect platforms to destination towns
5. Watch your town grow as tourists arrive!

## Upcoming Features

See the `current_todo.txt` file for planned features and enhancements. 