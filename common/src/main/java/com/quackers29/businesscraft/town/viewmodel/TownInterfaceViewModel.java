package com.quackers29.businesscraft.town.viewmodel;

import net.minecraft.network.FriendlyByteBuf;
import java.util.HashMap;
import java.util.Map;

/**
 * Server-authoritative view-model for the main Town Interface.
 * 
 * This class contains only display strings and status flags, ensuring the
 * client
 * acts as a "dumb terminal" with zero business logic.
 * 
 * Replaces:
 * - TownOverviewSyncPacket (which sent raw data)
 * - ContainerData integer slots in TownInterfaceMenu
 * - Client-side fallback calculations in TownInterfaceMenu
 */
public class TownInterfaceViewModel {

    public enum Status {
        NORMAL, WARNING, CRITICAL, INFO
    }

    // Basic Info
    private final String townName;
    private final String biomeFormatted;
    private final String biomeVariantFormatted;

    // Stats as Display Strings
    private final String populationDisplay; // e.g. "5/10"
    private final String touristsDisplay; // e.g. "2/5"
    private final String happinessDisplay; // e.g. "85%"
    private final String workUnitsDisplay; // e.g. "10/20"

    // Status Indicators
    private final Status populationStatus;
    private final Status happinessStatus;
    private final Status workUnitsStatus;

    // Settings (True/False state only)
    private final boolean spawningEnabled;
    private final boolean autoCollectEnabled;
    private final boolean taxesEnabled;

    // Configurable values that need to be edited
    private final int searchRadius;

    // Warnings (Server-side validation)
    private final String primaryWarning; // e.g. "Town Hall Missing!" or empty

    public TownInterfaceViewModel(
            String townName,
            String biomeFormatted,
            String biomeVariantFormatted,
            String populationDisplay,
            String touristsDisplay,
            String happinessDisplay,
            String workUnitsDisplay,
            Status populationStatus,
            Status happinessStatus,
            Status workUnitsStatus,
            boolean spawningEnabled,
            boolean autoCollectEnabled,
            boolean taxesEnabled,
            int searchRadius,
            String primaryWarning) {
        this.townName = townName;
        this.biomeFormatted = biomeFormatted;
        this.biomeVariantFormatted = biomeVariantFormatted;
        this.populationDisplay = populationDisplay;
        this.touristsDisplay = touristsDisplay;
        this.happinessDisplay = happinessDisplay;
        this.workUnitsDisplay = workUnitsDisplay;
        this.populationStatus = populationStatus;
        this.happinessStatus = happinessStatus;
        this.workUnitsStatus = workUnitsStatus;
        this.spawningEnabled = spawningEnabled;
        this.autoCollectEnabled = autoCollectEnabled;
        this.taxesEnabled = taxesEnabled;
        this.searchRadius = searchRadius;
        this.primaryWarning = primaryWarning;
    }

    public TownInterfaceViewModel(FriendlyByteBuf buf) {
        this.townName = buf.readUtf();
        this.biomeFormatted = buf.readUtf();
        this.biomeVariantFormatted = buf.readUtf();
        this.populationDisplay = buf.readUtf();
        this.touristsDisplay = buf.readUtf();
        this.happinessDisplay = buf.readUtf();
        this.workUnitsDisplay = buf.readUtf();
        this.populationStatus = buf.readEnum(Status.class);
        this.happinessStatus = buf.readEnum(Status.class);
        this.workUnitsStatus = buf.readEnum(Status.class);
        this.spawningEnabled = buf.readBoolean();
        this.autoCollectEnabled = buf.readBoolean();
        this.taxesEnabled = buf.readBoolean();
        this.searchRadius = buf.readInt();
        this.primaryWarning = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(townName);
        buf.writeUtf(biomeFormatted);
        buf.writeUtf(biomeVariantFormatted);
        buf.writeUtf(populationDisplay);
        buf.writeUtf(touristsDisplay);
        buf.writeUtf(happinessDisplay);
        buf.writeUtf(workUnitsDisplay);
        buf.writeEnum(populationStatus);
        buf.writeEnum(happinessStatus);
        buf.writeEnum(workUnitsStatus);
        buf.writeBoolean(spawningEnabled);
        buf.writeBoolean(autoCollectEnabled);
        buf.writeBoolean(taxesEnabled);
        buf.writeInt(searchRadius);
        buf.writeUtf(primaryWarning);
    }

    // Getters
    public String getTownName() {
        return townName;
    }

    public String getBiomeFormatted() {
        return biomeFormatted;
    }

    public String getBiomeVariantFormatted() {
        return biomeVariantFormatted;
    }

    public String getPopulationDisplay() {
        return populationDisplay;
    }

    public String getTouristsDisplay() {
        return touristsDisplay;
    }

    public String getHappinessDisplay() {
        return happinessDisplay;
    }

    public String getWorkUnitsDisplay() {
        return workUnitsDisplay;
    }

    public Status getPopulationStatus() {
        return populationStatus;
    }

    public Status getHappinessStatus() {
        return happinessStatus;
    }

    public Status getWorkUnitsStatus() {
        return workUnitsStatus;
    }

    public boolean isSpawningEnabled() {
        return spawningEnabled;
    }

    public boolean isAutoCollectEnabled() {
        return autoCollectEnabled;
    }

    public boolean isTaxesEnabled() {
        return taxesEnabled;
    }

    public int getSearchRadius() {
        return searchRadius;
    }

    public String getPrimaryWarning() {
        return primaryWarning;
    }

    public boolean hasWarning() {
        return !primaryWarning.isEmpty();
    }
}
