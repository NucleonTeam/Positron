package cn.nukkit.entity.weather;

import cn.nukkit.block.Block;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.level.GameRule;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.AxisAlignedBB;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.network.protocol.LevelSoundEventPacket;

import java.util.concurrent.ThreadLocalRandom;

public class EntityLightning extends Entity implements EntityLightningStrike {

    public static final int NETWORK_ID = 93;

    protected boolean isEffect = true;

    public int state;
    public int liveTime;


    @Override
    public int getNetworkId() {
        return NETWORK_ID;
    }

    public EntityLightning(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    @Override
    protected void initEntity() {
        super.initEntity();

        this.setHealth(4);
        this.setMaxHealth(4);

        this.state = 2;
        this.liveTime = ThreadLocalRandom.current().nextInt(3) + 1;
    }

    public boolean isEffect() {
        return this.isEffect;
    }

    public void setEffect(boolean e) {
        this.isEffect = e;
    }

    @Override
    public boolean attack(EntityDamageEvent source) {
        //false?
        source.setDamage(0);
        return super.attack(source);
    }

    @Override
    public boolean onUpdate(int currentTick) {
        if (removed) return false;

        int tickDiff = currentTick - this.lastUpdate;

        if (tickDiff <= 0 && !this.justCreated) {
            return true;
        }

        this.lastUpdate = currentTick;

        this.entityBaseTick(tickDiff);

        if (this.state == 2) {
            world.addLevelSoundEvent(position.toFloat(), LevelSoundEventPacket.SOUND_THUNDER);
            world.addLevelSoundEvent(position.toFloat(), LevelSoundEventPacket.SOUND_EXPLODE);
        }

        this.state--;

        if (this.state < 0) {
            if (this.liveTime == 0) {
                this.remove();
                return false;
            } else if (this.state < -ThreadLocalRandom.current().nextInt(10)) {
                this.liveTime--;
                this.state = 1;

                if (this.isEffect && world.gameRules.getBoolean(GameRule.DO_FIRE_TICK)) {
                    Block block = world.getBlock(position.toInt());
                }
            }
        }

        if (this.state >= 0) {
            if (this.isEffect) {
                AxisAlignedBB bb = getBoundingBox().grow(3, 3, 3);
                bb.setMaxX(bb.getMaxX() + 6);

                for (Entity entity: world.getCollidingEntities(bb, this)) {
                    entity.onStruckByLightning(this);
                }
            }
        }

        return true;
    }


}
