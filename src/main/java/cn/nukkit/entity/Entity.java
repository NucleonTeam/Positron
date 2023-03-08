package cn.nukkit.entity;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.*;
import cn.nukkit.event.entity.*;
import cn.nukkit.event.entity.EntityDamageEvent.DamageCause;
import cn.nukkit.item.Item;
import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.level.*;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.AxisAlignedBB;
import cn.nukkit.math.SimpleAxisAlignedBB;
import cn.nukkit.metadata.MetadataValue;
import cn.nukkit.metadata.Metadatable;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.DoubleTag;
import cn.nukkit.nbt.tag.FloatTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.network.protocol.*;
import cn.nukkit.network.protocol.types.EntityLink;
import cn.nukkit.plugin.Plugin;
import cn.nukkit.potion.Effect;
import cn.nukkit.utils.ChunkException;
import cn.nukkit.utils.MainLogger;
import com.google.common.collect.Iterables;
import lombok.Getter;
import org.spongepowered.math.vector.Vector2d;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3f;
import org.spongepowered.math.vector.Vector3i;
import ru.mc_positron.entity.EntityDataKeys;
import ru.mc_positron.entity.EntityFlags;
import ru.mc_positron.entity.attribute.Attributes;
import ru.mc_positron.entity.data.*;
import ru.mc_positron.math.BlockFace;
import ru.mc_positron.math.FastMath;
import ru.mc_positron.math.Point;
import ru.mc_positron.registry.Entities;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static cn.nukkit.network.protocol.SetEntityLinkPacket.*;

public abstract class Entity implements Metadatable {

    public static final int NETWORK_ID = -1;

    public abstract int getNetworkId();

    @Getter private final long id = Entities.getFreeEntityId();

    private static final Map<String, Class<? extends Entity>> knownEntities = new HashMap<>();
    private static final Map<String, String> shortNames = new HashMap<>();

    protected final Map<Integer, Player> hasSpawned = new HashMap<>();
    protected final Map<Integer, Effect> effects = new ConcurrentHashMap<>();
    public final List<Entity> passengers = new ArrayList<>();
    public Entity riding = null;
    public FullChunk chunk;
    protected EntityDamageEvent lastDamageCause = null;

    protected final EntityMetadata dataProperties = new EntityMetadata()
            .putLong(EntityDataKeys.FLAGS, 0)
            .putByte(EntityDataKeys.COLOR, 0)
            .putShort(EntityDataKeys.AIR, 400)
            .putShort(EntityDataKeys.MAX_AIR, 400)
            .putString(EntityDataKeys.NAMETAG, "")
            .putLong(EntityDataKeys.LEAD_HOLDER_EID, -1)
            .putFloat(EntityDataKeys.SCALE, 1f);

    public List<Block> blocksAround = new ArrayList<>();
    public List<Block> collisionBlocks = new ArrayList<>();

    @Getter protected Level world;
    @Getter protected Vector3d position = Vector3d.ZERO;
    protected Vector3d lastPosition = Vector3d.ZERO;
    @Getter protected Vector3d motion = Vector3d.ZERO;
    protected Vector3d lastMotion = Vector3d.ZERO;
    @Getter protected double pitch;
    @Getter protected double yaw;
    @Getter protected double headYaw;
    protected double lastPitch;
    protected double lastYaw;
    protected double lastHeadYaw;
    protected boolean firstMove = true;
    @Getter protected CompoundTag nbt = new CompoundTag();
    @Getter protected boolean removed = false;


    public double entityCollisionReduction = 0; // Higher than 0.9 will result a fast collisions
    public AxisAlignedBB boundingBox;
    public boolean onGround;

    public int deadTicks = 0;
    protected int age = 0;

    protected float health = 20;
    private int maxHealth = 20;

    protected float absorption = 0;

    protected float ySize = 0;
    public boolean keepMovement = false;

    public float fallDistance = 0;
    public int ticksLived = 0;
    public int lastUpdate;
    public int fireTicks = 0;

    public float scale = 1;

    public boolean isCollided = false;
    public boolean isCollidedHorizontally = false;
    public boolean isCollidedVertically = false;

    public int noDamageTicks;
    public boolean justCreated;
    public boolean fireProof;
    public boolean invulnerable;

    protected Server server;

    public double highestPosition;

    protected boolean isPlayer = this instanceof Player;

    private volatile boolean initialized;

    public float getHeight() {
        return 0;
    }

    public float getEyeHeight() {
        return this.getHeight() / 2 + 0.1f;
    }

    public float getWidth() {
        return 0;
    }

    public float getLength() {
        return 0;
    }

    protected double getStepHeight() {
        return 0;
    }

    public boolean canCollide() {
        return true;
    }

    protected float getGravity() {
        return 0;
    }

    protected float getDrag() {
        return 0;
    }

    protected float getBaseOffset() {
        return 0;
    }

    public Entity(FullChunk chunk, CompoundTag nbt) {
        if (this instanceof Player) {
            return;
        }

        this.init(chunk, nbt);
    }

    protected void initEntity() {
        if (this.nbt.contains("ActiveEffects")) {
            ListTag<CompoundTag> effects = this.nbt.getList("ActiveEffects", CompoundTag.class);
            for (CompoundTag e : effects.getAll()) {
                Effect effect = Effect.getEffect(e.getByte("Id"));
                if (effect == null) {
                    continue;
                }

                effect.setAmplifier(e.getByte("Amplifier")).setDuration(e.getInt("Duration")).setVisible(e.getBoolean("ShowParticles"));

                this.addEffect(effect);
            }
        }

        if (this.nbt.contains("CustomName")) {
            this.setNameTag(this.nbt.getString("CustomName"));
            if (this.nbt.contains("CustomNameVisible")) {
                this.setNameTagVisible(this.nbt.getBoolean("CustomNameVisible"));
            }
            if(this.nbt.contains("CustomNameAlwaysVisible")){
                this.setNameTagAlwaysVisible(this.nbt.getBoolean("CustomNameAlwaysVisible"));
            }
        }

        this.setDataFlag(EntityDataKeys.FLAGS, EntityFlags.HAS_COLLISION, true);
        this.dataProperties.putFloat(EntityDataKeys.BOUNDING_BOX_HEIGHT, this.getHeight());
        this.dataProperties.putFloat(EntityDataKeys.BOUNDING_BOX_WIDTH, this.getWidth());
        this.dataProperties.putInt(EntityDataKeys.HEALTH, (int) this.getHealth());

        this.scheduleUpdate();
    }

