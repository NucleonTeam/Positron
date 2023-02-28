package cn.nukkit.network.protocol;

import lombok.ToString;
import ru.mc_positron.entity.attribute.Attribute;

@ToString
public class UpdateAttributesPacket extends DataPacket {

    public static final byte NETWORK_ID = ProtocolInfo.UPDATE_ATTRIBUTES_PACKET;

    public Attribute.Entry[] entries;
    public long entityId;
    public long frame;

    @Override
    public byte pid() {
        return NETWORK_ID;
    }

    public void decode() {

    }

    public void encode() {
        reset();

        putEntityRuntimeId(entityId);

        if (entries == null) {
            putUnsignedVarInt(0);
        } else {
            putUnsignedVarInt(entries.length);
            for (var entry: entries) {
                var attribute = entry.getAttribute();

                putLFloat(attribute.getMinValue());
                putLFloat(attribute.getMaxValue());
                putLFloat(entry.getValue());
                putLFloat(attribute.getDefaultValue());
                putString(attribute.getIdentifier());
                putUnsignedVarInt(0); // Modifiers
            }
        }

        putUnsignedVarInt(frame);
    }
}
