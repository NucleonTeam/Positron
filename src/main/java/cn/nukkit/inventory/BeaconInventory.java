package cn.nukkit.inventory;

import cn.nukkit.Player;
import cn.nukkit.level.Level;
import cn.nukkit.math.Vector3;
import org.spongepowered.math.vector.Vector3d;

public class BeaconInventory extends FakeBlockUIComponent {

    public BeaconInventory(PlayerUIInventory playerUI, Vector3d position, Level world) {
        super(playerUI, InventoryType.BEACON, 27, position, world);
    }

    @Override
    public void onClose(Player who) {
        super.onClose(who);

        // Drop item in slot if client doesn't automatically move it to player's inventory
        if (!who.isConnected()) {
            this.getHolder().getWorld().dropItem(new Vector3(getHolder().getPosition().add(0.5, 0.5, 0.5)), this.getItem(0));
        }
        this.clear(0);
    }
}