    protected final void init(FullChunk chunk, CompoundTag nbt) {
        if ((chunk == null || chunk.getProvider() == null)) {
            throw new ChunkException("Invalid garbage Chunk given to Entity");
        }

        if (this.initialized) {
            // We've already initialized this entity
            return;
        }
        this.initialized = true;
        this.justCreated = true;
        this.nbt = nbt;
        this.chunk = chunk;
        this.world = chunk.getProvider().getLevel();
        this.server = chunk.getProvider().getLevel().getServer();

        this.boundingBox = new SimpleAxisAlignedBB(0, 0, 0, 0, 0, 0);

        ListTag<DoubleTag> posList = this.nbt.getList("Pos", DoubleTag.class);
        ListTag<FloatTag> rotationList = this.nbt.getList("Rotation", FloatTag.class);
        ListTag<DoubleTag> motionList = this.nbt.getList("Motion", DoubleTag.class);
        this.setPositionAndRotation(
                new Vector3d(
                        posList.get(0).data,
                        posList.get(1).data,
                        posList.get(2).data
                ),
                rotationList.get(0).data,
                rotationList.get(1).data
        );

        this.setMotion(new Vector3d(
                motionList.get(0).data,
                motionList.get(1).data,
                motionList.get(2).data)
        );

        if (!this.nbt.contains("FallDistance")) {
            this.nbt.putFloat("FallDistance", 0);
        }
        this.fallDistance = this.nbt.getFloat("FallDistance");
        this.highestPosition = this.position.y() + this.nbt.getFloat("FallDistance");

        if (!this.nbt.contains("Fire") || this.nbt.getShort("Fire") > 32767) {
            this.nbt.putShort("Fire", 0);
        }
        this.fireTicks = this.nbt.getShort("Fire");

        if (!this.nbt.contains("Air")) {
            this.nbt.putShort("Air", 300);
        }
        this.setDataProperty(new ShortEntityData(EntityDataKeys.AIR, this.nbt.getShort("Air")), false);

        if (!this.nbt.contains("OnGround")) {
            this.nbt.putBoolean("OnGround", false);
        }
        this.onGround = this.nbt.getBoolean("OnGround");

        if (!this.nbt.contains("Invulnerable")) {
            this.nbt.putBoolean("Invulnerable", false);
        }
        this.invulnerable = this.nbt.getBoolean("Invulnerable");

        if (!this.nbt.contains("Scale")) {
            this.nbt.putFloat("Scale", 1);
        }
        this.scale = this.nbt.getFloat("Scale");
        this.setDataProperty(new FloatEntityData(EntityDataKeys.SCALE, scale), false);

        this.chunk.addEntity(this);
        this.world.addEntity(this);

        this.initEntity();

        this.lastUpdate = this.server.getTick();
        this.server.getPluginManager().callEvent(new EntitySpawnEvent(this));

        this.scheduleUpdate();
    }

    public boolean hasCustomName() {
        return !this.getNameTag().isEmpty();
    }

    public String getNameTag() {
        return this.getDataProperties().getString(EntityDataKeys.NAMETAG);
    }

    public boolean isNameTagVisible() {
        return this.getDataFlag(EntityDataKeys.FLAGS, EntityFlags.CAN_SHOW_NAMETAG);
    }

    public boolean isNameTagAlwaysVisible() {
        return this.getDataProperties().getByte(EntityDataKeys.ALWAYS_SHOW_NAMETAG) == 1;
    }

    public void setNameTag(String name) {
        this.setDataProperty(new StringEntityData(EntityDataKeys.NAMETAG, name));
    }

    public void setNameTagVisible() {
        this.setNameTagVisible(true);
    }

    public void setNameTagVisible(boolean value) {
        this.setDataFlag(EntityDataKeys.FLAGS, EntityFlags.CAN_SHOW_NAMETAG, value);
    }

    public void setNameTagAlwaysVisible() {
        this.setNameTagAlwaysVisible(true);
    }

    public void setNameTagAlwaysVisible(boolean value) {
        this.setDataProperty(new ByteEntityData(EntityDataKeys.ALWAYS_SHOW_NAMETAG, value ? 1 : 0));
    }

    public void setScoreTag(String score) {
        this.setDataProperty(new StringEntityData(EntityDataKeys.SCORE_TAG, score));
    }

    public String getScoreTag() {
        return this.getDataProperties().getString(EntityDataKeys.SCORE_TAG);
    }

    public boolean isSneaking() {
        return this.getDataFlag(EntityDataKeys.FLAGS, EntityFlags.SNEAKING);
    }

    public void setSneaking() {
        this.setSneaking(true);
    }

    public void setSneaking(boolean value) {
        this.setDataFlag(EntityDataKeys.FLAGS, EntityFlags.SNEAKING, value);
    }

    public boolean isSwimming() {
        return this.getDataFlag(EntityDataKeys.FLAGS, EntityFlags.SWIMMING);
    }

    public void setSwimming() {
        this.setSwimming(true);
    }

    public void setSwimming(boolean value) {
        this.setDataFlag(EntityDataKeys.FLAGS, EntityFlags.SWIMMING, value);
    }

    public boolean isSprinting() {
        return this.getDataFlag(EntityDataKeys.FLAGS, EntityFlags.SPRINTING);
    }

    public void setSprinting() {
        this.setSprinting(true);
    }

    public void setSprinting(boolean value) {
        this.setDataFlag(EntityDataKeys.FLAGS, EntityFlags.SPRINTING, value);
    }

    public boolean isGliding() {
        return this.getDataFlag(EntityDataKeys.FLAGS, EntityFlags.GLIDING);
    }

    public void setGliding() {
        this.setGliding(true);
    }

    public void setGliding(boolean value) {
        this.setDataFlag(EntityDataKeys.FLAGS, EntityFlags.GLIDING, value);
    }

    public boolean isImmobile() {
        return this.getDataFlag(EntityDataKeys.FLAGS, EntityFlags.IMMOBILE);
    }

    public void setImmobile() {
        this.setImmobile(true);
    }

    public void setImmobile(boolean value) {
        this.setDataFlag(EntityDataKeys.FLAGS, EntityFlags.IMMOBILE, value);
    }

    public boolean canClimb() {
        return this.getDataFlag(EntityDataKeys.FLAGS, EntityFlags.CAN_CLIMB);
    }

    public void setCanClimb() {
        this.setCanClimb(true);
    }

    public void setCanClimb(boolean value) {
        this.setDataFlag(EntityDataKeys.FLAGS, EntityFlags.CAN_CLIMB, value);
    }

    public boolean canClimbWalls() {
        return this.getDataFlag(EntityDataKeys.FLAGS, EntityFlags.WALLCLIMBING);
    }

    public void setCanClimbWalls() {
        this.setCanClimbWalls(true);
    }

    public void setCanClimbWalls(boolean value) {
        this.setDataFlag(EntityDataKeys.FLAGS, EntityFlags.WALLCLIMBING, value);
    }

    public void setScale(float scale) {
        this.scale = scale;
        this.setDataProperty(new FloatEntityData(EntityDataKeys.SCALE, this.scale));
        this.recalculateBoundingBox();
    }

    public float getScale() {
        return this.scale;
    }

    public List<Entity> getPassengers() {
        return passengers;
    }

    public Entity getPassenger() {
        return Iterables.getFirst(this.passengers, null);
    }

    public boolean isPassenger(Entity entity) {
        return this.passengers.contains(entity);
    }

    public boolean isControlling(Entity entity) {
        return this.passengers.indexOf(entity) == 0;
    }

    public boolean hasControllingPassenger() {
        return !this.passengers.isEmpty() && isControlling(this.passengers.get(0));
    }

    public Entity getRiding() {
        return riding;
    }

