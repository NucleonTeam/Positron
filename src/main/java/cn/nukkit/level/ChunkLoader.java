package cn.nukkit.level;

import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.Vector3;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3i;

public interface ChunkLoader {

    int getLoaderId();

    Vector3d getPosition();

    Level getWorld();

    int getChunkX();

    int getChunkZ();

    void onChunkChanged(FullChunk chunk);
}
