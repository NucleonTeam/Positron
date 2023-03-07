package cn.nukkit.network.protocol;

import cn.nukkit.network.protocol.types.*;
import lombok.ToString;
import org.spongepowered.math.vector.Vector2d;
import org.spongepowered.math.vector.Vector3f;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@ToString
public class PlayerAuthInputPacket extends DataPacket {
    public static final byte NETWORK_ID = ProtocolInfo.PLAYER_AUTH_INPUT_PACKET;

    public float yaw;
    public float pitch;
    public float headYaw;
    public Vector3f position;
    public Vector2d motion;
    public Set<AuthInputAction> inputData = EnumSet.noneOf(AuthInputAction.class);
    public InputMode inputMode;
    public ClientPlayMode playMode;
    public AuthInteractionModel interactionModel;
    public Vector3f vrGazeDirection;
    public long tick;
    public Vector3f delta;
    // private ItemStackRequest itemStackRequest;
    public Map<PlayerActionType, PlayerBlockActionData> blockActionData = new EnumMap<>(PlayerActionType.class);

    @Override
    public byte pid() {
        return NETWORK_ID;
    }

    @Override
    public void decode() {
        pitch = getLFloat();
        yaw = getLFloat();
        position = getVector3f();
        motion = new Vector2d(getLFloat(), getLFloat());
        headYaw = getLFloat();

        long inputData = this.getUnsignedVarLong();
        for (int i = 0; i < AuthInputAction.size(); i++) {
            if ((inputData & (1L << i)) != 0) {
                this.inputData.add(AuthInputAction.from(i));
            }
        }

        this.inputMode = InputMode.fromOrdinal((int) this.getUnsignedVarInt());
        this.playMode = ClientPlayMode.fromOrdinal((int) this.getUnsignedVarInt());
        this.interactionModel = AuthInteractionModel.fromOrdinal((int) this.getUnsignedVarInt());

        if (this.playMode == ClientPlayMode.REALITY) {
            this.vrGazeDirection = this.getVector3f();
        }

        this.tick = this.getUnsignedVarLong();
        this.delta = this.getVector3f();

        if (this.inputData.contains(AuthInputAction.PERFORM_ITEM_STACK_REQUEST)) {
            // TODO: this.itemStackRequest = readItemStackRequest(buf, protocolVersion);
            // We are safe to leave this for later, since it is only sent with ServerAuthInventories
        }

        if (this.inputData.contains(AuthInputAction.PERFORM_BLOCK_ACTIONS)) {
            int arraySize = this.getVarInt();
            for (int i = 0; i < arraySize; i++) {
                PlayerActionType type = PlayerActionType.from(this.getVarInt());
                switch (type) {
                    case START_DESTROY_BLOCK:
                    case ABORT_DESTROY_BLOCK:
                    case CRACK_BLOCK:
                    case PREDICT_DESTROY_BLOCK:
                    case CONTINUE_DESTROY_BLOCK:
                        this.blockActionData.put(type, new PlayerBlockActionData(type, this.getSignedBlockPosition(), this.getVarInt()));
                        break;

                    default:
                        this.blockActionData.put(type, new PlayerBlockActionData(type, null, -1));
                }
            }
        }
    }

    @Override
    public void encode() {
        // Noop
    }
}
