package com.quackers29.businesscraft.town.viewmodel;
import com.quackers29.businesscraft.town.viewmodel.IViewModel;

import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * View-Model for upgrade system display on the client side.
 * Contains ONLY pre-calculated display data from the server.
 * Client performs ZERO calculations or config file reads.
 *
 * This implements the "dumb terminal" pattern where:
 * - Server calculates all upgrade effects, costs, and research times
 * - Client receives ready-to-display data with upgrade names from server config
 * - No upgrade registry or effect calculation access on client
 */
public class UpgradeStatusViewModel implements IViewModel {

    /**
     * Display information for a single upgrade node at a specific level
     */
    public static class UpgradeDisplayInfo {
        private final String nodeId;                // e.g., "basic_farming"
        private final String displayName;           // e.g., "Basic Farming" (from server CSV)
        private final String category;              // e.g., "farming"
        private final String description;           // e.g., "Starts food production"
        private final int currentLevel;             // 0 if not unlocked, 1+ if unlocked
        private final int maxLevel;                 // -1 for infinite, else max repeats
        private final boolean isRepeatable;         // true if can be researched multiple times
        private final boolean isUnlocked;           // true if currentLevel > 0
        private final boolean isMaxed;              // true if at maximum level
        private final boolean canResearch;          // true if prerequisites met and can afford
        private final boolean isCurrentResearch;    // true if actively researching now
        
        // Pre-calculated display strings (NO CLIENT CALCULATIONS)
        private final String statusText;            // e.g., "Unlocked", "Researching...", "Locked", "Prerequisites Missing"
        private final String progressText;          // e.g., "45% Complete", "Ready to Research"
        private final String researchTimeText;      // e.g., "2.5 minutes" (with research speed applied)
        private final String baseResearchTimeText;  // e.g., "5.0 minutes (base)" (before research speed)
        private final String costsText;             // e.g., "Wood: 10, Stone: 5"
        private final String requirementsText;      // e.g., "Tourism Count: 100, Pop: 50"
        private final String effectsText;           // e.g., "+10% Research Speed, +5 Storage Cap"
        private final String prerequisitesText;     // e.g., "Requires: Basic Settlement"
        private final float aiScore;                // AI priority score from server
        private final float progressPercentage;     // 0.0 to 1.0 (for progress bars)

        public UpgradeDisplayInfo(String nodeId, String displayName, String category, String description,
                                 int currentLevel, int maxLevel, boolean isRepeatable, boolean isUnlocked,
                                 boolean isMaxed, boolean canResearch, boolean isCurrentResearch,
                                 String statusText, String progressText, String researchTimeText,
                                 String baseResearchTimeText, String costsText, String requirementsText,
                                 String effectsText, String prerequisitesText, float aiScore,
                                 float progressPercentage) {
            this.nodeId = nodeId;
            this.displayName = displayName;
            this.category = category;
            this.description = description;
            this.currentLevel = currentLevel;
            this.maxLevel = maxLevel;
            this.isRepeatable = isRepeatable;
            this.isUnlocked = isUnlocked;
            this.isMaxed = isMaxed;
            this.canResearch = canResearch;
            this.isCurrentResearch = isCurrentResearch;
            this.statusText = statusText;
            this.progressText = progressText;
            this.researchTimeText = researchTimeText;
            this.baseResearchTimeText = baseResearchTimeText;
            this.costsText = costsText;
            this.requirementsText = requirementsText;
            this.effectsText = effectsText;
            this.prerequisitesText = prerequisitesText;
            this.aiScore = aiScore;
            this.progressPercentage = progressPercentage;
        }

        public UpgradeDisplayInfo(FriendlyByteBuf buf) {
            this.nodeId = buf.readUtf();
            this.displayName = buf.readUtf();
            this.category = buf.readUtf();
            this.description = buf.readUtf();
            this.currentLevel = buf.readInt();
            this.maxLevel = buf.readInt();
            this.isRepeatable = buf.readBoolean();
            this.isUnlocked = buf.readBoolean();
            this.isMaxed = buf.readBoolean();
            this.canResearch = buf.readBoolean();
            this.isCurrentResearch = buf.readBoolean();
            this.statusText = buf.readUtf();
            this.progressText = buf.readUtf();
            this.researchTimeText = buf.readUtf();
            this.baseResearchTimeText = buf.readUtf();
            this.costsText = buf.readUtf();
            this.requirementsText = buf.readUtf();
            this.effectsText = buf.readUtf();
            this.prerequisitesText = buf.readUtf();
            this.aiScore = buf.readFloat();
            this.progressPercentage = buf.readFloat();
        }

