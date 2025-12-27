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

    public UpgradeNode(String id, String category, String displayName, List<String> prereqNodes,
            String description, List<Effect> effects) {
        this.id = id;
        this.category = category;
        this.displayName = displayName;
        this.prereqNodes = prereqNodes;
        this.description = description;
        this.effects = effects;
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
