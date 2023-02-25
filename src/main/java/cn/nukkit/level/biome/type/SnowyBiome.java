package cn.nukkit.level.biome.type;

public abstract class SnowyBiome extends GrassyBiome {
    public SnowyBiome() {
        super();
    }

    @Override
    public int getCoverId(int x, int z) {
        return SNOW_LAYER << 4;
    }

    @Override
    public boolean canRain() {
        return false;
    }
}
