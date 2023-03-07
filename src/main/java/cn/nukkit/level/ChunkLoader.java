package cn.nukkit.level;

import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.Vector3;
import org.spongepowered.math.vector.Vector3d;

public interface ChunkLoader {

    int getLoaderId();

    boolean isLoaderActive();

    Vector3d getPosition();

    Level getWorld();

    int getChunkX();

    int getChunkZ();

    void onChunkChanged(FullChunk chunk);

    void onChunkLoaded(FullChunk chunk);

    void onChunkUnloaded(FullChunk chunk);

    void onChunkPopulated(FullChunk chunk);

    void onBlockChanged(Vector3 block);
}
