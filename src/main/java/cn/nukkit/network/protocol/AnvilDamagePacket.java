package cn.nukkit.network.protocol;

import lombok.ToString;
import org.spongepowered.math.vector.Vector3i;

@ToString
public class AnvilDamagePacket extends DataPacket {

    public int damage;
    public Vector3i position;

    @Override
    public byte pid() {
        return ProtocolInfo.ANVIL_DAMAGE_PACKET;
    }

    @Override
    public void decode() {
        damage = getByte();
        position = getBlockVector3();
    }

    @Override
    public void encode() {

    }
}