        public void toBytes(FriendlyByteBuf buf) {
            buf.writeUtf(nodeId);
            buf.writeUtf(displayName);
            buf.writeUtf(category);
            buf.writeUtf(description);
            buf.writeInt(currentLevel);
            buf.writeInt(maxLevel);
            buf.writeBoolean(isRepeatable);
            buf.writeBoolean(isUnlocked);
            buf.writeBoolean(isMaxed);
            buf.writeBoolean(canResearch);
            buf.writeBoolean(isCurrentResearch);
            buf.writeUtf(statusText);
            buf.writeUtf(progressText);
            buf.writeUtf(researchTimeText);
            buf.writeUtf(baseResearchTimeText);
            buf.writeUtf(costsText);
            buf.writeUtf(requirementsText);
            buf.writeUtf(effectsText);
            buf.writeUtf(prerequisitesText);
            buf.writeFloat(aiScore);
            buf.writeFloat(progressPercentage);
        }

        // Getters for client display (NO CALCULATIONS)
        public String getNodeId() { return nodeId; }
        public String getDisplayName() { return displayName; }
        public String getCategory() { return category; }
        public String getDescription() { return description; }
        public int getCurrentLevel() { return currentLevel; }
        public int getMaxLevel() { return maxLevel; }
        public boolean isRepeatable() { return isRepeatable; }
        public boolean isUnlocked() { return isUnlocked; }
        public boolean isMaxed() { return isMaxed; }
        public boolean canResearch() { return canResearch; }
        public boolean isCurrentResearch() { return isCurrentResearch; }
        public String getStatusText() { return statusText; }
        public String getProgressText() { return progressText; }
        public String getResearchTimeText() { return researchTimeText; }
        public String getBaseResearchTimeText() { return baseResearchTimeText; }
        public String getCostsText() { return costsText; }
        public String getRequirementsText() { return requirementsText; }
        public String getEffectsText() { return effectsText; }
        public String getPrerequisitesText() { return prerequisitesText; }
        public float getAiScore() { return aiScore; }
        public float getProgressPercentage() { return progressPercentage; }
    }

    // Map of node IDs to their display information
    private final Map<String, UpgradeDisplayInfo> upgradeInfo;

    // Lists for UI rendering (pre-sorted by server)
    private final List<String> unlockedUpgradeIds;      // IDs of unlocked upgrades
    private final List<String> researchableUpgradeIds;  // IDs of upgrades that can be researched (sorted by AI score)
    private final List<String> lockedUpgradeIds;        // IDs of locked upgrades (prerequisites not met)

    // Overall upgrade summary (pre-calculated by server)
    private final String totalUnlockedUpgrades;     // e.g., "5 upgrades unlocked"
    private final String currentResearchStatus;     // e.g., "Researching: Basic Farming (45%)", "Idle - Select Research"
    private final String researchSpeedText;         // e.g., "Research Speed: 150%" (with breakdown)
    private final String researchSpeedTooltip;      // e.g., "Base: 100%\nLibrary: +30%\nSchool (+2): +20%"
    private final float researchSpeedMultiplier;    // e.g., 1.5 for 150% speed

    public UpgradeStatusViewModel(Map<String, UpgradeDisplayInfo> upgradeInfo,
                                 List<String> unlockedUpgradeIds,
                                 List<String> researchableUpgradeIds,
                                 List<String> lockedUpgradeIds,
                                 String totalUnlockedUpgrades,
                                 String currentResearchStatus,
                                 String researchSpeedText,
                                 String researchSpeedTooltip,
                                 float researchSpeedMultiplier) {
        this.upgradeInfo = new HashMap<>(upgradeInfo);
        this.unlockedUpgradeIds = new ArrayList<>(unlockedUpgradeIds);
        this.researchableUpgradeIds = new ArrayList<>(researchableUpgradeIds);
        this.lockedUpgradeIds = new ArrayList<>(lockedUpgradeIds);
        this.totalUnlockedUpgrades = totalUnlockedUpgrades;
        this.currentResearchStatus = currentResearchStatus;
        this.researchSpeedText = researchSpeedText;
        this.researchSpeedTooltip = researchSpeedTooltip;
        this.researchSpeedMultiplier = researchSpeedMultiplier;
    }

