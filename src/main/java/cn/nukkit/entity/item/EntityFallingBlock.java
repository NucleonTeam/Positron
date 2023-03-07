package cn.nukkit.entity.item;

import cn.nukkit.block.Block;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.entity.EntityBlockChangeEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.entity.EntityDamageEvent.DamageCause;
import cn.nukkit.level.GlobalBlockPalette;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import ru.mc_positron.entity.EntityFlags;
import ru.mc_positron.entity.data.IntEntityData;

public class EntityFallingBlock extends Entity {

    public static final int NETWORK_ID = 66;

    @Override
    public float getWidth() {
        return 0.98f;
    }

    @Override
    public float getLength() {
        return 0.98f;
    }

    @Override
    public float getHeight() {
        return 0.98f;
    }

    @Override
    protected float getGravity() {
        return 0.04f;
    }

    @Override
    protected float getDrag() {
        return 0.02f;
    }

    @Override
    protected float getBaseOffset() {
        return 0.49f;
    }

    @Override
    public boolean canCollide() {
        return false;
    }

    protected int blockId;
    protected int damage;

    public EntityFallingBlock(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    @Override
    protected void initEntity() {
        super.initEntity();

        if (getNbt() != null) {
            if (getNbt().contains("TileID")) {
                blockId = getNbt().getInt("TileID");
            } else if (getNbt().contains("Tile")) {
                blockId = getNbt().getInt("Tile");
                getNbt().putInt("TileID", blockId);
            }

            if (getNbt().contains("Data")) {
                damage = getNbt().getByte("Data");
            }
        }

        if (blockId == 0) {
            remove();
            return;
        }

        this.fireProof = true;
        this.setDataFlag(DATA_FLAGS, EntityFlags.FIRE_IMMUNE, true);

        setDataProperty(new IntEntityData(DATA_VARIANT, GlobalBlockPalette.getOrCreateRuntimeId(this.getBlock(), this.getDamage())));
    }

    public boolean canCollideWith(Entity entity) {
        return false;
    }

    @Override
    public boolean attack(EntityDamageEvent source) {
        return source.getCause() == DamageCause.VOID && super.attack(source);
    }

    @Override
    public boolean onUpdate(int currentTick) {
        if (removed) return false;

        int tickDiff = currentTick - lastUpdate;
        if (tickDiff <= 0 && !justCreated) return true;

        lastUpdate = currentTick;

        boolean hasUpdate = entityBaseTick(tickDiff);

        if (isAlive()) {
            motion = motion.sub(0, getGravity(), 0);
            move(motion);

            float friction = 1 - getDrag();
            motion = motion.mul(friction, 1 - getDrag(), friction);

            var pos = position.sub(0.5, 0, 0.5).round();

            if (onGround) {
                remove();
                Block block = world.getBlock(pos.toInt());

                EntityBlockChangeEvent event = new EntityBlockChangeEvent(this, block, Block.get(getBlock(), getDamage()));
                server.getPluginManager().callEvent(event);
                if (!event.isCancelled()) world.setBlock(new Vector3(pos), event.getTo(), true);
                hasUpdate = true;
            }

            updateMovement();
        }

        return hasUpdate || !onGround || Math.abs(motion.x()) > 0.00001 || Math.abs(motion.y()) > 0.00001 || Math.abs(motion.z()) > 0.00001;
    }

    public int getBlock() {
        return blockId;
    }

    public int getDamage() {
        return damage;
    }

    @Override
    public int getNetworkId() {
        return NETWORK_ID;
    }

    @Override
    public void saveNBT() {
        getNbt().putInt("TileID", blockId);
        getNbt().putByte("Data", damage);
    }

    @Override
    public boolean canBeMovedByCurrents() {
        return false;
    }

    @Override
    public void resetFallDistance() {
        if (removed) return;

        // For falling anvil: do not reset fall distance before dealing damage to entities
        highestPosition = position.y();
    }
}
