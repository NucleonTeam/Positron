package cn.nukkit.network.protocol;

import cn.nukkit.utils.Binary;
import lombok.ToString;
import ru.mc_positron.entity.data.EntityMetadata;

@ToString
public class SetEntityDataPacket extends DataPacket {

    public static final byte NETWORK_ID = ProtocolInfo.SET_ENTITY_DATA_PACKET;

    @Override
    public byte pid() {
        return NETWORK_ID;
    }

    public long eid;
    public EntityMetadata metadata;
    public long frame;

    @Override
    public void decode() {

    }

    @Override
    public void encode() {
        this.reset();
        this.putEntityRuntimeId(this.eid);
        this.put(Binary.writeMetadata(this.metadata));
        this.putUnsignedVarInt(0); // Entity properties int
        this.putUnsignedVarInt(0); // Entity properties float
        this.putUnsignedVarLong(this.frame);
    }
}
