package cn.nukkit.network.protocol;

import lombok.ToString;
import org.spongepowered.math.vector.Vector3f;

@ToString
public class MoveEntityAbsolutePacket extends DataPacket {
    public static final byte NETWORK_ID = ProtocolInfo.MOVE_ENTITY_ABSOLUTE_PACKET;

    public long eid;
    public Vector3f position;
    public double yaw;
    public double headYaw;
    public double pitch;
    public boolean onGround;
    public boolean teleport;
    public boolean forceMoveLocalEntity;

    @Override
    public byte pid() {
        return NETWORK_ID;
    }

    @Override
    public void decode() {
        eid = getEntityRuntimeId();
        int flags = getByte();
        onGround = (flags & 0x01) != 0;
        teleport = (flags & 0x02) != 0;
        forceMoveLocalEntity = (flags & 0x04) != 0;
        position = getVector3f();
        pitch = getByte() * (360d / 256d);
        headYaw = getByte() * (360d / 256d);
        yaw = getByte() * (360d / 256d);
    }

    @Override
    public void encode() {
        this.reset();
        this.putEntityRuntimeId(eid);
        byte flags = 0;

        if (onGround) flags |= 0x01;
        if (teleport) flags |= 0x02;
        if (forceMoveLocalEntity) flags |= 0x04;

        putByte(flags);
        putVector3f(position);
        putByte((byte) (pitch / (360d / 256d)));
        putByte((byte) (headYaw / (360d / 256d)));
        putByte((byte) (yaw / (360d / 256d)));
    }
}
