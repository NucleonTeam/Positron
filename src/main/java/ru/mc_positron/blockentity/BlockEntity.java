package ru.mc_positron.blockentity;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.level.Level;
import cn.nukkit.nbt.tag.CompoundTag;
import lombok.Getter;
import lombok.NonNull;
import org.spongepowered.math.vector.Vector3i;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public final class BlockEntity {

    private final static AtomicLong freeId = new AtomicLong(1);

    @Getter private final long id = freeId.getAndIncrement();
    @Getter private final BlockEntityType type;
    @Getter private boolean removed = false;
    @Getter private boolean placed = false;
    private Level world = null;
    private Vector3i pos = null;

    private final Object mutex = new Object();

    public BlockEntity(@NonNull BlockEntityType type) {
        this(type, new CompoundTag());
    }

    public BlockEntity(@NonNull BlockEntityType type, @NonNull CompoundTag nbt) {
        this.type = type;

        type.setBlockEntity(this);
        type.init(nbt);
    }

    public void place(@NonNull Level world, Vector3i pos) {
        synchronized (mutex) {
            if (placed || removed) return;

            this.world = world;
            this.pos = pos;

            type.onPlace(world, pos);

            removed = false;
            placed = true;
        }

        if (type instanceof SpawnableBlockEntityType spawnable) {
            spawnable.spawnToAll();
        }
    }

    public void destroy() {
        type.onBreak(world, pos);
        remove();
    }

    public void remove() {
        synchronized (mutex) {
            if (removed || !placed) return;
            removed = true;
        }
    }

    public boolean update() {
        if (isRemoved()) return false;

        type.onUpdate();
        return true;
    }

    public void interactByPlayer(@NonNull Player player) {
        type.onInteract(player);
    }

    public @NonNull Block getBlock() {
        return world.getBlock(pos.x(), pos.y(), pos.z());
    }

    public void setData(@NonNull CompoundTag data) {
        type.init(data);
    }

    public @NonNull CompoundTag getSaveData() {
        var data = type.getNbtData(
                new CompoundTag("")
                        .putString("id", type.getIdentifier())
                        .putInt("x", pos.x())
                        .putInt("y", pos.y())
                        .putInt("z", pos.z()));

        return Objects.requireNonNull(data);
    }

    public @NonNull Vector3i getPosition() {
        return Objects.requireNonNull(pos);
    }

    public @NonNull Level getWorld() {
        return Objects.requireNonNull(world);
    }
}
