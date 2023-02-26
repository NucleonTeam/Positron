package ru.mc_positron.world;

import cn.nukkit.level.Level;
import lombok.NonNull;

import java.util.Map;
import java.util.UUID;

public interface WorldManager {

    @NonNull Level getDefaultWorld();

    void registerWorld(@NonNull Level world);

    Level getWorld(@NonNull UUID uuid);

    void unregisterWorld(@NonNull Level world);

    @NonNull Map<UUID, Level> getWorlds();
}
