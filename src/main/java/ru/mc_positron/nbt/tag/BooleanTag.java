package ru.mc_positron.nbt.tag;

import cn.nukkit.nbt.stream.NBTInputStream;
import cn.nukkit.nbt.stream.NBTOutputStream;
import lombok.NonNull;

import java.io.IOException;

public class BooleanTag extends Tag<Byte, Boolean> {

    BooleanTag(String key) {
        super(key, Id.BYTE);
    }

    @Override
    public @NonNull Boolean read(@NonNull NBTInputStream stream) throws IOException {
        return stream.readByte() == 1;
    }

    @Override
    public void write(@NonNull NBTOutputStream stream, @NonNull Boolean value) throws IOException {
        stream.writeByte(value? 1 : 0);
    }
}
