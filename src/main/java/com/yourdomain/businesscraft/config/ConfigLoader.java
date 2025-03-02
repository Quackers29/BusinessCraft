package com.yourdomain.businesscraft.config;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mojang.logging.LogUtils;

public class ConfigLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final ConfigLoader INSTANCE = new ConfigLoader();
    
    // Vehicle-related config
    public static boolean enableCreateTrains = true;
    public static boolean enableMinecarts = true;
    public static int vehicleSearchRadius = 10;
    public static double minecartStopThreshold = 0.001;
    
    // Town-related config
    public static List<String> townNames = new ArrayList<>();
    public static int breadPerPop = 1;
    public static int minPopForTourists = 5;
    public static int minDistanceBetweenTowns = 100; // Minimum distance between towns in blocks
    public static int defaultStartingPopulation = 5; // Default starting population for new towns
    public static int maxTouristsPerTown = 10; // Maximum number of tourists per town
    public static int populationPerTourist = 10; // Population required for each tourist (1 tourist per 10 population)
    public static int maxPopBasedTourists = 20; // Maximum population-based tourists (200 pop = 20 tourists)
    
    private ConfigLoader() {
        loadConfig();
    }
    
    public static void loadConfig() {
        Properties props = new Properties();
        
        try {
            File configFile = new File("config/businesscraft.properties");
            if (!configFile.exists()) {
                configFile.getParentFile().mkdirs();
                saveConfig();
                return;
            }
            
            FileReader reader = new FileReader(configFile);
            props.load(reader);
            reader.close();
            
            // Vehicle settings
            enableCreateTrains = Boolean.parseBoolean(props.getProperty("enableCreateTrains", "true"));
            enableMinecarts = Boolean.parseBoolean(props.getProperty("enableMinecarts", "true"));
            vehicleSearchRadius = Integer.parseInt(props.getProperty("vehicleSearchRadius", "10"));
            minecartStopThreshold = Double.parseDouble(props.getProperty("minecartStopThreshold", "0.001"));
            
            // Town settings
            breadPerPop = Integer.parseInt(props.getProperty("breadPerPop", "1"));
            minPopForTourists = Integer.parseInt(props.getProperty("minPopForTourists", "5"));
            minDistanceBetweenTowns = Integer.parseInt(props.getProperty("minDistanceBetweenTowns", "100"));
            defaultStartingPopulation = Integer.parseInt(props.getProperty("defaultStartingPopulation", "5"));
            maxTouristsPerTown = Integer.parseInt(props.getProperty("maxTouristsPerTown", "10"));
            populationPerTourist = Integer.parseInt(props.getProperty("populationPerTourist", "10"));
            maxPopBasedTourists = Integer.parseInt(props.getProperty("maxPopBasedTourists", "20"));
            
            // Load town names
            String namesStr = props.getProperty("townNames", "");
            townNames = new ArrayList<>(Arrays.asList(namesStr.split(",")));
            if (townNames.isEmpty()) {
                townNames.addAll(Arrays.asList("Riverside", "Hillcrest", "Meadowbrook", "Oakville"));
            }
            
        } catch (IOException e) {
            LOGGER.error("Failed to load config: {}", e.getMessage());
        }
        
        // Log settings
        LOGGER.info("Enable Create Trains: {}", enableCreateTrains);
        LOGGER.info("Enable Minecarts: {}", enableMinecarts);
        LOGGER.info("Vehicle Search Radius: {}", vehicleSearchRadius);
        LOGGER.info("Bread Per Pop: {}", breadPerPop);
        LOGGER.info("Min Pop For Tourists: {}", minPopForTourists);
        LOGGER.info("Min Distance Between Towns: {}", minDistanceBetweenTowns);
        LOGGER.info("Default Starting Population: {}", defaultStartingPopulation);
        LOGGER.info("Max Tourists Per Town: {}", maxTouristsPerTown);
        LOGGER.info("Population Per Tourist: {}", populationPerTourist);
        LOGGER.info("Max Population-based Tourists: {}", maxPopBasedTourists);
        LOGGER.info("Town Names: {}", townNames);
    }
    
    public static void saveConfig() {
        Properties props = new Properties();
        
        // Vehicle settings
        props.setProperty("enableCreateTrains", String.valueOf(enableCreateTrains));
        props.setProperty("enableMinecarts", String.valueOf(enableMinecarts));
        props.setProperty("vehicleSearchRadius", String.valueOf(vehicleSearchRadius));
        props.setProperty("minecartStopThreshold", String.valueOf(minecartStopThreshold));
        
        // Town settings
        props.setProperty("breadPerPop", String.valueOf(breadPerPop));
        props.setProperty("minPopForTourists", String.valueOf(minPopForTourists));
        props.setProperty("minDistanceBetweenTowns", String.valueOf(minDistanceBetweenTowns));
        props.setProperty("defaultStartingPopulation", String.valueOf(defaultStartingPopulation));
        props.setProperty("maxTouristsPerTown", String.valueOf(maxTouristsPerTown));
        props.setProperty("populationPerTourist", String.valueOf(populationPerTourist));
        props.setProperty("maxPopBasedTourists", String.valueOf(maxPopBasedTourists));
        props.setProperty("townNames", String.join(",", townNames));
        
        try {
            File configFile = new File("config/businesscraft.properties");
            configFile.getParentFile().mkdirs();
            FileWriter writer = new FileWriter(configFile);
            props.store(writer, "BusinessCraft Configuration");
            writer.close();
        } catch (IOException e) {
            LOGGER.error("Failed to save config: {}", e.getMessage());
        }
    }
}