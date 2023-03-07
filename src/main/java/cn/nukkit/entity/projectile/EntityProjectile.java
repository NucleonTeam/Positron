package cn.nukkit.entity.projectile;

import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityLiving;
import cn.nukkit.event.entity.*;
import cn.nukkit.event.entity.EntityDamageEvent.DamageCause;
import cn.nukkit.level.MovingObjectPosition;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.AxisAlignedBB;
import cn.nukkit.nbt.tag.CompoundTag;
import org.spongepowered.math.vector.Vector3d;
import ru.mc_positron.entity.data.LongEntityData;
import ru.mc_positron.math.FastMath;

public abstract class EntityProjectile extends Entity {

    public static final int DATA_SHOOTER_ID = 17;

    public Entity shootingEntity;

    protected double getDamage() {
        return getNbt().contains("damage") ? getNbt().getDouble("damage") : getBaseDamage();
    }

    protected double getBaseDamage() {
        return 0;
    }

    public boolean hadCollision = false;

    public boolean closeOnCollide = true;

    protected double damage = 0;

    public static final int PICKUP_NONE = 0;
    public static final int PICKUP_ANY = 1;
    public static final int PICKUP_CREATIVE = 2;

    public EntityProjectile(FullChunk chunk, CompoundTag nbt) {
        this(chunk, nbt, null);
    }

    public EntityProjectile(FullChunk chunk, CompoundTag nbt, Entity shootingEntity) {
        super(chunk, nbt);
        this.shootingEntity = shootingEntity;
        if (shootingEntity != null) {
            this.setDataProperty(new LongEntityData(DATA_SHOOTER_ID, shootingEntity.getId()));
        }
    }

    public int getResultDamage() {
        return FastMath.ceilDouble(motion.distance(Vector3d.ZERO) * getDamage());
    }

    public boolean attack(EntityDamageEvent source) {
        return source.getCause() == DamageCause.VOID && super.attack(source);
    }

    public void onCollideWithEntity(Entity entity) {
        this.server.getPluginManager().callEvent(new ProjectileHitEvent(this, MovingObjectPosition.fromEntity(entity)));
        float damage = this.getResultDamage();

        EntityDamageEvent ev;
        if (this.shootingEntity == null) {
            ev = new EntityDamageByEntityEvent(this, entity, DamageCause.PROJECTILE, damage);
        } else {
            ev = new EntityDamageByChildEntityEvent(this.shootingEntity, this, entity, DamageCause.PROJECTILE, damage);
        }
        if (entity.attack(ev)) {
            this.hadCollision = true;

            if (this.fireTicks > 0) {
                EntityCombustByEntityEvent event = new EntityCombustByEntityEvent(this, entity, 5);
                this.server.getPluginManager().callEvent(event);
                if (!event.isCancelled()) {
                    entity.setOnFire(event.getDuration());
                }
            }
        }
        if (closeOnCollide) {
            this.remove();
        }
    }

    @Override
    protected void initEntity() {
        super.initEntity();

        this.setMaxHealth(1);
        this.setHealth(1);
        if (getNbt().contains("Age")) {
            this.age = getNbt().getShort("Age");
        }
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return (entity instanceof EntityLiving) && !this.onGround;
    }

    @Override
    public void saveNBT() {
        super.saveNBT();
        getNbt().putShort("Age", this.age);
    }

    @Override
    public boolean onUpdate(int currentTick) {
        if (removed) return false;

        int tickDiff = currentTick - this.lastUpdate;
        if (tickDiff <= 0 && !this.justCreated) {
            return true;
        }
        this.lastUpdate = currentTick;

        boolean hasUpdate = this.entityBaseTick(tickDiff);

        if (this.isAlive()) {

            MovingObjectPosition movingObjectPosition = null;

            if (!this.isCollided) motion = motion.sub(0, getGravity(), 0);

            var moveVector = position.add(motion);
            var list = world.getCollidingEntities(boundingBox.addCoord(motion).expand(1, 1, 1), this);

            double nearDistance = Integer.MAX_VALUE;
            Entity nearEntity = null;

            for (Entity entity : list) {
                if (/*!entity.canCollideWith(this) or */
                        (entity == shootingEntity && ticksLived < 5)
                        ) {
                    continue;
                }

                AxisAlignedBB axisalignedbb = entity.boundingBox.grow(0.3, 0.3, 0.3);
                MovingObjectPosition ob = axisalignedbb.calculateIntercept(getPosition(), moveVector);

                if (ob == null) {
                    continue;
                }

                double distance = position.distanceSquared(ob.hitVector);

                if (distance < nearDistance) {
                    nearDistance = distance;
                    nearEntity = entity;
                }
            }

            if (nearEntity != null) {
                movingObjectPosition = MovingObjectPosition.fromEntity(nearEntity);
            }

            if (movingObjectPosition != null) {
                if (movingObjectPosition.entityHit != null) {
                    onCollideWithEntity(movingObjectPosition.entityHit);
                    return true;
                }
            }

            move(motion);

            if (isCollided && !hadCollision) { //collide with block
                hadCollision = true;

                motion = Vector3d.ZERO;

                server.getPluginManager().callEvent(new ProjectileHitEvent(this, MovingObjectPosition.fromBlock(position.floorX(), position.floorX(), position.floorZ(), -1, position)));
                return false;
            } else if (!isCollided && hadCollision) {
                hadCollision = false;
            }

            if (!hadCollision || Math.abs(motion.x()) > 0.00001 || Math.abs(motion.y()) > 0.00001 || Math.abs(motion.z()) > 0.00001) {
                double f = Math.sqrt(FastMath.square(motion.x()) + FastMath.square(motion.z()));
                yaw = Math.atan2(motion.x(), motion.z()) * 180 / Math.PI;
                pitch = Math.atan2(motion.y(), f) * 180 / Math.PI;
                hasUpdate = true;
            }

            updateMovement();
        }

        return hasUpdate;
    }
}
