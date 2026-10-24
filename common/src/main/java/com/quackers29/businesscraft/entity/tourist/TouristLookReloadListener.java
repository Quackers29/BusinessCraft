package com.quackers29.businesscraft.entity.tourist;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Server-data reload listener. Safe on a dedicated server: no client classes.
 * Datapacks add files at {@code data/<namespace>/tourist_looks/<id>.json}.
 */
public class TouristLookReloadListener extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(TouristLookReloadListener.class);

    public TouristLookReloadListener() {
        super(new Gson(), "tourist_looks");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
        LinkedHashMap<String, TouristLook> byId = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            JsonElement element = entry.getValue();
            if (element == null || !element.isJsonObject()) {
                continue;
            }
            String path = entry.getKey().getPath();
            int slash = path.lastIndexOf('/');
            String fallback = slash >= 0 ? path.substring(slash + 1) : path;
            try {
                TouristLook look = TouristLook.parse(fallback, element.toString());
                if (byId.put(look.id(), look) != null) {
                    LOGGER.warn("Duplicate tourist look id {} from {}", look.id(), entry.getKey());
                }
            } catch (RuntimeException e) {
                LOGGER.error("Skipping tourist look {}: {}", entry.getKey(), e.getMessage());
            }
        }
        if (byId.isEmpty()) {
            LOGGER.warn("No tourist looks in data packs; keeping the builtin catalog");
            return;
        }
        List<TouristLook> loaded = new ArrayList<>(byId.values());
        TouristLooks.replace(loaded);
        LOGGER.info("Loaded {} tourist looks", loaded.size());
    }
}
