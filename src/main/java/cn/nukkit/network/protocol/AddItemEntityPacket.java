package cn.nukkit.network.protocol;

import cn.nukkit.item.Item;
import cn.nukkit.utils.Binary;
import lombok.ToString;
import org.spongepowered.math.vector.Vector3f;
import ru.mc_positron.entity.data.EntityMetadata;

@ToString
public class AddItemEntityPacket extends DataPacket {
    public static final byte NETWORK_ID = ProtocolInfo.ADD_ITEM_ENTITY_PACKET;

    @Override
    public byte pid() {
        return NETWORK_ID;
    }

    public long entityUniqueId;
    public long entityRuntimeId;
    public Item item;
    public Vector3f position;
    public Vector3f speed;
    public EntityMetadata metadata = new EntityMetadata();
    public boolean isFromFishing = false;

    @Override
    public void decode() {

    }

    @Override
    public void encode() {
        reset();
        putEntityUniqueId(entityUniqueId);
        putEntityRuntimeId(entityRuntimeId);
        putSlot(item);
        putVector3f(position);
        putVector3f(speed);
        put(Binary.writeMetadata(metadata));
        putBoolean(isFromFishing);
    }
}
