package cn.nukkit.level.biome.impl.forest;

import cn.nukkit.level.biome.type.GrassyBiome;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ForestBiome extends GrassyBiome {
    public static final int TYPE_NORMAL = 0;
    public static final int TYPE_BIRCH = 1;
    public static final int TYPE_BIRCH_TALL = 2;

    public final int type;

    public ForestBiome() {
        this(TYPE_NORMAL);
    }

    public ForestBiome(int type) {
        super();

        this.type = type;
    }

    @Override
    public String getName() {
        switch (this.type) {
            case TYPE_BIRCH:
                return "Birch Forest";
            case TYPE_BIRCH_TALL:
                return "Birch Forest M";
            default:
                return "Forest";
        }
    }
}
