package cn.nukkit.block;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import cn.nukkit.level.Level;
import ru.mc_positron.math.BlockFace;
import cn.nukkit.utils.BlockColor;

public abstract class BlockMushroom extends BlockFlowable {

    public BlockMushroom() {
        this(0);
    }

    public BlockMushroom(int meta) {
        super(0);
    }

    @Override
    public int onUpdate(int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            if (!canStay()) {
                getWorld().useBreakOn(getPosition());

                return Level.BLOCK_UPDATE_NORMAL;
            }
        }
        return 0;
    }

    @Override
    public boolean place(Item item, Block block, Block target, BlockFace face, double fx, double fy, double fz, Player player) {
        if (canStay()) {
            getWorld().setBlock(block.getPosition(), this, true, true);
            return true;
        }
        return false;
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public boolean onActivate(Item item, Player player) {
        return false;
    }

    public boolean grow() {
        getWorld().setBlock(getPosition(), Block.get(BlockID.AIR), true, false);

        return false;
    }

    public boolean canStay() {
        return false;
    }

    @Override
    public BlockColor getColor() {
        return BlockColor.FOLIAGE_BLOCK_COLOR;
    }

    @Override
    public boolean canSilkTouch() {
        return true;
    }

    protected abstract int getType();
}
