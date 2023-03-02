package ru.mc_positron.nbt.tag;

import cn.nukkit.nbt.stream.NBTInputStream;
import cn.nukkit.nbt.stream.NBTOutputStream;
import lombok.NonNull;

import java.io.IOException;

public class FloatTag extends Tag<Float> {

    FloatTag(String key) {
        super(key, Id.FLOAT);
    }

    @Override
    public @NonNull Float read(@NonNull NBTInputStream stream) throws IOException {
        return stream.readFloat();
    }

    @Override
    public void write(@NonNull NBTOutputStream stream, @NonNull Float value) throws IOException {
        stream.writeFloat(value);
    }
}
