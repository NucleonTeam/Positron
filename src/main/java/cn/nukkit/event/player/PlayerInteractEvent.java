package cn.nukkit.event.player;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.event.Cancellable;
import cn.nukkit.event.HandlerList;
import cn.nukkit.item.Item;
import cn.nukkit.level.Position;
import org.spongepowered.math.vector.Vector3d;
import ru.mc_positron.math.BlockFace;
import cn.nukkit.math.Vector3;

public class PlayerInteractEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlers() {
        return handlers;
    }

    protected Block blockTouched;
    protected Vector3d touchVector;
    protected final BlockFace blockFace;
    protected final Item item;
    protected final Action action;

    public PlayerInteractEvent(Player player, Item item, Vector3d block, BlockFace face) {
        this(player, item, block, face, Action.RIGHT_CLICK_BLOCK);
    }

    public PlayerInteractEvent(Player player, Item item, Block block, BlockFace face, Action action)  {
        this(player, item, block.getPosition().toDouble(), face, Action.RIGHT_CLICK_BLOCK);

        this.blockTouched = block;
        this.touchVector = Vector3d.ZERO;
    }

    public PlayerInteractEvent(Player player, Item item, Vector3d block, BlockFace face, Action action) {
        this.touchVector = block;
        this.blockTouched = Block.get(Block.AIR, 0, block.toInt(), player.getWorld());

        this.player = player;
        this.item = item;
        this.blockFace = face;
        this.action = action;
    }

    public Action getAction() {
        return action;
    }

    public Item getItem() {
        return item;
    }

    public Block getBlock() {
        return blockTouched;
    }

    public Vector3d getTouchVector() {
        return touchVector;
    }

    public BlockFace getFace() {
        return blockFace;
    }

    public enum Action {
        LEFT_CLICK_BLOCK,
        RIGHT_CLICK_BLOCK,
        LEFT_CLICK_AIR,
        RIGHT_CLICK_AIR,
        PHYSICAL
    }
}
