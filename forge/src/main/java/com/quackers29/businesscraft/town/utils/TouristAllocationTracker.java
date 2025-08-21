package com.quackers29.businesscraft.town.utils;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.quackers29.businesscraft.debug.DebugConfig;

/**
 * Utility class for tracking tourist allocations between towns
 * Ensures fair distribution of tourists based on population proportions
 */
public class TouristAllocationTracker {
    private static final Logger LOGGER = LoggerFactory.getLogger(TouristAllocationTracker.class);
    
    // Map of origin town ID to target town allocation trackers
    private static final Map<UUID, DestinationTracker> originTrackers = new HashMap<>();
    
    /**
     * Records a tourist spawn for tracking allocation fairness
     * 
     * @param originTownId The origin town ID
     * @param destinationTownId The destination town ID 
     */
    public static void recordTouristSpawn(UUID originTownId, UUID destinationTownId) {
        if (originTownId == null || destinationTownId == null) return;
        
        DestinationTracker tracker = originTrackers.computeIfAbsent(originTownId, 
            id -> new DestinationTracker());
        
        tracker.recordSpawn(destinationTownId);
        DebugConfig.debug(LOGGER, DebugConfig.TOURIST_ENTITY, "Recorded tourist spawn from {} to {}, current allocation: {}", 
            originTownId, destinationTownId, tracker.getAllocationStats());
    }
    
    /**
     * Removes a tourist from the tracker when they're removed from the game
     * 
     * @param originTownId The origin town ID
     * @param destinationTownId The destination town ID
     */
    public static void recordTouristRemoval(UUID originTownId, UUID destinationTownId) {
        if (originTownId == null || destinationTownId == null) return;
        
        DestinationTracker tracker = originTrackers.get(originTownId);
        if (tracker != null) {
            tracker.recordRemoval(destinationTownId);
            DebugConfig.debug(LOGGER, DebugConfig.TOURIST_ENTITY, "Recorded tourist removal from {} to {}, current allocation: {}", 
                originTownId, destinationTownId, tracker.getAllocationStats());
        }
    }
    
    /**
     * Selects the next destination town based on fair population-based allocation
     * 
     * @param originTownId The origin town ID
     * @param destinationOptions Map of possible destination town IDs to their population
     * @return The selected destination town ID
     */
    public static UUID selectFairDestination(UUID originTownId, Map<UUID, Integer> destinationOptions) {
        if (originTownId == null || destinationOptions == null || destinationOptions.isEmpty()) {
            return null;
        }
        
        // Get or create a tracker for this origin town
        DestinationTracker tracker = originTrackers.computeIfAbsent(originTownId, 
            id -> new DestinationTracker());
        
        // Let the tracker select the next fair destination
        return tracker.selectNextDestination(destinationOptions);
    }
    
    /**
     * Inner class to track allocations for a specific origin town
     */
    private static class DestinationTracker {
        // Track current tourist count per destination
        private final Map<UUID, Integer> currentAllocations = new HashMap<>();
        // Track target allocations based on population proportion
        private final Map<UUID, Double> targetAllocations = new HashMap<>();
        
        // Total number of active tourists
        private int totalTourists = 0;
        
        /**
         * Records a tourist spawn to a destination
         */
        public void recordSpawn(UUID destinationTownId) {
            currentAllocations.put(destinationTownId, 
                currentAllocations.getOrDefault(destinationTownId, 0) + 1);
            totalTourists++;
        }
        
        /**
         * Records a tourist removal from a destination
         */
        public void recordRemoval(UUID destinationTownId) {
            int current = currentAllocations.getOrDefault(destinationTownId, 0);
            if (current > 0) {
                currentAllocations.put(destinationTownId, current - 1);
                totalTourists = Math.max(0, totalTourists - 1);
            }
        }
        
        /**
         * Calculates the fairness gap between target and actual allocations
         * 
         * @param townId The town ID to check
         * @param population The town's population
         * @param totalPopulation The total population of all destinations
         * @return The fairness gap (negative means under-allocated, positive means over-allocated)
         */
        private double calculateFairnessGap(UUID townId, int population, int totalPopulation) {
            if (totalPopulation == 0 || totalTourists == 0) return 0;
            
            // Calculate target allocation based on population proportion
            double targetProportion = (double) population / totalPopulation;
            double targetCount = targetProportion * totalTourists;
            
            // Calculate current allocation
            int currentCount = currentAllocations.getOrDefault(townId, 0);
            
            // Store target allocation for reference
            targetAllocations.put(townId, targetCount);
            
            // Return the difference (how over/under allocated this town is)
            return currentCount - targetCount;
        }
        
        /**
         * Selects the next destination based on fairness gaps
         */
        public UUID selectNextDestination(Map<UUID, Integer> destinationOptions) {
            if (destinationOptions.isEmpty()) return null;
            
            // If only one option, return it
            if (destinationOptions.size() == 1) {
                return destinationOptions.keySet().iterator().next();
            }
            
            // Calculate total population
            int totalPopulation = 0;
            for (int population : destinationOptions.values()) {
                totalPopulation += population;
            }
            
            // If no population data, use equal weighting
            if (totalPopulation == 0) {
                List<UUID> townIds = new ArrayList<>(destinationOptions.keySet());
                return townIds.get(new Random().nextInt(townIds.size()));
            }
            
            // Calculate fairness gaps for all destinations
            Map<UUID, Double> fairnessGaps = new HashMap<>();
            for (Map.Entry<UUID, Integer> entry : destinationOptions.entrySet()) {
                fairnessGaps.put(entry.getKey(), 
                    calculateFairnessGap(entry.getKey(), entry.getValue(), totalPopulation));
            }
            
            // There's a small chance (10%) to select a random under-allocated town
            // instead of always picking the most under-allocated one
            Random random = new Random();
            if (random.nextDouble() < 0.10) {
                // Build a list of all under-allocated towns (negative fairness gap)
                List<UUID> underAllocatedTowns = new ArrayList<>();
                for (Map.Entry<UUID, Double> entry : fairnessGaps.entrySet()) {
                    if (entry.getValue() < 0) {
                        underAllocatedTowns.add(entry.getKey());
                    }
                }
                
                // If we have under-allocated towns, pick one randomly
                if (!underAllocatedTowns.isEmpty()) {
                    return underAllocatedTowns.get(random.nextInt(underAllocatedTowns.size()));
                }
            }
            
            // Default behavior: Find the town with the most negative fairness gap (most under-allocated)
            UUID selectedTown = null;
            double lowestGap = Double.MAX_VALUE;
            
            for (Map.Entry<UUID, Double> entry : fairnessGaps.entrySet()) {
                if (entry.getValue() < lowestGap) {
                    lowestGap = entry.getValue();
                    selectedTown = entry.getKey();
                }
            }
            
            return selectedTown;
        }
        
        /**
         * Gets a string representation of the current allocation stats
         */
        public String getAllocationStats() {
            StringBuilder sb = new StringBuilder();
            sb.append("Total tourists: ").append(totalTourists).append(", Allocations: {");
            
            boolean first = true;
            for (Map.Entry<UUID, Integer> entry : currentAllocations.entrySet()) {
                if (!first) sb.append(", ");
                first = false;
                
                double target = targetAllocations.getOrDefault(entry.getKey(), 0.0);
                sb.append(entry.getKey().toString(), 0, 8)
                  .append(": ").append(entry.getValue())
                  .append("/").append(String.format("%.1f", target));
            }
            
            sb.append("}");
            return sb.toString();
        }
    }
} 