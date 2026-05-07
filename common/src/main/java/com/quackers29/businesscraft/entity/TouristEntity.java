package com.quackers29.businesscraft.entity;

import com.quackers29.businesscraft.platform.Platform;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.api.PlatformAccess;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import com.quackers29.businesscraft.debug.DebugConfig;

public class TouristEntity extends Villager {
    private static final Logger LOGGER = LoggerFactory.getLogger(TouristEntity.class);

    // Synced entity data for live updates on client
    private static final EntityDataAccessor<Float> DATA_DISTANCE_TRAVELED =
        SynchedEntityData.defineId(TouristEntity.class, EntityDataSerializers.FLOAT);

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

    // Override merchant offers (Villager already implements Merchant)
    private MerchantOffers customOffers;

    // Distance-based leveling config
    private static final double DISTANCE_PER_LEVEL = 20.0; // 20m per level
    private static final int MAX_LEVEL = 3; // Max level 3 (starts at 1)

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

        String displayName;
        if (destinationTownId.equals(ANY_TOWN_DESTINATION)) {
            displayName = "Tourist to Any Town";
            this.destinationTownName = ANY_TOWN_NAME;
        } else {
            displayName = "Tourist to " + destinationName;
        }
        this.setCustomName(Component.literal(displayName));

        PlatformAccess.getTouristHelper().addStandardTouristTags(
                this,
                originTown,
                platform,
                destinationTownId.toString(),
                destinationName);

