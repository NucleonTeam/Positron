package cn.nukkit;

import cn.nukkit.metadata.Metadatable;

import java.util.UUID;

public interface IPlayer extends Metadatable {

    boolean isOnline();

    String getName();

    UUID getUniqueId();

    Player getPlayer();

    Server getServer();

    Long getFirstPlayed();

    Long getLastPlayed();

    boolean hasPlayedBefore();
}
