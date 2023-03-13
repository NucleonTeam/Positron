package ru.mc_positron.boot.configuration;

import lombok.NonNull;
import lombok.SneakyThrows;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.serialize.SerializationException;
import ru.mc_positron.math.FastMath;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Objects;

public final class FileConfiguration extends DefaultConfiguration {

    private final CommentedConfigurationNode root;

    private final int logLevel;
    private final String motd;
    private final String subMotd;
    private final InetSocketAddress hostAddress;
    private final int viewDistance;
    private final int defaultGameMode;
    private final boolean enableQuery;
    private final boolean needXbox;

    @SneakyThrows
    public FileConfiguration() {
        var loader = HoconConfigurationLoader.builder()
                .path(Path.of("server.conf"))
                .build();
        root = loader.load();

        logLevel = FastMath.clamp(node("LogLevel", super.getLogLevel()).getInt(), 1, 3);
        motd = node("Motd", super.getMotd()).getString();
        subMotd = node("SubMotd", super.getSubMotd()).getString();
        hostAddress = new InetSocketAddress(
                Objects.requireNonNull(node("Host", "Address", "0.0.0.0").getString()),
                node("Host", "Port", 19132).getInt());
        viewDistance = node("ViewDistance", super.getViewDistance()).getInt();
        defaultGameMode = node("DefaultGameMode", super.getDefaultGameMode()).getInt();
        enableQuery = node("EnableQuery", super.canEnableQuery()).getBoolean();
        needXbox = node("OnlyForXboxAuthorizedPlayers", super.isOnlyForXboxAuthorizedPlayers()).getBoolean();

        loader.save(root);
    }

    private CommentedConfigurationNode node(String key, Object defaultValue) throws SerializationException {
        var node = root.node(key);
        if (node.empty()) node.set(defaultValue);
        return node;
    }

    private CommentedConfigurationNode node(String key, String subKey, Object defaultValue) throws SerializationException {
        var node = root.node(key, subKey);
        if (node.empty()) node.set(defaultValue);
        return node;
    }

    @Override
    public int getLogLevel() {
        return logLevel;
    }

    @NonNull
    @Override
    public String getMotd() {
        return motd;
    }

    @NonNull
    @Override
    public String getSubMotd() {
        return subMotd;
    }

    @Override
    public @NonNull InetSocketAddress getHostAddress() {
        return hostAddress;
    }

    @Override
    public int getViewDistance() {
        return viewDistance;
    }

    @Override
    public int getDefaultGameMode() {
        return defaultGameMode;
    }

    @Override
    public boolean canEnableQuery() {
        return enableQuery;
    }

    @Override
    public boolean isOnlyForXboxAuthorizedPlayers() {
        return needXbox;
    }
}
