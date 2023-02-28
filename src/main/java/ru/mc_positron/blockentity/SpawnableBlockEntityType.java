package ru.mc_positron.blockentity;

import cn.nukkit.Player;
import cn.nukkit.nbt.NBTIO;
import cn.nukkit.nbt.tag.CompoundTag;
import ru.mc_positron.network.packet.BlockEntityDataPacket;
import lombok.NonNull;

import java.io.IOException;
import java.nio.ByteOrder;

public abstract class SpawnableBlockEntityType extends BlockEntityType {

    public SpawnableBlockEntityType(String identifier) {
        super(identifier);
    }

    public abstract @NonNull CompoundTag getSpawnNbt();

    public void spawnToAll() {
        var blockEntity = getBlockEntity();
        if (blockEntity.isRemoved() || !blockEntity.isPlaced()) return;

        blockEntity.getWorld().getPlayers().values().stream()
                .filter(p -> p.spawned)
                .forEach(this::spawnTo);
    }

    public void spawnTo(@NonNull Player player) {
        var blockEntity = getBlockEntity();
        if (blockEntity.isRemoved() || !blockEntity.isPlaced()) return;

        var pk = new BlockEntityDataPacket();
        pk.position = blockEntity.getPosition();

        try {
            pk.nbt = NBTIO.write(getSpawnNbt(), ByteOrder.LITTLE_ENDIAN, true);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        player.dataPacket(pk);
    }

    public boolean tryUpdateNbtByPlayer(@NonNull Player player, @NonNull CompoundTag tag) {
        return false;
    }
}
