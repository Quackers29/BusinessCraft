package com.quackers29.businesscraft.platform;

import com.quackers29.businesscraft.api.ITownDataProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Forge implementation of PlatformService.
 * Provides platform-specific functionality for the common business logic.
 */
public class ForgePlatformService implements PlatformService {
    
    private final ItemService itemService = new ForgeItemService();
    private final WorldService worldService = new ForgeWorldService();
    private final PositionFactory positionFactory = new ForgePositionFactory();
    private final DataSerializationService dataService = new ForgeDataSerializationService();
    
    @Override
    public ItemService getItemService() {
        return itemService;
    }
    
    @Override
    public WorldService getWorldService() {
        return worldService;
    }
    
    @Override
    public PositionFactory getPositionFactory() {
        return positionFactory;
    }
    
    @Override
    public DataSerializationService getDataSerializationService() {
        return dataService;
    }
    
    private static class ForgeItemService implements ItemService {
        @Override
        public Object getBreadItem() {
            return Items.BREAD;
        }
        
        @Override
        public String getItemDisplayName(Object item) {
            if (item instanceof Item) {
                return ((Item) item).getDescription().getString();
            }
            return item != null ? item.toString() : "Unknown";
        }
        
        @Override
        public boolean areItemsEqual(Object item1, Object item2) {
            return item1 == item2; // Items are singletons in Minecraft
        }
    }
    
    private static class ForgeWorldService implements WorldService {
        @Override
        public boolean isPositionLoaded(ITownDataProvider.Position position) {
            // For demonstration - in real implementation would check world chunks
            return position != null;
        }
        
        @Override
        public double calculateDistance(ITownDataProvider.Position pos1, ITownDataProvider.Position pos2) {
            if (pos1 == null || pos2 == null) return 0.0;
            
            double dx = pos1.getX() - pos2.getX();
            double dy = pos1.getY() - pos2.getY();
            double dz = pos1.getZ() - pos2.getZ();
            
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
    }
    
    private static class ForgePositionFactory implements PositionFactory {
        @Override
        public ITownDataProvider.Position createPosition(int x, int y, int z) {
            return new ForgePosition(x, y, z);
        }
        
        @Override
        public ITownDataProvider.Position fromPlatformPosition(Object platformPosition) {
            if (platformPosition instanceof BlockPos) {
                return new ForgePosition((BlockPos) platformPosition);
            }
            throw new IllegalArgumentException("Expected BlockPos, got: " + 
                (platformPosition != null ? platformPosition.getClass().getSimpleName() : "null"));
        }
    }
    
    private static class ForgeDataSerializationService implements DataSerializationService {
        @Override
        public Object createDataContainer() {
            return new CompoundTag();
        }
        
        @Override
        public void saveString(Object container, String key, String value) {
            if (container instanceof CompoundTag) {
                ((CompoundTag) container).putString(key, value);
            }
        }
        
        @Override
        public String loadString(Object container, String key) {
            if (container instanceof CompoundTag) {
                return ((CompoundTag) container).getString(key);
            }
            return "";
        }
        
        @Override
        public void saveInt(Object container, String key, int value) {
            if (container instanceof CompoundTag) {
                ((CompoundTag) container).putInt(key, value);
            }
        }
        
        @Override
        public int loadInt(Object container, String key) {
            if (container instanceof CompoundTag) {
                return ((CompoundTag) container).getInt(key);
            }
            return 0;
        }
        
        @Override
        public void saveLong(Object container, String key, long value) {
            if (container instanceof CompoundTag) {
                ((CompoundTag) container).putLong(key, value);
            }
        }
        
        @Override
        public long loadLong(Object container, String key) {
            if (container instanceof CompoundTag) {
                return ((CompoundTag) container).getLong(key);
            }
            return 0L;
        }
    }
}