package cn.nukkit.inventory;

import cn.nukkit.Player;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.entity.EntityHumanType;
import cn.nukkit.network.protocol.ContainerClosePacket;
import cn.nukkit.network.protocol.ContainerOpenPacket;

public class PlayerEnderChestInventory extends BaseInventory {

    public PlayerEnderChestInventory(EntityHumanType player) {
        super(player, InventoryType.ENDER_CHEST);
    }

    @Override
    public EntityHuman getHolder() {
        return (EntityHuman) this.holder;
    }

    @Override
    public void onOpen(Player who) {
        if (who != this.getHolder()) {
            return;
        }
        super.onOpen(who);
        ContainerOpenPacket containerOpenPacket = new ContainerOpenPacket();
        containerOpenPacket.windowId = who.getWindowId(this);
        containerOpenPacket.type = this.getType().getNetworkType();
    }

    @Override
    public void onClose(Player who) {
        ContainerClosePacket containerClosePacket = new ContainerClosePacket();
        containerClosePacket.windowId = who.getWindowId(this);
        containerClosePacket.wasServerInitiated = who.getClosingWindowId() != containerClosePacket.windowId;
        who.dataPacket(containerClosePacket);
        super.onClose(who);


        super.onClose(who);
    }
}
