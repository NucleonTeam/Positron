package ru.mc_positron.blockentity;

import cn.nukkit.Player;
import cn.nukkit.level.Level;
import cn.nukkit.nbt.tag.CompoundTag;
import lombok.NonNull;
import org.spongepowered.math.vector.Vector3i;

import java.util.Objects;

public abstract class BlockEntityType {

    private final String identifier;
    private BlockEntity blockEntity = null;

    public BlockEntityType(String identifier) {
        this.identifier = identifier;
    }

    public final @NonNull String getIdentifier() {
        return identifier;
    }

    final void setBlockEntity(@NonNull BlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    public final @NonNull BlockEntity getBlockEntity() {
        Objects.requireNonNull(blockEntity);
        return blockEntity;
    }

    public abstract void init(@NonNull CompoundTag data);

    public abstract @NonNull CompoundTag getNbtData(@NonNull CompoundTag template);

    public void onPlace(@NonNull Level world, @NonNull Vector3i pos) {

    }

    public void onBreak(@NonNull Level world, @NonNull Vector3i pos) {

    }

    public void onInteract(@NonNull Player player) {

    }

    public void onUpdate() {

    }
}
