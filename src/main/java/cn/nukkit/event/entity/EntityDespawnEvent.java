package cn.nukkit.event.entity;

import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityCreature;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.entity.item.EntityItem;
import cn.nukkit.entity.projectile.EntityProjectile;
import cn.nukkit.event.HandlerList;
import org.spongepowered.math.vector.Vector3d;

public class EntityDespawnEvent extends EntityEvent {
    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlers() {
        return handlers;
    }

    private final int entityType;

    public EntityDespawnEvent(Entity entity) {
        this.entity = entity;
        this.entityType = entity.getNetworkId();
    }

    public Vector3d getPosition() {
        return entity.getPosition();
    }

    public int getType() {
        return this.entityType;
    }

    public boolean isCreature() {
        return this.entity instanceof EntityCreature;
    }

    public boolean isHuman() {
        return this.entity instanceof EntityHuman;
    }

    public boolean isProjectile() {
        return this.entity instanceof EntityProjectile;
    }

    public boolean isVehicle() {
        return this.entity instanceof Entity;
    }

    public boolean isItem() {
        return this.entity instanceof EntityItem;
    }

}
