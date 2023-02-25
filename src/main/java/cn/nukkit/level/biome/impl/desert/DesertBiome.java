package cn.nukkit.level.biome.impl.desert;

import cn.nukkit.level.biome.type.SandyBiome;

public class DesertBiome extends SandyBiome {
    public DesertBiome() {
        this.setBaseHeight(0.125f);
        this.setHeightVariation(0.05f);
    }

    @Override
    public String getName() {
        return "Desert";
    }

    @Override
    public boolean canRain() {
        return false;
    }
}
