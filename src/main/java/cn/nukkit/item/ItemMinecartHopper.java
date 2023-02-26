package cn.nukkit.item;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.level.Level;
import cn.nukkit.math.BlockFace;
import cn.nukkit.utils.Rail;

public class ItemMinecartHopper extends Item {

    public ItemMinecartHopper() {
        this(0, 1);
    }

    public ItemMinecartHopper(Integer meta) {
        this(meta, 1);
    }

    public ItemMinecartHopper(Integer meta, int count) {
        super(MINECART_WITH_HOPPER, meta, count, "Minecart with Hopper");
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
