package cn.nukkit.block;

import cn.nukkit.Player;
import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.item.Item;
import cn.nukkit.math.BlockFace;
import cn.nukkit.math.BlockFace.Plane;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.nbt.tag.Tag;

import java.util.Map;

public class BlockTrappedChest extends BlockChest {

    public BlockTrappedChest() {
        this(0);
    }

    public BlockTrappedChest(int meta) {
        super(meta);
    }

    @Override
    public int getId() {
        return TRAPPED_CHEST;
    }

    @Override
    public String getName() {
        return "Trapped Chest";
    }

    @Override
    public int getStrongPower(BlockFace side) {
        return side == BlockFace.UP ? this.getWeakPower(side) : 0;
    }

    @Override
    public boolean isPowerSource() {
        return true;
    }
}
