package ru.mc_positron.entity.data;

import cn.nukkit.utils.BinaryStream;
import lombok.Getter;
import lombok.NonNull;

public class ShortEntityData extends EntityData<Integer> {

    @Getter private Integer data;

    public ShortEntityData(int id, int data) {
        super(id);
        this.data = data;
    }

    public void setData(Integer data) {
        this.data = data == null? 0 : data;
    }

    @Override
    public void writeTo(@NonNull BinaryStream stream) {
        stream.putLShort(data);
    }

    @Override
    public @NonNull Type getType() {
        return Type.SHORT;
    }

    @Override
    public String toString() {
        return data + "s";
    }
}
