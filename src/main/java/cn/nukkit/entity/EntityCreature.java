package cn.nukkit.entity;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import org.spongepowered.math.vector.Vector3d;

public abstract class EntityCreature extends EntityLiving {

    @Override
    public boolean onInteract(Player player, Item item, Vector3d clickedPos) {
        return false;
    }

    // Structured like this so I can override nametags in player and dragon classes
    // without overriding onInteract.
    protected boolean applyNameTag(Player player, Item item) {
        if (item.hasCustomName()) {
            this.setNameTag(item.getCustomName());
            this.setNameTagVisible(true);
            return true; // onInteract: true = decrease count
        }
        return false;
    }
}
