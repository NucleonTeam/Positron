package cn.nukkit.level.generator.object;

import cn.nukkit.block.Block;
import cn.nukkit.level.ChunkManager;
import cn.nukkit.math.NukkitRandom;
import cn.nukkit.math.Vector3;
import org.spongepowered.math.vector.Vector3i;

public abstract class BasicGenerator {

    //also autism, see below
    public abstract boolean generate(ChunkManager level, NukkitRandom rand, Vector3 position);

    public void setDecorationDefaults() {
    }

    protected void setBlockAndNotifyAdequately(ChunkManager level, Vector3i pos, Block state) {
        setBlock(level, new Vector3(pos.x(), pos.y(), pos.z()), state);
    }

    protected void setBlockAndNotifyAdequately(ChunkManager level, Vector3 pos, Block state) {
        setBlock(level, pos, state);
    }

    //what autism is this? why are we using floating-point vectors for setting block IDs?
    protected void setBlock(ChunkManager level, Vector3 v, Block b) {
        level.setBlockAt((int) v.x, (int) v.y, (int) v.z, b.getId(), b.getDamage());
    }
}
