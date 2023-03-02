package ru.mc_positron.nbt.tag;

import cn.nukkit.nbt.stream.NBTInputStream;
import cn.nukkit.nbt.stream.NBTOutputStream;
import lombok.NonNull;

import java.io.IOException;

public class IntegerTag extends Tag<Integer> {

    IntegerTag(String key) {
        super(key, Id.INT);
    }

    @Override
    public @NonNull Integer read(@NonNull NBTInputStream stream) throws IOException {
        return stream.readInt();
    }

    @Override
    public void write(@NonNull NBTOutputStream stream, @NonNull Integer value) throws IOException {
        stream.writeInt(value);
    }
}
