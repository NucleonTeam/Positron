package cn.nukkit.level.biome.impl.jungle;

public class JungleMBiome extends JungleBiome {
    public JungleMBiome() {
        super();

        this.setBaseHeight(0.2f);
        this.setHeightVariation(0.4f);
    }

    @Override
    public String getName() {
        return "Jungle M";
    }
}
