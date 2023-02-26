package cn.nukkit.level.biome.impl.mushroom;

import cn.nukkit.level.biome.type.GrassyBiome;

public class MushroomIslandBiome extends GrassyBiome {
    public MushroomIslandBiome() {
        this.setBaseHeight(0.2f);
        this.setHeightVariation(0.3f);
    }

    @Override
    public String getName() {
        return "Mushroom Island";
    }

    @Override
    public int getSurfaceId(int x, int y, int z) {
        return 2;
    }
}
