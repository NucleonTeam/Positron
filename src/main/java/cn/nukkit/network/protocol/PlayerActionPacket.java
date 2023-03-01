package cn.nukkit.network.protocol;

import lombok.ToString;
import org.spongepowered.math.vector.Vector3i;

@ToString
public class PlayerActionPacket extends DataPacket {

    public static final byte NETWORK_ID = ProtocolInfo.PLAYER_ACTION_PACKET;

    public static final int ACTION_START_BREAK = 0;
    public static final int ACTION_ABORT_BREAK = 1;
    public static final int ACTION_STOP_BREAK = 2;
    public static final int ACTION_GET_UPDATED_BLOCK = 3;
    public static final int ACTION_DROP_ITEM = 4;
    public static final int ACTION_START_SLEEPING = 5;
    public static final int ACTION_STOP_SLEEPING = 6;
    public static final int ACTION_RESPAWN = 7;
    public static final int ACTION_JUMP = 8;
    public static final int ACTION_START_SPRINT = 9;
    public static final int ACTION_STOP_SPRINT = 10;
    public static final int ACTION_START_SNEAK = 11;
    public static final int ACTION_STOP_SNEAK = 12;
    public static final int ACTION_CREATIVE_PLAYER_DESTROY_BLOCK = 13;
    public static final int ACTION_DIMENSION_CHANGE_ACK = 14; //sent when spawning in a different dimension to tell the server we spawned
    public static final int ACTION_START_GLIDE = 15;
    public static final int ACTION_STOP_GLIDE = 16;
    public static final int ACTION_BUILD_DENIED = 17;
    public static final int ACTION_CONTINUE_BREAK = 18;
    public static final int ACTION_SET_ENCHANTMENT_SEED = 20;
    public static final int ACTION_START_SWIMMING = 21;
    public static final int ACTION_STOP_SWIMMING = 22;
    public static final int ACTION_START_SPIN_ATTACK = 23;
    public static final int ACTION_STOP_SPIN_ATTACK = 24;
    public static final int ACTION_INTERACT_BLOCK = 25;
    public static final int ACTION_PREDICT_DESTROY_BLOCK = 26;
    public static final int ACTION_CONTINUE_DESTROY_BLOCK = 27;
    public static final int ACTION_START_ITEM_USE_ON = 28;
    public static final int ACTION_STOP_ITEM_USE_ON = 29;

    public long entityId;
    public int action;
    public Vector3i position;
    public Vector3i resultPosition;
    public int face;

    @Override
    public void decode() {
        entityId = getEntityRuntimeId();
        action = getVarInt();
        position = getBlockVector3();
        resultPosition = getBlockVector3();
        face = getVarInt();
    }

    @Override
    public void encode() {
        reset();
        putEntityRuntimeId(entityId);
        putVarInt(action);
        putBlockVector3(position);
        putBlockVector3(resultPosition);
        putVarInt(face);
    }

    @Override
    public byte pid() {
        return NETWORK_ID;
    }

}
