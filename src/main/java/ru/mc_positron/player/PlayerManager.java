package ru.mc_positron.player;

import cn.nukkit.Player;
import cn.nukkit.network.SourceInterface;
import lombok.NonNull;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.UUID;

public interface PlayerManager {

    @NonNull Player addPlayerConnection(@NonNull SourceInterface networkInterface, @NonNull InetSocketAddress address);

    void removePlayerConnection(@NonNull Player player);

    void addPlayer(@NonNull Player player);

    void removePlayer(@NonNull Player player);

    @NonNull Collection<Player> getConnectedPlayers();

    @NonNull Collection<Player> getPlayers();

    default Player getPlayerByName(@NonNull String playerName) {
        for (var player: getPlayers()) {
            if (player.getName().equalsIgnoreCase(playerName)) return player;
        }

        return null;
    }

    default Player findPlayer(@NonNull String input) {
        int max = Integer.MIN_VALUE;
        Player current = null;
        final String lowerInput = input.toLowerCase();

        for (var player: getPlayers()) {
            String name = player.getName().toLowerCase();

            if (!name.startsWith(lowerInput)) continue;
            if (max > name.length()) continue;

            max = name.length();
            current = player;
        }

        return current;
    }

    default Player getPlayerByUUID(@NonNull UUID uuid) {
        for (var player: getPlayers()) {
            if (uuid.equals(player.getUuid())) return player;
        }

        return null;
    }

    default Player getPlayerConnection(@NonNull InetSocketAddress address) {
        for (var connection: getConnectedPlayers()) {
            if (address.equals(connection.getSocketAddress())) return connection;
        }

        return null;
    }
}
