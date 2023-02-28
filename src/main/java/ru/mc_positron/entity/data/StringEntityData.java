package ru.mc_positron.entity.data;

import cn.nukkit.utils.BinaryStream;
import lombok.Getter;
import lombok.NonNull;

import java.nio.charset.StandardCharsets;

public class StringEntityData extends EntityData<String> {

    @Getter private String data;

    public StringEntityData(int id, @NonNull String data) {
        super(id);
        this.data = data;
    }

    @Override
    public @NonNull Type getType() {
        return Type.STRING;
    }

    @Override
    public void setData(@NonNull String data) {
        this.data = data;
    }

    @Override
    public void writeTo(@NonNull BinaryStream stream) {
        stream.putUnsignedVarInt(data.getBytes(StandardCharsets.UTF_8).length);
        stream.put(data.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String toString() {
        return data;
    }
}
