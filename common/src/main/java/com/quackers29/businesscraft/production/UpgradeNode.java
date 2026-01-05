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
    private float researchMinutes = 0;
    private List<ResourceAmount> costs = null;
    // Repeat logic
    private int maxRepeats = 0; // 0 or 1 = once, >1 = repeats, -1 = infinite
    private float costMultiplier = 1.0f;

    private float benefitMultiplier = 1.0f;

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
            String costStr = parts[1].trim();
            try {
                // Determine multiplier (Always exponential/compound now)
                // Remove optional caret if user left it in legacy config to avoid crashing
                if (costStr.startsWith("^")) {
                    costStr = costStr.substring(1);
                }
                this.costMultiplier = Float.parseFloat(costStr);
            } catch (NumberFormatException e) {
                this.costMultiplier = 1.0f;
            }
        }

        if (parts.length > 2) {
            String benefitStr = parts[2].trim();
            try {
                this.benefitMultiplier = Float.parseFloat(benefitStr);
            } catch (NumberFormatException e) {
                this.benefitMultiplier = 1.0f;
            }
        }
    }

    public void setRequirements(float researchMinutes, List<ResourceAmount> costs) {
        this.researchMinutes = researchMinutes;
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

    public float getBenefitMultiplier() {
        return benefitMultiplier;
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

    public float getResearchMinutes() {
        return researchMinutes;
    }

    public List<ResourceAmount> getCosts() {
        return costs;
    }

    /**
     * Calculates the value of an effect at a specific level.
     * Centralizes logic to ensure UI and Server consistency.
     */
    public float calculateEffectValue(Effect effect, int level) {
        if (level <= 0)
            return 0f;

        // Exponential/Compound: Base * Mult^(Level - 1)
        // Level 1: Mult^0 = 1.0 -> Base Value
        // Level 2: Mult^1 = 1.1 -> Base * 1.1 etc.
        boolean useExponentialBenefit = Math.abs(benefitMultiplier - 1.0f) > 0.0001f;

        if (useExponentialBenefit) {
            return effect.getValue() * (float) Math.pow(benefitMultiplier, level - 1);
        } else {
            // Linear: Base * Level (Accumulation)
            // Note: Use simple level multiplication for Linear as requested previously?
            // "Linear (Default, No 3rd param): Base * Level"
            // If we want consistent "Level 1 = Base", then Linear should just be Base *
            // Level.
            // (1 * Base = Base). So this logic remains fine.
            return effect.getValue() * level;
        }
    }
}
