package cn.nukkit.block;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.event.block.BlockSpreadEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.Level;
import cn.nukkit.level.particle.BoneMealParticle;
import cn.nukkit.math.NukkitRandom;
import cn.nukkit.math.Vector3;
import cn.nukkit.utils.BlockColor;

import java.util.Random;

public class BlockGrass extends BlockDirt {

    public BlockGrass() {
        this(0);
    }

    public BlockGrass(int meta) {
        // Grass can't have meta.
        super(0);
    }

    @Override
    public int getId() {
        return GRASS;
    }

    @Override
    public double getHardness() {
        return 0.6;
    }

    @Override
    public double getResistance() {
        return 3;
    }

    @Override
    public String getName() {
        return "Grass Block";
    }

    @Override
    public boolean onActivate(Item item, Player player) {
        return false;
    }

    @Override
    public int onUpdate(int type) {
        return 0;
    }

    @Override
    public BlockColor getColor() {
        return BlockColor.GRASS_BLOCK_COLOR;
    }

    @Override
    public boolean canSilkTouch() {
        return true;
    }

    @Override
    public int getFullId() {
        return this.getId() << 4;
    }

    @Override
    public void setDamage(int meta) {

    }
}
