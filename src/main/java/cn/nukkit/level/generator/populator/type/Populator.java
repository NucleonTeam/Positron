package cn.nukkit.level.generator.populator.type;

import cn.nukkit.block.BlockID;
import cn.nukkit.level.ChunkManager;
import cn.nukkit.level.format.FullChunk;

import java.util.Random;


public abstract class Populator implements BlockID {

    public abstract void populate(ChunkManager level, int chunkX, int chunkZ, Random random, FullChunk chunk);

    protected int getHighestWorkableBlock(ChunkManager level, int x, int z, FullChunk chunk)    {
        return chunk.getHighestBlockAt(x & 0xF, z & 0xF);
    }
}
