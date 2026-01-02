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

    @Override
    public void tick() {
        if (currentResearchNode != null) {
            UpgradeNode node = UpgradeRegistry.get(currentResearchNode);
            if (node == null) {
                currentResearchNode = null;
                return;
            }

            // Research progress logic
            researchProgress += 1.0f / com.quackers29.businesscraft.config.ConfigLoader.dailyTickInterval;

            if (researchProgress >= node.getResearchDays()) {
                completeResearch();
            }
        } else {
            // Idle - AI check
            if (aiCheckCooldown-- <= 0) {
                aiCheckCooldown = 200; // Check every ~10 seconds

                String nextNode = com.quackers29.businesscraft.town.ai.TownResearchAI.selectNextResearch(town);
                if (nextNode != null) {
                    startResearch(nextNode);
                }
            }
        }
    }

    public int getUpgradeLevel(String nodeId) {
        return upgradeLevels.getOrDefault(nodeId, 0);
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

        List<ResourceAmount> costs = node.getCosts();
        if (costs != null && !costs.isEmpty()) {
            float multiplier = (float) Math.pow(node.getCostMultiplier(), currentLevel);
            for (ResourceAmount ra : costs) {
                float cost = ra.amount * multiplier;
                float stock = town.getTrading().getStock(ra.resourceId);
                if (stock < cost)
                    return false;
            }
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
        List<ResourceAmount> costs = node.getCosts();
        if (costs != null && !costs.isEmpty()) {
            float multiplier = (float) Math.pow(node.getCostMultiplier(), currentLevel);

            // Check if can afford
            for (ResourceAmount ra : costs) {
                float cost = ra.amount * multiplier;
                float stock = town.getTrading().getStock(ra.resourceId);
                if (stock < cost) {
                    LOGGER.debug("StartResearch failed: {} cannot afford {}. Has {}, needs {}",
                            nodeId, ra.resourceId, stock, cost);
                    return; // Cannot afford
                }
            }
            // Deduct
            for (ResourceAmount ra : costs) {
                float cost = ra.amount * multiplier;
                town.getTrading().adjustStock(ra.resourceId, -cost);
            }
        }

        LOGGER.info("Starting research: {} (Lvl {}) for town {}", nodeId, currentLevel + 1, town.getName());
        this.currentResearchNode = nodeId;
        this.researchProgress = 0;

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

        recalculateModifiers();
        town.markDirty();
    }

    // Recalculates all active modifiers based on unlocked nodes
    private void recalculateModifiers() {
        activeModifiers.clear();
        for (Map.Entry<String, Integer> entry : upgradeLevels.entrySet()) {
            String nodeId = entry.getKey();
            int level = entry.getValue();

            UpgradeNode node = UpgradeRegistry.get(nodeId);
            if (node == null)
                continue;

            for (Effect effect : node.getEffects()) {
                // Effects are multiplied by level for repeated upgrades
                activeModifiers.merge(effect.getTarget(), effect.getValue() * level, Float::sum);
            }
        }
    }

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
        ListTag list = new ListTag();
        for (Map.Entry<String, Integer> entry : upgradeLevels.entrySet()) {
            CompoundTag nodeTag = new CompoundTag();
            nodeTag.putString("id", entry.getKey());
            nodeTag.putInt("lvl", entry.getValue());
            list.add(nodeTag);
        }
        tag.put("unlockedNodes", list); // Reuse key but different format?
        // Wait, backward compatibility. If I change format, old saves might break or
        // fail to load.
        // Old format: List<String>. New format: List<CompoundTag>.
        // Tag types are distinct. list.getString(i) vs getCompound(i).
        // ListTag stores a type.
        // Let's use a new key "upgradeLevels" and fallback to "unlockedNodes" for
        // legacy.

        tag.put("upgradeLevels", list);

        if (currentResearchNode != null) {
            tag.putString("currentResearch", currentResearchNode);
            tag.putFloat("researchProgress", researchProgress);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        unlockedNodes.clear();
        upgradeLevels.clear();

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
        } else {
            currentResearchNode = null;
            researchProgress = 0;
        }
        recalculateModifiers();
    }
}
