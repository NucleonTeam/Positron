package cn.nukkit.network.protocol;

import lombok.ToString;
import org.spongepowered.math.vector.Vector3f;

/**
 * @author Nukkit Project Team
 */
@ToString
public class RespawnPacket extends DataPacket {

    public static final byte NETWORK_ID = ProtocolInfo.RESPAWN_PACKET;

    public static final int STATE_SEARCHING_FOR_SPAWN = 0;
    public static final int STATE_READY_TO_SPAWN = 1;
    public static final int STATE_CLIENT_READY_TO_SPAWN = 2;

    public Vector3f position;
    public int respawnState = STATE_SEARCHING_FOR_SPAWN;
    public long runtimeEntityId;

    @Override
    public void decode() {
        position = getVector3f();
        respawnState = getByte();
        runtimeEntityId = getEntityRuntimeId();
    }

    @Override
    public void encode() {
        reset();
        putVector3f(position);
        putByte((byte) respawnState);
        putEntityRuntimeId(runtimeEntityId);
    }

    @Override
    public byte pid() {
        return NETWORK_ID;
    }

}
