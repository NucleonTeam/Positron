package cn.nukkit.level.biome.impl.taiga;

public class ColdTaigaBiome extends TaigaBiome {
    public ColdTaigaBiome() {
        super();

        this.setBaseHeight(0.2f);
        this.setHeightVariation(0.2f);
    }

    @Override
    public String getName() {
        return "Cold Taiga";
    }

    @Override
    public int getCoverId(int x, int z) {
        return 1;
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