    public Map<Integer, Effect> getEffects() {
        return effects;
    }

    public void removeAllEffects() {
        for (Effect effect : this.effects.values()) {
            this.removeEffect(effect.getId());
        }
    }

    public void removeEffect(int effectId) {
        if (this.effects.containsKey(effectId)) {
            Effect effect = this.effects.get(effectId);
            this.effects.remove(effectId);
            effect.remove(this);

            this.recalculateEffectColor();
        }
    }

    public Effect getEffect(int effectId) {
        return this.effects.getOrDefault(effectId, null);
    }

    public boolean hasEffect(int effectId) {
        return this.effects.containsKey(effectId);
    }

    public void addEffect(Effect effect) {
        if (effect == null) {
            return; //here add null means add nothing
        }

        effect.add(this);

        this.effects.put(effect.getId(), effect);

        this.recalculateEffectColor();

        if (effect.getId() == Effect.HEALTH_BOOST) {
            this.setHealth(this.getHealth() + 4 * (effect.getAmplifier() + 1));
        }

    }

    public void recalculateBoundingBox() {
        this.recalculateBoundingBox(true);
    }

    public void recalculateBoundingBox(boolean send) {
        float height = this.getHeight() * this.scale;
        double radius = (this.getWidth() * this.scale) / 2d;
        this.boundingBox.setBounds(position.sub(radius, radius, radius), position.add(radius, height, radius));

        FloatEntityData bbH = new FloatEntityData(EntityDataKeys.BOUNDING_BOX_HEIGHT, this.getHeight());
        FloatEntityData bbW = new FloatEntityData(EntityDataKeys.BOUNDING_BOX_WIDTH, this.getWidth());
        this.dataProperties.put(bbH);
        this.dataProperties.put(bbW);
        if (send) {
            sendData(this.hasSpawned.values().toArray(new Player[0]), new EntityMetadata().put(bbH).put(bbW));
        }
    }

    protected void recalculateEffectColor() {
        int[] color = new int[3];
        int count = 0;
        boolean ambient = true;
        for (Effect effect : this.effects.values()) {
            if (effect.isVisible()) {
                int[] c = effect.getColor();
                color[0] += c[0] * (effect.getAmplifier() + 1);
                color[1] += c[1] * (effect.getAmplifier() + 1);
                color[2] += c[2] * (effect.getAmplifier() + 1);
                count += effect.getAmplifier() + 1;
                if (!effect.isAmbient()) {
                    ambient = false;
                }
            }
        }

        if (count > 0) {
            int r = (color[0] / count) & 0xff;
            int g = (color[1] / count) & 0xff;
            int b = (color[2] / count) & 0xff;

            this.setDataProperty(new IntEntityData(EntityDataKeys.POTION_COLOR, (r << 16) + (g << 8) + b));
            this.setDataProperty(new ByteEntityData(EntityDataKeys.POTION_AMBIENT, ambient ? 1 : 0));
        } else {
            this.setDataProperty(new IntEntityData(EntityDataKeys.POTION_COLOR, 0));
            this.setDataProperty(new ByteEntityData(EntityDataKeys.POTION_AMBIENT, 0));
        }
    }

    public static Entity createEntity(String name, FullChunk chunk, CompoundTag nbt, Object... args) {
        Entity entity = null;

        if (knownEntities.containsKey(name)) {
            Class<? extends Entity> clazz = knownEntities.get(name);

            if (clazz == null) {
                return null;
            }

            for (Constructor constructor : clazz.getConstructors()) {
                if (entity != null) {
                    break;
                }

                if (constructor.getParameterCount() != (args == null ? 2 : args.length + 2)) {
                    continue;
                }

                try {
                    if (args == null || args.length == 0) {
                        entity = (Entity) constructor.newInstance(chunk, nbt);
                    } else {
                        Object[] objects = new Object[args.length + 2];

                        objects[0] = chunk;
                        objects[1] = nbt;
                        System.arraycopy(args, 0, objects, 2, args.length);
                        entity = (Entity) constructor.newInstance(objects);

                    }
                } catch (Exception e) {
                    MainLogger.getLogger().logException(e);
                }

            }
        }

        return entity;
    }

    public static boolean registerEntity(String name, Class<? extends Entity> clazz) {
        return registerEntity(name, clazz, false);
    }

    public static boolean registerEntity(String name, Class<? extends Entity> clazz, boolean force) {
        if (clazz == null) {
            return false;
        }
        try {
            int networkId = clazz.getField("NETWORK_ID").getInt(null);
            knownEntities.put(String.valueOf(networkId), clazz);
        } catch (Exception e) {
            if (!force) {
                return false;
            }
        }

        knownEntities.put(name, clazz);
        shortNames.put(clazz.getSimpleName(), name);
        return true;
    }

    public static CompoundTag getDefaultNBT(Vector3d pos) {
        return getDefaultNBT(pos, null);
    }

    public static CompoundTag getDefaultNBT(Vector3d pos, Vector3d motion) {
        return getDefaultNBT(pos, motion, 0, 0);
    }

    public static CompoundTag getDefaultNBT(Vector3d pos, Vector3d motion, float yaw, float pitch) {
        return new CompoundTag()
                .putList(new ListTag<DoubleTag>("Pos")
                        .add(new DoubleTag("", pos.x()))
                        .add(new DoubleTag("", pos.y()))
                        .add(new DoubleTag("", pos.z())))
                .putList(new ListTag<DoubleTag>("Motion")
                        .add(new DoubleTag("", motion != null ? motion.x() : 0))
                        .add(new DoubleTag("", motion != null ? motion.y() : 0))
                        .add(new DoubleTag("", motion != null ? motion.z() : 0)))
                .putList(new ListTag<FloatTag>("Rotation")
                        .add(new FloatTag("", yaw))
                        .add(new FloatTag("", pitch)));
    }

    public void saveNBT() {
        if (!(this instanceof Player)) {
            this.nbt.putString("id", this.getSaveId());
            if (!this.getNameTag().equals("")) {
                this.nbt.putString("CustomName", this.getNameTag());
                this.nbt.putBoolean("CustomNameVisible", this.isNameTagVisible());
                this.nbt.putBoolean("CustomNameAlwaysVisible", this.isNameTagAlwaysVisible());
            } else {
                this.nbt.remove("CustomName");
                this.nbt.remove("CustomNameVisible");
                this.nbt.remove("CustomNameAlwaysVisible");
            }
        }

        this.nbt.putList(new ListTag<DoubleTag>("Pos")
                .add(new DoubleTag("0", position.x()))
                .add(new DoubleTag("1", position.y()))
                .add(new DoubleTag("2", position.z()))
        );

        this.nbt.putList(new ListTag<DoubleTag>("Motion")
                .add(new DoubleTag("0", motion.x()))
                .add(new DoubleTag("1", motion.y()))
                .add(new DoubleTag("2", motion.z()))
        );

        this.nbt.putList(new ListTag<FloatTag>("Rotation")
                .add(new FloatTag("0", (float) this.yaw))
                .add(new FloatTag("1", (float) this.pitch))
        );

        this.nbt.putFloat("FallDistance", this.fallDistance);
        this.nbt.putShort("Fire", this.fireTicks);
        this.nbt.putShort("Air", this.getDataProperties().getShort(EntityDataKeys.AIR));
        this.nbt.putBoolean("OnGround", this.onGround);
        this.nbt.putBoolean("Invulnerable", this.invulnerable);
        this.nbt.putFloat("Scale", this.scale);

        if (!this.effects.isEmpty()) {
            ListTag<CompoundTag> list = new ListTag<>("ActiveEffects");
            for (Effect effect : this.effects.values()) {
                list.add(new CompoundTag(String.valueOf(effect.getId()))
                        .putByte("Id", effect.getId())
                        .putByte("Amplifier", effect.getAmplifier())
                        .putInt("Duration", effect.getDuration())
                        .putBoolean("Ambient", false)
                        .putBoolean("ShowParticles", effect.isVisible())
                );
            }

            this.nbt.putList(list);
        } else {
            this.nbt.remove("ActiveEffects");
        }
    }

