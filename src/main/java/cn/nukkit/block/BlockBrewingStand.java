package cn.nukkit.block;

import cn.nukkit.Player;
import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.inventory.ContainerInventory;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemBrewingStand;
import cn.nukkit.item.ItemTool;
import cn.nukkit.math.BlockFace;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.nbt.tag.StringTag;
import cn.nukkit.nbt.tag.Tag;
import cn.nukkit.utils.BlockColor;

import java.util.Map;

public class BlockBrewingStand extends BlockTransparentMeta {

    public BlockBrewingStand() {
        this(0);
    }

    public BlockBrewingStand(int meta) {
        super(meta);
    }

    @Override
    public String getName() {
        return "Brewing Stand";
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public double getHardness() {
        return 0.5;
    }

    @Override
    public double getResistance() {
        return 2.5;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_PICKAXE;
    }

    @Override
    public int getId() {
        return BREWING_STAND_BLOCK;
    }

    @Override
    public int getLightLevel() {
        return 1;
    }

    @Override
    public boolean place(Item item, Block block, Block target, BlockFace face, double fx, double fy, double fz, Player player) {
        return false;
    }

    @Override
    public boolean onActivate(Item item, Player player) {
        return true;
    }

    @Override
    public Item toItem() {
        return new ItemBrewingStand();
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
    public BlockColor getColor() {
        return BlockColor.IRON_BLOCK_COLOR;
    }

    @Override
    public boolean isSolid() {
        return false;
    }

    @Override
    public double getMinX() {
        return this.x + 7 / 16.0;
    }

    @Override
    public double getMinZ() {
        return this.z + 7 / 16.0;
    }

    @Override
    public double getMaxX() {
        return this.x + 1 - 7 / 16.0;
    }

    @Override
    public double getMaxY() {
        return this.y + 1 - 2 / 16.0;
    }

    @Override
    public double getMaxZ() {
        return this.z + 1 - 7 / 16.0;
    }

    @Override
    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride() {
        return super.getComparatorInputOverride();
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }
}
