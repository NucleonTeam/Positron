package cn.nukkit.network.protocol;

import lombok.ToString;
import org.spongepowered.math.vector.Vector3i;

@ToString
public class ItemFrameDropItemPacket extends DataPacket {

    public static final byte NETWORK_ID = ProtocolInfo.ITEM_FRAME_DROP_ITEM_PACKET;

    public Vector3i position;

    @Override
    public void decode() {
        position = getBlockVector3();
    }

    @Override
    public void encode() {

    }

    @Override
    public byte pid() {
        return NETWORK_ID;
    }
}
