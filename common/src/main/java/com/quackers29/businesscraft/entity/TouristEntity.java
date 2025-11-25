package com.quackers29.businesscraft.entity;

import com.quackers29.businesscraft.platform.Platform;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.utils.TouristUtils;
import com.quackers29.businesscraft.town.utils.TownNotificationUtils;
import com.quackers29.businesscraft.config.ConfigLoader;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import com.quackers29.businesscraft.debug.DebugConfig;

public class TouristEntity extends Villager {
    private static final Logger LOGGER = LoggerFactory.getLogger(TouristEntity.class);
    
    // Special constant for "Any Town" destination
    public static final UUID ANY_TOWN_DESTINATION = new UUID(0, 0);
    public static final String ANY_TOWN_NAME = "Any Town";
    
    // Tourist expiry settings - calculate from ConfigLoader
    private static final int DEFAULT_EXPIRY_TICKS = (int)(ConfigLoader.touristExpiryMinutes * 60 * 20); // Convert minutes to ticks
    private int expiryTicks = DEFAULT_EXPIRY_TICKS;
    private boolean hasNotifiedOriginTown = false;
    
    // Flag to track if tourist has already received a ride extension
    private boolean hasReceivedRideExtension = false;
    
    // Tourist origin and destination information
    private UUID originTownId;
    private String originTownName;
    private UUID destinationTownId;
    private String destinationTownName;
    private long spawnTime;
    
    // Track initial spawn position to determine if tourist has moved
    private double spawnPosX;
    private double spawnPosY;
    private double spawnPosZ;
    private boolean hasMoved = false;
    private static final double MOVEMENT_THRESHOLD = 2.0; // Consider moved if more than 2 blocks away from spawn
    
    // Track recent position to determine if tourist is currently moving (for expiry logic)
    private double recentPosX;
    private double recentPosY;
    private double recentPosZ;
    private int positionUpdateTicks = 0;
    private boolean isCurrentlyStationary = true; // Track current movement state between checks
    private static final int POSITION_UPDATE_INTERVAL = 20; // Check movement every 20 ticks (1 second)
    private static final double STATIONARY_THRESHOLD = 0.5; // Consider stationary if moved less than 0.5 blocks in 1 second

