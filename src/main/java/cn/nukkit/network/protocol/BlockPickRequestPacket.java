package cn.nukkit.network.protocol;

import lombok.ToString;
import org.spongepowered.math.vector.Vector3i;

@ToString
public class BlockPickRequestPacket extends DataPacket {

    public static final byte NETWORK_ID = ProtocolInfo.BLOCK_PICK_REQUEST_PACKET;

    public Vector3i position;
    public boolean addUserData;
    public int selectedSlot;

    @Override
    public byte pid() {
        return NETWORK_ID;
    }

    @Override
    public void decode() {
        position = getSignedBlockPosition();
        addUserData = getBoolean();
        selectedSlot = getByte();
    }

    @Override
    public void encode() {

    }
}