    public String getName() {
        if (this.hasCustomName()) {
            return this.getNameTag();
        } else {
            return this.getSaveId();
        }
    }

    public final String getSaveId() {
        return shortNames.getOrDefault(this.getClass().getSimpleName(), "");
    }

    public void spawnTo(Player player) {
        if (!this.hasSpawned.containsKey(player.getLoaderId()) && this.chunk != null && player.usedChunks.containsKey(Level.chunkHash(this.chunk.getX(), this.chunk.getZ()))) {
            this.hasSpawned.put(player.getLoaderId(), player);
            player.dataPacket(createAddEntityPacket());
        }

        if (this.riding != null) {
            this.riding.spawnTo(player);

            SetEntityLinkPacket pkk = new SetEntityLinkPacket();
            pkk.vehicleUniqueId = this.riding.getId();
            pkk.riderUniqueId = this.getId();
            pkk.type = 1;
            pkk.immediate = 1;

            player.dataPacket(pkk);
        }
    }

    protected DataPacket createAddEntityPacket() {
        var pk = new AddEntityPacket();

        pk.type = getNetworkId();
        pk.entityUniqueId = getId();
        pk.entityRuntimeId = getId();
        pk.yaw = (float) yaw;
        pk.headYaw = (float) yaw;
        pk.pitch = (float) pitch;
        pk.position = position.toFloat().add(0, getBaseOffset(), 0);
        pk.speed = motion.toFloat();
        pk.metadata = dataProperties;

        pk.links = new EntityLink[passengers.size()];
        for (int i = 0; i < pk.links.length; i++) {
            pk.links[i] = new EntityLink(getId(), passengers.get(i).getId(), i == 0 ? EntityLink.TYPE_RIDER : TYPE_PASSENGER, false, false);
        }

        return pk;
    }

    public Map<Integer, Player> getViewers() {
        return hasSpawned;
    }

    public void sendPotionEffects(Player player) {
        for (Effect effect : this.effects.values()) {
            MobEffectPacket pk = new MobEffectPacket();
            pk.eid = this.getId();
            pk.effectId = effect.getId();
            pk.amplifier = effect.getAmplifier();
            pk.particles = effect.isVisible();
            pk.duration = effect.getDuration();
            pk.eventId = MobEffectPacket.EVENT_ADD;

            player.dataPacket(pk);
        }
    }

    public void sendData(Player player) {
        var pk = new SetEntityDataPacket();
        pk.eid = this.getId();
        pk.metadata = dataProperties;

        player.dataPacket(pk);
    }

    public void sendData(Player[] players, EntityMetadata data) {
        var pk = new SetEntityDataPacket();
        pk.eid = this.getId();
        pk.metadata = data == null ? this.dataProperties : data;

        for (Player player : players) {
            if (player == this) {
                continue;
            }
            player.dataPacket(pk.clone());
        }
        if (this instanceof Player) {
            ((Player) this).dataPacket(pk);
        }
    }

    public void despawnFrom(Player player) {
        if (this.hasSpawned.containsKey(player.getLoaderId())) {
            RemoveEntityPacket pk = new RemoveEntityPacket();
            pk.eid = this.getId();
            player.dataPacket(pk);
            this.hasSpawned.remove(player.getLoaderId());
        }
    }

    public boolean attack(EntityDamageEvent source) {
        if (hasEffect(Effect.FIRE_RESISTANCE)
                && (source.getCause() == DamageCause.FIRE
                || source.getCause() == DamageCause.FIRE_TICK
                || source.getCause() == DamageCause.LAVA)) {
            return false;
        }

        getServer().getPluginManager().callEvent(source);
        if (source.isCancelled()) {
            return false;
        }

        // Make fire aspect to set the target in fire before dealing any damage so the target is in fire on death even if killed by the first hit
        if (source instanceof EntityDamageByEntityEvent) {
            Enchantment[] enchantments = ((EntityDamageByEntityEvent) source).getWeaponEnchantments();
            if (enchantments != null) {
                for (Enchantment enchantment : enchantments) {
                    enchantment.doAttack(((EntityDamageByEntityEvent) source).getDamager(), this);
                }
            }
        }

        if (this.absorption > 0) {  // Damage Absorption
            this.setAbsorption(Math.max(0, this.getAbsorption() + source.getDamage(EntityDamageEvent.DamageModifier.ABSORPTION)));
        }
        setLastDamageCause(source);

        setHealth(getHealth() - source.getFinalDamage());
        return true;
    }

    public boolean attack(float damage) {
        return this.attack(new EntityDamageEvent(this, DamageCause.CUSTOM, damage));
    }

    public void heal(EntityRegainHealthEvent source) {
        this.server.getPluginManager().callEvent(source);
        if (source.isCancelled()) {
            return;
        }
        this.setHealth(this.getHealth() + source.getAmount());
    }

    public void heal(float amount) {
        this.heal(new EntityRegainHealthEvent(this, amount, EntityRegainHealthEvent.CAUSE_REGEN));
    }

    public float getHealth() {
        return health;
    }

    public boolean isAlive() {
        return this.health > 0;
    }

    public void setHealth(float health) {
        if (this.health == health) {
            return;
        }

        if (health < 1) {
            if (this.isAlive()) {
                this.kill();
            }
        } else if (health <= this.getMaxHealth() || health < this.health) {
            this.health = health;
        } else {
            this.health = this.getMaxHealth();
        }

        setDataProperty(new IntEntityData(EntityDataKeys.HEALTH, (int) this.health));
    }

    public void setLastDamageCause(EntityDamageEvent type) {
        this.lastDamageCause = type;
    }

    public EntityDamageEvent getLastDamageCause() {
        return lastDamageCause;
    }

    public int getMaxHealth() {
        return maxHealth + (this.hasEffect(Effect.HEALTH_BOOST) ? 4 * (this.getEffect(Effect.HEALTH_BOOST).getAmplifier() + 1) : 0);
    }

    public void setMaxHealth(int maxHealth) {
        this.maxHealth = maxHealth;
    }

    public boolean canCollideWith(Entity entity) {
        return !this.justCreated && this != entity;
    }