    public TouristEntity(EntityType<? extends Villager> entityType, Level level) {
        super(entityType, level);
        this.spawnTime = level.getGameTime();
        
        // Default constructor - position will be set properly by setPos later
        // We'll update spawnPosX, spawnPosY, spawnPosZ when setPos is called the first time
        
        // Reduce speed significantly to make tourists very slow
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.000001);
    }
    
    public TouristEntity(EntityType<? extends Villager> entityType, Level level, 
                        Town originTown, Platform platform,
                        UUID destinationTownId, String destinationName) {
        this(entityType, level);
        
        // Store origin town information
        if (originTown != null) {
            this.originTownId = originTown.getId();
            this.originTownName = originTown.getName();
        }
        
        // Store destination information
        this.destinationTownId = destinationTownId;
        this.destinationTownName = destinationName;
        
        // Set display name
        String displayName;
        if (destinationTownId.equals(ANY_TOWN_DESTINATION)) {
            displayName = "Tourist to Any Town";
            this.destinationTownName = ANY_TOWN_NAME;
        } else {
            displayName = "Tourist to " + destinationName;
        }
        this.setCustomName(Component.literal(displayName));
        
        // Add standard tourist tags
        TouristUtils.addStandardTouristTags(
            this, 
            originTown, 
            platform, 
            destinationTownId.toString(), 
            destinationName
        );
        
        // Set random profession and max level
        this.setRandomProfession();
    }
    
    @Override
    protected void registerGoals() {
        // Simplified goals for tourists - mainly just looking around
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 5.0F));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.3D));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
        
        // We deliberately omit many typical villager goals like trading,
        // farming, working, breeding, panic, etc.
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // Check if the tourist has moved from spawn position
        if (!hasMoved && !this.level().isClientSide) {
            double dx = this.getX() - this.spawnPosX;
            double dy = this.getY() - this.spawnPosY;
            double dz = this.getZ() - this.spawnPosZ;
            double distanceSquared = dx * dx + dy * dy + dz * dz;
            
            if (distanceSquared > MOVEMENT_THRESHOLD * MOVEMENT_THRESHOLD) {
                hasMoved = true;
                DebugConfig.debug(LOGGER, DebugConfig.TOURIST_ENTITY, "Tourist has moved from spawn position, distance: {}", Math.sqrt(distanceSquared));
            }
        }
        
        // Handle expiry - only expire when stationary (regardless of riding state)
        if (!this.level().isClientSide && ConfigLoader.enableTouristExpiry) {
            positionUpdateTicks++;
            
            // Check if tourist is currently moving every POSITION_UPDATE_INTERVAL ticks
            if (positionUpdateTicks >= POSITION_UPDATE_INTERVAL) {
                double dx = this.getX() - this.recentPosX;
                double dy = this.getY() - this.recentPosY;
                double dz = this.getZ() - this.recentPosZ;
                double recentMovementSquared = dx * dx + dy * dy + dz * dz;
                double distanceMoved = Math.sqrt(recentMovementSquared);
                
                // Update stationary state based on movement
                isCurrentlyStationary = recentMovementSquared < (STATIONARY_THRESHOLD * STATIONARY_THRESHOLD);
                
                // Update recent position for next check
                this.recentPosX = this.getX();
                this.recentPosY = this.getY();
                this.recentPosZ = this.getZ();
                positionUpdateTicks = 0;
                
                DebugConfig.debug(LOGGER, DebugConfig.TOURIST_ENTITY, "Tourist {} movement check: distance={:.3f}, stationary={}, riding={}, expiry={}s", 
                    this.getId(), distanceMoved, isCurrentlyStationary, this.isPassenger(), expiryTicks/20);
            }
            
            // Only decrement expiry timer when tourist is stationary
            if (isCurrentlyStationary) {
                expiryTicks--;
                
                // Expiry reached, prepare to "quit"
                if (expiryTicks <= 0) {
                    DebugConfig.debug(LOGGER, DebugConfig.TOURIST_ENTITY, "Tourist {} expired after being stationary", this.getId());
                    
                    // Notify origin town if not already done
                    if (!hasNotifiedOriginTown) {
                        notifyOriginTownOfQuitting();
                        hasNotifiedOriginTown = true;
                    }
                    
                    // Remove the entity
                    this.discard();
                }
            } else {
                // Debug every few seconds when moving to avoid spam
                if (level().getGameTime() % 100 == 0) { // Every 5 seconds
                    DebugConfig.debug(LOGGER, DebugConfig.TOURIST_ENTITY, "Tourist {} is moving, expiry timer paused at {}s", 
                        this.getId(), expiryTicks/20);
                }
            }
        }
    }
    
    private void notifyOriginTownOfQuitting() {
        if (this.level() instanceof ServerLevel serverLevel && originTownId != null) {
            // Always update the origin town's tourist count to ensure proper replenishment
            Town originTown = TownNotificationUtils.removeTouristFromOrigin(serverLevel, originTownId);
            
            // Only send visual notifications if tourist has moved from spawn or notification is disabled
            if (hasMoved && ConfigLoader.notifyOnTouristDeparture && originTown != null) {
                // Display notification only if the tourist has moved from spawn position
                TownNotificationUtils.displayTouristDepartureNotification(
                    serverLevel,
                    originTown,
                    originTownName,
                    destinationTownName != null ? destinationTownName : "unknown destination",
                    false, // Not died, just quitting due to expiry
                    this.blockPosition()
                );
            } else {
                DebugConfig.debug(LOGGER, DebugConfig.TOURIST_ENTITY, "Tourist quit without moving from spawn position, skipping visual notification");
            }
        }
    }
    
    @Override
    public void die(net.minecraft.world.damagesource.DamageSource cause) {
        // Notify the origin town before dying
        if (!this.level().isClientSide && originTownId != null) {
            if (this.level() instanceof ServerLevel serverLevel) {
                // Always update the origin town's tourist count
                Town originTown = TownNotificationUtils.removeTouristFromOrigin(serverLevel, originTownId);
                
                // Always show visual notification for deaths regardless of movement
                if (ConfigLoader.notifyOnTouristDeparture && originTown != null) {
                    TownNotificationUtils.displayTouristDepartureNotification(
                        serverLevel,
                        originTown,
                        originTownName,
                        destinationTownName != null ? destinationTownName : "unknown destination",
                        true, // Died
                        this.blockPosition()
                    );
                }
                
                // Mark as notified to prevent double-counting
                hasNotifiedOriginTown = true;
            }
        }
        
        super.die(cause);
    }
    
    public void setExpiryTicks(int ticks) {
        this.expiryTicks = ticks;
    }
    
    public int getExpiryTicks() {
        return this.expiryTicks;
    }
    
    public long getSpawnTime() {
        return this.spawnTime;
    }
    
    public UUID getOriginTownId() {
        return this.originTownId;
    }
    
    public String getOriginTownName() {
        return this.originTownName;
    }
    
    public UUID getDestinationTownId() {
        return this.destinationTownId;
    }
    
    public String getDestinationTownName() {
        return this.destinationTownName;
    }
    
    private void setRandomProfession() {
        // Set tourists to unemployed villagers for base clothing look
        this.setVillagerData(this.getVillagerData()
            .setProfession(VillagerProfession.NONE)
            .setLevel(1));
    }
    
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ExpiryTicks", expiryTicks);
        tag.putBoolean("HasNotifiedOrigin", hasNotifiedOriginTown);
        tag.putLong("SpawnTime", spawnTime);
        tag.putBoolean("HasReceivedRideExtension", hasReceivedRideExtension);
        tag.putDouble("SpawnPosX", spawnPosX);
        tag.putDouble("SpawnPosY", spawnPosY);
        tag.putDouble("SpawnPosZ", spawnPosZ);
        tag.putBoolean("HasMoved", hasMoved);
        tag.putDouble("RecentPosX", recentPosX);
        tag.putDouble("RecentPosY", recentPosY);
        tag.putDouble("RecentPosZ", recentPosZ);
        tag.putInt("PositionUpdateTicks", positionUpdateTicks);
        tag.putBoolean("IsCurrentlyStationary", isCurrentlyStationary);
        
        if (originTownId != null) {
            tag.putUUID("OriginTownId", originTownId);
        }
        if (originTownName != null) {
            tag.putString("OriginTownName", originTownName);
        }
        if (destinationTownId != null) {
            tag.putUUID("DestinationTownId", destinationTownId);
        }
        if (destinationTownName != null) {
            tag.putString("DestinationTownName", destinationTownName);
        }
    }
    
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        
        if (tag.contains("ExpiryTicks")) {
            expiryTicks = tag.getInt("ExpiryTicks");
        }
        if (tag.contains("HasNotifiedOrigin")) {
            hasNotifiedOriginTown = tag.getBoolean("HasNotifiedOrigin");
        }
        if (tag.contains("SpawnTime")) {
            spawnTime = tag.getLong("SpawnTime");
        }
        if (tag.contains("HasReceivedRideExtension")) {
            hasReceivedRideExtension = tag.getBoolean("HasReceivedRideExtension");
        }
        if (tag.contains("SpawnPosX")) {
            spawnPosX = tag.getDouble("SpawnPosX");
        }
        if (tag.contains("SpawnPosY")) {
            spawnPosY = tag.getDouble("SpawnPosY");
        }
        if (tag.contains("SpawnPosZ")) {
            spawnPosZ = tag.getDouble("SpawnPosZ");
        }
        if (tag.contains("HasMoved")) {
            hasMoved = tag.getBoolean("HasMoved");
        }
        if (tag.contains("RecentPosX")) {
            recentPosX = tag.getDouble("RecentPosX");
        }
        if (tag.contains("RecentPosY")) {
            recentPosY = tag.getDouble("RecentPosY");
        }
        if (tag.contains("RecentPosZ")) {
            recentPosZ = tag.getDouble("RecentPosZ");
        }
        if (tag.contains("PositionUpdateTicks")) {
            positionUpdateTicks = tag.getInt("PositionUpdateTicks");
        }
        if (tag.contains("IsCurrentlyStationary")) {
            isCurrentlyStationary = tag.getBoolean("IsCurrentlyStationary");
        }
        
        if (tag.contains("OriginTownId")) {
            originTownId = tag.getUUID("OriginTownId");
        }
        if (tag.contains("OriginTownName")) {
            originTownName = tag.getString("OriginTownName");
        }
        if (tag.contains("DestinationTownId")) {
            destinationTownId = tag.getUUID("DestinationTownId");
        }
        if (tag.contains("DestinationTownName")) {
            destinationTownName = tag.getString("DestinationTownName");
        }
    }
    
    // Disable breeding more thoroughly by preventing the production of baby entities
    @Nullable
    @Override
    public Villager getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return null; // Return null to prevent breeding
    }
    
    // Override the canBreed method to ensure these entities never breed
    @Override
    public boolean canBreed() {
        return false;
    }
    
    // Prevent tourists from picking up items
    @Override
    public boolean wantsToPickUp(ItemStack itemStack) {
        return false;
    }
    
    // Factory method for attribute builder
    public static AttributeSupplier.Builder createAttributes() {
        return Villager.createAttributes();
    }
    
    public static TouristEntity create(EntityType<TouristEntity> entityType, Level level) {
        return new TouristEntity(entityType, level);
    }
    
    
    @Override
    public boolean startRiding(Entity entity, boolean force) {
        boolean result = super.startRiding(entity, force);
        
        if (result && !hasReceivedRideExtension) {
            // Check if the vehicle is a minecart or Create train carriage
            if (entity instanceof AbstractMinecart || entity.getClass().getName().contains("create.content.trains")) {
                // Recalculate expiry ticks from current config value instead of using static constant
                this.expiryTicks = (int)(ConfigLoader.touristExpiryMinutes * 60 * 20);
                hasReceivedRideExtension = true;
                DebugConfig.debug(LOGGER, DebugConfig.TOURIST_ENTITY, "Resetting expiry timer for tourist to {} minutes ({})", 
                    ConfigLoader.touristExpiryMinutes, this.expiryTicks);
            }
        }
        
        return result;
    }
    
    // For simpler mounting calls
    @Override
    public boolean startRiding(Entity entity) {
        return this.startRiding(entity, false);
    }
    
    /**
     * Override setPos to initialize the spawn position on first call
     * This ensures we capture the actual spawn position
     */
    @Override
    public void setPos(double x, double y, double z) {
        // If spawn position hasn't been set yet (all values are 0), this is our first positioning
        // So we store it as the spawn position
        if (spawnPosX == 0 && spawnPosY == 0 && spawnPosZ == 0) {
            this.spawnPosX = x;
            this.spawnPosY = y;
            this.spawnPosZ = z;
            
            // Also initialize recent position for movement tracking
            this.recentPosX = x;
            this.recentPosY = y;
            this.recentPosZ = z;
        }
        
        // Call the parent implementation to actually set the position
        super.setPos(x, y, z);
    }
} 
