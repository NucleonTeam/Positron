package cn.nukkit.network.protocol;

import lombok.ToString;
import org.spongepowered.math.vector.Vector3i;

@ToString
public class CommandBlockUpdatePacket extends DataPacket {

    public boolean isBlock;
    public Vector3i position;
    public int commandBlockMode;
    public boolean isRedstoneMode;
    public boolean isConditional;
    public long minecartEid;
    public String command;
    public String lastOutput;
    public String name;
    public boolean shouldTrackOutput;

    @Override
    public byte pid() {
        return ProtocolInfo.COMMAND_BLOCK_UPDATE_PACKET;
    }

    @Override
    public void decode() {
        isBlock = getBoolean();
        if (isBlock) {
            position = getBlockVector3();
            commandBlockMode = (int) getUnsignedVarInt();
            isRedstoneMode = getBoolean();
            isConditional = getBoolean();
        } else {
            minecartEid = getEntityRuntimeId();
        }
        command = getString();
        lastOutput = getString();
        name = getString();
        shouldTrackOutput = getBoolean();
    }

    @Override
    public void encode() {
        reset();
        putBoolean(this.isBlock);
        if (isBlock) {
            putBlockVector3(position);
            putUnsignedVarInt(commandBlockMode);
            putBoolean(isRedstoneMode);
            putBoolean(isConditional);
        } else {
            putEntityRuntimeId(minecartEid);
        }

        putString(command);
        putString(lastOutput);
        putString(name);
        putBoolean(shouldTrackOutput);
    }
}
