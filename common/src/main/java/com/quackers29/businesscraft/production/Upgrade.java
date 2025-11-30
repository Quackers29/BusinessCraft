package com.quackers29.businesscraft.production;

import java.util.HashMap;
import java.util.Map;

public class Upgrade {
    private final String id;
    private final String name;
    private final int populationReq;
    private final Map<String, Float> inputRates = new HashMap<>();
    private final Map<String, Float> outputRates = new HashMap<>();

    public Upgrade(String id, String name, int populationReq) {
        this.id = id;
        this.name = name;
        this.populationReq = populationReq;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getPopulationReq() {
        return populationReq;
    }

    public Map<String, Float> getInputRates() {
        return inputRates;
    }

    public Map<String, Float> getOutputRates() {
        return outputRates;
    }

    public void addInput(String resourceId, float rate) {
        inputRates.put(resourceId, rate);
    }

    public void addOutput(String resourceId, float rate) {
        outputRates.put(resourceId, rate);
    }
}