    protected boolean checkObstruction(Vector3d position) {
        if (world.getCollisionCubes(this, this.getBoundingBox(), false).length == 0) {
            return false;
        }

        int i = position.floorX();
        int j = position.floorY();
        int k = position.floorZ();

        double diffX = position.x() - i;
        double diffY = position.y() - j;
        double diffZ = position.z() - k;

        if (!Block.transparent[world.getBlockIdAt(i, j, k)]) {
            boolean flag = Block.transparent[world.getBlockIdAt(i - 1, j, k)];
            boolean flag1 = Block.transparent[world.getBlockIdAt(i + 1, j, k)];
            boolean flag2 = Block.transparent[world.getBlockIdAt(i, j - 1, k)];
            boolean flag3 = Block.transparent[world.getBlockIdAt(i, j + 1, k)];
            boolean flag4 = Block.transparent[world.getBlockIdAt(i, j, k - 1)];
            boolean flag5 = Block.transparent[world.getBlockIdAt(i, j, k + 1)];

            int direction = -1;
            double limit = 9999;

            if (flag) {
                limit = diffX;
                direction = 0;
            }

            if (flag1 && 1 - diffX < limit) {
                limit = 1 - diffX;
                direction = 1;
            }

            if (flag2 && diffY < limit) {
                limit = diffY;
                direction = 2;
            }

            if (flag3 && 1 - diffY < limit) {
                limit = 1 - diffY;
                direction = 3;
            }

            if (flag4 && diffZ < limit) {
                limit = diffZ;
                direction = 4;
            }

            if (flag5 && 1 - diffZ < limit) {
                direction = 5;
            }

            double force = new Random().nextDouble() * 0.2 + 0.1;

            if (direction == 0) {
                motion = new Vector3d(-force, motion.y(), motion.z());
                return true;
            }

            if (direction == 1) {
                motion = new Vector3d(force, motion.y(), motion.z());
                return true;
            }

            if (direction == 2) {
                motion = new Vector3d(motion.x(), -force, motion.z());
                return true;
            }

            if (direction == 3) {
                motion = new Vector3d(motion.x(), force, motion.z());
                return true;
            }

            if (direction == 4) {
                motion = new Vector3d(motion.x(), motion.y(), -force);
                return true;
            }

            if (direction == 5) {
                motion = new Vector3d(motion.x(), motion.y(), force);
                return true;
            }
        }

        return false;
    }

    public boolean entityBaseTick(int tickDiff) {
        if (!this.isPlayer) {
            this.blocksAround = null;
            this.collisionBlocks = null;
        }
        this.justCreated = false;

        if (!this.isAlive()) {
            this.removeAllEffects();
            this.despawnFromAll();
            if (!this.isPlayer) {
                this.remove();
            }
            return false;
        }

        updatePassengers();

        if (!this.effects.isEmpty()) {
            for (Effect effect : this.effects.values()) {
                if (effect.canTick()) {
                    effect.applyEffect(this);
                }
                effect.setDuration(effect.getDuration() - tickDiff);

                if (effect.getDuration() <= 0) {
                    this.removeEffect(effect.getId());
                }
            }
        }

        boolean hasUpdate = false;

        this.checkBlockCollision();

        if (position.y() <= -16 && this.isAlive()) {
            if (this instanceof Player) {
                Player player = (Player) this;
                if (!player.isCreative()) this.attack(new EntityDamageEvent(this, DamageCause.VOID, 10));
            } else {
                this.attack(new EntityDamageEvent(this, DamageCause.VOID, 10));
                hasUpdate = true;
            }
        }

        if (this.fireTicks > 0) {
            if (this.fireProof) {
                this.fireTicks -= 4 * tickDiff;
                if (this.fireTicks < 0) {
                    this.fireTicks = 0;
                }
            } else {
                if (!this.hasEffect(Effect.FIRE_RESISTANCE) && ((this.fireTicks % 20) == 0 || tickDiff > 20)) {
                    this.attack(new EntityDamageEvent(this, DamageCause.FIRE_TICK, 1));
                }
                this.fireTicks -= tickDiff;
            }
            if (this.fireTicks <= 0) {
                this.extinguish();
            } else if (!this.fireProof && (!(this instanceof Player) || !((Player) this).isSpectator())) {
                this.setDataFlag(EntityDataKeys.FLAGS, EntityFlags.ONFIRE, true);
                hasUpdate = true;
            }
        }

        if (this.noDamageTicks > 0) {
            this.noDamageTicks -= tickDiff;
            if (this.noDamageTicks < 0) {
                this.noDamageTicks = 0;
            }
        }

        this.age += tickDiff;
        this.ticksLived += tickDiff;
        return hasUpdate;
    }

    public void updateMovement() {
        double diffPosition = position.distanceSquared(lastPosition);
        double diffRotation = FastMath.square(yaw - lastYaw) + FastMath.square(pitch - lastPitch);
        double diffMotion = motion.distanceSquared(lastMotion);

        if (diffPosition > 0.0001 || diffRotation > 1.0) {
            lastPosition = position;
            lastPitch = pitch;
            lastYaw = yaw;
            lastHeadYaw = headYaw;

            // If you want to achieve headYaw in movement. You can override it by yourself. Changing would break some mob plugins.
            addMovement(position.add(0, isPlayer? 0 : getBaseOffset(), 0), yaw, pitch, yaw);
        }

        if (diffMotion > 0.0025 || (diffMotion > 0.0001 && this.getMotion().lengthSquared() <= 0.0001)) {
            lastMotion = motion;

            addMotion(motion);
        }
    }

    public void addMovement(Vector3d position, double yaw, double pitch, double headYaw) {
        world.addEntityMovement(this, position, yaw, pitch, headYaw);
    }

    public void addMotion(Vector3d motion) {
        var pk = new SetEntityMotionPacket();

        pk.eid = this.id;
        pk.motion = motion.toFloat();

        Server.broadcastPacket(hasSpawned.values(), pk);
    }

    public Vector3d getDirectionVector() {
        double pitch = ((this.pitch + 90) * Math.PI) / 180;
        double yaw = ((this.yaw + 90) * Math.PI) / 180;
        double x = Math.sin(pitch) * Math.cos(yaw);
        double z = Math.sin(pitch) * Math.sin(yaw);
        double y = Math.cos(pitch);

        return new Vector3d(x, y, z).normalize();
    }

    public Vector2d getDirectionPlane() {
        return new Vector2d(-Math.cos(Math.toRadians(this.yaw) - Math.PI / 2), -Math.sin(Math.toRadians(this.yaw) - Math.PI / 2)).normalize();
    }

    public boolean onUpdate(int currentTick) {
        if (this.removed) {
            return false;
        }

        if (!this.isAlive()) {
            ++this.deadTicks;
            if (this.deadTicks >= 10) {
                this.despawnFromAll();
                if (!this.isPlayer) {
                    this.remove();
                }
            }
            return this.deadTicks < 10;
        }

        int tickDiff = currentTick - this.lastUpdate;

        if (tickDiff <= 0) {
            return false;
        }

        this.lastUpdate = currentTick;

        boolean hasUpdate = this.entityBaseTick(tickDiff);

        this.updateMovement();

        return hasUpdate;
    }

