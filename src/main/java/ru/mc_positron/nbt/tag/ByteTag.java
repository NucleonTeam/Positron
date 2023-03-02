package ru.mc_positron.nbt.tag;

import cn.nukkit.nbt.stream.NBTInputStream;
import cn.nukkit.nbt.stream.NBTOutputStream;
import lombok.NonNull;

import java.io.IOException;

public class ByteTag extends Tag<Byte> {

    ByteTag(String key) {
        super(key, Id.BYTE);
    }

    @Override
    public @NonNull Byte read(@NonNull NBTInputStream stream) throws IOException {
        return stream.readByte();
    }

    @Override
    public void write(@NonNull NBTOutputStream stream, @NonNull Byte value) throws IOException {
        stream.writeByte(value);
    }
}
