package com.quackers29.businesscraft.entity;

import com.quackers29.businesscraft.platform.Platform;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.town.utils.TownNotificationUtils;
import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.economy.JourneyProgress;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.world.phys.AABB;
import com.quackers29.businesscraft.debug.DebugConfig;
import com.quackers29.businesscraft.entity.tourist.TouristAppearance;
import com.quackers29.businesscraft.entity.tourist.TouristLook;
import com.quackers29.businesscraft.entity.tourist.TouristLooks;

public class TouristEntity extends Villager {
    private static final Logger LOGGER = LoggerFactory.getLogger(TouristEntity.class);
    /** Lowers the humanoid model into minecarts and Create seats (villager default offset is too high). */
    private static final double RIDING_Y_OFFSET = -0.2625D;

    // Synced entity data for live updates on client
    private static final EntityDataAccessor<Float> DATA_DISTANCE_TRAVELED =
        SynchedEntityData.defineId(TouristEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_SKIN_TIER =
        SynchedEntityData.defineId(TouristEntity.class, EntityDataSerializers.INT);
    /** Packed skin/outfit/headwear/accessory. Chosen on the server, synced to clients. */
    private static final EntityDataAccessor<Integer> DATA_LOOK =
        SynchedEntityData.defineId(TouristEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TOURIST_TYPE =
        SynchedEntityData.defineId(TouristEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_ORIGIN_LABEL =
        SynchedEntityData.defineId(TouristEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_DEST_LABEL =
        SynchedEntityData.defineId(TouristEntity.class, EntityDataSerializers.STRING);

    public static final int SKIN_TIER_BASIC = 0;
    public static final int SKIN_TIER_EXPERIENCED = 1;
    public static final int SKIN_TIER_LUXURY = 2;

    // Special constant for "Any Town" destination
    public static final UUID ANY_TOWN_DESTINATION = new UUID(0, 0);
    public static final String ANY_TOWN_NAME = "Any Town";

    private int expiryTicks;
    private boolean hasNotifiedOriginTown = false;

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
    private static final double MOVEMENT_THRESHOLD = 2.0;

    private double recentPosX;
    private double recentPosY;
    private double recentPosZ;
    private int positionUpdateTicks = 0;
    private boolean isCurrentlyStationary = true;
    private static final int POSITION_UPDATE_INTERVAL = 40; // 2 seconds
    private static final double STATIONARY_THRESHOLD = 0.5;

    // Track total distance traveled
    private double totalDistanceTraveled = 0.0;

    // Speed reaction tracking
    private int lastSpeedReactionTick = 0;
    private static final double SPEED_THRESHOLD = 0.5; // blocks per second
    private static final int SPEED_REACTION_COOLDOWN = 100; // 10 seconds

    // Social interaction tracking
    @Nullable
    private TouristEntity gossipPartner;

    // Override merchant offers (Villager already implements Merchant)
    private MerchantOffers customOffers;

    /**
     * Showcase sets this so one interaction still opens the vanilla merchant
     * screen. Players never set it. It exists to photograph the old UI.
     */
    public static boolean showcaseVanillaScreen = false;

    /** Look id saved beside the packed int so reloads stay readable. Not synced. */
    private String lookId = "";

    /**
     * Personal name, saved in NBT. The vanilla custom name is kept in step with it
     * so the client can draw the name tag. Empty until {@link #ensurePersonalName()}.
     */
    private String personalName = "";

    public TouristEntity(EntityType<? extends Villager> entityType, Level level) {
        super(entityType, level);
        this.spawnTime = level.getGameTime();

        // Calculate expiry ticks from config at spawn time (not static to avoid class load order issues)
        this.expiryTicks = (int) (ConfigLoader.touristExpiryMinutes * 60 * 20);
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.000001);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_DISTANCE_TRAVELED, 0.0f);
        this.entityData.define(DATA_SKIN_TIER, SKIN_TIER_BASIC);
        this.entityData.define(DATA_LOOK, TouristAppearance.UNSET);
        this.entityData.define(DATA_TOURIST_TYPE, TouristType.SIGHTSEER.index());
        this.entityData.define(DATA_ORIGIN_LABEL, "");
        this.entityData.define(DATA_DEST_LABEL, "");
    }

    public TouristEntity(EntityType<? extends Villager> entityType, Level level,
            Town originTown, Platform platform,
            UUID destinationTownId, String destinationName) {
        this(entityType, level);

        if (originTown != null) {
            this.originTownId = originTown.getId();
            this.originTownName = originTown.getName();
        }

        this.destinationTownId = destinationTownId;
        this.destinationTownName = destinationName;

        if (destinationTownId.equals(ANY_TOWN_DESTINATION)) {
            this.destinationTownName = ANY_TOWN_NAME;
        }

        assignSpawnVariety();
        refreshSyncedLabels();
        this.setCustomNameVisible(true);

        PlatformAccess.getTouristHelper().addStandardTouristTags(
                this,
                originTown,
                platform,
                destinationTownId.toString(),
                destinationName);

        this.setRandomProfession();
        if (!level.isClientSide) {
            this.assignLook();
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 3.0F)); // Closer range
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.3D));
        this.goalSelector.addGoal(4, new com.quackers29.businesscraft.entity.ai.goal.TouristGossipGoal(this));
        this.goalSelector.addGoal(5, new com.quackers29.businesscraft.entity.ai.goal.TouristTargetGazeGoal(this)); // Rare: track distant target
        this.goalSelector.addGoal(6, new com.quackers29.businesscraft.entity.ai.goal.TouristGazeGoal(this)); // Common: look left/right
    }

    @Override
    protected void customServerAiStep() {
        // Override to prevent villager brain updates that interfere with trading
        // Don't call super.customServerAiStep() - this prevents villager brain from running
        // This stops the villager brain from calling stopTrading() automatically
    }

    @Override
    public void updateTrades() {
        // Override to prevent villager from resetting offers
        DebugConfig.debug(LOGGER, DebugConfig.TOURIST_ENTITY, "updateTrades() called - ignoring");
    }

    @Override
    public boolean canRestock() {
        return false; // Tourists don't restock
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.getLookPacked() == TouristAppearance.UNSET) {
            this.assignLook();
        }
        if (!this.level().isClientSide) {
            this.ensurePersonalName();
        }

        if (!hasMoved && !this.level().isClientSide) {
            double dx = this.getX() - this.spawnPosX;
            double dy = this.getY() - this.spawnPosY;
            double dz = this.getZ() - this.spawnPosZ;
            double distanceSquared = dx * dx + dy * dy + dz * dz;

            if (distanceSquared > MOVEMENT_THRESHOLD * MOVEMENT_THRESHOLD) {
                hasMoved = true;
                DebugConfig.debug(LOGGER, DebugConfig.TOURIST_ENTITY,
                        "Tourist has moved from spawn position, distance: {}", Math.sqrt(distanceSquared));
            }
        }

        // Speed-based reactions - runs independently of expiry system
        if (!this.level().isClientSide) {
            positionUpdateTicks++;

            if (positionUpdateTicks >= POSITION_UPDATE_INTERVAL) {
                double dx = this.getX() - this.recentPosX;
                double dy = this.getY() - this.recentPosY;
                double dz = this.getZ() - this.recentPosZ;
                double recentMovementSquared = dx * dx + dy * dy + dz * dz;
                double distanceMoved = Math.sqrt(recentMovementSquared);
                double speed = distanceMoved / (POSITION_UPDATE_INTERVAL / 20.0); // blocks per second

                isCurrentlyStationary = recentMovementSquared < (STATIONARY_THRESHOLD * STATIONARY_THRESHOLD);

                // Accumulate total distance traveled
                totalDistanceTraveled += distanceMoved;

                // Sync to client via entity data
                this.entityData.set(DATA_DISTANCE_TRAVELED, (float) totalDistanceTraveled);

                // Update level based on distance traveled
                updateLevelFromDistance();

                // Speed-based reactions (celebrate when going fast)
                int currentTick = this.tickCount;
                if (speed > SPEED_THRESHOLD) {
                    int cooldownRemaining = SPEED_REACTION_COOLDOWN - (currentTick - lastSpeedReactionTick);
                    if (cooldownRemaining > 0) {
                        DebugConfig.debug(LOGGER, DebugConfig.TOURIST_ENTITY,
                                "Tourist {} fast ({}b/s) but cooldown remains: {} ticks",
                                this.getId(), String.format("%.1f", speed), cooldownRemaining);
                    } else {
                        // 50% chance to celebrate when going fast (increased for testing)
                        if (this.random.nextFloat() < 0.25f) {
                            this.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.35f, 1.05f);
                            this.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                            lastSpeedReactionTick = currentTick;
                            DebugConfig.debug(LOGGER, DebugConfig.TOURIST_ENTITY,
                                    "Tourist {} CELEBRATING high speed: {} blocks/s",
                                    this.getId(), String.format("%.1f", speed));
                        } else {
                            DebugConfig.debug(LOGGER, DebugConfig.TOURIST_ENTITY,
                                    "Tourist {} fast ({}b/s) but RNG missed (50% chance)",
                                    this.getId(), String.format("%.1f", speed));
                        }
                    }
                }

                this.recentPosX = this.getX();
                this.recentPosY = this.getY();
                this.recentPosZ = this.getZ();
                positionUpdateTicks = 0;

                DebugConfig.debug(LOGGER, DebugConfig.TOURIST_ENTITY,
                        "Tourist {} movement check: distance={} m, speed={} b/s, total={} m, stationary={}, riding={}",
                        this.getId(), String.format("%.3f", distanceMoved), String.format("%.1f", speed),
                        String.format("%.1f", totalDistanceTraveled), isCurrentlyStationary, this.isPassenger());
            }
        }

        if (!this.level().isClientSide && ConfigLoader.enableTouristExpiry) {
            if (isCurrentlyStationary) {
                expiryTicks--;

                if (expiryTicks <= 0) {
                    DebugConfig.debug(LOGGER, DebugConfig.TOURIST_ENTITY, "Tourist {} expired after being stationary",
                            this.getId());

                    if (!hasNotifiedOriginTown) {
                        notifyOriginTownOfQuitting();
                        hasNotifiedOriginTown = true;
                    }

                    this.discard();
                }
            } else {
                if (level().getGameTime() % 100 == 0) {
                    DebugConfig.debug(LOGGER, DebugConfig.TOURIST_ENTITY,
                            "Tourist {} is moving, expiry timer paused at {}s",
                            this.getId(), expiryTicks / 20);
                }
            }
        }
    }

    private void notifyOriginTownOfQuitting() {
        if (this.level() instanceof ServerLevel serverLevel && originTownId != null) {
            Town originTown = TownNotificationUtils.removeTouristFromOrigin(serverLevel, originTownId);

            if (hasMoved && ConfigLoader.notifyOnTouristDeparture && originTown != null) {
                TownNotificationUtils.displayTouristDepartureNotification(
                        serverLevel,
                        originTown,
                        originTownName,
                        destinationTownName != null ? destinationTownName : "unknown destination",
                        false,
                        this.blockPosition());
            } else {
                DebugConfig.debug(LOGGER, DebugConfig.TOURIST_ENTITY,
                        "Tourist quit without moving from spawn position, skipping visual notification");
            }
        }
    }

    @Override
    public void die(net.minecraft.world.damagesource.DamageSource cause) {
        if (!this.level().isClientSide && originTownId != null) {
            if (this.level() instanceof ServerLevel serverLevel) {
                Town originTown = TownNotificationUtils.removeTouristFromOrigin(serverLevel, originTownId);

                if (ConfigLoader.notifyOnTouristDeparture && originTown != null) {
                    TownNotificationUtils.displayTouristDepartureNotification(
                            serverLevel,
                            originTown,
                            originTownName,
                            destinationTownName != null ? destinationTownName : "unknown destination",
                            true,
                            this.blockPosition());
                }

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

    public UUID getDestinationTownId() {
        return this.destinationTownId;
    }

    public boolean isGossiping() {
        return gossipPartner != null && gossipPartner.isAlive();
    }

    public void setGossipPartner(@Nullable TouristEntity partner) {
        this.gossipPartner = partner;
    }

    @Nullable
    public TouristEntity getGossipPartner() {
        return this.gossipPartner;
    }

    private void setRandomProfession() {
        this.setVillagerData(this.getVillagerData()
                .setProfession(VillagerProfession.NONE)
                .setLevel(1));
        syncSkinTierFromLevel();
    }

    public int getSkinTier() {
        return this.entityData.get(DATA_SKIN_TIER);
    }

    /** Packed look, or {@link TouristAppearance#UNSET} until the server chooses one. */
    public int getLookPacked() {
        return this.entityData.get(DATA_LOOK);
    }

    public String getLookId() {
        if (this.lookId != null && !this.lookId.isBlank()) {
            return this.lookId;
        }
        int packed = this.getLookPacked();
        if (packed == TouristAppearance.UNSET) {
            return "";
        }
        for (TouristLook look : TouristLooks.all()) {
            if (look.packed() == packed) {
                return look.id();
            }
        }
        return "";
    }

    public String getPersonalName() {
        if (!TouristNames.isPlaceholder(this.personalName)) {
            return this.personalName;
        }
        Component custom = this.getCustomName();
        if (custom != null) {
            String text = custom.getString().trim();
            if (!TouristNames.isPlaceholder(text)) {
                return text;
            }
        }
        return this.personalName == null ? "" : this.personalName;
    }

    public void setPersonalName(String name) {
        this.personalName = name == null ? "" : name.trim();
        if (!TouristNames.isPlaceholder(this.personalName)) {
            this.setCustomName(Component.literal(this.personalName));
        }
    }

    /**
     * Gives a saved tourist a name the first time the server sees them without one.
     * A real custom name already on the entity (not the old "Tourist" label) is kept.
     * Nearby tourists are skipped so a platform does not fill up with the same name.
     */
    public void ensurePersonalName() {
        if (this.level().isClientSide || !TouristNames.isPlaceholder(this.personalName)) {
            return;
        }
        Component custom = this.getCustomName();
        if (custom != null && !TouristNames.isPlaceholder(custom.getString())) {
            this.setPersonalName(custom.getString());
            return;
        }
        Set<String> full = new HashSet<>();
        Set<String> first = new HashSet<>();
        AABB box = this.getBoundingBox().inflate(TouristNames.NEARBY_BLOCKS);
        for (TouristEntity other : this.level().getEntitiesOfClass(TouristEntity.class, box, entity -> entity != this)) {
            String nearby = other.getPersonalName();
            if (!TouristNames.isPlaceholder(nearby)) {
                full.add(nearby);
                first.add(TouristNames.firstOf(nearby));
            }
        }
        this.setPersonalName(TouristNames.pick(this.getRandom()::nextInt, full, first));
    }

    /**
     * Pins the journey and the waiting time shown on the tourist screen.
     * Seconds are stored as the spawn timestamp and the expiry countdown.
     */
    public void applyScreenClock(int journeySeconds, int remainingSeconds) {
        long now = this.level().getGameTime();
        this.spawnTime = now - (long) Math.max(0, journeySeconds) * 20L;
        this.expiryTicks = Math.max(0, remainingSeconds) * 20;
        this.isCurrentlyStationary = true;
    }

    /** Server and showcase use this so the choice is stored before the entity is tracked. */
    public void applyLook(TouristLook look) {
        this.lookId = look.id();
        this.entityData.set(DATA_LOOK, look.packed());
    }

    private void assignLook() {
        TouristLook look = TouristLooks.pick(this.getRandom()::nextInt);
        this.applyLook(look);
        if (look.type() != null) {
            this.entityData.set(DATA_TOURIST_TYPE, look.type().index());
        }
        this.customOffers = null;
    }

    /**
     * Showcase (and tests that have a live entity) use this to pin the name-tag lines.
     */
    public void applyDisplaySample(String origin, String destination, TouristType type, double distance) {
        this.originTownName = origin;
        this.destinationTownName = destination;
        if (type != null) {
            this.entityData.set(DATA_TOURIST_TYPE, type.index());
        }
        this.totalDistanceTraveled = Math.max(0.0, distance);
        this.entityData.set(DATA_DISTANCE_TRAVELED, (float) this.totalDistanceTraveled);
        int rank = JourneyProgress.levelForDistance(this.totalDistanceTraveled, ConfigLoader.touristLevelDistances);
        this.setVillagerData(this.getVillagerData().setLevel(rank));
        syncSkinTierFromLevel();
        refreshSyncedLabels();
        this.setPersonalName("Eleanor Hart");
        this.setCustomNameVisible(true);
        this.customOffers = null;
    }

    /**
     * Pins the journey shown on the tourist screen. {@code anyTown} uses the
     * saved "Any Town" destination id, which is not a real town.
     */
    public void applyJourneyForScreen(String origin, String destination, boolean anyTown,
            TouristType type, double distance) {
        applyDisplaySample(origin, anyTown ? ANY_TOWN_NAME : destination, type, distance);
        if (anyTown) {
            this.destinationTownId = ANY_TOWN_DESTINATION;
            this.destinationTownName = ANY_TOWN_NAME;
        } else {
            if (this.destinationTownId == null || ANY_TOWN_DESTINATION.equals(this.destinationTownId)) {
                this.destinationTownId = UUID.randomUUID();
            }
            this.destinationTownName = destination;
        }
        refreshSyncedLabels();
        this.customOffers = null;
    }

    public String getOriginTownName() {
        if (this.originTownName != null && !this.originTownName.isBlank()) {
            return this.originTownName;
        }
        String synced = getSyncedOriginLabel();
        return synced == null || synced.isBlank() ? "Unknown" : synced;
    }

    public String getDestinationTownName() {
        if (isAnyTownDestination()) {
            return ANY_TOWN_NAME;
        }
        if (this.destinationTownName != null && !this.destinationTownName.isBlank()) {
            return this.destinationTownName;
        }
        String synced = getSyncedDestLabel();
        return synced == null || synced.isBlank() ? "Unknown" : synced;
    }

    public boolean isStationary() {
        return this.isCurrentlyStationary;
    }

    public int getJourneyTicks() {
        long age = this.level().getGameTime() - this.spawnTime;
        if (age < 0L) {
            return 0;
        }
        if (age > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) age;
    }

    public TouristType getTouristType() {
        return TouristType.fromIndex(this.entityData.get(DATA_TOURIST_TYPE));
    }

    public String getSyncedOriginLabel() {
        return this.entityData.get(DATA_ORIGIN_LABEL);
    }

    public String getSyncedDestLabel() {
        return this.entityData.get(DATA_DEST_LABEL);
    }

    public boolean isAnyTownDestination() {
        return ANY_TOWN_DESTINATION.equals(destinationTownId);
    }

    /** Flavour label only. Headwear comes from the packed look, not a tint. */
    private void assignSpawnVariety() {
        TouristType type = TouristType.random(this.random);
        this.entityData.set(DATA_TOURIST_TYPE, type.index());
    }

    private void refreshSyncedLabels() {
        this.entityData.set(DATA_ORIGIN_LABEL,
                TouristDisplayFormat.sanitize(originTownName, "Unknown"));
        String dest = destinationTownName != null ? destinationTownName : "Unknown";
        if (isAnyTownDestination()) {
            dest = ANY_TOWN_NAME;
        }
        this.entityData.set(DATA_DEST_LABEL, TouristDisplayFormat.sanitize(dest, "Unknown"));
    }

    public double getTotalDistanceTraveled() {
        return this.level().isClientSide ? this.entityData.get(DATA_DISTANCE_TRAVELED) : totalDistanceTraveled;
    }

    private static int skinTierForLevel(int level) {
        int max = Math.max(1, JourneyProgress.maxLevel(ConfigLoader.touristLevelDistances));
        return Math.min(Math.max(level, 1), max) - 1;
    }

    private void syncSkinTierFromLevel() {
        this.entityData.set(DATA_SKIN_TIER, skinTierForLevel(this.getVillagerData().getLevel()));
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
        tag.putDouble("TotalDistanceTraveled", totalDistanceTraveled);
        tag.putInt("LastSpeedReactionTick", lastSpeedReactionTick);

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
        if (this.getLookPacked() != TouristAppearance.UNSET) {
            tag.putInt("TouristLook", this.getLookPacked());
            tag.putString("TouristLookId", this.lookId == null ? "" : this.lookId);
        }
        tag.putInt("TouristType", this.entityData.get(DATA_TOURIST_TYPE));
        if (!TouristNames.isPlaceholder(this.personalName)) {
            tag.putString("PersonalName", this.personalName);
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
        if (tag.contains("TotalDistanceTraveled")) {
            totalDistanceTraveled = tag.getDouble("TotalDistanceTraveled");
        }
        if (tag.contains("LastSpeedReactionTick")) {
            lastSpeedReactionTick = tag.getInt("LastSpeedReactionTick");
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
        if (tag.contains("TouristLook")) {
            this.entityData.set(DATA_LOOK, tag.getInt("TouristLook"));
        }
        if (tag.contains("TouristLookId")) {
            this.lookId = tag.getString("TouristLookId");
        }
        if (tag.contains("TouristType")) {
            this.entityData.set(DATA_TOURIST_TYPE, tag.getInt("TouristType"));
        }
        if (tag.contains("PersonalName")) {
            String saved = tag.getString("PersonalName");
            if (!TouristNames.isPlaceholder(saved)) {
                this.setPersonalName(saved);
            }
        }

        syncSkinTierFromLevel();
        refreshSyncedLabels();
    }

    @Nullable
    @Override
    public Villager getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return null;
    }

    @Override
    public boolean canBreed() {
        return false;
    }

    @Override
    public boolean wantsToPickUp(ItemStack itemStack) {
        return false;
    }

    // Disable automatic ambient sounds - only play sounds through gossip goal
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    public double getMyRidingOffset() {
        return this.isPassenger() ? RIDING_Y_OFFSET : super.getMyRidingOffset();
    }

    @Override
    public int getAmbientSoundInterval() {
        return Integer.MAX_VALUE; // Never play automatically
    }

    @Override
    public void playAmbientSound() {
        // Do nothing - sounds controlled by gossip goal only
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Villager.createAttributes();
    }

    public static TouristEntity create(EntityType<TouristEntity> entityType, Level level) {
        return new TouristEntity(entityType, level);
    }

    @Override
    public boolean startRiding(Entity entity, boolean force) {
        boolean result = super.startRiding(entity, force);

        if (result && !this.level().isClientSide) {
            if (entity instanceof AbstractMinecart || entity.getClass().getName().contains("create.content.trains")) {
                if (this.level() instanceof ServerLevel serverLevel) {
                    TouristEffects.playBoarding(serverLevel, this, entity);
                }
            }
        }

        if (result && !hasReceivedRideExtension) {
            // Check if the vehicle is a minecart or Create train carriage
            if (entity instanceof AbstractMinecart || entity.getClass().getName().contains("create.content.trains")) {
                // Recalculate expiry ticks from current config value instead of using static
                // constant
                this.expiryTicks = (int) (ConfigLoader.touristExpiryMinutes * 60 * 20);
                hasReceivedRideExtension = true;
                DebugConfig.debug(LOGGER, DebugConfig.TOURIST_ENTITY,
                        "Resetting expiry timer for tourist to {} minutes ({})",
                        ConfigLoader.touristExpiryMinutes, this.expiryTicks);
            }
        }

        return result;
    }

    @Override
    public boolean startRiding(Entity entity) {
        return this.startRiding(entity, false);
    }

    @Override
    public void setPos(double x, double y, double z) {
        if (spawnPosX == 0 && spawnPosY == 0 && spawnPosZ == 0) {
            this.spawnPosX = x;
            this.spawnPosY = y;
            this.spawnPosZ = z;

            this.recentPosX = x;
            this.recentPosY = y;
            this.recentPosZ = z;
        }
        super.setPos(x, y, z);
    }

    @Override
    public net.minecraft.world.InteractionResult mobInteract(Player player, net.minecraft.world.InteractionHand hand) {
        if (!this.level().isClientSide) {
            if (hand == net.minecraft.world.InteractionHand.MAIN_HAND) {
                DebugConfig.debug(LOGGER, DebugConfig.TOURIST_ENTITY,
                    "Tourist mobInteract - opening trading screen for player {}", player.getName().getString());

                // Play greeting sound
                this.playSound(SoundEvents.VILLAGER_YES, 1.0f, 1.0f);

                this.setTradingPlayer(player);
                if (showcaseVanillaScreen) {
                    // Old villager window, kept so the showcase can photograph it.
                    this.openTradingScreen(player, this.getDisplayName(), this.getVillagerData().getLevel());
                } else {
                    this.ensurePersonalName();
                this.openTouristScreen(player);
                }
            }
            return net.minecraft.world.InteractionResult.CONSUME;
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    public void setTradingPlayer(@Nullable Player player) {
        super.setTradingPlayer(player);
        DebugConfig.debug(LOGGER, DebugConfig.TOURIST_ENTITY,
            "Set trading player to {}", player != null ? player.getName().getString() : "null");
    }

    @Override
    public void stopTrading() {
        // Don't call super - prevents villager brain from interfering
        this.setTradingPlayer(null);

        // Play farewell sound
        if (!this.level().isClientSide) {
            this.playSound(SoundEvents.VILLAGER_NO, 1.0f, 1.0f);
        }

        DebugConfig.debug(LOGGER, DebugConfig.TOURIST_ENTITY, "Stopped trading");
    }


    // Override Merchant methods from Villager
    @Override
    public MerchantOffers getOffers() {
        if (this.customOffers == null) {
            this.customOffers = createOffers();
        }
        return this.customOffers;
    }

    @Override
    public void overrideOffers(MerchantOffers offers) {
        this.customOffers = offers;
    }

    /**
     * Refresh offers preserving use counts
     */
    public void refreshOffersWithLiveData() {
        MerchantOffers oldOffers = this.customOffers;
        MerchantOffers newOffers = createOffers();
        TouristTradeCatalog.copyUses(oldOffers, newOffers);
        this.customOffers = newOffers;
    }

    @Override
    public int getVillagerXp() {
        double distance = this.level().isClientSide ? this.entityData.get(DATA_DISTANCE_TRAVELED) : totalDistanceTraveled;
        int currentLevel = this.getVillagerData().getLevel();
        return JourneyProgress.xpForDistance(distance, currentLevel, ConfigLoader.touristLevelDistances);
    }

    @Override
    public boolean showProgressBar() {
        return true; // Show XP bar for distance progress
    }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        DebugConfig.debug(LOGGER, DebugConfig.TOURIST_ENTITY, "notifyTrade() called");
        offer.increaseUses();
        // XP is now distance-based, not trade-based
    }

    /**
     * Update tourist level based on distance traveled
     */
    private void updateLevelFromDistance() {
        int maxLevel = JourneyProgress.maxLevel(ConfigLoader.touristLevelDistances);
        int targetLevel = Math.min(
                JourneyProgress.levelForDistance(totalDistanceTraveled, ConfigLoader.touristLevelDistances),
                maxLevel);

        int currentLevel = this.getVillagerData().getLevel();
        if (targetLevel > currentLevel && targetLevel <= maxLevel) {
            this.setVillagerData(this.getVillagerData().setLevel(targetLevel));
            syncSkinTierFromLevel();
            this.refreshOffersWithLiveData();
            if (this.level() instanceof ServerLevel serverLevel) {
                TouristEffects.playRankUp(serverLevel, this.position(), targetLevel);
            }
            DebugConfig.debug(LOGGER, DebugConfig.TOURIST_ENTITY,
                "Tourist leveled up to {} ({}) at {:.1f}m",
                targetLevel, JourneyProgress.tierName(targetLevel), totalDistanceTraveled);
        }
    }

    @Override
    public boolean isClientSide() {
        return this.level().isClientSide;
    }

    /**
     * Real deals only. The old barrier "info" row is not included, except when
     * the showcase is photographing the vanilla merchant screen.
     */
    private MerchantOffers createOffers() {
        int level = this.getVillagerData().getLevel();
        double distance = this.level().isClientSide
                ? this.entityData.get(DATA_DISTANCE_TRAVELED)
                : totalDistanceTraveled;
        TouristTradeCatalog.TradeContext context = new TouristTradeCatalog.TradeContext(
                getOriginTownName(),
                getDestinationTownName(),
                isAnyTownDestination(),
                JourneyProgress.tierName(level),
                distance,
                TouristTradeCatalog.formatDuration(getJourneyTicks() / 20),
                TouristTradeCatalog.formatDuration(Math.max(0, this.expiryTicks) / 20),
                this.isCurrentlyStationary);
        if (showcaseVanillaScreen) {
            return TouristTradeCatalog.offersForVanillaScreenshot(level, context, getLookId());
        }
        MerchantOffers offers = TouristTradeCatalog.offersFor(level, context, getLookId());
        DebugConfig.debug(LOGGER, DebugConfig.TOURIST_ENTITY,
                "Total offers created: {} (level {})", offers.size(), level);
        return offers;
    }

    private void openTouristScreen(Player player) {
        com.quackers29.businesscraft.api.PlatformAccess.getNetwork()
                .openScreen(player, new com.quackers29.businesscraft.menu.TouristMenuProvider(this));
    }
}
