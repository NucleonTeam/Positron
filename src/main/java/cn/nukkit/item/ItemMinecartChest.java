package cn.nukkit.item;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.level.Level;
import cn.nukkit.math.BlockFace;
import cn.nukkit.utils.Rail;

public class ItemMinecartChest extends Item {

    public ItemMinecartChest() {
        this(0, 1);
    }

    public ItemMinecartChest(Integer meta) {
        this(meta, 1);
    }

    public ItemMinecartChest(Integer meta, int count) {
        super(MINECART_WITH_CHEST, meta, count, "Minecart with Chest");
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public boolean onActivate(Level level, Player player, Block block, Block target, BlockFace face, double fx, double fy, double fz) {
        /*
        if (Rail.isRailBlock(target)) {
            Rail.Orientation type = ((BlockRail) target).getOrientation();
            double adjacent = 0.0D;
            if (type.isAscending()) {
                adjacent = 0.5D;
            }

            return true;
        }
        */
        return false;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }
}
