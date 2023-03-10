package ru.mc_positron.registry;

import cn.nukkit.Server;
import lombok.NonNull;

import java.util.function.Consumer;

public final class Registry {

    private static Registry instance = null;
    private boolean completedInitialization;
    private final BlockEntities blockEntities;
    private final Entities entities;

    private Registry() {
        instance = this;

        completedInitialization = false;
        blockEntities = new BlockEntities();
        entities = new Entities();
    }

    public static Consumer<Server> init() {
        if (instance != null) throw new IllegalStateException("Module already initialized");

        new Registry();
        return server -> instance.completeInitialization(server);
    }

    private void completeInitialization(@NonNull Server server) {
        completedInitialization = true;

        entities.completeInitialization();
    }

    public static void requireNotInitialized() throws IllegalStateException {
        if (instance.completedInitialization) {
            throw new IllegalStateException("You can't register new content after initialization");
        }
    }

    public static @NonNull BlockEntities blockEntities() {
        return instance.blockEntities;
    }

    public static @NonNull Entities entities() {
        return instance.entities;
    }
}
