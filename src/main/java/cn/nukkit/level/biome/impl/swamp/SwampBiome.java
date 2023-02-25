package cn.nukkit.level.biome.impl.swamp;

import cn.nukkit.level.biome.type.GrassyBiome;

public class SwampBiome extends GrassyBiome {

    public SwampBiome() {
        super();

        this.setBaseHeight(-0.2f);
        this.setHeightVariation(0.1f);
    }

    @Override
    public String getName() {
        return "Swamp";
    }
}
