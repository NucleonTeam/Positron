package cn.nukkit.level.biome.impl.mesa;

public class MesaPlateauFBiome extends MesaPlateauBiome {
    public MesaPlateauFBiome() {
        super();
    }

    @Override
    public int getCoverId(int x, int z) {
        return GRASS << 4;
    }

    @Override
    public String getName() {
        return "Mesa Plateau F";
    }
}
