package com.quackers29.businesscraft.town.viewmodel;

import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import net.minecraft.ChatFormatting;
import com.quackers29.businesscraft.town.viewmodel.TownInterfaceViewModel.Status;

/**
 * Server-side builder for TownInterfaceViewModel.
 * 
 * Handles all business logic for formatting town statistics into display
 * strings.
 * Ensures the client receives ready-to-render data.
 */
public class TownInterfaceViewModelBuilder {

    public static TownInterfaceViewModel build(Town town, TownInterfaceEntity entity) {
        if (town == null) {
            // Fallback for when town data isn't available yet
            return createErrorViewModel("Loading Town Data...", entity);
        }

        // 1. Biome Formatting
        String rawBiome = town.getBiome();
        String biomeFormatted = "Unknown";
        if (rawBiome != null && !rawBiome.isEmpty()) {
            biomeFormatted = formatBiomeName(rawBiome);
        }

        String biomeVariant = town.getBiomeVariant();
        String biomeVariantFormatted = (biomeVariant != null && !biomeVariant.isEmpty())
                ? formatBiomeName(biomeVariant)
                : "Standard";

        // 2. Population Display & Status
        long pop = town.getPopulation();
        int cap = (int) town.getUpgrades().getModifier("pop_cap");
        if (cap == 0)
            cap = 5; // Baseline fallback

        String popDisplay = String.format("%d / %d", pop, cap);
        Status popStatus = Status.NORMAL;
        if (pop >= cap) {
            popStatus = Status.WARNING; // Capped
        } else if (pop == 0) {
            popStatus = Status.CRITICAL; // Abandoned?
        }

        // 3. Happiness Display & Status
        float happiness = town.getHappiness();
        String happyDisplay = String.format("%.0f%%", happiness);
        Status happyStatus = Status.NORMAL;
        if (happiness < 30)
            happyStatus = Status.CRITICAL;
        else if (happiness < 50)
            happyStatus = Status.WARNING;
        else if (happiness > 80)
            happyStatus = Status.INFO; // High happiness

        // 4. Tourists Display
        long tourists = town.getTouristCount();
        long maxTourists = town.getMaxTourists();
        String touristDisplay = String.format("%d / %d", tourists, maxTourists);

        // 5. Work Units
        long wu = town.getWorkUnits();
        long wuCap = town.getWorkUnitCap();
        String wuDisplay = String.format("%d / %d", wu, wuCap);
        Status wuStatus = Status.NORMAL;
        if (wu >= wuCap && wuCap > 0)
            wuStatus = Status.WARNING;

        // 6. Settings
        boolean spawning = town.canSpawnTourists();
        // Uses entity method for auto-collect as it might be block-specific in
        // future/current logic
        boolean autoCollect = false; // Placeholder if not in Town object yet
        boolean taxes = false; // Placeholder

        // 7. Search Radius
        int radius = town.getSearchRadius();

        // 8. Cumulative Tourism Stats (for config requirements like "tourism" and "tourism_dist")
        long totalTouristsArrived = town.getTotalTouristsArrived();
        double totalTouristDistance = town.getTotalTouristDistance();
        float borderRadius = town.getBoundaryRadius();

        // 9. Warnings
        String warning = "";
        if (pop == 0)
            warning = "No Population!";

        return new TownInterfaceViewModel(
                town.getName(),
                biomeFormatted,
                biomeVariantFormatted,
                popDisplay,
                touristDisplay,
                happyDisplay,
                wuDisplay,
                popStatus,
                happyStatus,
                wuStatus,
                spawning,
                autoCollect,
                taxes,
                radius,
                (int) totalTouristsArrived,
                totalTouristDistance,
                borderRadius,
                warning);
    }

    private static TownInterfaceViewModel createErrorViewModel(String message, TownInterfaceEntity entity) {
        int radius = (entity != null) ? entity.getSearchRadius() : 10;
        return new TownInterfaceViewModel(
                message, "Unknown", "Unknown",
                "-/-", "-/-", "-%", "-/-",
                Status.CRITICAL, Status.CRITICAL, Status.CRITICAL,
                false, false, false,
                radius,
                0,    // totalTouristsArrived
                0.0,  // totalTouristDistance
                50f,  // borderRadius default
                "Town Data Unavailable");
    }

    private static String formatBiomeName(String raw) {
        if (raw.contains(":")) {
            raw = raw.substring(raw.lastIndexOf(":") + 1);
        }
        // Capitalize words (snake_case -> Title Case)
        String[] words = raw.replace("_", " ").split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.length() > 0) {
                sb.append(Character.toUpperCase(w.charAt(0)));
                sb.append(w.substring(1).toLowerCase());
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }
}
