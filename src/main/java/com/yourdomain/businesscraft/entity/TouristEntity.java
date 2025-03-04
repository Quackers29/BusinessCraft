package com.yourdomain.businesscraft.entity;

import com.yourdomain.businesscraft.platform.Platform;
import com.yourdomain.businesscraft.town.Town;
import com.yourdomain.businesscraft.town.utils.TouristUtils;
import com.yourdomain.businesscraft.town.utils.TownNotificationUtils;
import com.yourdomain.businesscraft.config.ConfigLoader;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
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
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class TouristEntity extends Villager {
    // Special constant for "Any Town" destination
    public static final UUID ANY_TOWN_DESTINATION = new UUID(0, 0);
    public static final String ANY_TOWN_NAME = "Any Town";
    
    // Tourist expiry settings - calculate from ConfigLoader
    private static final int DEFAULT_EXPIRY_TICKS = ConfigLoader.touristExpiryMinutes * 60 * 20; // Convert minutes to ticks
    private int expiryTicks = DEFAULT_EXPIRY_TICKS;
    private boolean hasNotifiedOriginTown = false;
    
    // Tourist origin and destination information
    private UUID originTownId;
    private String originTownName;
    private UUID destinationTownId;
    private String destinationTownName;
    private long spawnTime;
    
    public TouristEntity(EntityType<? extends Villager> entityType, Level level) {
        super(entityType, level);
        this.spawnTime = level.getGameTime();
        
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
        
        // Handle expiry
        if (!this.level().isClientSide && ConfigLoader.enableTouristExpiry) {
            expiryTicks--;
            
            // Expiry reached, prepare to "quit"
            if (expiryTicks <= 0) {
                // Notify origin town if not already done
                if (!hasNotifiedOriginTown) {
                    notifyOriginTownOfQuitting();
                    hasNotifiedOriginTown = true;
                }
                
                // Remove the entity
                this.discard();
            }
        }
    }
    
    private void notifyOriginTownOfQuitting() {
        if (this.level() instanceof ServerLevel serverLevel && originTownId != null && ConfigLoader.notifyOnTouristDeparture) {
            // Use the notification utility to send a message to the origin town
            TownNotificationUtils.notifyTouristDeparture(
                serverLevel,
                originTownId,
                originTownName,
                destinationTownName != null ? destinationTownName : "unknown destination",
                false, // Not died, just quitting due to expiry
                this.blockPosition()
            );
        }
    }
    
    @Override
    public void die(net.minecraft.world.damagesource.DamageSource cause) {
        // Notify the origin town before dying if configured
        if (!this.level().isClientSide && originTownId != null && ConfigLoader.notifyOnTouristDeparture) {
            if (this.level() instanceof ServerLevel serverLevel) {
                TownNotificationUtils.notifyTouristDeparture(
                    serverLevel,
                    originTownId,
                    originTownName,
                    destinationTownName != null ? destinationTownName : "unknown destination",
                    true, // Died
                    this.blockPosition()
                );
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
        VillagerProfession[] professions = {
            VillagerProfession.ARMORER,
            VillagerProfession.BUTCHER,
            VillagerProfession.CARTOGRAPHER,
            VillagerProfession.CLERIC,
            VillagerProfession.FARMER,
            VillagerProfession.FISHERMAN,
            VillagerProfession.FLETCHER,
            VillagerProfession.LEATHERWORKER,
            VillagerProfession.LIBRARIAN,
            VillagerProfession.MASON,
            VillagerProfession.SHEPHERD,
            VillagerProfession.TOOLSMITH,
            VillagerProfession.WEAPONSMITH
        };
        VillagerProfession randomProfession = professions[this.getRandom().nextInt(professions.length)];
        this.setVillagerData(this.getVillagerData()
            .setProfession(randomProfession)
            .setLevel(6));
    }
    
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ExpiryTicks", expiryTicks);
        tag.putBoolean("HasNotifiedOrigin", hasNotifiedOriginTown);
        tag.putLong("SpawnTime", spawnTime);
        
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
} 