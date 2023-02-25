package cn.nukkit.level.biome.impl.river;


public class FrozenRiverBiome extends RiverBiome {
    public FrozenRiverBiome() {
        super();
    }

    @Override
    public String getName() {
        return "Frozen River";
    }

    @Override
    public boolean isFreezing() {
        return true;
    }

    @Override
    public boolean canRain() {
        return false;
    }
}
