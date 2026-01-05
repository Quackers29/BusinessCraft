package com.quackers29.businesscraft.town.components;

import com.quackers29.businesscraft.data.parsers.DataParser.ResourceAmount;
import com.quackers29.businesscraft.data.parsers.Effect;
import com.quackers29.businesscraft.production.UpgradeNode;
import com.quackers29.businesscraft.production.UpgradeRegistry;
import com.quackers29.businesscraft.town.Town;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class TownUpgradeComponent implements TownComponent {
    private static final Logger LOGGER = LoggerFactory.getLogger(TownUpgradeComponent.class);

    private final Town town;
    private final Set<String> unlockedNodes = new HashSet<>();
    private final Map<String, Integer> upgradeLevels = new HashMap<>(); // node -> level
    private final Map<String, Float> activeModifiers = new HashMap<>(); // target -> value
    private final Map<String, Float> aiScores = new HashMap<>();

    public Map<String, Float> getAiScores() {
        return Collections.unmodifiableMap(aiScores);
    }

    // Research tracking
    private String currentResearchNode = null;
    private float researchProgress = 0; // days accumulated

    public String getCurrentResearchNode() {
        return currentResearchNode;
    }

    public float getResearchProgress() {
        return researchProgress;
    }

    public TownUpgradeComponent(Town town) {
        this.town = town;
    }

    private int aiCheckCooldown = 0;
    private long idleTicks = 0;

    @Override
    public void tick() {
        if (currentResearchNode != null) {
            UpgradeNode node = UpgradeRegistry.get(currentResearchNode);
            if (node == null) {
                currentResearchNode = null;
                return;
            }

            // Research progress logic
            researchProgress += 1.0f / 1200.0f;

            if (researchProgress >= node.getResearchMinutes()) {
                completeResearch();
            }
        } else {
            // Idle - AI check
            idleTicks++;
            if (aiCheckCooldown-- <= 0) {
                aiCheckCooldown = 200; // Check every ~10 seconds

                // Update scores
                Map<String, Float> scores = com.quackers29.businesscraft.town.ai.TownResearchAI
                        .calculatePriorities(town);
                this.aiScores.clear();
                this.aiScores.putAll(scores);

                // Pick best
                String nextNode = com.quackers29.businesscraft.town.ai.TownResearchAI.selectBestResearch(town, scores,
                        idleTicks);

                if (nextNode != null) {
                    startResearch(nextNode);
                }
            }
        }
    }

    public int getUpgradeLevel(String nodeId) {
        return upgradeLevels.getOrDefault(nodeId, 0);
    }

    public List<ResourceAmount> getUpgradeCost(String nodeId) {
        UpgradeNode node = UpgradeRegistry.get(nodeId);
        if (node == null)
            return Collections.emptyList();

        int currentLevel = getUpgradeLevel(nodeId); // Logic usually asks for cost of *next* level.
        // If current is 0, cost is for Level 1.
        // If current is 5, cost is for Level 6.
        // Code uses `currentLevel` to calc multiplier.
        // For Level 1 (current=0), mult should be 1.0.

        List<ResourceAmount> costs = node.getCosts();
        if (costs == null || costs.isEmpty())
            return Collections.emptyList();

        // Always use Exponential/Geometric scaling for costs (Simpler configuration)
        // Multiplier 1.1 means "Multiply by 1.1 each level" (Compound)
        // Level 0: 1.0
        // Level 1: 1.1
        // Level 2: 1.21
        float multiplier = (float) Math.pow(node.getCostMultiplier(), currentLevel);

        List<ResourceAmount> effectiveCosts = new ArrayList<>();

        for (ResourceAmount ra : costs) {
            effectiveCosts.add(new ResourceAmount(ra.resourceId, (int) Math.ceil(ra.amount * multiplier)));
        }
        return effectiveCosts;
    }

    public boolean canAffordResearch(String nodeId) {
        UpgradeNode node = UpgradeRegistry.get(nodeId);
        if (node == null)
            return false;

        int currentLevel = getUpgradeLevel(nodeId);

        // Check repeat limits
        if (!node.isRepeatable()) {
            if (currentLevel >= 1)
                return false;
        } else {
            if (node.getMaxRepeats() != -1 && currentLevel >= node.getMaxRepeats())
                return false;
        }

        List<ResourceAmount> costs = getUpgradeCost(nodeId);
        for (ResourceAmount ra : costs) {
            float stock = town.getTrading().getStock(ra.resourceId);
            if (stock < ra.amount)
                return false;
        }
        return true;
    }

    public void startResearch(String nodeId) {
        UpgradeNode node = UpgradeRegistry.get(nodeId);
        if (node == null) {
            LOGGER.error("StartResearch failed: {} not found in registry", nodeId);
            return;
        }

        int currentLevel = getUpgradeLevel(nodeId);
        // Check if fully maxed
        if (!node.isRepeatable()) {
            if (currentLevel >= 1) {
                LOGGER.debug("StartResearch failed: {} already unlocked", nodeId);
                return;
            }
        } else {
            if (node.getMaxRepeats() != -1 && currentLevel >= node.getMaxRepeats()) {
                LOGGER.debug("StartResearch failed: {} maxed out at level {}", nodeId, currentLevel);
                return;
            }
        }

        // check prereqs
        if (node.getPrereqNodes() != null) {
            for (String pre : node.getPrereqNodes()) {
                if (!isUnlocked(pre)) {
                    LOGGER.debug("StartResearch failed: {} missing prereq {}", nodeId, pre);
                    return; // Prereq missing
                }
            }
        }

        // Calculate dynamic costs
        List<ResourceAmount> costs = getUpgradeCost(nodeId); // Use helper

        // Check if can afford
        for (ResourceAmount ra : costs) {
            float stock = town.getTrading().getStock(ra.resourceId);
            if (stock < ra.amount) {
                LOGGER.debug("StartResearch failed: {} cannot afford {}. Has {}, needs {}",
                        nodeId, ra.resourceId, stock, ra.amount);
                return; // Cannot afford
            }
        }
        // Deduct
        for (ResourceAmount ra : costs) {
            // Do not consume stats like tourism_count
            if (ra.resourceId.startsWith("tourism_"))
                continue;

            town.getTrading().adjustStock(ra.resourceId, -ra.amount);
        }

        LOGGER.info("Starting research: {} (Lvl {}) for town {}", nodeId, currentLevel + 1, town.getName());
        this.currentResearchNode = nodeId;
        this.researchProgress = 0;
        this.idleTicks = 0;

        // Notification
        net.minecraft.server.level.ServerLevel level = com.quackers29.businesscraft.town.utils.TownNotificationUtils
                .getLevelForTown(town);
        if (level != null) {
            String suffix = (node.isRepeatable()) ? " " + (currentLevel + 1) : "";
            net.minecraft.network.chat.Component message = net.minecraft.network.chat.Component
                    .literal("Research started: ")
                    .withStyle(net.minecraft.ChatFormatting.BLUE)
                    .append(net.minecraft.network.chat.Component.literal(node.getDisplayName() + suffix)
                            .withStyle(net.minecraft.ChatFormatting.WHITE));

            com.quackers29.businesscraft.town.utils.TownNotificationUtils.broadcastToTown(level, town, message);
        }

        town.markDirty();
    }

    public void completeResearch() {
        if (currentResearchNode != null) {
            String nodeId = currentResearchNode;

            UpgradeNode node = UpgradeRegistry.get(nodeId);
            int newLevel = getUpgradeLevel(nodeId) + 1;

            // Notification setup
            String displayName = (node != null) ? node.getDisplayName() : nodeId;
            if (node != null && node.isRepeatable()) {
                displayName += " " + newLevel;
            }

            unlockNode(nodeId); // Increments level
            currentResearchNode = null;
            researchProgress = 0;

            net.minecraft.server.level.ServerLevel level = com.quackers29.businesscraft.town.utils.TownNotificationUtils
                    .getLevelForTown(town);
            if (level != null) {
                net.minecraft.network.chat.Component message = net.minecraft.network.chat.Component
                        .literal("Research completed: ")
                        .withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.BOLD)
                        .append(net.minecraft.network.chat.Component.literal(displayName)
                                .withStyle(net.minecraft.ChatFormatting.WHITE));

                com.quackers29.businesscraft.town.utils.TownNotificationUtils.broadcastToTown(level, town, message);
            }

            town.markDirty();
        }
    }

    public void unlockNode(String nodeId) {
        int lvl = upgradeLevels.getOrDefault(nodeId, 0);

        upgradeLevels.put(nodeId, lvl + 1);
        unlockedNodes.add(nodeId);

        // Check for chained unlocks (Upgrade unlocking another Upgrade)
        UpgradeNode node = UpgradeRegistry.get(nodeId);
        if (node != null) {
            for (Effect effect : node.getEffects()) {
                String target = effect.getTarget();
                // If the effect name matches an existing Upgrade Node, unlock it recursively
                // Avoid unlocking self to prevent direct loops (though indirect loops A->B->A
                // still possible)
                if (!target.equals(nodeId) && UpgradeRegistry.get(target) != null) {
                    // Chained unlocks trigger a level up for that node too?
                    // Or typically just "unlock level 1".
                    // The 'unlockNode' function bumps the level.
                    // Let's assume chained unlocks bump level.
                    unlockNode(target);
                }
            }
        }

        recalculateModifiers();
        town.markDirty();
    }

    private final Map<String, Float> flatModifiers = new HashMap<>(); // Permanent modifiers (e.g. from Biome)

    public void addFlatModifier(String key, float value) {
        flatModifiers.put(key, value);
        recalculateModifiers();
        town.markDirty();
    }

    public void accumulateFlatModifier(String key, float delta) {
        float current = flatModifiers.getOrDefault(key, 0f);
        flatModifiers.put(key, current + delta);
        recalculateModifiers();
        town.markDirty();
    }

    // Recalculates all active modifiers based on unlocked nodes and flat modifiers
    private void recalculateModifiers() {
        activeModifiers.clear();

        // Add flat modifiers first
        activeModifiers.putAll(flatModifiers);

        for (Map.Entry<String, Integer> entry : upgradeLevels.entrySet()) {
            String nodeId = entry.getKey();
            int level = entry.getValue();

            UpgradeNode node = UpgradeRegistry.get(nodeId);
            if (node == null)
                continue;

            for (Effect effect : node.getEffects()) {
                float value = node.calculateEffectValue(effect, level);
                activeModifiers.merge(effect.getTarget(), value, Float::sum);
            }
        }
    }

    // ... getters ...

    public float getModifier(String target) {
        return activeModifiers.getOrDefault(target, 0f);
    }

    public boolean isUnlocked(String nodeId) {
        return upgradeLevels.getOrDefault(nodeId, 0) > 0;
    }

    public Set<String> getUnlockedNodes() {
        return Collections.unmodifiableSet(unlockedNodes);
    }

    public Map<String, Integer> getUpgradeLevels() {
        return Collections.unmodifiableMap(upgradeLevels);
    }

    @Override
    public void save(CompoundTag tag) {
        // ... existing save logic ...
        ListTag list = new ListTag();
        for (Map.Entry<String, Integer> entry : upgradeLevels.entrySet()) {
            CompoundTag nodeTag = new CompoundTag();
            nodeTag.putString("id", entry.getKey());
            nodeTag.putInt("lvl", entry.getValue());
            list.add(nodeTag);
        }
        tag.put("upgradeLevels", list);

        if (currentResearchNode != null) {
            tag.putString("currentResearch", currentResearchNode);
            tag.putFloat("researchProgress", researchProgress);
        } else {
            tag.putLong("idleTicks", idleTicks);
        }

        // Save flat modifiers
        CompoundTag flatTag = new CompoundTag();
        flatModifiers.forEach(flatTag::putFloat);
        tag.put("flatModifiers", flatTag);
    }

    @Override
    public void load(CompoundTag tag) {
        unlockedNodes.clear();
        upgradeLevels.clear();
        flatModifiers.clear();

        // ... existing load logic for upgradeLevels ...
        if (tag.contains("upgradeLevels")) {
            ListTag list = tag.getList("upgradeLevels", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag nodeTag = list.getCompound(i);
                String id = nodeTag.getString("id");
                int lvl = nodeTag.getInt("lvl");
                upgradeLevels.put(id, lvl);
                unlockedNodes.add(id);
            }
        } else if (tag.contains("unlockedNodes")) {
            // Legacy load
            ListTag list = tag.getList("unlockedNodes", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                String id = list.getString(i);
                upgradeLevels.put(id, 1);
                unlockedNodes.add(id);
            }
        }

        if (tag.contains("currentResearch")) {
            currentResearchNode = tag.getString("currentResearch");
            researchProgress = tag.getFloat("researchProgress");
            idleTicks = 0;
        } else {
            currentResearchNode = null;
            researchProgress = 0;
            idleTicks = tag.getLong("idleTicks");
        }

        if (tag.contains("flatModifiers")) {
            CompoundTag flatTag = tag.getCompound("flatModifiers");
            for (String key : flatTag.getAllKeys()) {
                flatModifiers.put(key, flatTag.getFloat(key));
            }
        }

        recalculateModifiers();
    }
}
