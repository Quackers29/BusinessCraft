# Phase 10.1.3 Common Module Business Logic Architecture

## 🏗️ ARCHITECTURE DESIGN

### **Core Design Principles**
1. **Zero Platform Dependencies** - Common module must compile without Forge or Fabric
2. **Service-Oriented Abstraction** - All platform operations through service interfaces
3. **Data Format Compatibility** - Identical NBT structure across platforms
4. **Single Source of Truth** - Business logic only in common module

## 📁 PROPOSED COMMON MODULE STRUCTURE

```
common/src/main/java/com/quackers29/businesscraft/
├── town/
│   ├── Town.java                 # Core business entity (migrated from forge)
│   ├── TownManager.java          # Business logic controller (migrated from forge)  
│   ├── components/
│   │   ├── TownEconomyComponent.java    # Already platform-agnostic
│   │   ├── TownResources.java          # Needs registry abstraction
│   │   └── VisitHistoryRecord.java     # Replaces ForgeVisitHistoryRecord
│   ├── data/
│   │   ├── ITownPersistence.java       # New: Platform-agnostic persistence
│   │   ├── TownDataManager.java        # New: Common data operations
│   │   └── [helpers migrated selectively]
│   └── service/
│       ├── TownBoundaryService.java    # Ready to migrate
│       ├── TownValidationService.java  # Ready to migrate
│       └── TownBusinessLogic.java      # Already exists
├── platform/
│   ├── InventoryHelper.java           # New: For ItemStackHandler abstraction
│   └── [existing platform services]
```

## 🔧 KEY INTERFACES TO CREATE

### **1. ITownPersistence Interface**
```java
/**
 * Platform-agnostic town data persistence interface.
 * Uses DataStorageHelper for actual platform-specific storage.
 */
public interface ITownPersistence {
    /**
     * Save town data to persistent storage
     * @param data Town data to save (platform-agnostic format)
     */
    void save(Map<String, Object> data);
    
    /**
     * Load town data from persistent storage
     * @return Loaded data in platform-agnostic format
     */
    Map<String, Object> load();
    
    /**
     * Mark data as dirty for next save cycle
     */
    void markDirty();
    
    /**
     * Get unique identifier for this persistence context
     */
    String getIdentifier();
}
```

### **2. InventoryHelper Interface** (extends platform services)
```java
/**
 * Platform-agnostic inventory operations.
 * Abstracts ItemStackHandler (Forge) vs equivalent Fabric systems.
 */
public interface InventoryHelper {
    /**
     * Create a new inventory container with specified slots
     */
    Object createInventory(int slots);
    
    /**
     * Serialize inventory to NBT-compatible format
     */
    Object serializeInventory(Object inventory);
    
    /**
     * Deserialize inventory from NBT-compatible format  
     */
    Object deserializeInventory(Object data);
    
    /**
     * Get item count from inventory
     */
    int getItemCount(Object inventory, Object item);
    
    /**
     * Add items to inventory
     */
    boolean addItem(Object inventory, Object item, int count);
}
```

### **3. Enhanced RegistryHelper** (extend existing)
```java
/**
 * Additional registry operations needed for town management
 */
public interface RegistryHelper {
    // [existing methods...]
    
    /**
     * Get item by resource location string
     */
    Object getItem(String resourceLocation);
    
    /**
     * Get resource location string from item
     */
    String getItemId(Object item);
    
    /**
     * Serialize item to NBT-compatible format
     */
    Object serializeItem(Object item);
    
    /**
     * Deserialize item from NBT-compatible format
     */
    Object deserializeItem(Object data);
}
```

## 🔄 DATA MIGRATION STRATEGY

### **Town Data Structure** (Platform-Agnostic)
```java
public class Town {
    // Core data (already platform-agnostic)
    private UUID id;
    private int[] position;  // BlockPos → int array [x, y, z]
    private String name;
    
    // Replace platform-specific components
    private List<VisitHistoryRecord> visitHistory;  // Not ForgeVisitHistoryRecord
    private Map<String, Integer> resources;         // item ID string → count
    private Map<String, Object> persistentData;     // Generic data storage
    
    // Use platform services for operations
    private transient ITownPersistence persistence;
}
```

### **TownManager Architecture** (Platform-Agnostic)
```java
public class TownManager {
    // Use DataStorageHelper instead of direct SavedData
    private final Object level;  // ServerLevel (platform-agnostic reference)
    private final ITownPersistence persistence;
    
    public TownManager(Object level) {
        this.level = level;
        // Use platform services for persistence creation
        this.persistence = PlatformServices.getDataStorageHelper()
            .createTownPersistence(level, "businesscraft_towns");
    }
    
    // All business logic methods remain the same
    // But use platform services for platform-specific operations
}
```

## 🔧 MIGRATION IMPLEMENTATION STEPS

### **Step 1: Create Foundation Interfaces**
1. Create `ITownPersistence.java` in common module
2. Enhance `RegistryHelper.java` with additional methods
3. Create `InventoryHelper.java` interface
4. Add these to `PlatformServices.java`

### **Step 2: Platform Service Implementations**
1. **Forge implementations**:
   - `ForgeTownPersistence` using SavedData + DataStorageHelper
   - Enhanced `ForgeRegistryHelper` with item operations
   - `ForgeInventoryHelper` using ItemStackHandler
   
2. **Fabric implementations**:
   - `FabricTownPersistence` using PersistentState + DataStorageHelper  
   - Enhanced `FabricRegistryHelper` with item operations
   - `FabricInventoryHelper` using Fabric inventory systems

### **Step 3: Business Logic Migration**
1. Create `VisitHistoryRecord.java` (replace ForgeVisitHistoryRecord)
2. Move and adapt `Town.java` with platform service usage
3. Move and adapt `TownManager.java` with persistence abstraction
4. Update all helper classes to use platform services

### **Step 4: Integration Points**
1. Update platform modules to use common TownManager
2. Remove reflection-based services (`FabricTownManagerService`)
3. Test compilation and basic functionality

## ✅ VALIDATION APPROACH

### **Compilation Tests**
- Common module compiles with zero platform dependencies
- Both platform modules compile with common integration

### **Functionality Tests**
- Forge: Existing save data loads correctly
- Fabric: Can create and persist towns
- Both: Identical NBT save format

### **Integration Tests**  
- Cross-platform save compatibility
- Feature parity verification
- Performance comparison

## 🎯 SUCCESS METRICS

1. **Architecture Compliance**: Common module = 0 platform dependencies
2. **Feature Parity**: Forge functionality == Fabric functionality  
3. **Data Compatibility**: Cross-platform save file loading
4. **Code Quality**: Single business logic codebase maintained
5. **Performance**: No regression on either platform

This architecture design provides a clear roadmap for achieving true Enhanced MultiLoader Template compliance with full Fabric functionality parity.