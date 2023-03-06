package ru.mc_positron.nbt.tag;

import cn.nukkit.nbt.stream.NBTInputStream;
import cn.nukkit.nbt.stream.NBTOutputStream;
import lombok.NonNull;

import java.io.IOException;

public class DoubleTag extends Tag<Double, Double> {

    DoubleTag(String key) {
        super(key, Id.DOUBLE);
    }

    @Override
    public @NonNull Double read(@NonNull NBTInputStream stream) throws IOException {
        return stream.readDouble();
    }

    @Override
    public void write(@NonNull NBTOutputStream stream, @NonNull Double value) throws IOException {
        stream.writeDouble(value);
    }
}
