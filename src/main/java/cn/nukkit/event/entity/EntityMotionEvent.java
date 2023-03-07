package cn.nukkit.event.entity;

import cn.nukkit.entity.Entity;
import cn.nukkit.event.Cancellable;
import cn.nukkit.event.HandlerList;
import org.spongepowered.math.vector.Vector3d;

public class EntityMotionEvent extends EntityEvent implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlers() {
        return handlers;
    }

    private final Vector3d motion;

    public EntityMotionEvent(Entity entity, Vector3d motion) {
        this.entity = entity;
        this.motion = motion;
    }

    @Deprecated
    public Vector3d getVector() {
        return this.motion;
    }

    public Vector3d getMotion() {
        return this.motion;
    }
}
