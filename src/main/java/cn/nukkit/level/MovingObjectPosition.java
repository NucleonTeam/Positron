package cn.nukkit.level;

import cn.nukkit.entity.Entity;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3i;

public class MovingObjectPosition {

    /**
     * 0 = block, 1 = entity
     */
    public int typeOfHit;

    public Vector3i position;

    /**
     * Which side was hit. If its -1 then it went the full length of the ray trace.
     * Bottom = 0, Top = 1, East = 2, West = 3, North = 4, South = 5.
     */
    public int sideHit;

    public Vector3d hitVector;

    public Entity entityHit;

    public static MovingObjectPosition fromBlock(Vector3i position, int side, Vector3d hitVector) {
        MovingObjectPosition objectPosition = new MovingObjectPosition();
        objectPosition.typeOfHit = 0;
        objectPosition.position = position;
        objectPosition.hitVector = hitVector;
        return objectPosition;
    }

    public static MovingObjectPosition fromEntity(Entity entity) {
        MovingObjectPosition objectPosition = new MovingObjectPosition();
        objectPosition.typeOfHit = 1;
        objectPosition.entityHit = entity;
        objectPosition.hitVector = entity.getPosition();
        return objectPosition;
    }
}
