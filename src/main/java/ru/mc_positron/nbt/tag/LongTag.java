package ru.mc_positron.nbt.tag;

import cn.nukkit.nbt.stream.NBTInputStream;
import cn.nukkit.nbt.stream.NBTOutputStream;
import lombok.NonNull;

import java.io.IOException;

public class LongTag extends Tag<Long> {

    LongTag(String key) {
        super(key, Id.LONG);
    }

    @Override
    public @NonNull Long read(@NonNull NBTInputStream stream) throws IOException {
        return stream.readLong();
    }

    @Override
    public void write(@NonNull NBTOutputStream stream, @NonNull Long value) throws IOException {
        stream.writeLong(value);
    }
}
