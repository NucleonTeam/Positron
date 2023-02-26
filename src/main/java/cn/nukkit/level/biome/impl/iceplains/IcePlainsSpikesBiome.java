package cn.nukkit.level.biome.impl.iceplains;

import cn.nukkit.level.ChunkManager;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.generator.populator.type.Populator;
import cn.nukkit.math.NukkitRandom;

/**
 * author: DaPorkchop_
 * Nukkit Project
 */
public class IcePlainsSpikesBiome extends IcePlainsBiome {

    public IcePlainsSpikesBiome() {
        super();

        PopulatorIceSpikes iceSpikes = new PopulatorIceSpikes();
        this.addPopulator(iceSpikes);
    }

    @Override
    public int getSurfaceId(int x, int y, int z) {
        return 1;
    }

    public String getName() {
        return "Ice Plains Spikes";
    }

    @Override
    public boolean isFreezing() {
        return true;
    }

    /**
     * @author DaPorkchop_
     * <p>
     * Please excuse this mess, but it runs way faster than the correct method
     */
    private static class PopulatorIceSpikes extends Populator {

        @Override
        public void populate(ChunkManager level, int chunkX, int chunkZ, NukkitRandom random, FullChunk chunk) {

        }

        public int getHighestWorkableBlock(int x, int z, FullChunk chunk) {
            return chunk.getHighestBlockAt(x & 0xF, z & 0xF) - 5;
        }
    }
}
