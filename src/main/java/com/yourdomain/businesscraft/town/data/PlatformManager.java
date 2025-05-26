package com.yourdomain.businesscraft.town.data;

import com.yourdomain.businesscraft.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Manager class for platform operations.
 * Extracted from TownBlockEntity to improve code organization.
 * 
 * This class handles platform storage, creation, modification, and state management
 * while maintaining separation from the main block entity.
 */
public class PlatformManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformManager.class);
    private static final int MAX_PLATFORMS = 5;
    
    // Platform storage
    private final List<Platform> platforms = new ArrayList<>();
    private final List<Platform> clientPlatforms = new ArrayList<>();
    
    // Platform creation state
    private boolean isInPlatformCreationMode = false;
    private UUID platformBeingEdited = null;
    
    // Callback for notifying when changes occur
    private Runnable changeCallback;
    
    /**
     * Sets the callback to be invoked when platform data changes
     * @param callback The callback to invoke on changes
     */
    public void setChangeCallback(Runnable callback) {
        this.changeCallback = callback;
    }
    
    /**
     * Notifies that platform data has changed
     */
    private void notifyChanged() {
        if (changeCallback != null) {
            changeCallback.run();
        }
    }
    
    /**
     * Gets the list of all platforms
     * @param isClientSide Whether this is being called from client side
     * @return List of platforms
     */
    public List<Platform> getPlatforms(boolean isClientSide) {
        if (isClientSide) {
            return new ArrayList<>(clientPlatforms);
        }
        return new ArrayList<>(platforms);
    }
    
    /**
     * Adds a new platform
     * @return true if added successfully, false if at max capacity
     */
    public boolean addPlatform() {
        if (platforms.size() >= MAX_PLATFORMS) {
            return false;
        }
        
        Platform platform = new Platform();
        platform.setName("Platform " + (platforms.size() + 1));
        platforms.add(platform);
        notifyChanged();
        return true;
    }
    
    /**
     * Removes a platform by ID
     * @param platformId The ID of the platform to remove
     * @return true if removed, false if not found
     */
    public boolean removePlatform(UUID platformId) {
        boolean removed = platforms.removeIf(p -> p.getId().equals(platformId));
        if (removed) {
            notifyChanged();
        }
        return removed;
    }
    
    /**
     * Gets a platform by ID
     * @param platformId The platform ID
     * @return The platform, or null if not found
     */
    public Platform getPlatform(UUID platformId) {
        return platforms.stream()
            .filter(p -> p.getId().equals(platformId))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Sets the path start for a specific platform
     * @param platformId The platform ID
     * @param pos The start position
     * @return true if successful, false if platform not found
     */
    public boolean setPlatformPathStart(UUID platformId, BlockPos pos) {
        Platform platform = getPlatform(platformId);
        if (platform != null) {
            platform.setStartPos(pos);
            notifyChanged();
            return true;
        }
        return false;
    }
    
    /**
     * Sets the path end for a specific platform
     * @param platformId The platform ID
     * @param pos The end position
     * @return true if successful, false if platform not found
     */
    public boolean setPlatformPathEnd(UUID platformId, BlockPos pos) {
        Platform platform = getPlatform(platformId);
        if (platform != null) {
            platform.setEndPos(pos);
            notifyChanged();
            return true;
        }
        return false;
    }
    
    /**
     * Toggles a platform's enabled state
     * @param platformId The platform ID
     * @return true if successful, false if platform not found
     */
    public boolean togglePlatformEnabled(UUID platformId) {
        Platform platform = getPlatform(platformId);
        if (platform != null) {
            platform.setEnabled(!platform.isEnabled());
            notifyChanged();
            return true;
        }
        return false;
    }
    
    /**
     * Sets whether we're in platform path creation mode
     * @param mode Whether creation mode is enabled
     * @param platformId The platform being edited (null if mode is false)
     */
    public void setPlatformCreationMode(boolean mode, UUID platformId) {
        this.isInPlatformCreationMode = mode;
        this.platformBeingEdited = mode ? platformId : null;
    }
    
    /**
     * Gets whether we're in platform path creation mode
     * @return true if in creation mode
     */
    public boolean isInPlatformCreationMode() {
        return isInPlatformCreationMode;
    }
    
    /**
     * Gets the ID of the platform currently being edited
     * @return The platform ID, or null if none
     */
    public UUID getPlatformBeingEdited() {
        return platformBeingEdited;
    }
    
    /**
     * Checks if we can add more platforms
     * @return true if more platforms can be added
     */
    public boolean canAddMorePlatforms() {
        return platforms.size() < MAX_PLATFORMS;
    }
    
    /**
     * Gets the maximum number of platforms allowed
     * @return The maximum platform count
     */
    public int getMaxPlatforms() {
        return MAX_PLATFORMS;
    }
    
    /**
     * Gets the current number of platforms
     * @return The current platform count
     */
    public int getPlatformCount() {
        return platforms.size();
    }
    
    /**
     * Saves platform data to NBT
     * @param tag The compound tag to save to
     */
    public void saveToNBT(CompoundTag tag) {
        if (!platforms.isEmpty()) {
            ListTag platformsTag = new ListTag();
            for (Platform platform : platforms) {
                platformsTag.add(platform.toNBT());
            }
            tag.put("platforms", platformsTag);
        }
    }
    
    /**
     * Loads platform data from NBT
     * @param tag The compound tag to load from
     */
    public void loadFromNBT(CompoundTag tag) {
        platforms.clear();
        if (tag.contains("platforms")) {
            ListTag platformsTag = tag.getList("platforms", Tag.TAG_COMPOUND);
            for (int i = 0; i < platformsTag.size(); i++) {
                CompoundTag platformTag = platformsTag.getCompound(i);
                platforms.add(Platform.fromNBT(platformTag));
            }
        }
    }
    
    /**
     * Updates client-side platform cache
     * @param tag The compound tag containing platform data
     */
    public void updateClientPlatforms(CompoundTag tag) {
        clientPlatforms.clear();
        if (tag.contains("platforms")) {
            ListTag platformsTag = tag.getList("platforms", Tag.TAG_COMPOUND);
            for (int i = 0; i < platformsTag.size(); i++) {
                CompoundTag platformTag = platformsTag.getCompound(i);
                Platform platform = Platform.fromNBT(platformTag);
                clientPlatforms.add(platform);
            }
        }
    }
    
    /**
     * Creates a legacy platform from old path data if no platforms exist
     * @param pathStart The legacy path start
     * @param pathEnd The legacy path end
     */
    public void createLegacyPlatform(BlockPos pathStart, BlockPos pathEnd) {
        if (platforms.isEmpty() && pathStart != null && pathEnd != null) {
            Platform legacyPlatform = new Platform("Main Platform", true, pathStart, pathEnd);
            platforms.add(legacyPlatform);
            notifyChanged();
            LOGGER.info("Created legacy platform from old path data: {} to {}", pathStart, pathEnd);
        }
    }
    
    /**
     * Gets all enabled and complete platforms
     * @return List of enabled and complete platforms
     */
    public List<Platform> getEnabledPlatforms() {
        return platforms.stream()
            .filter(p -> p.isEnabled() && p.isComplete())
            .toList();
    }
    
    /**
     * Clears all platform data (useful for cleanup)
     */
    public void clear() {
        platforms.clear();
        clientPlatforms.clear();
        isInPlatformCreationMode = false;
        platformBeingEdited = null;
    }
} 