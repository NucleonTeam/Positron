package cn.nukkit.block;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemTool;
import cn.nukkit.math.AxisAlignedBB;
import ru.mc_positron.math.BlockFace;
import cn.nukkit.math.SimpleAxisAlignedBB;
import cn.nukkit.utils.Faceable;

public abstract class BlockStairs extends BlockTransparentMeta implements Faceable {

    protected BlockStairs(int meta) {
        super(meta);
    }

    @Override
    public double getMinY() {
        // TODO: this seems wrong
        return getPosition().y() + ((getDamage() & 0x04) > 0 ? 0.5 : 0);
    }

    @Override
    public double getMaxY() {
        // TODO: this seems wrong
        return getPosition().y() + ((getDamage() & 0x04) > 0 ? 1 : 0.5);
    }

    @Override
    public boolean place(Item item, Block block, Block target, BlockFace face, double fx, double fy, double fz, Player player) {
        int[] faces = new int[]{2, 1, 3, 0};
        this.setDamage(faces[player != null ? player.getDirection().getHorizontalIndex() : 0]);
        if ((fy > 0.5 && face != BlockFace.UP) || face == BlockFace.DOWN) {
            this.setDamage(this.getDamage() | 0x04); //Upside-down stairs
        }
        getWorld().setBlock(block.getPosition(), this, true, true);

        return true;
    }

    @Override
    public Item[] getDrops(Item item) {
        if (item.isPickaxe() && item.getTier() >= ItemTool.TIER_WOODEN) {
            return new Item[]{
                    toItem()
            };
        } else {
            return new Item[0];
        }
    }

    @Override
    public Item toItem() {
        Item item = super.toItem();
        item.setDamage(0);
        return item;
    }

    @Override
    public boolean collidesWithBB(AxisAlignedBB bb) {
        int damage = this.getDamage();
        int side = damage & 0x03;
        double f = 0;
        double f1 = 0.5;
        double f2 = 0.5;
        double f3 = 1;
        if ((damage & 0x04) > 0) {
            f = 0.5;
            f1 = 1;
            f2 = 0;
            f3 = 0.5;
        }

        if (bb.intersectsWith(new SimpleAxisAlignedBB(getPosition().toDouble().add(0, f, 0), getPosition().toDouble().add(1, f1, 1)))) {
            return true;
        }


        if (side == 0) {
            if (bb.intersectsWith(new SimpleAxisAlignedBB(getPosition().toDouble().add(0.5, f2, 0), getPosition().toDouble().add(1, f3, 1)))) {
                return true;
            }
        } else if (side == 1) {
            if (bb.intersectsWith(new SimpleAxisAlignedBB(getPosition().toDouble().add(0, f2, 0), getPosition().toDouble().add(0.5, f3, 1)))) {
                return true;
            }
        } else if (side == 2) {
            if (bb.intersectsWith(new SimpleAxisAlignedBB(getPosition().toDouble().add(0, f2, 0.5), getPosition().toDouble().add(1, f3, 1)))) {
                return true;
            }
        } else if (side == 3) {
            if (bb.intersectsWith(new SimpleAxisAlignedBB(getPosition().toDouble().add(0, f2, 0), getPosition().toDouble().add(1, f3, 0.5)))) {
                return true;
            }
        }

        return false;
    }

    @Override
    public BlockFace getBlockFace() {
        return BlockFace.fromHorizontalIndex(this.getDamage() & 0x7);
    }
}
