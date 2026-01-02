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

    public void startResearch(String nodeId) {
        if (unlockedNodes.contains(nodeId)) {
            LOGGER.debug("StartResearch failed: {} already unlocked", nodeId);
            return;
        }
        UpgradeNode node = UpgradeRegistry.get(nodeId);
        if (node == null) {
            LOGGER.error("StartResearch failed: {} not found in registry", nodeId);
            return;
        }

        // check prereqs
        if (node.getPrereqNodes() != null) {
            for (String pre : node.getPrereqNodes()) {
                if (!unlockedNodes.contains(pre)) {
                    LOGGER.debug("StartResearch failed: {} missing prereq {}", nodeId, pre);
                    return; // Prereq missing
                }
            }
        }

        // check costs
        if (node.getCosts() != null && !node.getCosts().isEmpty()) {
            // Check if can afford
            for (ResourceAmount ra : node.getCosts()) {
                float stock = town.getTrading().getStock(ra.resourceId);
                if (stock < ra.amount) {
                    LOGGER.debug("StartResearch failed: {} cannot afford {}. Has {}, needs {}",
                            nodeId, ra.resourceId, stock, ra.amount);
                    return; // Cannot afford
                }
            }
            // Dedut
            for (ResourceAmount ra : node.getCosts()) {
                town.getTrading().adjustStock(ra.resourceId, -ra.amount);
            }
        }

        LOGGER.info("Starting research: {} for town {}", nodeId, town.getName());
        this.currentResearchNode = nodeId;
        this.researchProgress = 0;

        // Notification
        net.minecraft.server.level.ServerLevel level = com.quackers29.businesscraft.town.utils.TownNotificationUtils
                .getLevelForTown(town);
        if (level != null) {
            net.minecraft.network.chat.Component message = net.minecraft.network.chat.Component
                    .literal("Research started: ")
                    .withStyle(net.minecraft.ChatFormatting.BLUE)
                    .append(net.minecraft.network.chat.Component.literal(node.getDisplayName())
                            .withStyle(net.minecraft.ChatFormatting.WHITE));

            com.quackers29.businesscraft.town.utils.TownNotificationUtils.broadcastToTown(level, town, message);
        }

        town.markDirty();
    }

    public void completeResearch() {
        if (currentResearchNode != null) {
            String nodeId = currentResearchNode;

            // Notification setup
            com.quackers29.businesscraft.production.UpgradeNode node = com.quackers29.businesscraft.production.UpgradeRegistry
                    .get(nodeId);
            String displayName = (node != null) ? node.getDisplayName() : nodeId;

            unlockNode(currentResearchNode);
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
        if (unlockedNodes.add(nodeId)) {
            UpgradeNode node = UpgradeRegistry.get(nodeId);
            if (node != null) {
                recalculateModifiers();
            }
            town.markDirty();
        }
    }

    // Recalculates all active modifiers based on unlocked nodes
    private void recalculateModifiers() {
        activeModifiers.clear();
        for (String nodeId : unlockedNodes) {
            UpgradeNode node = UpgradeRegistry.get(nodeId);
            if (node == null)
                continue;

            for (Effect effect : node.getEffects()) {
                activeModifiers.merge(effect.getTarget(), effect.getValue(), Float::sum);
            }
        }
    }

    public float getModifier(String target) {
        return activeModifiers.getOrDefault(target, 0f);
    }

    public boolean isUnlocked(String nodeId) {
        return unlockedNodes.contains(nodeId);
    }

    public Set<String> getUnlockedNodes() {
        return Collections.unmodifiableSet(unlockedNodes);
    }

    @Override
    public void save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (String node : unlockedNodes) {
            list.add(StringTag.valueOf(node));
        }
        tag.put("unlockedNodes", list);

        if (currentResearchNode != null) {
            tag.putString("currentResearch", currentResearchNode);
            tag.putFloat("researchProgress", researchProgress);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        unlockedNodes.clear();
        if (tag.contains("unlockedNodes")) {
            ListTag list = tag.getList("unlockedNodes", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                unlockedNodes.add(list.getString(i));
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
