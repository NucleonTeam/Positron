package ru.mc_positron.nbt.tag;

import cn.nukkit.nbt.stream.NBTInputStream;
import cn.nukkit.nbt.stream.NBTOutputStream;
import lombok.NonNull;

import java.io.IOException;

public class ShortTag extends Tag<Short, Integer> {

    ShortTag(String key) {
        super(key, Id.SHORT);
    }

    @Override
    public @NonNull Integer read(@NonNull NBTInputStream stream) throws IOException {
        return (int) stream.readShort();
    }

    @Override
    public void write(@NonNull NBTOutputStream stream, @NonNull Integer value) throws IOException {
        stream.writeShort(value.shortValue());
    }
}
