package cn.nukkit.item;

import cn.nukkit.Player;
import cn.nukkit.block.*;
import cn.nukkit.event.block.BlockIgniteEvent;
import cn.nukkit.level.Level;
import cn.nukkit.math.BlockFace;
import cn.nukkit.network.protocol.LevelSoundEventPacket;

import java.util.concurrent.ThreadLocalRandom;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ItemFlintSteel extends ItemTool {
    
    public ItemFlintSteel() {
        this(0, 1);
    }

    public ItemFlintSteel(Integer meta) {
        this(meta, 1);
    }

    public ItemFlintSteel(Integer meta, int count) {
        super(FLINT_STEEL, meta, count, "Flint and Steel");
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public boolean onActivate(Level level, Player player, Block block, Block target, BlockFace face, double fx, double fy, double fz) {
        if (player.isAdventure()) {
            return false;
        }

        if (block.getId() == AIR && (target instanceof BlockSolid || target instanceof BlockSolidMeta)) {
        }
        return false;
    }

    @Override
    public int getMaxDurability() {
        return ItemTool.DURABILITY_FLINT_STEEL;
    }
}
