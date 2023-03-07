package cn.nukkit.block;

import cn.nukkit.Player;
import cn.nukkit.event.block.DoorToggleEvent;
import cn.nukkit.item.Item;
import cn.nukkit.math.AxisAlignedBB;
import cn.nukkit.math.Vector3;
import ru.mc_positron.math.BlockFace;
import cn.nukkit.math.SimpleAxisAlignedBB;
import cn.nukkit.network.protocol.LevelEventPacket;
import cn.nukkit.utils.Faceable;

public abstract class BlockDoor extends BlockTransparentMeta implements Faceable {

    public static int DOOR_OPEN_BIT = 0x04;
    public static int DOOR_TOP_BIT = 0x08;
    public static int DOOR_HINGE_BIT = 0x01;
    public static int DOOR_POWERED_BIT = 0x02;

    protected BlockDoor(int meta) {
        super(meta);
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public boolean isSolid() {
        return false;
    }

    public int getFullDamage() {
        int meta;

        if (isTop()) {
            meta = this.down().getDamage();
        } else {
            meta = this.getDamage();
        }
        return (this.getId() << 5 ) + (meta & 0x07 | (isTop() ? 0x08 : 0) | (isRightHinged() ? 0x10 :0));
    }

    @Override
    protected AxisAlignedBB recalculateBoundingBox() {

        double f = 0.1875;

        var bb = new SimpleAxisAlignedBB(getPosition().toDouble(), getPosition().toDouble().add(1, 2, 1));

        int j = isTop() ? (this.down().getDamage() & 0x03) : getDamage() & 0x03;
        boolean isOpen = isOpen();
        boolean isRight = isRightHinged();

        if (j == 0) {
            if (isOpen) {
                if (!isRight) {
                    bb.setBounds(getPosition().toDouble(), getPosition().toDouble().add(1, 1, f));
                } else {
                    bb.setBounds(getPosition().toDouble().add(0, 0, 1 -f), getPosition().toDouble().add(1, 1, 1));
                }
            } else {
                bb.setBounds(getPosition().toDouble(), getPosition().toDouble().add(f, 1, 1));
            }
        } else if (j == 1) {
            if (isOpen) {
                if (!isRight) {
                    bb.setBounds(getPosition().toDouble().add(1 - f, 0, 0), getPosition().toDouble().add(1, 1, 1));
                } else {
                    bb.setBounds(getPosition().toDouble(), getPosition().toDouble().add(f, 1, 1));
                }
            } else {
                bb.setBounds(getPosition().toDouble(), getPosition().toDouble().add(1, 1, f));
            }
        } else if (j == 2) {
            if (isOpen) {
                if (!isRight) {
                    bb.setBounds(getPosition().toDouble().add(0, 0, 1 - f), getPosition().toDouble().add(1, 1, 1));
                } else {
                    bb.setBounds(getPosition().toDouble(), getPosition().toDouble().add(1, 1, f));
                }
            } else {
                bb.setBounds(getPosition().toDouble().add(1 - f, 0, 0), getPosition().toDouble().add(1, 1, 1));
            }
        } else if (j == 3) {
            if (isOpen) {
                if (!isRight) {
                    bb.setBounds(getPosition().toDouble(), getPosition().toDouble().add(f, 1, 1));
                } else {
                    bb.setBounds(getPosition().toDouble().add(1 - f, 0, 0), getPosition().toDouble().add(1, 1,1));
                }
            } else {
                bb.setBounds(getPosition().toDouble().add(0, 0, 1 - f), getPosition().toDouble().add(1, 1, 1));
            }
        }

        return bb;
    }

    @Override
    public int onUpdate(int type) {
        return 0;
    }

    @Override
    public boolean place(Item item, Block block, Block target, BlockFace face, double fx, double fy, double fz, Player player) {
        if (getPosition().y() > 254) return false;
        if (face == BlockFace.UP) {
            Block blockUp = this.up();
            Block blockDown = this.down();
            if (!blockUp.canBeReplaced() || blockDown.isTransparent()) {
                return false;
            }
            int[] faces = {1, 2, 3, 0};
            int direction = faces[player != null ? player.getDirection().getHorizontalIndex() : 0];

            Block left = this.getSide(player.getDirection().rotateYCCW());
            Block right = this.getSide(player.getDirection().rotateY());
            int metaUp = DOOR_TOP_BIT;
            if (left.getId() == this.getId() || (!right.isTransparent() && left.isTransparent())) { //Door hinge
                metaUp |= DOOR_HINGE_BIT;
            }

            this.setDamage(direction);
            getWorld().setBlock(block.getPosition(), this, true, false); //Bottom
            getWorld().setBlock(blockUp.getPosition(), Block.get(this.getId(), metaUp), true, true); //Top

            if (!this.isOpen() && getWorld().isBlockPowered(new Vector3(getPosition().toDouble()))) {
                this.toggle(null);
                metaUp |= DOOR_POWERED_BIT;
                getWorld().setBlockDataAt(blockUp.getPosition().x(), blockUp.getPosition().y(), blockUp.getPosition().z(), metaUp);
            }

            return true;
        }

        return false;
    }

    @Override
    public boolean onBreak(Item item) {
        if (isTop(this.getDamage())) {
            Block down = this.down();
            if (down.getId() == this.getId()) {
                getWorld().setBlock(down.getPosition(), Block.get(BlockID.AIR), true);
            }
        } else {
            Block up = this.up();
            if (up.getId() == this.getId()) {
                getWorld().setBlock(up.getPosition(), Block.get(BlockID.AIR), true);
            }
        }
        getWorld().setBlock(getPosition(), Block.get(BlockID.AIR), true);

        return true;
    }

    @Override
    public boolean onActivate(Item item) {
        return this.onActivate(item, null);
    }

    @Override
    public boolean onActivate(Item item, Player player) {
        if (!this.toggle(player)) {
            return false;
        }

        getWorld().addLevelEvent(new Vector3(getPosition().toDouble().add(0.5, 0.5, 0.5)), LevelEventPacket.EVENT_SOUND_DOOR);
        return true;
    }

    public boolean toggle(Player player) {
        DoorToggleEvent event = new DoorToggleEvent(this, player);
        getWorld().getServer().getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return false;
        }

        Block down;
        if (isTop()) {
            down = this.down();
        } else {
            down = this;
        }
        if (down.up().getId() != down.getId()) {
            return false;
        }
        down.setDamage(down.getDamage() ^ DOOR_OPEN_BIT);
        getWorld().setBlock(down.getPosition(), down, true, true);
        return true;
    }

    public boolean isOpen() {
        if (isTop(this.getDamage())) {
            return (this.down().getDamage() & DOOR_OPEN_BIT) > 0;
        } else {
            return (this.getDamage() & DOOR_OPEN_BIT) > 0;
        }
    }
    public boolean isTop() {
        return isTop(this.getDamage());
    }

    public boolean isTop(int meta) {
        return (meta & DOOR_TOP_BIT) != 0;
    }

    public boolean isRightHinged() {
        if (isTop()) {
            return (this.getDamage() & DOOR_HINGE_BIT ) > 0;
        }
        return (this.up().getDamage() & DOOR_HINGE_BIT) > 0;
    }

    @Override
    public BlockFace getBlockFace() {
        return BlockFace.fromHorizontalIndex(this.getDamage() & 0x07);
    }
}
