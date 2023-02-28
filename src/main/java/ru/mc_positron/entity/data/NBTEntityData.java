package ru.mc_positron.entity.data;

import cn.nukkit.nbt.NBTIO;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.utils.BinaryStream;
import lombok.Getter;
import lombok.NonNull;

import java.io.IOException;
import java.nio.ByteOrder;

public class NBTEntityData extends EntityData<CompoundTag> {

    @Getter private CompoundTag data;

    public NBTEntityData(int id, @NonNull CompoundTag tag) {
        super(id);
        this.data = tag;
    }

    @Override
    public void setData(@NonNull CompoundTag data) {
        this.data = data;
    }

    @Override
    public void writeTo(@NonNull BinaryStream stream) {
        try {
            stream.put(NBTIO.write(data, ByteOrder.LITTLE_ENDIAN, true));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public @NonNull Type getType() {
        return Type.NBT;
    }

    @Override
    public String toString() {
        return data.toString();
    }
}
