package cn.nukkit.metadata;

import cn.nukkit.Player;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class PlayerMetadataStore extends MetadataStore {

    @Override
    protected String disambiguate(Metadatable player, String metadataKey) {
        if (!(player instanceof Player)) {
            throw new IllegalArgumentException("Argument must be an IPlayer instance");
        }
        return (((Player) player).getName() + ":" + metadataKey).toLowerCase();
    }
}
