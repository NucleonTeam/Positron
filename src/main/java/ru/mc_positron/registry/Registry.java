package ru.mc_positron.registry;

import cn.nukkit.Server;
import lombok.NonNull;

import java.util.function.Consumer;

public class Registry {

    private static Registry instance = null;
    private boolean completedInitialization = false;

    private Registry() {

    }

    public static Consumer<Server> init() {
        if (instance != null) throw new IllegalStateException("Module already initialized");

        instance = new Registry();
        return server -> instance.completeInitialization(server);
    }

    private void completeInitialization(@NonNull Server server) {
        completedInitialization = true;
    }

    public static void requireNotInitialized() throws IllegalStateException {
        if (instance.completedInitialization) {
            throw new IllegalStateException("You can't register new content after initialization");
        }
    }
}
