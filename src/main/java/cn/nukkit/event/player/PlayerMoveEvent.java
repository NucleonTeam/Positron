package cn.nukkit.event.player;

import cn.nukkit.Player;
import cn.nukkit.event.Cancellable;
import cn.nukkit.event.HandlerList;
import ru.mc_positron.math.Point;

public class PlayerMoveEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlers() {
        return handlers;
    }

    private Point from;
    private Point to;

    private boolean resetBlocksAround;

    public PlayerMoveEvent(Player player, Point from, Point to) {
        this(player, from, to, true);
    }

    public PlayerMoveEvent(Player player, Point from, Point to, boolean resetBlocks) {
        this.player = player;
        this.from = from;
        this.to = to;
        this.resetBlocksAround = resetBlocks;
    }

    public Point getFrom() {
        return from;
    }

    public void setFrom(Point from) {
        this.from = from;
    }

    public Point getTo() {
        return to;
    }

    public void setTo(Point to) {
        this.to = to;
    }

    public boolean isResetBlocksAround() {
        return resetBlocksAround;
    }

    public void setResetBlocksAround(boolean value) {
        this.resetBlocksAround = value;
    }

    @Override
    public void setCancelled() {
        super.setCancelled();
    }
}
