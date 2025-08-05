# BusinessCraft Production System - Simplified Approach

## Overview

The production system has been simplified for initial implementation, focusing on core functionality without complex features that can be added later. This approach allows for faster development and testing while maintaining a clear path for future enhancements.

## Simplified Configuration Format

### Core Fields Only
```toml
[production.basic_wood_harvest]
produced_by = "logging"
base_rate = 10
output_item = "minecraft:oak_log"
inputs = { "minecraft:iron_axe" = 0.1 }
time_to_setup = 0
description = "Basic wood harvesting from oak trees"
```

### Removed Fields (Future Phases)
- `tier` - Will be added in Phase 4.1
- `prerequisites` - Will be added in Phase 4.2  
- `path` - Will be added in Phase 4.1
- `waste_output` - Will be added in Phase 4.3
- `vanilla_link` - Will be added in Phase 4.5
- `population_bonus` - Will be added in Phase 4.4

## Simplified Architecture

### ProductionLine Class
```java
public class ProductionLine {
    private String id;
    private String producedBy;
    private int baseRate;
    private Item outputItem;
    private Map<Item, Double> inputs;
    private int timeToSetup;
    private String description;
}
```

### Town Production Component
```java
public class TownProductionComponent implements TownComponent {
    private Map<String, ProductionLine> activeLines = new HashMap<>();
    private Map<String, ProductionProgress> setupProgress = new HashMap<>();
    private String townClass = "general";
}
```

## Implementation Phases

### Phase 2: Core Production System (Simplified)
- **2.1 TOML Configuration System**: Basic parsing without complex fields
- **2.2 Production Component Architecture**: Core fields only
- **2.3 Production Manager System**: Basic town class restrictions
- **2.4 Basic Network Integration**: Essential sync and activation packets

### Phase 3: UI Integration
- **3.1 Production Tab Implementation**: Basic production line management
- **3.2 Production Management Interface**: Simple activation/deactivation

### Phase 4: Advanced Features (Future)
- **4.1 Tier System Implementation**: Add tier field and progression
- **4.2 Prerequisites System**: Add research/upgrade requirements
- **4.3 Waste Management System**: Add waste output handling
- **4.4 Population Scaling**: Add population-based bonuses
- **4.5 Vanilla Integration**: Add villager profession links

## Benefits of Simplified Approach

### Development Speed
- Faster initial implementation
- Easier testing and debugging
- Reduced complexity during development

### User Experience
- Simpler configuration for modpack creators
- Easier to understand and use
- Less overwhelming for new users

### Future Flexibility
- Clear upgrade path for advanced features
- Modular design allows gradual enhancement
- Backward compatibility maintained

## Sample Production Lines

### Basic Resource Production
```toml
[production.basic_wood_harvest]
produced_by = "logging"
base_rate = 10
output_item = "minecraft:oak_log"
inputs = { "minecraft:iron_axe" = 0.1 }
time_to_setup = 0
description = "Basic wood harvesting from oak trees"

[production.basic_wheat_farming]
produced_by = "farming"
base_rate = 15
output_item = "minecraft:wheat"
inputs = { "minecraft:iron_hoe" = 0.05 }
time_to_setup = 0
description = "Basic wheat farming"
```

### Simple Processing
```toml
[production.wood_planks]
produced_by = "logging"
base_rate = 20
output_item = "minecraft:oak_planks"
inputs = { "minecraft:oak_log" = 1 }
time_to_setup = 30
description = "Convert logs to planks"

[production.bread_baking]
produced_by = "farming"
base_rate = 12
output_item = "minecraft:bread"
inputs = { "minecraft:wheat" = 3 }
time_to_setup = 60
description = "Bake wheat into bread"
```

### Advanced Processing
```toml
[production.advanced_tool_crafting]
produced_by = "any"
base_rate = 2
output_item = "minecraft:iron_axe"
inputs = { 
    "minecraft:iron_ingot" = 3,
    "minecraft:oak_planks" = 2
}
time_to_setup = 120
description = "Craft iron tools"
```

## Town Classes

### Supported Town Classes
- `"logging"` - Wood and forestry production
- `"farming"` - Agricultural and food production
- `"mining"` - Mineral and ore processing
- `"tourism"` - Tourist services and emerald generation
- `"any"` - Available to all town types

### Town Class Assignment
Towns can be assigned a class through:
- Configuration file
- UI selection
- Automatic detection based on production lines
- Default: `"general"` (can use `"any"` production lines)

## Production Processing

### Daily Production Cycle (Simplified)
1. **Resource Check**: Verify required inputs are available
2. **Consumption**: Remove consumed resources from town storage
3. **Production**: Generate output items based on base rate
4. **Storage**: Add products to town resource storage

### Production Rate Calculation (Simplified)
```java
public int calculateProductionRate(ProductionLine line, Town town) {
    int baseRate = line.getBaseRate();
    
    // Future: Apply population bonus
    // Future: Apply efficiency modifiers
    // Future: Apply town class bonuses
    // Future: Apply tier penalties for high-tier towns
    
    return Math.max(1, baseRate);
}
```

## Migration Strategy

### Backward Compatibility
- Existing towns start with `"general"` class
- All towns can use `"any"` production lines
- No existing functionality is removed or changed

### Gradual Enhancement
1. **Phase 2**: Basic production system
2. **Phase 3**: UI integration
3. **Phase 4**: Advanced features (tiers, prerequisites, waste, etc.)
4. **Phase 5**: Optimization and polish

## Success Criteria (Simplified)

### Functional Requirements
- [ ] Production lines generate resources based on configuration
- [ ] Resource consumption works correctly
- [ ] Town class restrictions function properly
- [ ] UI displays production status accurately

### Performance Requirements
- [ ] Daily processing completes within 50ms for 100 towns
- [ ] Memory usage remains stable during production calculations
- [ ] Network packets are optimized for minimal bandwidth usage

### User Experience Requirements
- [ ] Production system is intuitive and easy to understand
- [ ] UI provides clear feedback on production status
- [ ] Configuration is easy to modify for modpack creators
- [ ] Error messages are helpful and actionable

## Future Enhancement Path

### Phase 4.1: Tier System
- Add `tier` field to production lines
- Implement tier-based restrictions and bonuses
- Add tier progression system
- Create tier-based UI filtering

### Phase 4.2: Prerequisites System
- Add `prerequisites` field to production lines
- Implement research/upgrade system
- Create prerequisite checking logic
- Add prerequisite UI indicators

### Phase 4.3: Waste Management
- Add `waste_output` field to production lines
- Implement waste collection and storage
- Add waste recycling production lines
- Create waste disposal penalties and incentives

### Phase 4.4: Population Scaling
- Add `population_bonus` field to production lines
- Implement population-based production bonuses
- Add efficiency modifiers based on town size
- Create production capacity limits

### Phase 4.5: Vanilla Integration
- Add `vanilla_link` field to production lines
- Link production lines to villager professions
- Implement villager hiring for production lines
- Add villager skill progression system

This simplified approach provides a solid foundation for the production system while maintaining clear paths for future enhancements. 