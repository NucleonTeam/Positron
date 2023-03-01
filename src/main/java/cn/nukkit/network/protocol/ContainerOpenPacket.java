package cn.nukkit.network.protocol;

import lombok.ToString;
import org.spongepowered.math.vector.Vector3i;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
@ToString
public class ContainerOpenPacket extends DataPacket {
    public static final byte NETWORK_ID = ProtocolInfo.CONTAINER_OPEN_PACKET;

    @Override
    public byte pid() {
        return NETWORK_ID;
    }

    public int windowId;
    public int type;
    public Vector3i position;
    public long entityId = -1;

    @Override
    public void decode() {
        windowId = getByte();
        type = getByte();
        position = getBlockVector3();
        entityId = getEntityUniqueId();
    }

    @Override
    public void encode() {
        reset();
        putByte((byte) windowId);
        putByte((byte) type);
        putBlockVector3(position);
        putEntityUniqueId(entityId);
    }
}
