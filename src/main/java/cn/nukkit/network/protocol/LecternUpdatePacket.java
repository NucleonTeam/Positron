package cn.nukkit.network.protocol;

import lombok.ToString;
import org.spongepowered.math.vector.Vector3i;

@ToString
public class LecternUpdatePacket extends DataPacket {

    public static final byte NETWORK_ID = ProtocolInfo.LECTERN_UPDATE_PACKET;

    public int page;
    public int totalPages;
    public Vector3i blockPosition;
    public boolean dropBook;

    @Override
    public byte pid() {
        return NETWORK_ID;
    }

    @Override
    public void decode() {
        page = getByte();
        totalPages = getByte();
        blockPosition = getBlockVector3();
        dropBook = getBoolean();
    }

    @Override
    public void encode() {
    }
}
