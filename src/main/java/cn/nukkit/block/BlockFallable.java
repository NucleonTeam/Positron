package cn.nukkit.block;

import cn.nukkit.entity.Entity;
import cn.nukkit.entity.item.EntityFallingBlock;
import cn.nukkit.event.block.BlockFallEvent;
import cn.nukkit.level.Level;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.DoubleTag;
import cn.nukkit.nbt.tag.FloatTag;
import cn.nukkit.nbt.tag.ListTag;


public abstract class BlockFallable extends BlockSolid {

    protected BlockFallable() {
    }

    public int onUpdate(int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            Block down = this.down();
            if (down.getId() == AIR || down instanceof BlockLiquid) {
                BlockFallEvent event = new BlockFallEvent(this);
                getWorld().getServer().getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    return type;
                }

                var pos = getPosition();
                getWorld().setBlock(pos, Block.get(Block.AIR), true, true);
                CompoundTag nbt = new CompoundTag()
                        .putList(new ListTag<DoubleTag>("Pos")
                                .add(new DoubleTag("", pos.x() + 0.5))
                                .add(new DoubleTag("", pos.y()))
                                .add(new DoubleTag("", pos.z() + 0.5)))
                        .putList(new ListTag<DoubleTag>("Motion")
                                .add(new DoubleTag("", 0))
                                .add(new DoubleTag("", 0))
                                .add(new DoubleTag("", 0)))

                        .putList(new ListTag<FloatTag>("Rotation")
                                .add(new FloatTag("", 0))
                                .add(new FloatTag("", 0)))
                        .putInt("TileID", this.getId())
                        .putByte("Data", this.getDamage());

                EntityFallingBlock fall = (EntityFallingBlock) Entity.createEntity("FallingSand", getWorld().getChunk(getChunkX(), getChunkZ()), nbt);

                if (fall != null) {
                    fall.spawnToAll();
                }
            }
        }
        return type;
    }
}
