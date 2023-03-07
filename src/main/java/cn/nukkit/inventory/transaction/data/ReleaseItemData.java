package cn.nukkit.inventory.transaction.data;

import cn.nukkit.item.Item;
import lombok.ToString;
import org.spongepowered.math.vector.Vector3d;

@ToString
public class ReleaseItemData implements TransactionData {

    public int actionType;
    public int hotbarSlot;
    public Item itemInHand;
    public Vector3d headRot;
}
