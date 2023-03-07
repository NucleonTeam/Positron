package cn.nukkit.network.protocol;

import cn.nukkit.entity.item.*;
import cn.nukkit.entity.weather.EntityLightning;
import cn.nukkit.network.protocol.types.EntityLink;
import cn.nukkit.utils.Binary;
import com.google.common.collect.ImmutableMap;
import lombok.ToString;
import org.spongepowered.math.vector.Vector3f;
import ru.mc_positron.entity.attribute.Attribute;
import ru.mc_positron.entity.data.EntityMetadata;

@ToString
public class AddEntityPacket extends DataPacket {
    public static final byte NETWORK_ID = ProtocolInfo.ADD_ENTITY_PACKET;

    public static ImmutableMap<Integer, String> LEGACY_IDS = ImmutableMap.<Integer, String>builder()
            .put(51, "minecraft:npc")
            .put(63, "minecraft:player")
            .put(107, "minecraft:balloon")
            .put(20, "minecraft:iron_golem")
            .put(21, "minecraft:snow_golem")
            .put(100, "minecraft:command_block_minecart")
            .put(61, "minecraft:armor_stand")
            .put(EntityItem.NETWORK_ID, "minecraft:item")
            .put(EntityFallingBlock.NETWORK_ID, "minecraft:falling_block")
            .put(70, "minecraft:eye_of_ender_signal")
            .put(76, "minecraft:shulker_bullet")
            .put(79, "minecraft:dragon_fireball")
            .put(85, "minecraft:fireball")
            .put(88, "minecraft:leash_knot")
            .put(89, "minecraft:wither_skull")
            .put(91, "minecraft:wither_skull_dangerous")
            .put(EntityLightning.NETWORK_ID, "minecraft:lightning_bolt")
            .put(94, "minecraft:small_fireball")
            .put(102, "minecraft:llama_spit")
            .put(95, "minecraft:area_effect_cloud")
            .put(101, "minecraft:lingering_potion")
            .put(103, "minecraft:evocation_fang")
            .put(104, "minecraft:evocation_illager")
            .put(56, "minecraft:agent")
            .put(106, "minecraft:ice_bomb")
            .put(62, "minecraft:tripod_camera")
            .build();

    @Override
    public byte pid() {
        return NETWORK_ID;
    }

    public long entityUniqueId;
    public long entityRuntimeId;
    public int type;
    public String id;
    public Vector3f position;
    public Vector3f speed = Vector3f.ZERO;
    public float yaw;
    public float pitch;
    public float headYaw;
    public float bodyYaw = -1;
    public EntityMetadata metadata = new EntityMetadata();
    public Attribute.Entry[] attributes = new Attribute.Entry[0];
    public EntityLink[] links = new EntityLink[0];

    @Override
    public void decode() {

    }

    @Override
    public void encode() {
        this.reset();
        this.putEntityUniqueId(this.entityUniqueId);
        this.putEntityRuntimeId(this.entityRuntimeId);
        if (id == null) {
            id = LEGACY_IDS.get(type);
        }
        this.putString(this.id);
        this.putVector3f(position);
        this.putVector3f(speed);
        this.putLFloat(this.pitch);
        this.putLFloat(this.yaw);
        this.putLFloat(this.headYaw);
        this.putLFloat(this.bodyYaw == -1 ? this.yaw : this.bodyYaw);
        this.putAttributeList(this.attributes);
        this.put(Binary.writeMetadata(this.metadata));
        this.putUnsignedVarInt(0); // Entity properties int
        this.putUnsignedVarInt(0); // Entity properties float
        this.putUnsignedVarInt(this.links.length);
        for (EntityLink link : links) {
            putEntityLink(link);
        }
    }
}
