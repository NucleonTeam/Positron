package ru.mc_positron.boot.configuration;

import cn.nukkit.Player;
import lombok.NonNull;

import java.net.InetSocketAddress;

public interface PositronConfiguration {

    default int getLogLevel() {
        return 1;
    }

    default @NonNull String getMotd() {
        return "Positron server";
    }

    default @NonNull String getSubMotd() {
        return "Minecraft server";
    }

    default @NonNull InetSocketAddress getHostAddress() {
        return new InetSocketAddress("0.0.0.0", 19132);
    }

    default int getViewDistance() {
        return 10;
    }

    default int getDefaultGameMode() {
        return Player.CREATIVE;
    }

    default boolean canEnableQuery() {
        return true;
    }

    default boolean isOnlyForXboxAuthorizedPlayers() {
        return false;
    }
}
