package cn.nukkit.inventory.transaction.data;

import cn.nukkit.item.Item;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3f;
import org.spongepowered.math.vector.Vector3i;
import ru.mc_positron.math.BlockFace;
import lombok.ToString;

@ToString
public class UseItemData implements TransactionData {

    public int actionType;
    public Vector3i blockPos;
    public BlockFace face;
    public int hotbarSlot;
    public Item itemInHand;
    public Vector3d playerPos;
    public Vector3f clickPos;
    public int blockRuntimeId;
}
