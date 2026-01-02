package com.quackers29.businesscraft.production;

import com.quackers29.businesscraft.data.parsers.Effect;
import com.quackers29.businesscraft.data.parsers.DataParser.ResourceAmount;
import java.util.List;

public class UpgradeNode {
    private final String id;
    private final String category;
    private final String displayName;
    private final List<String> prereqNodes;
    private final String description;
    private final List<Effect> effects;

    // Requirements from upgrade_requirements.csv
    private float researchDays = 0;
    private List<ResourceAmount> costs = null;
    // Repeat logic
    private int maxRepeats = 0; // 0 or 1 = once, >1 = repeats, -1 = infinite
    private float costMultiplier = 1.0f;

    public UpgradeNode(String id, String category, String displayName, String repeatConfig, List<String> prereqNodes,
            String description, List<Effect> effects) {
        this.id = id;
        this.category = category;
        this.displayName = displayName;
        this.prereqNodes = prereqNodes;
        this.description = description;
        this.effects = effects;

        parseRepeatConfig(repeatConfig);
    }

    private void parseRepeatConfig(String config) {
        if (config == null || config.trim().isEmpty()) {
            this.maxRepeats = 1;
            return;
        }

        String[] parts = config.split(":");
        String countStr = parts[0].trim();

        if ("infinite".equalsIgnoreCase(countStr)) {
            this.maxRepeats = -1;
        } else {
            try {
                this.maxRepeats = Integer.parseInt(countStr);
            } catch (NumberFormatException e) {
                this.maxRepeats = 1;
            }
        }

        if (parts.length > 1) {
            try {
                this.costMultiplier = Float.parseFloat(parts[1].trim());
            } catch (NumberFormatException e) {
                this.costMultiplier = 1.0f;
            }
        }
    }

    public void setRequirements(float researchDays, List<ResourceAmount> costs) {
        this.researchDays = researchDays;
        this.costs = costs;
    }

    public String getId() {
        return id;
    }

    public String getCategory() {
        return category;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isRepeatable() {
        return maxRepeats == -1 || maxRepeats > 1;
    }

    public int getMaxRepeats() {
        return maxRepeats;
    }

    public float getCostMultiplier() {
        return costMultiplier;
    }

    public List<String> getPrereqNodes() {
        return prereqNodes;
    }

    public String getDescription() {
        return description;
    }

    public List<Effect> getEffects() {
        return effects;
    }

    public float getResearchDays() {
        return researchDays;
    }

    public List<ResourceAmount> getCosts() {
        return costs;
    }
}
