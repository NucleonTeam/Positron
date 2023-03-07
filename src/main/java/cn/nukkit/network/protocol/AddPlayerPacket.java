package cn.nukkit.network.protocol;

import cn.nukkit.Server;
import cn.nukkit.item.Item;
import cn.nukkit.utils.Binary;
import lombok.ToString;
import org.spongepowered.math.vector.Vector3f;
import ru.mc_positron.entity.data.EntityMetadata;

import java.util.UUID;

@ToString
public class AddPlayerPacket extends DataPacket {
    public static final byte NETWORK_ID = ProtocolInfo.ADD_PLAYER_PACKET;

    @Override
    public byte pid() {
        return NETWORK_ID;
    }

    public UUID uuid;
    public String username;
    public long entityUniqueId;
    public long entityRuntimeId;
    public String platformChatId = "";
    public Vector3f position;
    public Vector3f speed;
    public float pitch;
    public float yaw;
    public Item item;
    public int gameType = Server.getInstance().getGamemode();
    public EntityMetadata metadata = new EntityMetadata();
    //public EntityLink links = new EntityLink[0];
    public String deviceId = "";
    public int buildPlatform = -1;

    @Override
    public void decode() {

    }

    @Override
    public void encode() {
        reset();
        putUUID(uuid);
        putString(username);
        // this.putEntityUniqueId(this.entityUniqueId);
        putEntityRuntimeId(entityRuntimeId);
        putString(platformChatId);
        putVector3f(position);
        putVector3f(speed);
        putLFloat(pitch);
        putLFloat(yaw); //TODO headrot
        putLFloat(yaw);
        putSlot(item);
        putVarInt(gameType);
        put(Binary.writeMetadata(metadata));
        putUnsignedVarInt(0); // Entity properties int
        putUnsignedVarInt(0); // Entity properties float
        putLLong(entityUniqueId);
        putUnsignedVarInt(0); // playerPermission
        putUnsignedVarInt(0); // commandPermission
        putUnsignedVarInt(1); // abilitiesLayer size
        putLShort(1); // BASE layer type
        putLInt(262143); // abilitiesSet - all abilities
        putLInt(63); // abilityValues - survival abilities
        putLFloat(0.1f); // flySpeed
        putLFloat(0.05f); // walkSpeed
        putUnsignedVarInt(0); //TODO: Entity links
        putString(deviceId);
        putLInt(buildPlatform);
    }
}
