package cn.nukkit.network.protocol;

import lombok.ToString;
import org.spongepowered.math.vector.Vector3i;

/**
 * @author Nukkit Project Team
 */
@ToString
public class SetSpawnPositionPacket extends DataPacket {

    public static final byte NETWORK_ID = ProtocolInfo.SET_SPAWN_POSITION_PACKET;

    public static final int TYPE_PLAYER_SPAWN = 0;
    public static final int TYPE_WORLD_SPAWN = 1;

    public int spawnType;
    public Vector3i position;
    public int dimension = 0;

    @Override
    public void decode() {

    }

    @Override
    public void encode() {
        this.reset();
        this.putVarInt(this.spawnType);
        this.putBlockVector3(position);
        this.putVarInt(dimension);
        this.putBlockVector3(position);
    }

    @Override
    public byte pid() {
        return NETWORK_ID;
    }

}
