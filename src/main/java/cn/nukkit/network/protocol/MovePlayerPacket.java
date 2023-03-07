package cn.nukkit.network.protocol;

import lombok.ToString;
import org.spongepowered.math.vector.Vector3f;

@ToString
public class MovePlayerPacket extends DataPacket {

    public static final byte NETWORK_ID = ProtocolInfo.MOVE_PLAYER_PACKET;

    public static final int MODE_NORMAL = 0;
    public static final int MODE_RESET = 1;
    public static final int MODE_TELEPORT = 2;
    public static final int MODE_PITCH = 3; //facepalm Mojang

    public long eid;
    public Vector3f position;
    public float yaw;
    public float headYaw;
    public float pitch;
    public int mode = MODE_NORMAL;
    public boolean onGround;
    public long ridingEid;
    public int int1 = 0;
    public int int2 = 0;
    public long frame;

    @Override
    public void decode() {
        eid = getEntityRuntimeId();
        position = getVector3f();
        pitch = getLFloat();
        yaw = getLFloat();
        headYaw = getLFloat();
        mode = getByte();
        onGround = getBoolean();
        ridingEid = getEntityRuntimeId();
        if (mode == MODE_TELEPORT) {
            int1 = getLInt();
            int2 = getLInt();
        }
        frame = getUnsignedVarLong();
    }

    @Override
    public void encode() {
        reset();
        putEntityRuntimeId(eid);
        putVector3f(position);
        putLFloat(pitch);
        putLFloat(yaw);
        putLFloat(headYaw);
        putByte((byte) mode);
        putBoolean(onGround);
        putEntityRuntimeId(ridingEid);
        if (mode == MODE_TELEPORT) {
            putLInt(int1);
            putLInt(int2);
        }
        putUnsignedVarLong(frame);
    }

    @Override
    public byte pid() {
        return NETWORK_ID;
    }

}
