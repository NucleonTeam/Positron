package ru.mc_positron.network.packet;

import cn.nukkit.network.protocol.DataPacket;
import cn.nukkit.network.protocol.ProtocolInfo;
import lombok.ToString;
import org.spongepowered.math.vector.Vector3i;

import java.util.Objects;

@ToString(exclude = "namedTag")
public class BlockEntityDataPacket extends DataPacket {

    public static final byte NETWORK_ID = ProtocolInfo.BLOCK_ENTITY_DATA_PACKET;

    public Vector3i position = null;
    public byte[] nbt = null;

    @Override
    public byte pid() {
        return NETWORK_ID;
    }

    @Override
    public void decode() {
        var vec = getBlockVector3();

        position = new Vector3i(vec.x, vec.y, vec.z);
        nbt = get();
    }

    @Override
    public void encode() {
        Objects.requireNonNull(position);
        Objects.requireNonNull(nbt);

        reset();
        putBlockVector3(position.x(), position.y(), position.z());
        put(nbt);
    }
}