    public boolean mountEntity(Entity entity) {
        return mountEntity(entity, TYPE_RIDE);
    }

    /**
     * Mount an Entity from a/into vehicle
     *
     * @param entity The target Entity
     * @return {@code true} if the mounting successful
     */
    public boolean mountEntity(Entity entity, byte mode) {
        Objects.requireNonNull(entity, "The target of the mounting entity can't be null");

        if (isPassenger(entity) || entity.riding != null && !entity.riding.dismountEntity(entity, false)) {
            return false;
        }

        // Entity entering a vehicle
        EntityVehicleEnterEvent ev = new EntityVehicleEnterEvent(entity, this);
        server.getPluginManager().callEvent(ev);
        if (ev.isCancelled()) {
            return false;
        }

        broadcastLinkPacket(entity, mode);

        // Add variables to entity
        entity.riding = this;
        entity.setDataFlag(EntityDataKeys.FLAGS, EntityFlags.RIDING, true);
        passengers.add(entity);

        entity.setSeatPosition(getMountedOffset(entity));
        updatePassengerPosition(entity);
        return true;
    }

    public boolean dismountEntity(Entity entity) {
        return this.dismountEntity(entity, true);
    }

    public boolean dismountEntity(Entity entity, boolean sendLinks) {
        // Run the events
        EntityVehicleExitEvent ev = new EntityVehicleExitEvent(entity, this);
        server.getPluginManager().callEvent(ev);
        if (ev.isCancelled()) {
            int seatIndex = this.passengers.indexOf(entity);
            if (seatIndex == 0) {
                this.broadcastLinkPacket(entity, TYPE_RIDE);
            } else if (seatIndex != -1) {
                this.broadcastLinkPacket(entity, TYPE_PASSENGER);
            }
            return false;
        }

        if (sendLinks) {
            broadcastLinkPacket(entity, TYPE_REMOVE);
        }

        // Refurbish the entity
        entity.riding = null;
        entity.setDataFlag(EntityDataKeys.FLAGS, EntityFlags.RIDING, false);
        passengers.remove(entity);

        entity.setSeatPosition(Vector3f.ZERO);
        updatePassengerPosition(entity);

        return true;
    }

    protected void broadcastLinkPacket(Entity rider, byte type) {
        SetEntityLinkPacket pk = new SetEntityLinkPacket();
        pk.vehicleUniqueId = getId();         // To the?
        pk.riderUniqueId = rider.getId(); // From who?
        pk.type = type;

        Server.broadcastPacket(this.hasSpawned.values(), pk);
    }

    public void updatePassengers() {
        if (this.passengers.isEmpty()) {
            return;
        }

        for (Entity passenger : new ArrayList<>(this.passengers)) {
            if (!passenger.isAlive()) {
                dismountEntity(passenger);
                continue;
            }

            updatePassengerPosition(passenger);
        }
    }

    protected void updatePassengerPosition(Entity passenger) {
        passenger.setPosition(position.add(passenger.getSeatPosition().toDouble()));
    }

    public void setSeatPosition(Vector3f pos) {
        this.setDataProperty(new Vector3fEntityData(EntityDataKeys.RIDER_SEAT_POSITION, pos));
    }

    public Vector3f getSeatPosition() {
        return getDataProperties().getFloatPosition(EntityDataKeys.RIDER_SEAT_POSITION);
    }

    public Vector3f getMountedOffset(Entity entity) {
        return new Vector3f(0, getHeight() * 0.75f, 0);
    }

    public final void scheduleUpdate() {
        world.updateEntities.put(this.id, this);
    }

    public boolean isOnFire() {
        return this.fireTicks > 0;
    }

    public void setOnFire(int seconds) {
        int ticks = seconds * 20;
        if (ticks > this.fireTicks) {
            this.fireTicks = ticks;
        }
    }

    public float getAbsorption() {
        return absorption;
    }

    public void setAbsorption(float absorption) {
        if (absorption != this.absorption) {
            this.absorption = absorption;
            if (this instanceof Player)
                ((Player) this).setAttribute(Attributes.ABSORPTION, absorption);
        }
    }

    public BlockFace getDirection() {
        double rotation = this.yaw % 360;
        if (rotation < 0) {
            rotation += 360.0;
        }
        if ((0 <= rotation && rotation < 45) || (315 <= rotation && rotation < 360)) {
            return BlockFace.SOUTH;
        } else if (45 <= rotation && rotation < 135) {
            return BlockFace.WEST;
        } else if (135 <= rotation && rotation < 225) {
            return BlockFace.NORTH;
        } else if (225 <= rotation && rotation < 315) {
            return BlockFace.EAST;
        } else {
            return null;
        }
    }

    public void extinguish() {
        this.fireTicks = 0;
        this.setDataFlag(EntityDataKeys.FLAGS, EntityFlags.ONFIRE, false);
    }

    public boolean canTriggerWalking() {
        return true;
    }

    public void resetFallDistance() {
        this.highestPosition = 0;
    }

    protected void updateFallState(boolean onGround) {
        if (onGround) {
            fallDistance = (float) (highestPosition - position.y());

            if (fallDistance > 0) {
                // check if we fell into at least 1 block of water
                if (this instanceof EntityLiving) fall(fallDistance);
                resetFallDistance();
            }
        }
    }

    public AxisAlignedBB getBoundingBox() {
        return this.boundingBox;
    }

    public void fall(float fallDistance) {
        if (this.hasEffect(Effect.SLOW_FALLING)) {
            return;
        }

        Block down = world.getBlock(BlockFace.moveDown(position.toInt()));

        if (!this.isPlayer || world.getGameRules().getBoolean(GameRule.FALL_DAMAGE)) {
            float damage = (float) Math.floor(fallDistance - 3 - (this.hasEffect(Effect.JUMP) ? this.getEffect(Effect.JUMP).getAmplifier() + 1 : 0));

            if (damage > 0) {
                this.attack(new EntityDamageEvent(this, DamageCause.FALL, damage));
            }
        }
    }

    public void moveFlying(float strafe, float forward, float friction) {
        // This is special for Nukkit! :)
        float speed = strafe * strafe + forward * forward;
        if (speed >= 1.0E-4F) {
            speed = FastMath.sqrt(speed);
            if (speed < 1.0F) {
                speed = 1.0F;
            }
            speed = friction / speed;
            strafe *= speed;
            forward *= speed;
            float nest = FastMath.sin((float) (this.yaw * 3.1415927F / 180.0F));
            float place = FastMath.cos((float) (this.yaw * 3.1415927F / 180.0F));
            motion = motion.add(strafe * place - forward * nest, 0, forward * place + strafe * nest);
        }
    }

    public void onCollideWithPlayer(EntityHuman entityPlayer) {

    }

