package cn.nukkit.block;

import cn.nukkit.math.AxisAlignedBB;
import cn.nukkit.math.SimpleAxisAlignedBB;
import cn.nukkit.utils.LevelException;

public abstract class BlockThin extends BlockTransparent {

    protected BlockThin() {
    }

    @Override
    public boolean isSolid() {
        return false;
    }

    protected AxisAlignedBB recalculateBoundingBox() {
        final double offNW = 7.0 / 16.0;
        final double offSE = 9.0 / 16.0;
        final double onNW = 0.0;
        final double onSE = 1.0;
        double w = offNW;
        double e = offSE;
        double n = offNW;
        double s = offSE;
        try {
            boolean north = this.canConnect(this.north());
            boolean south = this.canConnect(this.south());
            boolean west = this.canConnect(this.west());
            boolean east = this.canConnect(this.east());
            w = west ? onNW : offNW;
            e = east ? onSE : offSE;
            n = north ? onNW : offNW;
            s = south ? onSE : offSE;
        } catch (LevelException ignore) {
            //null sucks
        }
        return new SimpleAxisAlignedBB(getPosition().toDouble().add(w, 0, n), getPosition().toDouble().add(e, 1, s));
    }

    public boolean canConnect(Block block) {
        return block.isSolid() || block.getId() == this.getId();
    }

}
