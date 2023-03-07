package cn.nukkit.network.protocol;

import lombok.ToString;
import org.spongepowered.math.vector.Vector3f;

@ToString
public class ChangeDimensionPacket extends DataPacket {

    public static final byte NETWORK_ID = ProtocolInfo.CHANGE_DIMENSION_PACKET;

    public int dimension;
    public Vector3f position;
    public boolean respawn;

    @Override
    public void decode() {

    }

    @Override
    public void encode() {
        this.reset();
        this.putVarInt(this.dimension);
        this.putVector3f(position);
        this.putBoolean(this.respawn);
    }

    @Override
    public byte pid() {
        return NETWORK_ID;
    }
}
