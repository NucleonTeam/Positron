package cn.nukkit.inventory;

import cn.nukkit.level.Level;
import lombok.Getter;
import org.spongepowered.math.vector.Vector3d;

@Getter
public class FakeBlockMenu implements InventoryHolder {

    private final Vector3d position;
    private final Level world;
    private final Inventory inventory;

    public FakeBlockMenu(Inventory inventory, Vector3d position, Level world) {
        this.position = position;
        this.world = world;
        this.inventory = inventory;
    }
}
