package ru.mc_positron.entity.data;

import cn.nukkit.utils.BinaryStream;
import lombok.Getter;
import lombok.NonNull;

public class LongEntityData extends EntityData<Long> {

    @Getter private Long data;

    public LongEntityData(int id, long data) {
        super(id);
        this.data = data;
    }

    public void setData(Long data) {
        this.data = data == null? 0 : data;
    }

    @Override
    public void writeTo(@NonNull BinaryStream stream) {
        stream.putVarLong(data);
    }

    @Override
    public @NonNull Type getType() {
        return Type.LONG;
    }

    @Override
    public String toString() {
        return data + "l";
    }
}
