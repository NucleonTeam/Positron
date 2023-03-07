package cn.nukkit.entity;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.event.entity.EntityDamageByChildEntityEvent;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.entity.EntityDamageEvent.DamageCause;
import cn.nukkit.event.entity.EntityDeathEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.GameRule;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.FloatTag;
import cn.nukkit.network.protocol.AnimatePacket;
import cn.nukkit.network.protocol.EntityEventPacket;
import cn.nukkit.network.protocol.LevelSoundEventPacket;
import cn.nukkit.potion.Effect;
import cn.nukkit.utils.BlockIterator;
import org.spongepowered.math.vector.Vector3d;
import ru.mc_positron.entity.EntityFlags;
import ru.mc_positron.entity.data.ShortEntityData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public abstract class EntityLiving extends Entity {

    public EntityLiving(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    @Override
    protected float getGravity() {
        return 0.08f;
    }

    @Override
    protected float getDrag() {
        return 0.02f;
    }

    protected int attackTime = 0;

    protected boolean invisible = false;

    protected float movementSpeed = 0.1f;

    protected int turtleTicks = 0;

    @Override
    protected void initEntity() {
        super.initEntity();

        if (this.namedTag.contains("HealF")) {
            this.namedTag.putFloat("Health", this.namedTag.getShort("HealF"));
            this.namedTag.remove("HealF");
        }

        if (!this.namedTag.contains("Health") || !(this.namedTag.get("Health") instanceof FloatTag)) {
            this.namedTag.putFloat("Health", this.getMaxHealth());
        }

        this.health = this.namedTag.getFloat("Health");
    }

    @Override
    public void setHealth(float health) {
        boolean wasAlive = this.isAlive();
        super.setHealth(health);
        if (this.isAlive() && !wasAlive) {
            EntityEventPacket pk = new EntityEventPacket();
            pk.eid = this.getId();
            pk.event = EntityEventPacket.RESPAWN;
            Server.broadcastPacket(this.hasSpawned.values(), pk);
        }
    }

    @Override
    public void saveNBT() {
        super.saveNBT();
        this.namedTag.putFloat("Health", this.getHealth());
    }

    public boolean hasLineOfSight(Entity entity) {
        //todo
        return true;
    }

    public void collidingWith(Entity ent) { // can override (IronGolem|Bats)
        ent.applyEntityCollision(this);
    }

    @Override
    public boolean attack(EntityDamageEvent source) {
        if (this.noDamageTicks > 0) {
            return false;
        } else if (this.attackTime > 0) {
            EntityDamageEvent lastCause = this.getLastDamageCause();
            if (lastCause != null && lastCause.getDamage() >= source.getDamage()) {
                return false;
            }
        }

        if (super.attack(source)) {
            if (source instanceof EntityDamageByEntityEvent) {
                Entity damager = ((EntityDamageByEntityEvent) source).getDamager();
                if (source instanceof EntityDamageByChildEntityEvent) {
                    damager = ((EntityDamageByChildEntityEvent) source).getChild();
                }

                //Critical hit
                if (damager instanceof Player && !damager.onGround) {
                    AnimatePacket animate = new AnimatePacket();
                    animate.action = AnimatePacket.Action.CRITICAL_HIT;
                    animate.eid = getId();

                    world.addChunkPacket(damager.getChunkX(), damager.getChunkZ(), animate);
                    world.addLevelSoundEvent(new Vector3(position), LevelSoundEventPacket.SOUND_ATTACK_STRONG);

                    source.setDamage(source.getDamage() * 1.5f);
                }

                if (damager.isOnFire() && !(damager instanceof Player)) {
                    this.setOnFire(2 * this.server.getDifficulty());
                }

                double deltaX = position.x() - damager.position.x();
                double deltaZ = position.z() - damager.position.z();
                this.knockBack(damager, source.getDamage(), deltaX, deltaZ, ((EntityDamageByEntityEvent) source).getKnockBack());
            }

            EntityEventPacket pk = new EntityEventPacket();
            pk.eid = this.getId();
            pk.event = this.getHealth() <= 0 ? EntityEventPacket.DEATH_ANIMATION : EntityEventPacket.HURT_ANIMATION;
            Server.broadcastPacket(this.hasSpawned.values(), pk);

            this.attackTime = source.getAttackCooldown();

            return true;
        } else {
            return false;
        }
    }

    public void knockBack(Entity attacker, double damage, double x, double z) {
        this.knockBack(attacker, damage, x, z, 0.4);
    }

    public void knockBack(Entity attacker, double damage, double x, double z, double base) {
        double f = Math.sqrt(x * x + z * z);
        if (f <= 0) return;
        f = 1 / f;

        var newMotion = motion.div(2d).add(x * f * base, base, z * f * base);
        if (newMotion.y() > base) newMotion = new Vector3d(newMotion.x(), base, newMotion.z());
        setMotion(newMotion);
    }

    @Override
    public void kill() {
        if (!this.isAlive()) return;
        super.kill();

        var ev = new EntityDeathEvent(this, getDrops());
        server.getPluginManager().callEvent(ev);

        if (world.getGameRules().getBoolean(GameRule.DO_ENTITY_DROPS)) {
            for (Item item: ev.getDrops()) {
                world.dropItem(new Vector3(position), item);
            }
        }
    }

    @Override
    public boolean entityBaseTick(int tickDiff) {
        boolean isBreathing = !this.isInsideOfWater();

        if (this instanceof Player) {
            if (isBreathing) {
                turtleTicks = 200;
            } else if (turtleTicks > 0) {
                isBreathing = true;
                turtleTicks--;
            }

            if ((((Player) this).isCreative() || ((Player) this).isSpectator())) {
                isBreathing = true;
            }
        }
        
        this.setDataFlag(DATA_FLAGS, EntityFlags.BREATHING, isBreathing);

        boolean hasUpdate = super.entityBaseTick(tickDiff);

        if (this.isAlive()) {

            if (this.isInsideOfSolid()) {
                hasUpdate = true;
                this.attack(new EntityDamageEvent(this, DamageCause.SUFFOCATION, 1));
            }

            if (this.isOnLadder() || this.hasEffect(Effect.LEVITATION) || this.hasEffect(Effect.SLOW_FALLING)) {
                this.resetFallDistance();
            }

            if (!this.hasEffect(Effect.WATER_BREATHING) && this.isInsideOfWater()) {
                if ((this instanceof Player && (((Player) this).isCreative() || ((Player) this).isSpectator()))) {
                    this.setAirTicks(400);
                } else {
                    if (turtleTicks == 0 || turtleTicks == 200) {
                        hasUpdate = true;
                        int airTicks = this.getAirTicks() - tickDiff;

                        if (airTicks <= -20) {
                            airTicks = 0;
                            this.attack(new EntityDamageEvent(this, DamageCause.DROWNING, 2));
                        }

                        setAirTicks(airTicks);
                    }
                }
            } else {
                int airTicks = getAirTicks();

                if (airTicks < 400) {
                    setAirTicks(Math.min(400, airTicks + tickDiff * 5));
                }
            }
        }

        if (this.attackTime > 0) {
            this.attackTime -= tickDiff;
        }

        // Used to check collisions with magma blocks
        Block block = world.getBlock(position.toInt().sub(0, 1, 0));
        //if (block instanceof BlockMagma) block.onEntityCollide(this);

        return hasUpdate;
    }

    public Item[] getDrops() {
        return new Item[0];
    }

    public Block[] getLineOfSight(int maxDistance) {
        return this.getLineOfSight(maxDistance, 0);
    }

    public Block[] getLineOfSight(int maxDistance, int maxLength) {
        return this.getLineOfSight(maxDistance, maxLength, new Integer[]{});
    }

    @Deprecated
    public Block[] getLineOfSight(int maxDistance, int maxLength, Map<Integer, Object> transparent) {
        return this.getLineOfSight(maxDistance, maxLength, transparent.keySet().toArray(new Integer[0]));
    }

    public Block[] getLineOfSight(int maxDistance, int maxLength, Integer[] transparent) {
        if (maxDistance > 120) {
            maxDistance = 120;
        }

        if (transparent != null && transparent.length == 0) {
            transparent = null;
        }

        List<Block> blocks = new ArrayList<>();

        BlockIterator itr = new BlockIterator(world, new Vector3(getPosition()), new Vector3(getDirectionVector()), this.getEyeHeight(), maxDistance);

        while (itr.hasNext()) {
            Block block = itr.next();
            blocks.add(block);

            if (maxLength != 0 && blocks.size() > maxLength) {
                blocks.remove(0);
            }

            int id = block.getId();

            if (transparent == null) {
                if (id != 0) {
                    break;
                }
            } else {
                if (Arrays.binarySearch(transparent, id) < 0) {
                    break;
                }
            }
        }

        return blocks.toArray(new Block[0]);
    }

    public Block getTargetBlock(int maxDistance) {
        return getTargetBlock(maxDistance, new Integer[]{});
    }

    @Deprecated
    public Block getTargetBlock(int maxDistance, Map<Integer, Object> transparent) {
        return getTargetBlock(maxDistance, transparent.keySet().toArray(new Integer[0]));
    }

    public Block getTargetBlock(int maxDistance, Integer[] transparent) {
        try {
            Block[] blocks = this.getLineOfSight(maxDistance, 1, transparent);
            Block block = blocks[0];
            if (block != null) {
                if (transparent != null && transparent.length != 0) {
                    if (Arrays.binarySearch(transparent, block.getId()) < 0) {
                        return block;
                    }
                } else {
                    return block;
                }
            }
        } catch (Exception ignored) {

        }

        return null;
    }

    public void setMovementSpeed(float speed) {
        this.movementSpeed = speed;
    }

    public float getMovementSpeed() {
        return this.movementSpeed;
    }

    public int getAirTicks() {
        return this.getDataProperties().getShort(DATA_AIR);
    }

    public void setAirTicks(int ticks) {
        this.setDataProperty(new ShortEntityData(DATA_AIR, ticks));
    }
}
