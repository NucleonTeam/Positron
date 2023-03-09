package cn.nukkit.item;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.projectile.EntityProjectile;
import cn.nukkit.event.entity.ProjectileLaunchEvent;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.DoubleTag;
import cn.nukkit.nbt.tag.FloatTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.network.protocol.LevelSoundEventPacket;

/**
 * @author CreeperFace
 */
public abstract class ProjectileItem extends Item {

    public ProjectileItem(int id, Integer meta, int count, String name) {
        super(id, meta, count, name);
    }

    abstract public String getProjectileEntityType();

    abstract public float getThrowForce();

    public boolean onClickAir(Player player, Vector3 directionVector) {
        CompoundTag nbt = new CompoundTag()
                .putList(new ListTag<DoubleTag>("Pos")
                        .add(new DoubleTag("", player.getPosition().x()))
                        .add(new DoubleTag("", player.getPosition().y() + player.getEyeHeight() - 0.30000000149011612))
                        .add(new DoubleTag("", player.getPosition().z())))
                .putList(new ListTag<DoubleTag>("Motion")
                        .add(new DoubleTag("", directionVector.x))
                        .add(new DoubleTag("", directionVector.y))
                        .add(new DoubleTag("", directionVector.z)))
                .putList(new ListTag<FloatTag>("Rotation")
                        .add(new FloatTag("", (float) player.getYaw()))
                        .add(new FloatTag("", (float) player.getPitch())));

        this.correctNBT(nbt);

        /* TODO: Переделать эту хуйню
        Entity projectile = Entity.createEntity(this.getProjectileEntityType(), player.getWorld().getChunk(player.getChunkX(), player.getChunkZ()), nbt, player);
        if (projectile != null) {

            projectile.setMotion(projectile.getMotion().mul(this.getThrowForce()));

            if (projectile instanceof EntityProjectile) {
                ProjectileLaunchEvent ev = new ProjectileLaunchEvent((EntityProjectile) projectile);

                player.getServer().getPluginManager().callEvent(ev);
                if (ev.isCancelled()) {
                    projectile.kill();
                } else {
                    if (!player.isCreative()) {
                        this.count--;
                    }
                    projectile.spawnToAll();
                    player.getWorld().addLevelSoundEvent(player.getPosition().toFloat(), LevelSoundEventPacket.SOUND_BOW);
                }
            }
        } else {
            return false;
        }
         */
        return true;
    }

    protected void correctNBT(CompoundTag nbt) {

    }
}
