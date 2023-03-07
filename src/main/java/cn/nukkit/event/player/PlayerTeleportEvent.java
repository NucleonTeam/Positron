package cn.nukkit.event.player;

import cn.nukkit.Player;
import cn.nukkit.event.Cancellable;
import cn.nukkit.event.HandlerList;
import ru.mc_positron.math.Point;

public class PlayerTeleportEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlers() {
        return handlers;
    }

    private Point from;
    private Point to;

    private PlayerTeleportEvent(Player player) {
        this.player = player;
    }

    public PlayerTeleportEvent(Player player, Point from, Point to) {
        this(player);
        this.from = from;
        this.to = to;
    }

    public Point getFrom() {
        return from;
    }

    public Point getTo() {
        return to;
    }
}
