package cn.nukkit.block;

import cn.nukkit.utils.BlockColor;

public abstract class BlockTransparent extends Block {

    @Override
    public boolean isTransparent() {
        return true;
    }

    @Override
    public BlockColor getColor() {
        return BlockColor.TRANSPARENT_BLOCK_COLOR;
    }

}
