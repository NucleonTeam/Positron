package ru.mc_positron.nbt.tag;

import cn.nukkit.nbt.stream.NBTInputStream;
import cn.nukkit.nbt.stream.NBTOutputStream;
import lombok.NonNull;

import java.io.IOException;

public class StringTag extends Tag<String, String> {

    StringTag(String key) {
        super(key, Id.STRING);
    }

    @Override
    public @NonNull String read(@NonNull NBTInputStream stream) throws IOException {
        return stream.readUTF();
    }

    @Override
    public void write(@NonNull NBTOutputStream stream, @NonNull String value) throws IOException {
        stream.writeUTF(value);
    }
}
