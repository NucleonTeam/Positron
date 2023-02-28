package ru.mc_positron.entity.data;

import cn.nukkit.utils.BinaryStream;
import lombok.Getter;
import lombok.NonNull;
import org.spongepowered.math.vector.Vector3i;

public class Vector3iEntityData extends EntityData<Vector3i> {

    @Getter private Vector3i data;

    public Vector3iEntityData(int id, @NonNull Vector3i data) {
        super(id);
        this.data = data;
    }

    @Override
    public void setData(@NonNull Vector3i data) {
        this.data = new Vector3i(data.x(), data.y(), data.z());
    }

    @Override
    public void writeTo(@NonNull BinaryStream stream) {
        stream.putVarInt(data.x());
        stream.putVarInt(data.y());
        stream.putVarInt(data.z());
    }

    @Override
    public @NonNull Type getType() {
        return Type.POS;
    }

    @Override
    public String toString() {
        return "(" + data.x() + ", " + data.y() + ", " + data.z() + ")";
    }
}
