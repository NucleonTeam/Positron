package ru.mc_positron.player;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.network.SourceInterface;
import cn.nukkit.network.protocol.PlayerListPacket;
import com.google.common.collect.ImmutableMap;
import lombok.NonNull;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PositronPlayerManager implements PlayerManager {

    private final Server server;
    private final ConcurrentHashMap<InetSocketAddress, Player> playerConnections = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Player> onlinePlayers = new ConcurrentHashMap<>();

    public PositronPlayerManager(@NonNull Server server) {
        this.server = server;
    }

    @Override
    public @NonNull Player addPlayerConnection(@NonNull SourceInterface networkInterface,
                                               @NonNull InetSocketAddress address) {

        var connection = new Player(networkInterface, null, address);
        playerConnections.put(address, connection);
        return connection;
    }

    @Override
    public void removePlayerConnection(@NonNull Player player) {
        playerConnections.remove(player.getSocketAddress());
    }

    @Override
    public void addPlayer(@NonNull Player player) {
        onlinePlayers.put(player.getUuid(), player);
        server.updatePlayerListData(player.getUniqueId(), player.getId(), player.getDisplayName(), player.getSkin(),
                player.getLoginChainData().getXUID());
    }

    @Override
    public void removePlayer(@NonNull Player player) {
        if (!onlinePlayers.containsKey(player.getUuid())) return;
        onlinePlayers.remove(player.getUuid());

        var pk = new PlayerListPacket();
        pk.type = PlayerListPacket.TYPE_REMOVE;
        pk.entries = new PlayerListPacket.Entry[] {
                new PlayerListPacket.Entry(player.getUniqueId())
        };

        Server.broadcastPacket(getPlayers(), pk);
    }

    @Override
    public @NonNull Collection<Player> getConnectedPlayers() {
        return ImmutableMap.copyOf(playerConnections).values();
    }

    @Override
    public @NonNull Collection<Player> getPlayers() {
        return ImmutableMap.copyOf(onlinePlayers).values();
    }

    @Override
    public Player getPlayerByUUID(@NonNull UUID uuid) {
        return onlinePlayers.getOrDefault(uuid, null);
    }

    @Override
    public Player getPlayerConnection(@NonNull InetSocketAddress address) {
        return playerConnections.getOrDefault(address, null);
    }

    public @NonNull Server getServer() {
        return server;
    }
}
