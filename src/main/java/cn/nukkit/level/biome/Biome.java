package cn.nukkit.level.biome;

import cn.nukkit.block.BlockID;
import cn.nukkit.level.generator.populator.type.Populator;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public abstract class Biome implements BlockID {

    public static final int MAX_BIOMES = 256;
    public static final Biome[] biomes = new Biome[MAX_BIOMES];
    public static final List<Biome> unorderedBiomes = new ObjectArrayList<>();
    private static final Int2ObjectMap<String> runtimeId2Identifier = new Int2ObjectOpenHashMap<>();

    private final ArrayList<Populator> populators = new ArrayList<>();
    private int id;

    static {
        try (InputStream stream = Biome.class.getClassLoader().getResourceAsStream("biome_id_map.json")) {
            JsonObject json = JsonParser.parseReader(new InputStreamReader(stream)).getAsJsonObject();
            for (String identifier : json.keySet()) {
                int biomeId = json.get(identifier).getAsInt();
                runtimeId2Identifier.put(biomeId, identifier);
            }
        } catch (NullPointerException | IOException e) {
            throw new AssertionError("Unable to load biome mapping from biome_id_map.json", e);
        }
    }

    public static int getBiomeIdOrCorrect(int biomeId) {
        if (runtimeId2Identifier.get(biomeId) == null) {
            return EnumBiome.PLAINS.id;
        }
        return biomeId;
    }

    protected static void register(int id, Biome biome) {
        biome.setId(id);
        biomes[id] = biome;
        unorderedBiomes.add(biome);
    }

    public static Biome getBiome(int id) {
        return EnumBiome.PLAINS.biome;
    }

    /**
     * Get Biome by name.
     *
     * @param name Name of biome. Name could contain symbol "_" instead of space
     * @return Biome. Null - when biome was not found
     */
    public static Biome getBiome(String name) {
        for (Biome biome : biomes) {
            if (biome != null) {
                if (biome.getName().equalsIgnoreCase(name.replace("_", " "))) return biome;
            }
        }
        return null;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public abstract String getName();

    @Override
    public int hashCode() {
        return getId();
    }

    @Override
    public boolean equals(Object obj) {
        return hashCode() == obj.hashCode();
    }

    public boolean canRain() {
        return true;
    }
}
