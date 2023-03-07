package cn.nukkit.event.player;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.Cancellable;
import cn.nukkit.event.HandlerList;
import cn.nukkit.item.Item;
import org.spongepowered.math.vector.Vector3d;

public class PlayerInteractEntityEvent extends PlayerEvent implements Cancellable {


    private static final HandlerList handlers = new HandlerList();

    protected final Entity entity;
    protected final Item item;
    protected final Vector3d clickedPos;

    public PlayerInteractEntityEvent(Player player, Entity entity, Item item, Vector3d clickedPos) {
        this.player = player;
        this.entity = entity;
        this.item = item;
        this.clickedPos = clickedPos;
    }

    public Entity getEntity() {
        return this.entity;
    }

    public Item getItem() {
        return this.item;
    }

    public Vector3d getClickedPos() {
        return clickedPos;
    }

    public static HandlerList getHandlers() {
        return handlers;
    }
}
