package ru.mc_positron.entity.data;

import cn.nukkit.utils.BinaryStream;
import lombok.Getter;
import lombok.NonNull;

public class ByteEntityData extends EntityData<Integer> {

    @Getter private Integer data;

    public ByteEntityData(int id, int data) {
        super(id);
        this.data = data;
    }

    @Override
    public @NonNull Type getType() {
        return Type.BYTE;
    }

    @Override
    public void setData(Integer data) {
        this.data = data == null? 0 : data;
    }

    @Override
    public void writeTo(@NonNull BinaryStream stream) {
        stream.putByte(data.byteValue());
    }

    @Override
    public String toString() {
        return data + "b";
    }
}
