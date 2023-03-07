package cn.nukkit.event.level;

import cn.nukkit.event.HandlerList;
import cn.nukkit.level.Level;
import ru.mc_positron.math.Point;

public class SpawnChangeEvent extends LevelEvent {

    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlers() {
        return handlers;
    }

    private final Point previousSpawn;

    public SpawnChangeEvent(Level level, Point previousSpawn) {
        super(level);
        this.previousSpawn = previousSpawn;
    }

    public Point getPreviousSpawn() {
        return previousSpawn;
    }
}
