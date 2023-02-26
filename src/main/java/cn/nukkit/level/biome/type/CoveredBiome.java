package cn.nukkit.level.biome.type;

import cn.nukkit.level.biome.Biome;
import cn.nukkit.level.format.FullChunk;

public abstract class CoveredBiome extends Biome {
    public int getCoverId(int x, int z) {
        return AIR << 4;
    }

    public int getSurfaceDepth(int x, int y, int z) {
        return 1;
    }

    public abstract int getSurfaceId(int x, int y, int z);

    public int getGroundDepth(int x, int y, int z) {
        return 4;
    }

    public abstract int getGroundId(int x, int y, int z);

    public void doCover(int x, int z, FullChunk chunk) {

    }
}
