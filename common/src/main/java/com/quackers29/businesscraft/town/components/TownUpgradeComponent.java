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

    public TownUpgradeComponent(Town town) {
        this.town = town;
    }

    @Override
    public void tick() {
        if (currentResearchNode != null) {
            UpgradeNode node = UpgradeRegistry.get(currentResearchNode);
            if (node == null) {
                currentResearchNode = null;
                return;
            }

            // Simple logic: research progresses automatically if selected.
            // Future requirement: consume research resources daily?
            // "upgrade_requirements.csv" has "required_items" (setup cost?) and
            // "research_days".
            // Implementation: We assume "required_items" are paid UP FRONT to start.
            // "research_days" is time to complete.

            // Increment progress
            // ConfigLoader.dailyTickInterval is how many ticks per day.
            // We need a reference.
            // Assuming this tick() runs every tick?
            // Or only once per day?
            // Town.tick() calls components. usually every tick.

            // Let's assume tick() is every tick. We need to know day length.
            // Hardcoded reference or config?
            // ConfigLoader.dailyTickInterval is available.

            researchProgress += 1.0f / com.quackers29.businesscraft.config.ConfigLoader.dailyTickInterval;

            if (researchProgress >= node.getResearchDays()) {
                completeResearch();
            }
        }
    }

    public void startResearch(String nodeId) {
        if (unlockedNodes.contains(nodeId))
            return;
        UpgradeNode node = UpgradeRegistry.get(nodeId);
        if (node == null)
            return;

        // check prereqs
        if (node.getPrereqNodes() != null) {
            for (String pre : node.getPrereqNodes()) {
                if (!unlockedNodes.contains(pre))
                    return; // Prereq missing
            }
        }

        // check costs
        if (node.getCosts() != null && !node.getCosts().isEmpty()) {
            // Check if can afford
            for (ResourceAmount ra : node.getCosts()) {
                if (town.getTrading().getStock(ra.resourceId) < ra.amount) {
                    return; // Cannot afford
                }
            }
            // Dedut
            for (ResourceAmount ra : node.getCosts()) {
                town.getTrading().adjustStock(ra.resourceId, -ra.amount);
            }
        }

        this.currentResearchNode = nodeId;
        this.researchProgress = 0;
        town.markDirty();
    }

    public void completeResearch() {
        if (currentResearchNode != null) {
            unlockNode(currentResearchNode);
            currentResearchNode = null;
            researchProgress = 0;
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
