package cn.nukkit.level.biome.impl.taiga;

public class MegaTaigaBiome extends TaigaBiome {
    public MegaTaigaBiome() {
        super();

        this.setBaseHeight(0.2f);
        this.setHeightVariation(0.2f);
    }

    @Override
    public String getName() {
        return "Mega Taiga";
    }
}
