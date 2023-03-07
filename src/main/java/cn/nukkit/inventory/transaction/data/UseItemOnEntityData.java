package cn.nukkit.inventory.transaction.data;

import cn.nukkit.item.Item;
import org.spongepowered.math.vector.Vector3d;

public class UseItemOnEntityData implements TransactionData {

    public long entityRuntimeId;
    public int actionType;
    public int hotbarSlot;
    public Item itemInHand;
    public Vector3d playerPos;
    public Vector3d clickPos;

}
