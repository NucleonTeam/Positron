package cn.nukkit.event.player;

import cn.nukkit.Player;
import cn.nukkit.event.Cancellable;
import cn.nukkit.event.HandlerList;

import java.util.HashSet;
import java.util.Set;

public class PlayerChatEvent extends PlayerMessageEvent implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlers() {
        return handlers;
    }

    protected String format;

    protected Set<Player> recipients = new HashSet<>();

    public PlayerChatEvent(Player player, String message) {
        this(player, message, "chat.type.text", null);
    }

    public PlayerChatEvent(Player player, String message, String format, Set<Player> recipients) {
        this.player = player;
        this.message = message;

        this.format = format;

        if (recipients == null) return;

        this.recipients = recipients;
    }

    /**
     * Changes the player that is sending the message
     *
     * @param player messenger
     */
    public void setPlayer(Player player) {
        this.player = player;
    }

    public String getFormat() {
        return this.format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Set<Player> getRecipients() {
        return this.recipients;
    }

    public void setRecipients(Set<Player> recipients) {
        this.recipients = recipients;
    }
}
