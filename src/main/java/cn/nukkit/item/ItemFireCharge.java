package cn.nukkit.item;

import cn.nukkit.block.*;
import cn.nukkit.math.BlockFace;
import cn.nukkit.level.Level;
import cn.nukkit.Player;

import java.util.concurrent.ThreadLocalRandom;
import cn.nukkit.event.block.BlockIgniteEvent;
import cn.nukkit.network.protocol.LevelEventPacket;

/**
 * Created by PetteriM1
 */
public class ItemFireCharge extends Item {

    public ItemFireCharge() {
        this(0, 1);
    }

    public ItemFireCharge(Integer meta) {
        this(meta, 1);
    }

    public ItemFireCharge(Integer meta, int count) {
        super(FIRE_CHARGE, 0, count, "Fire Charge");
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
}
