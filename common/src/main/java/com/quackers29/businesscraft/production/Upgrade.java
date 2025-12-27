package com.quackers29.businesscraft.production;

import java.util.HashMap;
import java.util.Map;

public class Upgrade {
    public enum UpgradeTrack {
        RESOURCES, SERVICES, POPULATION
    }

    private final String id;
    private final String name;
    private final int populationReq;
    private final UpgradeTrack track;
    private final int tier;
    private final float happinessBonus;
    private final int housingCapacity;

    private final Map<String, Float> inputRates = new HashMap<>();
    private final Map<String, Float> outputRates = new HashMap<>();
    private final Map<String, Integer> buildCost = new HashMap<>();

    public Upgrade(String id, String name, int populationReq, UpgradeTrack track, int tier, float happinessBonus,
            int housingCapacity) {
        this.id = id;
        this.name = name;
        this.populationReq = populationReq;
        this.track = track;
        this.tier = tier;
        this.happinessBonus = happinessBonus;
        this.housingCapacity = housingCapacity;
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

    public UpgradeTrack getTrack() {
        return track;
    }

    public int getTier() {
        return tier;
    }

    public float getHappinessBonus() {
        return happinessBonus;
    }

    public int getHousingCapacity() {
        return housingCapacity;
    }

    public Map<String, Float> getInputRates() {
        return inputRates;
    }

    public Map<String, Float> getOutputRates() {
        return outputRates;
    }

    public Map<String, Integer> getBuildCost() {
        return buildCost;
    }

    public void addInput(String resourceId, float rate) {
        inputRates.put(resourceId, rate);
    }

    public void addOutput(String resourceId, float rate) {
        outputRates.put(resourceId, rate);
    }

    public void addBuildCost(String resourceId, int amount) {
        buildCost.put(resourceId, amount);
    }
}