    public void applyEntityCollision(Entity entity) {
        if (entity.riding != this && !entity.passengers.contains(this)) {
            double dx = entity.position.x() - position.x();
            double dy = entity.position.z() - position.z();
            double dz = FastMath.getDirection(dx, dy);

            if (dz >= 0.009999999776482582D) {
                dz = FastMath.sqrt((float) dz);
                dx /= dz;
                dy /= dz;
                double d3 = 1.0D / dz;

                if (d3 > 1.0D) {
                    d3 = 1.0D;
                }

                dx *= d3;
                dy *= d3;
                dx *= 0.05000000074505806;
                dy *= 0.05000000074505806;
                dx *= 1F + entityCollisionReduction;

                if (this.riding == null) {
                    motion = motion.sub(dx, 0, dy);
                }
            }
        }
    }

    public void onStruckByLightning(Entity entity) {
        if (this.attack(new EntityDamageByEntityEvent(entity, this, DamageCause.LIGHTNING, 5))) {
            if (this.fireTicks < 8 * 20) {
                this.setOnFire(8);
            }
        }
    }

    public boolean onInteract(Player player, Item item, Vector3d clickedPos) {
        return onInteract(player, item);
    }

    public boolean onInteract(Player player, Item item) {
        return false;
    }

    protected boolean setWorld(Level targetLevel) {
        if (this.removed) {
            return false;
        }

        if (world != null) {
            EntityLevelChangeEvent ev = new EntityLevelChangeEvent(this, world, targetLevel);
            server.getPluginManager().callEvent(ev);
            if (ev.isCancelled()) {
                return false;
            }

            world.removeEntity(this);
            if (chunk != null) chunk.removeEntity(this);
            despawnFromAll();
        }

        world = targetLevel;
        world.addEntity(this);
        chunk = null;

        return true;
    }

    public Point getPoint() {
        return Point.of(position, yaw, pitch, headYaw);
    }

    public boolean isInsideOfWater() {
        var block = world.getBlock(position.add(0, getEyeHeight(), 0).toInt());

        //TODO
        return false;
    }

    public boolean isInsideOfSolid() {
        var block = world.getBlock(position.add(0, getEyeHeight(), 0).toInt());
        AxisAlignedBB bb = block.getBoundingBox();

        return bb != null && block.isSolid() && !block.isTransparent() && bb.intersectsWith(this.getBoundingBox());
    }

    public boolean isInsideOfFire() {
        return false;
    }

    public boolean isOnLadder() {
        return false;
    }

    public boolean fastMove(Vector3d delta) {
        if (Vector3d.ZERO.equals(delta)) return true;

        var newBB = boundingBox.getOffsetBoundingBox(delta);

        if (server.getAllowFlight() || !world.hasCollision(this, newBB, false)) {
            boundingBox = newBB;
        }

        position = new Vector3d(
                (boundingBox.getMinX() + boundingBox.getMaxX()) / 2,
                boundingBox.getMinY() - ySize,
                (boundingBox.getMinZ() + boundingBox.getMaxZ()) / 2
        );

        checkChunks();

        if (!onGround || delta.y() != 0) {
            AxisAlignedBB bb = boundingBox.clone();
            bb.setMinY(bb.getMinY() - 0.75);

            onGround = world.getCollisionBlocks(bb).length > 0;
        }
        isCollided = onGround;
        updateFallState(onGround);
        return true;
    }

    public boolean move(Vector3d delta) {
        if (Vector3d.ZERO.equals(delta)) return true;

        double dx = delta.x();
        double dy = delta.y();
        double dz = delta.z();

        if (keepMovement) {
            boundingBox.offset(delta);
            setPosition(new Vector3d((boundingBox.getMinX() + boundingBox.getMaxX()) / 2, boundingBox.getMinY(), (boundingBox.getMinZ() + boundingBox.getMaxZ()) / 2));
            onGround = isPlayer;
            return true;
        } else {

            this.ySize *= 0.4;

            double movX = dx;
            double movY = dy;
            double movZ = dz;

            var axisalignedbb = boundingBox.clone();
            var list = world.getCollisionCubes(this, boundingBox.addCoord(delta), false);

            for (var bb: list) dy = bb.calculateYOffset(boundingBox, dy);
            boundingBox.offset(0, dy, 0);

            var fallingFlag = (onGround || (dy != movY && movY < 0));
            for (var bb: list) dx = bb.calculateXOffset(boundingBox, dx);
            boundingBox.offset(dx, 0, 0);

            for (var bb : list) dz = bb.calculateZOffset(boundingBox, dz);
            boundingBox.offset(0, 0, dz);

            if (getStepHeight() > 0 && fallingFlag && ySize < 0.05 && (movX != dx || movZ != dz)) {
                double cx = dx;
                double cy = dy;
                double cz = dz;
                dx = movX;
                dy = getStepHeight();
                dz = movZ;
                var axisalignedbb1 = boundingBox.clone();
                boundingBox.setBB(axisalignedbb);

                list = world.getCollisionCubes(this, boundingBox.addCoord(dx, dy, dz), false);

                for (var bb:  list) dy = bb.calculateYOffset(boundingBox, dy);
                boundingBox.offset(0, dy, 0);

                for (var bb: list) dx = bb.calculateXOffset(boundingBox, dx);
                boundingBox.offset(dx, 0, 0);

                for (var bb: list) dz = bb.calculateZOffset(boundingBox, dz);
                boundingBox.offset(0, 0, dz);
                boundingBox.offset(0, 0, dz);

                if ((cx * cx + cz * cz) >= (dx * dx + dz * dz)) {
                    dx = cx;
                    dy = cy;
                    dz = cz;
                    boundingBox.setBB(axisalignedbb1);
                } else {
                    ySize += 0.5;
                }

            }

            position = new Vector3d(
                    (boundingBox.getMinX() + boundingBox.getMaxX()) / 2,
                    boundingBox.getMinY() - ySize,
                    (boundingBox.getMinZ() + boundingBox.getMaxZ()) / 2
            );

            checkChunks();
            checkGroundState(movX, movY, movZ, dx, dy, dz);
            updateFallState(onGround);

            motion = motion.mul(movX != dx? 0 : 1, movY != dy? 0 : 1, movZ != dz? 0 : 1);

            //TODO: vehicle collision events (first we need to spawn them!)
            return true;
        }
    }

    protected void checkGroundState(double movX, double movY, double movZ, double dx, double dy, double dz) {
        isCollidedVertically = movY != dy;
        isCollidedHorizontally = (movX != dx || movZ != dz);
        isCollided = (isCollidedHorizontally || isCollidedVertically);
        onGround = (movY != dy && movY < 0);
    }

    public List<Block> getBlocksAround() {
        if (blocksAround == null) {
            int minX = FastMath.floorDouble(boundingBox.getMinX());
            int minY = FastMath.floorDouble(boundingBox.getMinY());
            int minZ = FastMath.floorDouble(boundingBox.getMinZ());
            int maxX = FastMath.ceilDouble(boundingBox.getMaxX());
            int maxY = FastMath.ceilDouble(boundingBox.getMaxY());
            int maxZ = FastMath.ceilDouble(boundingBox.getMaxZ());

            blocksAround = new ArrayList<>();

            for (int z = minZ; z <= maxZ; ++z) {
                for (int x = minX; x <= maxX; ++x) {
                    for (int y = minY; y <= maxY; ++y) {
                        Block block = world.getBlock(new Vector3i(x, y, z));
                        blocksAround.add(block);
                    }
                }
            }
        }

        return blocksAround;
    }

