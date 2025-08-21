# BusinessCraft Production System Technical Specification

## Overview

The Production System is a comprehensive manufacturing and resource generation system that transforms towns from simple resource collectors into complex, interdependent economic hubs. The system supports configurable production lines, tiered progression, and sophisticated resource management.

## Architecture

### Core Components

#### 1. TownProductionComponent
```java
public class TownProductionComponent implements TownComponent {
    private Map<String, ProductionLine> activeLines = new HashMap<>();
    private Map<String, ProductionProgress> setupProgress = new HashMap<>();
    private int productionTier = 0;
    private String townClass = "general";
    private Map<Item, Integer> wasteStorage = new HashMap<>();
    
    // NBT serialization
    public void save(CompoundTag tag);
    public void load(CompoundTag tag);
    
    // Production management
    public boolean activateLine(String lineId, ProductionManager manager);
    public void deactivateLine(String lineId);
    public void processDailyProduction(Town town);
}
```

#### 2. ProductionLine (Simplified)
```java
public class ProductionLine {
    private String id;
    private String producedBy;
    private int baseRate;
    private Item outputItem;
    private Map<Item, Double> inputs;
    private int timeToSetup;
    private String description;
    
    // Validation
    public boolean canBeProducedBy(String townClass);
    public boolean hasRequiredResources(Town town);
}
```

#### 3. ProductionProgress
```java
public class ProductionProgress {
    private String lineId;
    private long startTime;
    private int setupTimeSeconds;
    private boolean isActive;
    
    public boolean isSetupComplete();
    public int getRemainingSetupTime();
    public void activate();
}
```

#### 4. ProductionManager
```java
public class ProductionManager {
    private static ProductionManager instance;
    private Map<String, ProductionLine> availableLines = new HashMap<>();
    private Map<UUID, TownProductionComponent> townProductions = new HashMap<>();
    
    // Singleton pattern
    public static ProductionManager getInstance();
    
    // Daily processing
    public void processDailyProduction(ServerLevel level);
    public void processTownProduction(Town town);
    
    // Line management
    public boolean canActivateLine(Town town, String lineId);
    public void activateProductionLine(Town town, String lineId);
    public void deactivateProductionLine(Town town, String lineId);
    
    // Configuration
    public void loadProductionLines();
    public void validateProductionLines();
}
```

## Configuration System

### TOML Format
The production system uses TOML configuration files for easy modification and modpack integration.

#### Required Fields (Initial Implementation)
- `produced_by`: Town class restriction ("logging", "farming", "mining", "tourism", "any")
- `base_rate`: Items produced per Minecraft day
- `output_item`: Primary product (vanilla or modded item ID)
- `inputs`: Required resources with consumption rates
- `time_to_setup`: Seconds required to activate production line
- `description`: Human-readable description

#### Future Fields (Phase 4+)
- `tier`: Production complexity (0-5)
- `prerequisites`: Research requirements from research.toml
- `path`: Production category for organization
- `waste_output`: Byproducts that must be managed
- `vanilla_link`: Associated villager profession
- `population_bonus`: Production scaling with population

### Configuration Loading
```java
public class ProductionConfigLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductionConfigLoader.class);
    private final Map<String, ProductionLine> productionLines = new HashMap<>();
    
    public void loadProductionConfig(Path configPath);
    public ProductionLine parseProductionLine(String id, TomlTable table);
    public Item validateItemId(String itemId);
    public void registerWithHotReload();
}
```

## Integration Points

### Town System Integration
```java
// Add to Town.java
private final TownProductionComponent production = new TownProductionComponent();

public TownProductionComponent getProductionComponent() {
    return production;
}

// Integrate with existing resource system
@Override
public void addResource(Item item, int count) {
    economy.addResource(item, count);
    // Trigger production processing if needed
    production.checkProductionTriggers(this, item);
}
```

### UI Integration
```java
public class ProductionTab extends BaseTownTab {
    private final List<ProductionLineComponent> lineComponents = new ArrayList<>();
    private final ProductionLineFilter filter = new ProductionLineFilter();
    
    @Override
    public void buildContent(BCScreenBuilder builder) {
        // Build production line list
        // Add activation controls
        // Show progress indicators
        // Display efficiency metrics
    }
}
```

