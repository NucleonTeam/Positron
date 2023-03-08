package cn.nukkit.entity.item;

import cn.nukkit.Server;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.entity.EntityDamageEvent.DamageCause;
import cn.nukkit.event.entity.ItemDespawnEvent;
import cn.nukkit.event.entity.ItemSpawnEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.NBTIO;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.network.protocol.AddItemEntityPacket;
import cn.nukkit.network.protocol.DataPacket;
import cn.nukkit.network.protocol.EntityEventPacket;
import ru.mc_positron.entity.EntityDataKeys;
import ru.mc_positron.entity.EntityFlags;
import ru.mc_positron.math.FastMath;

public class EntityItem extends Entity {

    public static final int NETWORK_ID = 64;

    public EntityItem(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    @Override
    public int getNetworkId() {
        return NETWORK_ID;
    }

    protected String owner;
    protected String thrower;

    protected Item item;

    protected int pickupDelay;

    @Override
    public float getWidth() {
        return 0.25f;
    }

    @Override
    public float getLength() {
        return 0.25f;
    }

    @Override
    public float getHeight() {
        return 0.25f;
    }

    @Override
    public float getGravity() {
        return 0.04f;
    }

    @Override
    public float getDrag() {
        return 0.02f;
    }

    @Override
    protected float getBaseOffset() {
        return 0.125f;
    }

    @Override
    public boolean canCollide() {
        return false;
    }

    @Override
    protected void initEntity() {
        super.initEntity();

        this.setMaxHealth(5);
        this.setHealth(getNbt().getShort("Health"));

        if (getNbt().contains("Age")) {
            this.age = getNbt().getShort("Age");
        }

        if (getNbt().contains("PickupDelay")) {
            this.pickupDelay = getNbt().getShort("PickupDelay");
        }

        if (getNbt().contains("Owner")) {
            this.owner = getNbt().getString("Owner");
        }

        if (getNbt().contains("Thrower")) {
            this.thrower = getNbt().getString("Thrower");
        }

        if (!getNbt().contains("Item")) {
            this.remove();
            return;
        }

        this.item = NBTIO.getItemHelper(getNbt().getCompound("Item"));
        this.setDataFlag(EntityDataKeys.FLAGS, EntityFlags.GRAVITY, true);

        int id = this.item.getId();

        this.server.getPluginManager().callEvent(new ItemSpawnEvent(this));
    }

    @Override
    public boolean attack(EntityDamageEvent source) {
        DamageCause cause = source.getCause();
        if ((cause == DamageCause.VOID || cause == DamageCause.CONTACT || cause == DamageCause.FIRE_TICK
                || (cause == DamageCause.ENTITY_EXPLOSION || cause == DamageCause.BLOCK_EXPLOSION) && !this.isInsideOfWater()
                && (this.item == null) && super.attack(source))) {
            if (this.item == null || this.isAlive()) {
                return true;
            }
            int id = this.item.getId();
            CompoundTag nbt = this.item.getNamedTag();
            if (nbt == null) {
                return true;
            }
            ListTag<CompoundTag> items = nbt.getList("Items", CompoundTag.class);
            for (int i = 0; i < items.size(); i++) {
                CompoundTag itemTag = items.get(i);
                Item item = NBTIO.getItemHelper(itemTag);
                if (item.isNull()) {
                    continue;
                }
                world.dropItem(new Vector3(position), item);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onUpdate(int currentTick) {
        if (removed) return false;

        int tickDiff = currentTick - this.lastUpdate;

        if (tickDiff <= 0 && !this.justCreated) {
            return true;
        }

        this.lastUpdate = currentTick;
        
        if (this.age % 60 == 0 && this.onGround && this.getItem() != null && this.isAlive()) {
            if (this.getItem().getCount() < this.getItem().getMaxStackSize()) {
                for (Entity entity: world.getNearbyEntities(getBoundingBox().grow(1, 1, 1), this, false)) {
                    if (entity instanceof EntityItem) {
                        if (!entity.isAlive()) {
                            continue;
                        }
                        Item closeItem = ((EntityItem) entity).getItem();
                        if (!closeItem.equals(getItem(), true, true)) {
                            continue;
                        }
                        if (!entity.isOnGround()) {
                            continue;
                        }
                        int newAmount = this.getItem().getCount() + closeItem.getCount();
                        if (newAmount > this.getItem().getMaxStackSize()) {
                            continue;
                        }
                        entity.remove();
                        this.getItem().setCount(newAmount);
                        EntityEventPacket packet = new EntityEventPacket();
                        packet.eid = getId();
                        packet.data = newAmount;
                        packet.event = EntityEventPacket.MERGE_ITEMS;
                        Server.broadcastPacket(this.getViewers().values(), packet);
                    }
                }
            }
        }

        boolean hasUpdate = this.entityBaseTick(tickDiff);

        if (!this.fireProof && this.isInsideOfFire()) {
            this.kill();
        }

        if (this.isAlive()) {
            if (this.pickupDelay > 0 && this.pickupDelay < 32767) {
                this.pickupDelay -= tickDiff;
                if (this.pickupDelay < 0) {
                    this.pickupDelay = 0;
                }
            }/* else { // Done in Player#checkNearEntities
                for (Entity entity : this.level.getNearbyEntities(this.boundingBox.grow(1, 0.5, 1), this)) {
                    if (entity instanceof Player) {
                        if (((Player) entity).pickupEntity(this, true)) {
                            return true;
                        }
                    }
                }
            }*/

            int bid = world.getBlockIdAt(position.floorX(), FastMath.floorDouble(position.y() + 0.53), position.floorZ());
            if (!isOnGround()) motion = motion.sub(0, getGravity(), 0);
            if (checkObstruction(position)) hasUpdate = true;

            move(motion);

            double friction = 1 - this.getDrag();

            if (this.onGround && (Math.abs(motion.x()) > 0.00001 || Math.abs(motion.z()) > 0.00001)) {
                friction *= world.getBlock(position.sub(0, 1, 1).toInt()).getFrictionFactor();
            }

            motion = motion.mul(friction, 1 - getDrag(), friction);

            if (onGround) motion = motion.mul(1, -0.5, 0);
            updateMovement();

            if (age > 6000) {
                var event = new ItemDespawnEvent(this);
                server.getPluginManager().callEvent(event);

                if (event.isCancelled()) age = 0;
                else {
                    kill();
                    hasUpdate = true;
                }
            }
        }

        return hasUpdate || !onGround || Math.abs(motion.x()) > 0.00001 || Math.abs(motion.y()) > 0.00001 || Math.abs(motion.z()) > 0.00001;
    }

    @Override
    public void saveNBT() {
        super.saveNBT();
        if (item != null) { // Yes, a item can be null... I don't know what causes this, but it can happen.
            getNbt().putCompound("Item", NBTIO.putItemHelper(item, -1));
            getNbt().putShort("Health", (int) getHealth());
            getNbt().putShort("Age", age);
            getNbt().putShort("PickupDelay", pickupDelay);

            if (owner != null) getNbt().putString("Owner", owner);
            if (thrower != null) getNbt().putString("Thrower", thrower);
        }
    }

    @Override
    public String getName() {
        return hasCustomName()? getNameTag() : (item == null ? "" : (item.hasCustomName()? item.getCustomName() : item.getName()));
    }

    public Item getItem() {
        return item;
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return false;
    }

    public int getPickupDelay() {
        return pickupDelay;
    }

    public void setPickupDelay(int pickupDelay) {
        this.pickupDelay = pickupDelay;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getThrower() {
        return thrower;
    }

    public void setThrower(String thrower) {
        this.thrower = thrower;
    }

    @Override
    public DataPacket createAddEntityPacket() {
        var pk = new AddItemEntityPacket();
        pk.entityUniqueId = getId();
        pk.entityRuntimeId = getId();
        pk.position = position.add(0, getBaseOffset(), 0).toFloat();
        pk.speed = motion.toFloat();
        pk.metadata = dataProperties;
        pk.item = getItem();

        return pk;
    }
}
