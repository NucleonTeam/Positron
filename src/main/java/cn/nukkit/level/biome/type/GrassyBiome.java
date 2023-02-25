package cn.nukkit.level.biome.type;

public abstract class GrassyBiome extends CoveredBiome {
    public GrassyBiome() {
    }

    @Override
    public int getSurfaceId(int x, int y, int z) {
        return GRASS << 4;
    }

    @Override
    public int getGroundId(int x, int y, int z) {
        return DIRT << 4;
    }
}
