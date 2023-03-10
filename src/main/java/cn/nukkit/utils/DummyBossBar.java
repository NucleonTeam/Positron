package cn.nukkit.utils;

import cn.nukkit.Player;
import cn.nukkit.network.protocol.*;
import org.spongepowered.math.vector.Vector3f;
import ru.mc_positron.entity.EntityDataKeys;
import ru.mc_positron.entity.attribute.Attribute;
import ru.mc_positron.entity.attribute.Attributes;
import ru.mc_positron.entity.data.EntityMetadata;

import java.util.concurrent.ThreadLocalRandom;

public class DummyBossBar {

    private final Player player;
    private final long bossBarId;

    private String text;
    private float length;
    private BossBarColor color;

    private DummyBossBar(Builder builder) {
        this.player = builder.player;
        this.bossBarId = builder.bossBarId;
        this.text = builder.text;
        this.length = builder.length;
        this.color = builder.color;
    }

    public static class Builder {
        private final Player player;
        private final long bossBarId;

        private String text = "";
        private float length = 100;
        private BossBarColor color = null;

        public Builder(Player player) {
            this.player = player;
            this.bossBarId = 1095216660480L + ThreadLocalRandom.current().nextLong(0, 0x7fffffffL);
        }

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public Builder length(float length) {
            if (length >= 0 && length <= 100) this.length = length;
            return this;
        }

        public Builder color(BossBarColor color) {
            this.color = color;
            return this;
        }

        public DummyBossBar build() {
            return new DummyBossBar(this);
        }
    }

    public Player getPlayer() {
        return player;
    }

    public long getBossBarId() {
        return bossBarId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        if (!this.text.equals(text)) {
            this.text = text;
            this.updateBossEntityNameTag();
            this.sendSetBossBarTitle();
        }
    }

    public float getLength() {
        return length;
    }

    public void setLength(float length) {
        if (this.length != length) {
            this.length = length;
            this.sendAttributes();
            this.sendSetBossBarLength();
        }
    }

    public void setColor(BossBarColor color) {
        if (this.color == null || !this.color.equals(color)) {
            this.color = color;
            this.sendSetBossBarTexture();
        }
    }

    public BossBarColor getColor() {
        return this.color;
    }

    private void createBossEntity() {
        AddEntityPacket pk = new AddEntityPacket();
        pk.type = 43;
        pk.entityUniqueId = bossBarId;
        pk.entityRuntimeId = bossBarId;
        pk.position = new Vector3f(player.getPosition().x(), -10, player.getPosition().z());
        pk.speed = Vector3f.ZERO;
        pk.metadata = new EntityMetadata()
                // Default Metadata tags
                .putLong(EntityDataKeys.FLAGS, 0)
                .putShort(EntityDataKeys.AIR, 400)
                .putShort(EntityDataKeys.MAX_AIR, 400)
                .putLong(EntityDataKeys.LEAD_HOLDER_EID, -1)
                .putString(EntityDataKeys.NAMETAG, text) // Set the entity name
                .putFloat(EntityDataKeys.SCALE, 0); // And make it invisible

        player.dataPacket(pk);
    }

    private void sendAttributes() {
        var pk = new UpdateAttributesPacket();
        pk.entityId = bossBarId;

        var attribute = Attributes.MAX_HEALTH;
        var entry = new Attribute.Entry(attribute.with(1f, 100f, 100f));

        pk.entries = new Attribute.Entry[]{ entry };
        player.dataPacket(pk);
    }

    private void sendShowBossBar() {
        BossEventPacket pkBoss = new BossEventPacket();
        pkBoss.bossEid = bossBarId;
        pkBoss.type = BossEventPacket.TYPE_SHOW;
        pkBoss.title = text;
        pkBoss.healthPercent = this.length / 100;
        player.dataPacket(pkBoss);
    }

    private void sendHideBossBar() {
        BossEventPacket pkBoss = new BossEventPacket();
        pkBoss.bossEid = bossBarId;
        pkBoss.type = BossEventPacket.TYPE_HIDE;
        player.dataPacket(pkBoss);
    }

    private void sendSetBossBarTexture() {
        BossEventPacket pk = new BossEventPacket();
        pk.bossEid = this.bossBarId;
        pk.type = BossEventPacket.TYPE_TEXTURE;
        pk.color = color.ordinal();
        player.dataPacket(pk);
    }

    private void sendSetBossBarTitle() {
        BossEventPacket pkBoss = new BossEventPacket();
        pkBoss.bossEid = bossBarId;
        pkBoss.type = BossEventPacket.TYPE_TITLE;
        pkBoss.title = text;
        pkBoss.healthPercent = this.length / 100;
        player.dataPacket(pkBoss);
    }

    private void sendSetBossBarLength() {
        BossEventPacket pkBoss = new BossEventPacket();
        pkBoss.bossEid = bossBarId;
        pkBoss.type = BossEventPacket.TYPE_HEALTH_PERCENT;
        pkBoss.healthPercent = this.length / 100;
        player.dataPacket(pkBoss);
    }

    /**
     * Don't let the entity go too far from the player, or the BossBar will disappear.
     * Update boss entity's position when teleport and each 5s.
     */
    public void updateBossEntityPosition() {
        MoveEntityAbsolutePacket pk = new MoveEntityAbsolutePacket();
        pk.eid = this.bossBarId;
        pk.position = new Vector3f(player.getPosition().x(), -10, player.getPosition().z());
        pk.headYaw = 0;
        pk.yaw = 0;
        pk.pitch = 0;
        player.dataPacket(pk);
    }

    private void updateBossEntityNameTag() {
        var pk = new SetEntityDataPacket();
        pk.eid = this.bossBarId;
        pk.metadata = new EntityMetadata().putString(EntityDataKeys.NAMETAG, this.text);
        player.dataPacket(pk);
    }

    private void removeBossEntity() {
        RemoveEntityPacket pkRemove = new RemoveEntityPacket();
        pkRemove.eid = bossBarId;
        player.dataPacket(pkRemove);
    }

    public void create() {
        createBossEntity();
        sendAttributes();
        sendShowBossBar();
        sendSetBossBarLength();
        if (color != null) this.sendSetBossBarTexture();
    }

    /**
     * Once the player has teleported, resend Show BossBar
     */
    public void reshow() {
        updateBossEntityPosition();
        sendShowBossBar();
        sendSetBossBarLength();
    }

    public void destroy() {
        sendHideBossBar();
        removeBossEntity();
    }

}