    public UpgradeStatusViewModel(FriendlyByteBuf buf) {
        // Read upgrade info
        int count = buf.readInt();
        this.upgradeInfo = new HashMap<>();

        for (int i = 0; i < count; i++) {
            UpgradeDisplayInfo info = new UpgradeDisplayInfo(buf);
            this.upgradeInfo.put(info.getNodeId(), info);
        }

        // Read sorted lists
        int unlockedCount = buf.readInt();
        this.unlockedUpgradeIds = new ArrayList<>();
        for (int i = 0; i < unlockedCount; i++) {
            this.unlockedUpgradeIds.add(buf.readUtf());
        }

        int researchableCount = buf.readInt();
        this.researchableUpgradeIds = new ArrayList<>();
        for (int i = 0; i < researchableCount; i++) {
            this.researchableUpgradeIds.add(buf.readUtf());
        }

        int lockedCount = buf.readInt();
        this.lockedUpgradeIds = new ArrayList<>();
        for (int i = 0; i < lockedCount; i++) {
            this.lockedUpgradeIds.add(buf.readUtf());
        }

        // Read summary data
        this.totalUnlockedUpgrades = buf.readUtf();
        this.currentResearchStatus = buf.readUtf();
        this.researchSpeedText = buf.readUtf();
        this.researchSpeedTooltip = buf.readUtf();
        this.researchSpeedMultiplier = buf.readFloat();
    }

    public void toBytes(FriendlyByteBuf buf) {
        // Write upgrade info
        buf.writeInt(upgradeInfo.size());

        upgradeInfo.values().forEach(info -> info.toBytes(buf));

        // Write sorted lists
        buf.writeInt(unlockedUpgradeIds.size());
        unlockedUpgradeIds.forEach(buf::writeUtf);

        buf.writeInt(researchableUpgradeIds.size());
        researchableUpgradeIds.forEach(buf::writeUtf);

        buf.writeInt(lockedUpgradeIds.size());
        lockedUpgradeIds.forEach(buf::writeUtf);

        // Write summary data
        buf.writeUtf(totalUnlockedUpgrades);
        buf.writeUtf(currentResearchStatus);
        buf.writeUtf(researchSpeedText);
        buf.writeUtf(researchSpeedTooltip);
        buf.writeFloat(researchSpeedMultiplier);
    }

    // Client-side getters (NO CALCULATIONS)
    public Map<String, UpgradeDisplayInfo> getUpgradeInfo() {
        return upgradeInfo;
    }

    public UpgradeDisplayInfo getUpgradeInfo(String nodeId) {
        return upgradeInfo.get(nodeId);
    }

    public List<String> getUnlockedUpgradeIds() {
        return unlockedUpgradeIds;
    }

    public List<String> getResearchableUpgradeIds() {
        return researchableUpgradeIds;
    }

    public List<String> getLockedUpgradeIds() {
        return lockedUpgradeIds;
    }

    public String getTotalUnlockedUpgrades() {
        return totalUnlockedUpgrades;
    }

    public String getCurrentResearchStatus() {
        return currentResearchStatus;
    }

    public String getResearchSpeedText() {
        return researchSpeedText;
    }

    public String getResearchSpeedTooltip() {
        return researchSpeedTooltip;
    }

    public float getResearchSpeedMultiplier() {
        return researchSpeedMultiplier;
    }

    public boolean hasUpgrade(String nodeId) {
        return upgradeInfo.containsKey(nodeId);
    }

    public int getUpgradeCount() {
        return upgradeInfo.size();
    }

    public int getUnlockedCount() {
        return unlockedUpgradeIds.size();
    }
}
