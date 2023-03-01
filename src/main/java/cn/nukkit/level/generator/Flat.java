package cn.nukkit.level.generator;

import cn.nukkit.block.BlockID;
import cn.nukkit.level.ChunkManager;
import cn.nukkit.math.Vector3;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Flat extends Generator {

    @Override
    public int getId() {
        return TYPE_FLAT;
    }

    private ChunkManager level;
    private final Map<String, Object> options;
    private Random random;

    @Override
    public ChunkManager getChunkManager() {
        return level;
    }

    @Override
    public Map<String, Object> getSettings() {
        return this.options;
    }

    @Override
    public String getName() {
        return "flat";
    }

    public Flat() {
        this(new HashMap<>());
    }

    public Flat(Map<String, Object> options) {
        this.options = options;
    }

    @Override
    public void init(ChunkManager level, Random random) {
        this.level = level;
        this.random = random;
    }

    @Override
    public void generateChunk(int chunkX, int chunkZ) {
        var chunk = level.getChunk(chunkX, chunkZ);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y <= 10; y++) {
                    if (y == 0) {
                        chunk.setBlock(x, y, z, BlockID.BEDROCK);
                        continue;
                    }

                    if (y == 10) {
                        chunk.setBlock(x, y, z, BlockID.GRASS);
                        continue;
                    }

                    chunk.setBlock(x, y, z, BlockID.DIRT);
                }
            }
        }
    }


    @Override
    public void populateChunk(int chunkX, int chunkZ) {

    }

    @Override
    public Vector3 getSpawn() {
        return new Vector3(128, 11, 128);
    }
}