        this.setRandomProfession();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 5.0F));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.3D));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
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

        // Force offer refresh for live updates when trading
        if (!this.level().isClientSide && this.getTradingPlayer() != null) {
            // Refresh offers for live data
            if (this.tickCount % 10 == 0) {
                // Regenerate offers preserving use counts
                this.refreshOffersWithLiveData();

                // Send packet to sync offers to client screen
                if (this.getTradingPlayer() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    if (serverPlayer.containerMenu instanceof net.minecraft.world.inventory.MerchantMenu) {
                        serverPlayer.connection.send(
                            new net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket(
                                serverPlayer.containerMenu.containerId,
                                this.getOffers(),
                                this.getVillagerData().getLevel(),
                                this.getVillagerXp(),
                                this.showProgressBar(),
                                this.canRestock()
                            )
                        );
                        DebugConfig.debug(LOGGER, DebugConfig.TOURIST_ENTITY,
                            "Sent merchant offers packet to client - {} offers, XP: {}",
                            this.getOffers().size(), this.getVillagerXp());
                    }
                }
            }
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

        if (!this.level().isClientSide && ConfigLoader.enableTouristExpiry) {
            positionUpdateTicks++;

            if (positionUpdateTicks >= POSITION_UPDATE_INTERVAL) {
                double dx = this.getX() - this.recentPosX;
                double dy = this.getY() - this.recentPosY;
                double dz = this.getZ() - this.recentPosZ;
                double recentMovementSquared = dx * dx + dy * dy + dz * dz;
                double distanceMoved = Math.sqrt(recentMovementSquared);

                isCurrentlyStationary = recentMovementSquared < (STATIONARY_THRESHOLD * STATIONARY_THRESHOLD);

                // Accumulate total distance traveled
                totalDistanceTraveled += distanceMoved;

                // Sync to client via entity data
                this.entityData.set(DATA_DISTANCE_TRAVELED, (float) totalDistanceTraveled);

                // Update level based on distance traveled
                updateLevelFromDistance();

                this.recentPosX = this.getX();
                this.recentPosY = this.getY();
                this.recentPosZ = this.getZ();
                positionUpdateTicks = 0;

                DebugConfig.debug(LOGGER, DebugConfig.TOURIST_ENTITY,
                        "Tourist {} movement check: distance={:.3f}, total={:.1f}m, stationary={}, riding={}, expiry={}s",
                        this.getId(), distanceMoved, totalDistanceTraveled, isCurrentlyStationary, this.isPassenger(), expiryTicks / 20);
            }

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
        tag.putDouble("TotalDistanceTraveled", totalDistanceTraveled);

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
        if (tag.contains("TotalDistanceTraveled")) {
            totalDistanceTraveled = tag.getDouble("TotalDistanceTraveled");
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

                // Use villager's built-in trading screen
                this.setTradingPlayer(player);
                this.openTradingScreen(player, this.getDisplayName(), this.getVillagerData().getLevel());
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

        // Preserve use counts from old offers (even when level up adds new trades)
        if (oldOffers != null) {
            for (MerchantOffer newOffer : newOffers) {
                // Find matching old offer by comparing items
                for (MerchantOffer oldOffer : oldOffers) {
                    if (offersMatch(oldOffer, newOffer)) {
                        // Copy use count to new offer
                        for (int j = 0; j < oldOffer.getUses(); j++) {
                            newOffer.increaseUses();
                        }
                        break;
                    }
                }
            }
        }

        this.customOffers = newOffers;
    }

    /**
     * Check if two offers are the same trade (same items + counts)
     */
    private boolean offersMatch(MerchantOffer offer1, MerchantOffer offer2) {
        return itemStacksMatch(offer1.getBaseCostA(), offer2.getBaseCostA()) &&
               itemStacksMatch(offer1.getCostB(), offer2.getCostB()) &&
               itemStacksMatch(offer1.getResult(), offer2.getResult());
    }

    /**
     * Check if two ItemStacks match (type, NBT, AND count)
     */
    private boolean itemStacksMatch(ItemStack stack1, ItemStack stack2) {
        if (stack1.isEmpty() && stack2.isEmpty()) return true;
        if (stack1.isEmpty() || stack2.isEmpty()) return false;
        return ItemStack.isSameItemSameTags(stack1, stack2) && stack1.getCount() == stack2.getCount();
    }

    @Override
    public int getVillagerXp() {
        // Map distance to Minecraft's villager XP thresholds
        // Level 1→2: 10 XP, Level 2→3: 70 XP, Level 3→4: 150 XP
        double distance = this.level().isClientSide ? this.entityData.get(DATA_DISTANCE_TRAVELED) : totalDistanceTraveled;
        int currentLevel = this.getVillagerData().getLevel();
        double distanceIntoCurrentLevel = distance - ((currentLevel - 1) * DISTANCE_PER_LEVEL);

        // Scale distance (0-20m) to vanilla XP thresholds based on level
        int maxXpForLevel;
        switch (currentLevel) {
            case 1: maxXpForLevel = 10; break;   // Novice → Apprentice
            case 2: maxXpForLevel = 70; break;   // Apprentice → Journeyman
            case 3: maxXpForLevel = 150; break;  // Journeyman → Expert
            default: maxXpForLevel = 10; break;
        }

        // Map distance progress to XP: (distance / 20m) * maxXpForLevel
        int xp = (int) ((distanceIntoCurrentLevel / DISTANCE_PER_LEVEL) * maxXpForLevel);
        return Math.max(0, Math.min(maxXpForLevel, xp));
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
        int targetLevel = 1 + (int) (totalDistanceTraveled / DISTANCE_PER_LEVEL);
        targetLevel = Math.min(targetLevel, MAX_LEVEL);

        int currentLevel = this.getVillagerData().getLevel();
        if (targetLevel > currentLevel && targetLevel <= MAX_LEVEL) {
            this.setVillagerData(this.getVillagerData().setLevel(targetLevel));
            DebugConfig.debug(LOGGER, DebugConfig.TOURIST_ENTITY,
                "Tourist leveled up to {} at {:.1f}m", targetLevel, totalDistanceTraveled);
        }
    }

    @Override
    public boolean isClientSide() {
        return this.level().isClientSide;
    }

    /**
     * Creates the merchant offers for this tourist.
     * First offer is an info display, followed by actual trades.
     */
    private MerchantOffers createOffers() {
        MerchantOffers offers = new MerchantOffers();

        DebugConfig.debug(LOGGER, DebugConfig.TOURIST_ENTITY, "Creating merchant offers for tourist");

        // Get synced distance for client-side rendering
        double distance = this.level().isClientSide ? this.entityData.get(DATA_DISTANCE_TRAVELED) : totalDistanceTraveled;

        // Info "trade" - displays tourist information (not actually tradeable)
        String journeyTime = formatTicks((int) (this.level().getGameTime() - this.spawnTime));
        String timeLeft = formatTicks(this.expiryTicks);

        // Create info display item with tourist details in lore
        ItemStack infoDisplay = new ItemStack(Items.PAPER);
        infoDisplay.setHoverName(Component.literal("§6Tourist Information"));

        // Add lore with tourist data
        net.minecraft.nbt.CompoundTag displayTag = infoDisplay.getOrCreateTagElement("display");
        net.minecraft.nbt.ListTag loreList = new net.minecraft.nbt.ListTag();
        loreList.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(Component.literal("§7Origin: §f" + (this.originTownName != null ? this.originTownName : "Unknown")))));
        loreList.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(Component.literal("§7Destination: §f" + (this.destinationTownName != null ? this.destinationTownName : "Unknown")))));
        loreList.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(Component.literal(""))));
        loreList.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(Component.literal("§7Journey Time: §f" + journeyTime))));
        loreList.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(Component.literal("§7Time Remaining: §f" + timeLeft))));
        loreList.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(Component.literal("§7Distance Traveled: §f" + String.format("%.1f", distance) + "m"))));
        loreList.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(Component.literal("§7Status: §f" + (this.isCurrentlyStationary ? "Stationary" : "Moving")))));
        displayTag.put("Lore", loreList);

        // Info offer - cannot be traded (uses Barrier as impossible cost)
        MerchantOffer infoOffer = new MerchantOffer(
            new ItemStack(Items.BARRIER), // Unobtainable in survival - can't complete trade
            infoDisplay,
            1, // Only 1 use (doesn't matter since cost is impossible)
            0, // No XP
            0.0f // No price multiplier
        );
        offers.add(infoOffer);
        DebugConfig.debug(LOGGER, DebugConfig.TOURIST_ENTITY, "Added info display offer (untradeable)");

        // Travel ticket trade - always available (free souvenir from tourist)
        ItemStack travelTicket = new ItemStack(Items.PAPER);
        travelTicket.setHoverName(Component.literal("§6Travel Ticket"));

        net.minecraft.nbt.CompoundTag ticketDisplayTag = travelTicket.getOrCreateTagElement("display");
        net.minecraft.nbt.ListTag ticketLoreList = new net.minecraft.nbt.ListTag();
        ticketLoreList.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(Component.literal("§7From: §f" + (this.originTownName != null ? this.originTownName : "Unknown")))));
        ticketLoreList.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(Component.literal("§7To: §f" + (this.destinationTownName != null ? this.destinationTownName : "Unknown")))));
        ticketLoreList.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(Component.literal(""))));
        ticketLoreList.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(Component.literal("§8A souvenir from a traveling tourist"))));
        ticketDisplayTag.put("Lore", ticketLoreList);

        MerchantOffer ticketTrade = new MerchantOffer(
            new ItemStack(Items.PAPER, 1), // Cost: 1 paper
            travelTicket,
            1,   
            0, // No XP
            0.0f // No price multiplier
        );
        offers.add(ticketTrade);
        DebugConfig.debug(LOGGER, DebugConfig.TOURIST_ENTITY, "Added travel ticket offer");

        // Add level-based trades
        int currentLevel = this.getVillagerData().getLevel();

        if (currentLevel >= 1) {
            MerchantOffer level1Trade = new MerchantOffer(
                new ItemStack(Items.BREAD, 8),
                new ItemStack(Items.IRON_INGOT, 2),
                1, // maxUses
                0, // villagerXp
                0.05f // priceMultiplier
            );
            offers.add(level1Trade);
        }

        if (currentLevel >= 2) {
            MerchantOffer level2Trade = new MerchantOffer(
                new ItemStack(Items.BREAD, 6),
                new ItemStack(Items.IRON_INGOT, 2),
                1, // maxUses
                0, // villagerXp
                0.1f // priceMultiplier
            );
            offers.add(level2Trade);
        }

        if (currentLevel >= 3) {
            MerchantOffer level3Trade = new MerchantOffer(
                new ItemStack(Items.BREAD, 4),
                new ItemStack(Items.IRON_INGOT, 2),
                1, // maxUses
                0, // villagerXp
                0.15f // priceMultiplier
            );
            offers.add(level3Trade);
        }

        DebugConfig.debug(LOGGER, DebugConfig.TOURIST_ENTITY, "Total offers created: {} (level {})", offers.size(), currentLevel);
        return offers;
    }

    private String formatTicks(int ticks) {
        int seconds = ticks / 20;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%dm %ds", minutes, seconds);
    }
}
