package ru.mc_positron.nbt.tag;

import cn.nukkit.nbt.stream.NBTInputStream;
import cn.nukkit.nbt.stream.NBTOutputStream;
import lombok.NonNull;

import java.io.IOException;

public class ByteArrayTag extends Tag<byte[], byte[]> {

    ByteArrayTag(String key) {
        super(key, Id.BYTE_ARRAY);
    }

    @Override
    public byte @NonNull [] read(@NonNull NBTInputStream stream) throws IOException {
        var data = new byte[stream.readInt()];
        stream.readFully(data);
        return data;
    }

    @Override
    public void write(@NonNull NBTOutputStream stream, byte @NonNull [] value) throws IOException {
        stream.writeInt(value.length);
        stream.write(value);
    }
}
