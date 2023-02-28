package ru.mc_positron.registry;

import lombok.NonNull;
import ru.mc_positron.blockentity.BlockEntityType;

import java.util.HashMap;
import java.util.Objects;
import java.util.function.Supplier;

public final class BlockEntities {

    private final HashMap<String, Supplier<@NonNull ? extends BlockEntityType>> blockEntities = new HashMap<>();

    BlockEntities() {
        registerDefaults();
    }

    public void register(@NonNull Supplier<@NonNull ? extends BlockEntityType> supplier) {
        Registry.requireNotInitialized();

        var obj = supplier.get();

        if (blockEntities.containsKey(obj.getIdentifier())) {
            throw new IllegalArgumentException("BlockEntityType with id " + obj + " already registered with class " +
                    blockEntities.get(obj.getIdentifier()).getClass().getName() +
                    ". You can't register new entity type with same id for class " + obj.getClass().getName());
        }

        blockEntities.put(obj.getIdentifier(), supplier);
    }

    public @NonNull BlockEntityType get(@NonNull String identifier) {
        return Objects.requireNonNull(blockEntities.get(identifier)).get();
    }

    private void registerDefaults() {

    }
}
