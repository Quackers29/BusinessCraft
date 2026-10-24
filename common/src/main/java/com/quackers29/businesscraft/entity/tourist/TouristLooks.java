package com.quackers29.businesscraft.entity.tourist;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.IntUnaryOperator;

/**
 * Catalog of tourist looks. The dedicated server and the client share this class;
 * it does not touch client rendering types.
 *
 * <h2>How to add a tourist look</h2>
 * <ol>
 *   <li>Add {@code data/&lt;namespace&gt;/tourist_looks/&lt;id&gt;.json} with
 *       {@code id}, {@code order}, {@code skin}, {@code outfit}, {@code headwear}, {@code accessory}.</li>
 *   <li>Add the id to {@code data/businesscraft/tourist_looks/index.txt} if it ships in the mod.
 *       A datapack only needs the JSON; the server reload listener picks it up.</li>
 *   <li>Add textures on the client:
 *       {@code assets/businesscraft/textures/entity/tourist/skin_N.png},
 *       {@code outfit_N.png}, and, when the index is not {@code 0},
 *       {@code headwear_N.png} / {@code accessory_N.png}. All are 64×64.
 *       Skin and outfit use the vanilla wide-arm player UV. Hat and accessory UVs
 *       match the boxes in {@code TouristModel}.</li>
 *   <li>Reusing an existing hat or accessory index needs no Java. A new shape
 *       (a new brim, a new held item) needs a part in {@code TouristModel} and a
 *       constant in {@code TouristParts}.</li>
 * </ol>
 *
 * <p>The server chooses a look when the tourist spawns, stores the id and the packed
 * int in NBT, and syncs the packed int. Existing tourists keep their numbers if the
 * catalog later grows.
 */
public final class TouristLooks {
    public static final TouristLook FALLBACK = new TouristLook("fallback", 0, 0, 0, 0, 0);
    private static final String INDEX = "/data/businesscraft/tourist_looks/index.txt";

    private static volatile List<TouristLook> looks = List.of();

    private TouristLooks() {
    }

    public static List<TouristLook> all() {
        List<TouristLook> current = looks;
        if (!current.isEmpty()) {
            return current;
        }
        synchronized (TouristLooks.class) {
            if (looks.isEmpty()) {
                looks = List.copyOf(loadFromClasspath());
            }
            return looks;
        }
    }

    /** Replaces the catalog after a datapack reload. Empty input is ignored. */
    public static void replace(List<TouristLook> loaded) {
        if (loaded == null || loaded.isEmpty()) {
            return;
        }
        List<TouristLook> sorted = new ArrayList<>(loaded);
        sorted.sort(Comparator.comparingInt(TouristLook::order).thenComparing(TouristLook::id));
        looks = List.copyOf(sorted);
    }

    public static TouristLook pick(IntUnaryOperator nextInt) {
        List<TouristLook> current = all();
        if (current.isEmpty()) {
            return FALLBACK;
        }
        int index = nextInt.applyAsInt(current.size());
        if (index < 0 || index >= current.size()) {
            return current.get(0);
        }
        return current.get(index);
    }

    static List<TouristLook> loadFromClasspath() {
        InputStream index = TouristLooks.class.getResourceAsStream(INDEX);
        if (index == null) {
            return List.of(FALLBACK);
        }
        List<TouristLook> loaded = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(index, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String id = line.trim();
                if (id.isEmpty() || id.startsWith("#")) {
                    continue;
                }
                String path = "/data/businesscraft/tourist_looks/" + id + ".json";
                InputStream json = TouristLooks.class.getResourceAsStream(path);
                if (json == null) {
                    throw new IllegalStateException("missing tourist look " + path);
                }
                try (InputStream in = json) {
                    loaded.add(TouristLook.parse(id, new String(in.readAllBytes(), StandardCharsets.UTF_8)));
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("failed to read tourist looks", e);
        }
        if (loaded.isEmpty()) {
            return List.of(FALLBACK);
        }
        return loaded;
    }
}
