package ru.mc_positron.entity.data;

import cn.nukkit.utils.BinaryStream;
import lombok.Getter;
import lombok.NonNull;

public class FloatEntityData extends EntityData<Float> {

    @Getter private Float data;

    public FloatEntityData(int id, float data) {
        super(id);
        this.data = data;
    }

    public void setData(Float data) {
        this.data = data == null? 0 : data;
    }

    @Override
    public void writeTo(@NonNull BinaryStream stream) {
        stream.putLFloat(data);
    }

    @Override
    public @NonNull Type getType() {
        return Type.FLOAT;
    }

    @Override
    public String toString() {
        return data + "f";
    }
}