    public List<Block> getCollisionBlocks() {
        if (collisionBlocks == null) {
            collisionBlocks = new ArrayList<>();

            for (Block b: getBlocksAround()) {
                if (b.collidesWithBB(getBoundingBox(), true)) {
                    collisionBlocks.add(b);
                }
            }
        }

        return collisionBlocks;
    }

    public boolean canBeMovedByCurrents() {
        return true;
    }

    protected void checkBlockCollision() {
        Vector3d vector = Vector3d.ZERO;

        for (Block block: getCollisionBlocks()) {
            block.onEntityCollide(this);
            block.addVelocityToEntity(this, vector);
        }

        if (vector.lengthSquared() > 0) {
            vector = vector.normalize();
            double d = 0.014d;
            motion = motion.add(vector.mul(d));
        }
    }

    public boolean setPositionAndRotation(Vector3d pos, double yaw, double pitch) {
        if (setPosition(pos)) {
            setRotation(yaw, pitch);
            return true;
        }

        return false;
    }

    public void setRotation(double yaw, double pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.scheduleUpdate();
    }

    public void setRotation(double yaw, double pitch, double headYaw) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.headYaw = headYaw;
        this.scheduleUpdate();
    }

    public boolean canPassThrough() {
        return true;
    }

    protected void checkChunks() {
        if (chunk == null || (chunk.getX() != (position.floorX() >> 4)) || chunk.getZ() != (position.floorZ() >> 4)) {
            if (chunk != null) {
                chunk.removeEntity(this);
            }
            chunk = world.getChunk(position.floorX() >> 4, position.floorZ() >> 4, true);

            if (!justCreated) {
                var newChunk = world.getChunkPlayers(position.floorX() >> 4, position.floorZ() >> 4);
                for (Player player: new ArrayList<>(hasSpawned.values())) {
                    if (!newChunk.containsKey(player.getLoaderId())) {
                        despawnFrom(player);
                    } else {
                        newChunk.remove(player.getLoaderId());
                    }
                }

                for (Player player: newChunk.values()) spawnTo(player);
            }

            if (chunk == null) return;

            chunk.addEntity(this);
        }
    }

    public boolean setPosition(Vector3d newPosition) {
        if (removed) return false;

        position = newPosition;
        recalculateBoundingBox(false);
        checkChunks();

        return true;
    }

    public boolean setMotion(Vector3d motion) {
        if (!justCreated) {
            var ev = new EntityMotionEvent(this, motion);
            server.getPluginManager().callEvent(ev);
            if (ev.isCancelled()) return false;
        }

        this.motion = motion;

        if (!justCreated) updateMovement();

        return true;
    }

    public boolean isOnGround() {
        return onGround;
    }

    public void kill() {
        this.health = 0;
        this.scheduleUpdate();

        for (Entity passenger : new ArrayList<>(this.passengers)) {
            dismountEntity(passenger);
        }
    }

    public boolean teleport(Vector3d pos) {
        return teleport(Point.of(pos, yaw, pitch, headYaw));
    }

    public boolean teleport(Point point) {
        if (riding != null && !riding.dismountEntity(this)) return false;

        ySize = 0;
        setMotion(Vector3d.ZERO);

        if (setPositionAndRotation(point.getPosition(), point.getYaw(), point.getPitch())) {
            resetFallDistance();
            onGround = true;
            updateMovement();

            return true;
        }

        return false;
    }

    public void respawnToAll() {
        Collection<Player> players = new ArrayList<>(this.hasSpawned.values());
        this.hasSpawned.clear();

        for (Player player : players) {
            this.spawnTo(player);
        }
    }

    public void spawnToAll() {
        if (this.chunk == null || this.removed) {
            return;
        }

        for (Player player: world.getChunkPlayers(this.chunk.getX(), this.chunk.getZ()).values()) {
            if (player.isOnline()) {
                this.spawnTo(player);
            }
        }
    }

    public void despawnFromAll() {
        for (Player player : new ArrayList<>(this.hasSpawned.values())) {
            this.despawnFrom(player);
        }
    }

    public void remove() {
        if (!removed) {
            removed = true;
            server.getPluginManager().callEvent(new EntityDespawnEvent(this));
            despawnFromAll();

            if (chunk != null) {
                chunk.removeEntity(this);
            }

            if (world != null) {
                world.removeEntity(this);
            }
        }
    }

    public boolean setDataProperty(EntityData<?> data) {
        return setDataProperty(data, true);
    }

    public boolean setDataProperty(EntityData<?> data, boolean send) {
        if (Objects.equals(data, this.dataProperties.get(data.getId()))) {
            return false;
        }

        this.dataProperties.put(data);
        if (send) {
            EntityMetadata metadata = new EntityMetadata();
            metadata.put(this.dataProperties.get(data.getId()));
            if (data.getId() == EntityDataKeys.FLAGS_EXTENDED) {
                metadata.put(this.dataProperties.get(EntityDataKeys.FLAGS));
            }
            this.sendData(this.hasSpawned.values().toArray(new Player[0]), metadata);
        }
        return true;
    }

    public EntityMetadata getDataProperties() {
        return this.dataProperties;
    }

    public void setDataFlag(int propertyId, int id) {
        this.setDataFlag(propertyId, id, true);
    }

    public void setDataFlag(int propertyId, int id, boolean value) {
        if (this.getDataFlag(propertyId, id) != value) {
            if (propertyId == EntityHuman.DATA_PLAYER_FLAGS) {
                byte flags = (byte) this.getDataProperties().getByte(propertyId);
                flags ^= 1 << id;
                this.setDataProperty(new ByteEntityData(propertyId, flags));
            } else {
                long flags = this.getDataProperties().getLong(propertyId);
                flags ^= 1L << id;
                this.setDataProperty(new LongEntityData(propertyId, flags));
            }

        }
    }

    public boolean getDataFlag(int propertyId, int id) {
        return (((propertyId == EntityHuman.DATA_PLAYER_FLAGS ? this.getDataProperties().getByte(propertyId) & 0xff : this.getDataProperties().getLong(propertyId))) & (1L << id)) > 0;
    }

    @Override
    public void setMetadata(String metadataKey, MetadataValue newMetadataValue) {
        this.server.getEntityMetadata().setMetadata(this, metadataKey, newMetadataValue);
    }

    @Override
    public List<MetadataValue> getMetadata(String metadataKey) {
        return this.server.getEntityMetadata().getMetadata(this, metadataKey);
    }

    @Override
    public boolean hasMetadata(String metadataKey) {
        return this.server.getEntityMetadata().hasMetadata(this, metadataKey);
    }

    @Override
    public void removeMetadata(String metadataKey, Plugin owningPlugin) {
        this.server.getEntityMetadata().removeMetadata(this, metadataKey, owningPlugin);
    }

    public Server getServer() {
        return server;
    }

    public final int getChunkX() {
        return position.floorX() >> 4;
    }

    public final int getChunkZ() {
        return position.floorZ() >> 4;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Entity other = (Entity) obj;
        return this.getId() == other.getId();
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = (int) (29 * hash + this.getId());
        return hash;
    }
}
