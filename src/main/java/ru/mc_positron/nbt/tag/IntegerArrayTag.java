package ru.mc_positron.nbt.tag;

import cn.nukkit.nbt.stream.NBTInputStream;
import cn.nukkit.nbt.stream.NBTOutputStream;
import lombok.NonNull;

import java.io.IOException;

public class IntegerArrayTag extends Tag<int[], int[]> {

    IntegerArrayTag(String key) {
        super(key, Id.INT_ARRAY);
    }

    @Override
    public int @NonNull [] read(@NonNull NBTInputStream stream) throws IOException {
        var data = new int[stream.readInt()];
        for (int i = 0; i < data.length; i++) data[i] = stream.readInt();
        return data;
    }

    @Override
    public void write(@NonNull NBTOutputStream stream, int @NonNull [] value) throws IOException {
        stream.writeInt(value.length);
        for (int index: value) stream.writeInt(index);
    }
}
