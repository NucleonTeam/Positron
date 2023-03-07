package cn.nukkit.event.player;

import cn.nukkit.Player;
import cn.nukkit.event.HandlerList;
import ru.mc_positron.math.Point;

public class PlayerRespawnEvent extends PlayerEvent {
    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlers() {
        return handlers;
    }

    private Point point;
    private boolean firstSpawn;

    public PlayerRespawnEvent(Player player, Point point) {
        this(player, point, false);
    }

    public PlayerRespawnEvent(Player player, Point point, boolean firstSpawn) {
        this.player = player;
        this.point = point;
        this.firstSpawn = firstSpawn;
    }

    public Point getRespawnPoint() {
        return point;
    }

    public void setRespawnPosition(Point position) {
        this.point = position;
    }

    public boolean isFirstSpawn() {
        return firstSpawn;
    }
}
