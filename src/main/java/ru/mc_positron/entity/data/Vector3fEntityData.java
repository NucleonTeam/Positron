package ru.mc_positron.entity.data;


import cn.nukkit.utils.BinaryStream;
import lombok.Getter;
import lombok.NonNull;
import org.spongepowered.math.vector.Vector3f;

public class Vector3fEntityData extends EntityData<Vector3f> {

    @Getter private Vector3f data;

    public Vector3fEntityData(int id, @NonNull Vector3f data) {
        super(id);
        this.data = data;
    }

    @Override
    public void setData(@NonNull Vector3f data) {
        this.data = data;
    }

    @Override
    public void writeTo(@NonNull BinaryStream stream) {
        stream.putLFloat(data.x());
        stream.putLFloat(data.y());
        stream.putLFloat(data.z());
    }

    @Override
    public @NonNull Type getType() {
        return Type.VECTOR;
    }

    @Override
    public String toString() {
        return "(" + data.x() + ", " + data.y() + ", " + data.z() + ")";
    }
}
