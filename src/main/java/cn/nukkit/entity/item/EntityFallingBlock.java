package cn.nukkit.entity.item;

import cn.nukkit.block.Block;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.entity.EntityBlockChangeEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.entity.EntityDamageEvent.DamageCause;
import cn.nukkit.level.GlobalBlockPalette;
import cn.nukkit.level.Level;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import lombok.NonNull;
import ru.mc_positron.entity.EntityDataKeys;
import ru.mc_positron.entity.EntityFlags;
import ru.mc_positron.entity.data.IntEntityData;
import ru.mc_positron.math.Point;

public class EntityFallingBlock extends Entity {

    public static final int NETWORK_ID = 66;

    protected int blockId;
    protected int damage;

    @Override
    public void init(@NonNull CompoundTag nbt) {
        if (nbt.contains("TileID")) {
            blockId = nbt.getInt("TileID");
        } else if (getNbt().contains("Tile")) {
            blockId = nbt.getInt("Tile");
            nbt.putInt("TileID", blockId);
        }

        if (nbt.contains("Data")) {
            damage = nbt.getByte("Data");
        }

        if (blockId == 0) {
            remove();
            return;
        }

        fireProof = true;
        setDataFlag(EntityDataKeys.FLAGS, EntityFlags.FIRE_IMMUNE, true);

        setDataProperty(new IntEntityData(EntityDataKeys.VARIANT, GlobalBlockPalette.getOrCreateRuntimeId(getBlock(), getDamage())));

        super.init(nbt);
    }

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
                if (!event.isCancelled()) world.setBlock(pos.toInt(), event.getTo(), true);
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
    public @NonNull CompoundTag getSaveData() {
        getNbt().putInt("TileID", blockId);
        getNbt().putByte("Data", damage);
        return super.getSaveData();
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
