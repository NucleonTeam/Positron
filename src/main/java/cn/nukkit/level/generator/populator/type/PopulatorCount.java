package cn.nukkit.level.generator.populator.type;

import cn.nukkit.level.ChunkManager;
import cn.nukkit.level.format.FullChunk;

import java.util.Random;


public abstract class PopulatorCount extends Populator {
    private int randomAmount;
    private int baseAmount;

    public final void setRandomAmount(int randomAmount) {
        this.randomAmount = randomAmount + 1;
    }

    public final void setBaseAmount(int baseAmount) {
        this.baseAmount = baseAmount;
    }

    @Override
    public final void populate(ChunkManager level, int chunkX, int chunkZ, Random random, FullChunk chunk) {
        int count = baseAmount + random.nextInt(randomAmount);
        for (int i = 0; i < count; i++) {
            populateCount(level, chunkX, chunkZ, random, chunk);
        }
    }

    protected abstract void populateCount(ChunkManager level, int chunkX, int chunkZ, Random random, FullChunk chunk);
}
