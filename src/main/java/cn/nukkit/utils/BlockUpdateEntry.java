package cn.nukkit.utils;

import cn.nukkit.block.Block;
import org.spongepowered.math.vector.Vector3i;

public class BlockUpdateEntry implements Comparable<BlockUpdateEntry> {
    private static long entryID = 0;

    public int priority;
    public long delay;

    public final Vector3i pos;
    public final Block block;

    public final long id;

    public BlockUpdateEntry(Vector3i pos, Block block) {
        this.pos = pos;
        this.block = block;
        this.id = entryID++;
    }

    public BlockUpdateEntry(Vector3i pos, Block block, long delay, int priority) {
        this.id = entryID++;
        this.pos = pos;
        this.priority = priority;
        this.delay = delay;
        this.block = block;
    }

    @Override
    public int compareTo(BlockUpdateEntry entry) {
        return this.delay < entry.delay ? -1 : (this.delay > entry.delay ? 1 : (this.priority != entry.priority ? this.priority - entry.priority : Long.compare(this.id, entry.id)));
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof BlockUpdateEntry)) {
            if (object instanceof Vector3i) {
                return pos.equals(object);
            }
            return false;
        } else {
            BlockUpdateEntry entry = (BlockUpdateEntry) object;
            return this.pos.equals(entry.pos) && Block.equals(this.block, entry.block, false);
        }
    }

    @Override
    public int hashCode() {
        return this.pos.hashCode();
    }
}