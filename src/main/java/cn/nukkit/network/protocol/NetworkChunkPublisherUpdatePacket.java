package cn.nukkit.network.protocol;

import lombok.ToString;
import org.spongepowered.math.vector.Vector3i;

@ToString
public class NetworkChunkPublisherUpdatePacket extends DataPacket {

    public Vector3i position;
    public int radius;

    @Override
    public byte pid() {
        return ProtocolInfo.NETWORK_CHUNK_PUBLISHER_UPDATE_PACKET;
    }

    @Override
    public void decode() {
        position = getSignedBlockPosition();
        radius = (int) getUnsignedVarInt();
    }

    @Override
    public void encode() {
        reset();
        putSignedBlockPosition(position);
        putUnsignedVarInt(radius);
        putInt(0); // Saved chunks
    }
}
