package com.quackers29.businesscraft.production;

import com.quackers29.businesscraft.data.parsers.Condition;
import com.quackers29.businesscraft.data.parsers.DataParser.ResourceAmount;
import java.util.List;

public class ProductionRecipe {
    private final String id;
    private final String displayName;
    private final float baseCycleTimeDays;
    private final List<ResourceAmount> inputs;
    private final List<ResourceAmount> outputs;
    private final List<Condition> conditions;

    public ProductionRecipe(String id, String displayName, float baseCycleTimeDays,
            List<ResourceAmount> inputs, List<ResourceAmount> outputs,
            List<Condition> conditions) {
        this.id = id;
        this.displayName = displayName;
        this.baseCycleTimeDays = baseCycleTimeDays;
        this.inputs = inputs;
        this.outputs = outputs;
        this.conditions = conditions;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public float getBaseCycleTimeDays() {
        return baseCycleTimeDays;
    }

    public List<ResourceAmount> getInputs() {
        return inputs;
    }

    public List<ResourceAmount> getOutputs() {
        return outputs;
    }

    public List<Condition> getConditions() {
        return conditions;
    }
}
