package cn.nukkit.network.protocol.types;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.spongepowered.math.vector.Vector3i;

@Data
@AllArgsConstructor
public class PlayerBlockActionData {

    private PlayerActionType action;
    private Vector3i position;
    private int facing;
}