### Network Integration
```java
// New packet types
public class ProductionSyncPacket extends BaseBlockEntityPacket {
    private Map<String, ProductionLineStatus> lineStatuses;
    private Map<Item, Integer> wasteStorage;
    private int productionTier;
}

public class ProductionLineActivationPacket extends BaseBlockEntityPacket {
    private String lineId;
    private boolean activate;
}
```

## Production Processing

### Daily Production Cycle
1. **Resource Check**: Verify required inputs are available
2. **Consumption**: Remove consumed resources from town storage
3. **Production**: Generate output items based on base rate and modifiers
4. **Waste Generation**: Create waste products if configured
5. **Population Scaling**: Apply population-based bonuses
6. **Storage**: Add products to town resource storage

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

### Waste Management (Future Feature)
```java
// This will be implemented in Phase 4.3
public void processWasteOutput(ProductionLine line, int productionAmount, Town town) {
    // Future implementation
    // Will handle waste_output field from configuration
}
```

## Performance Considerations

### Optimization Strategies
1. **Batch Processing**: Process all towns' production in a single tick
2. **Caching**: Cache production line lookups and calculations
3. **Lazy Loading**: Load production configurations on demand
4. **Efficient Storage**: Use optimized data structures for resource tracking

### Performance Monitoring
```java
public class ProductionMetrics {
    private long lastProcessingTime;
    private int townsProcessed;
    private int totalProductionLines;
    
    public void recordProcessingTime(long timeMs);
    public void generatePerformanceReport();
}
```

## Error Handling

### Configuration Validation
```java
public class ProductionConfigValidator {
    public List<ConfigError> validateProductionLine(String id, ProductionLine line);
    public boolean validateItemRegistry(String itemId);
    public boolean validatePrerequisites(List<String> prerequisites);
}
```

### Production Error Recovery
```java
public class ProductionErrorHandler {
    public void handleInsufficientResources(Town town, ProductionLine line);
    public void handleWasteOverflow(Town town, Item wasteItem);
    public void handleInvalidConfiguration(String lineId, String error);
}
```

## Testing Strategy

### Unit Tests
- Production line parsing and validation
- Production rate calculations
- Resource consumption and generation
- Waste management logic

### Integration Tests
- Town production component integration
- UI production tab functionality
- Network packet synchronization
- Configuration hot-reload

### Performance Tests
- Daily production processing with 100+ towns
- Memory usage during production calculations
- Network packet size optimization

## Future Enhancements

### Phase 2 Features
1. **Inter-town Trade**: Automated resource exchange between specialized towns
2. **Quality System**: Different quality outputs based on input quality
3. **Efficiency Upgrades**: Town upgrades that improve production efficiency
4. **Market System**: Dynamic pricing based on supply and demand

### Phase 3 Features
1. **Production Chains**: Multi-step production processes
2. **Specialization Bonuses**: Town specialization affects production rates
3. **Environmental Factors**: Biome and climate affect production
4. **Advanced Waste Management**: Waste processing and recycling chains

## Migration Strategy

### Backward Compatibility
- Existing towns start with tier 0 production capability
- No existing functionality is removed or changed
- Production system is additive to current town mechanics

### Gradual Rollout
1. **Phase 1**: Basic production lines (tier 0-1)
2. **Phase 2**: Advanced production with waste management
3. **Phase 3**: Population scaling and vanilla integration
4. **Phase 4**: Inter-town trade and advanced features

## Success Metrics

### Functional Requirements
- [ ] Production lines generate resources based on configuration
- [ ] Resource consumption works correctly
- [ ] Waste management functions properly
- [ ] Population scaling applies correctly
- [ ] UI displays production status accurately

### Performance Requirements
- [ ] Daily processing completes within 50ms for 100 towns
- [ ] Memory usage remains stable during production calculations
- [ ] Network packets are optimized for minimal bandwidth usage
- [ ] Configuration hot-reload works without performance impact

### User Experience Requirements
- [ ] Production system is intuitive and easy to understand
- [ ] UI provides clear feedback on production status
- [ ] Configuration is easy to modify for modpack creators
- [ ] Error messages are helpful and actionable 