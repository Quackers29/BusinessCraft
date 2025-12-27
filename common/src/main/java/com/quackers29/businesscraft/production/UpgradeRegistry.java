package com.quackers29.businesscraft.production;

import com.quackers29.businesscraft.api.PlatformAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class UpgradeRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeRegistry.class);
    private static final Map<String, Upgrade> UPGRADES = new HashMap<>();
    private static final String CONFIG_FILE_NAME = "town_upgrades.csv";

    public static void load() {
        UPGRADES.clear();
        Path configDir = PlatformAccess.platform.getConfigDirectory();
        File configFile = configDir.resolve(CONFIG_FILE_NAME).toFile();

        if (!configFile.exists()) {
            createDefaultConfig(configFile);
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue; // Skip header
                }

                String[] parts = line.split(",");
                if (parts.length >= 11) {
                    // id,name,pop_req,track,tier,happiness_bonus,housing_capacity,input_id,input_rate,output_id,output_rate
                    try {
                        String id = parts[0].trim();
                        String name = parts[1].trim();
                        int popReq = Integer.parseInt(parts[2].trim());
                        Upgrade.UpgradeTrack track = Upgrade.UpgradeTrack.valueOf(parts[3].trim().toUpperCase());
                        int tier = Integer.parseInt(parts[4].trim());
                        float happinessBonus = Float.parseFloat(parts[5].trim());
                        int housingCapacity = Integer.parseInt(parts[6].trim());

                        Upgrade upgrade = UPGRADES.computeIfAbsent(id,
                                k -> new Upgrade(k, name, popReq, track, tier, happinessBonus, housingCapacity));

                        String inputId = parts[7].trim();
                        float inputRate = Float.parseFloat(parts[8].trim());
                        if (!inputId.isEmpty() && !inputId.equals("none")) {
                            upgrade.addInput(inputId, inputRate);
                        }

                        String outputId = parts[9].trim();
                        float outputRate = Float.parseFloat(parts[10].trim());
                        if (!outputId.isEmpty() && !outputId.equals("none")) {
                            upgrade.addOutput(outputId, outputRate);
                        }
                    } catch (Exception e) {
                        LOGGER.error("Error parsing upgrade line: {}", line, e);
                    }
                }
            }
            LOGGER.info("Loaded {} town upgrades", UPGRADES.size());
        } catch (Exception e) {
            LOGGER.error("Failed to load {}", CONFIG_FILE_NAME, e);
        }
    }

    private static void createDefaultConfig(File file) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(
                    "id,name,pop_req,track,tier,happiness_bonus,housing_capacity,input_id,input_rate,output_id,output_rate\n");
            // Resources
            writer.write("wheat_farm,Wheat Farm,10,RESOURCES,1,0,0,none,0,food,1.0\n");
            writer.write("lumber_camp,Lumber Camp,15,RESOURCES,1,0,0,none,0,wood,1.0\n");
            writer.write("bakery,Bakery,20,RESOURCES,2,2,0,wheat,2.0,food,3.0\n");
            writer.write("iron_mine,Iron Mine,30,RESOURCES,2,0,0,wood,0.5,iron,0.5\n");
            // Services
            writer.write("school,School,50,SERVICES,1,10,0,none,0,none,0\n");
            writer.write("park,Park,20,SERVICES,1,5,0,none,0,none,0\n");
            // Population
            writer.write("small_house,Small House,0,POPULATION,1,0,5,wood,10,none,0\n");
            writer.write("apartment,Apartment,50,POPULATION,2,-5,20,wood,50,none,0\n");
        } catch (IOException e) {
            LOGGER.error("Failed to create default {}", CONFIG_FILE_NAME, e);
        }
    }

    public static Upgrade get(String id) {
        return UPGRADES.get(id);
    }

    public static Collection<Upgrade> getAll() {
        return UPGRADES.values();
    }
}
