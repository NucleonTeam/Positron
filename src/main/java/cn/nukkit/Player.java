package cn.nukkit;

import cn.nukkit.AdventureSettings.Type;
import cn.nukkit.block.*;
import cn.nukkit.entity.*;
import cn.nukkit.entity.data.*;
import cn.nukkit.entity.item.*;
import cn.nukkit.event.entity.EntityDamageByBlockEvent;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.entity.EntityDamageEvent.DamageCause;
import cn.nukkit.event.entity.EntityDamageEvent.DamageModifier;
import cn.nukkit.entity.mob.inventory.InventoryCloseEvent;
import cn.nukkit.event.player.*;
import cn.nukkit.event.player.PlayerAsyncPreLoginEvent.LoginResult;
import cn.nukkit.event.player.PlayerInteractEvent.Action;
import cn.nukkit.event.server.DataPacketReceiveEvent;
import cn.nukkit.event.server.DataPacketSendEvent;
import cn.nukkit.form.handler.FormResponseHandler;
import cn.nukkit.form.window.FormWindow;
import cn.nukkit.form.window.FormWindowCustom;
import cn.nukkit.inventory.*;
import cn.nukkit.inventory.transaction.CraftingTransaction;
import cn.nukkit.inventory.transaction.EnchantTransaction;
import cn.nukkit.inventory.transaction.InventoryTransaction;
import cn.nukkit.inventory.transaction.RepairItemTransaction;
import cn.nukkit.inventory.transaction.action.InventoryAction;
import cn.nukkit.inventory.transaction.data.ReleaseItemData;
import cn.nukkit.inventory.transaction.data.UseItemData;
import cn.nukkit.inventory.transaction.data.UseItemOnEntityData;
import cn.nukkit.item.*;
import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.lang.TextContainer;
import cn.nukkit.lang.TranslationContainer;
import cn.nukkit.level.*;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.particle.PunchBlockParticle;
import cn.nukkit.math.*;
import cn.nukkit.metadata.MetadataValue;
import cn.nukkit.metadata.Metadatable;
import cn.nukkit.nbt.NBTIO;
import cn.nukkit.nbt.tag.*;
import cn.nukkit.network.CompressionProvider;
import cn.nukkit.network.Network;
import cn.nukkit.network.SourceInterface;
import cn.nukkit.network.protocol.*;
import cn.nukkit.network.protocol.types.*;
import cn.nukkit.network.session.NetworkPlayerSession;
import cn.nukkit.plugin.Plugin;
import cn.nukkit.potion.Effect;
import cn.nukkit.resourcepacks.ResourcePack;
import cn.nukkit.scheduler.AsyncTask;
import cn.nukkit.utils.*;
import com.google.common.base.Strings;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import io.netty.util.internal.PlatformDependent;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import lombok.extern.log4j.Log4j2;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3i;
import ru.mc_positron.blockentity.SpawnableBlockEntityType;
import ru.mc_positron.entity.EntityFlags;
import ru.mc_positron.entity.attribute.Attribute;
import ru.mc_positron.entity.attribute.Attributes;
import ru.mc_positron.entity.data.ShortEntityData;
import ru.mc_positron.entity.data.StringEntityData;
import ru.mc_positron.entity.data.Vector3iEntityData;
import ru.mc_positron.math.BlockFace;
import ru.mc_positron.math.FastMath;
import ru.mc_positron.math.Point;
import ru.mc_positron.network.packet.BlockEntityDataPacket;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteOrder;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Log4j2
public class Player extends EntityHuman implements InventoryHolder, ChunkLoader, Metadatable {

    public static final int SURVIVAL = 0;
    public static final int CREATIVE = 1;
    public static final int ADVENTURE = 2;
    public static final int SPECTATOR = 3;
    public static final int VIEW = SPECTATOR;

    public static final int SURVIVAL_SLOTS = 36;
    public static final int CREATIVE_SLOTS = 112;

    public static final int CRAFTING_SMALL = 0;
    public static final int CRAFTING_BIG = 1;
    public static final int CRAFTING_ANVIL = 2;
    public static final int CRAFTING_ENCHANT = 3;
    public static final int CRAFTING_BEACON = 4;

    public static final float DEFAULT_SPEED = 0.1f;
    public static final float MAXIMUM_SPEED = 0.5f;
    public static final float DEFAULT_FLY_SPEED = 0.05f;

    public static final int PERMISSION_CUSTOM = 3;
    public static final int PERMISSION_OPERATOR = 2;
    public static final int PERMISSION_MEMBER = 1;
    public static final int PERMISSION_VISITOR = 0;

    public static final int ANVIL_WINDOW_ID = 2;
    public static final int ENCHANT_WINDOW_ID = 3;
    public static final int BEACON_WINDOW_ID = 4;

    protected static final int RESOURCE_PACK_CHUNK_SIZE = 8 * 1024; // 8KB

    protected final SourceInterface interfaz;
    protected final NetworkPlayerSession networkSession;

    public boolean playedBefore;
    public boolean spawned = false;
    public boolean loggedIn = false;
    public boolean locallyInitialized = false;
    private boolean verified = false;
    private int unverifiedPackets;
    public int gamemode;
    public long lastBreak;
    private Vector3i lastBreakPosition = Vector3i.ZERO;

    protected int windowCnt = 4;

    protected final BiMap<Inventory, Integer> windows = HashBiMap.create();

    protected final BiMap<Integer, Inventory> windowIndex = windows.inverse();
    protected final Set<Integer> permanentWindows = new IntOpenHashSet();
    private boolean inventoryOpen;
    protected int closingWindowId = Integer.MIN_VALUE;

    protected int messageCounter = 2;

    public Vector3d speed = null;

    private final Queue<Vector3d> clientMovements = PlatformDependent.newMpscQueue(4);

    public final HashSet<String> achievements = new HashSet<>();

    public int craftingType = CRAFTING_SMALL;

    protected PlayerUIInventory playerUIInventory;
    protected CraftingGrid craftingGrid;
    protected CraftingTransaction craftingTransaction;
    protected EnchantTransaction enchantTransaction;
    protected RepairItemTransaction repairItemTransaction;

    public long creationTime = 0;

    protected long randomClientId;

    protected Vector3d forceMovement = null;

    protected Vector3d teleportPosition = null;

    protected boolean connected = true;
    protected final InetSocketAddress socketAddress;
    protected boolean removeFormat = true;

    protected String username;
    protected String iusername;
    protected String displayName;

    protected int startAction = -1;

    protected Vector3d sleeping = null;
    protected Long clientID = null;

    private int loaderId;

    public final Map<Long, Boolean> usedChunks = new Long2ObjectOpenHashMap<>();

    protected int chunkLoadCount = 0;
    protected final Long2ObjectLinkedOpenHashMap<Boolean> loadQueue = new Long2ObjectLinkedOpenHashMap<>();
    protected int nextChunkOrderRun = 1;

    protected final Map<UUID, Player> hiddenPlayers = new HashMap<>();

    protected Vector3d newPosition = null;

    protected int chunkRadius;
    protected int viewDistance;
    protected final int chunksPerTick;
    protected final int spawnThreshold;

    protected Point spawnPoint = null;

    protected int inAirTicks = 0;
    protected int startAirTicks = 5;

    protected AdventureSettings adventureSettings;

    protected boolean checkMovement = true;

    private int exp = 0;
    private int expLevel = 0;

    protected PlayerFood foodData = null;

    private Entity killer = null;

    private final AtomicReference<Locale> locale = new AtomicReference<>(null);

    private int hash;

    private String buttonText = "";

    protected boolean enableClientCommand = true;

    private LoginChainData loginChainData;

    public Block breakingBlock = null;
    private PlayerBlockActionData lastBlockAction;

    protected int formWindowCount = 0;
    protected Map<Integer, FormWindow> formWindows = new Int2ObjectOpenHashMap<>();
    protected Map<Integer, FormWindow> serverSettings = new Int2ObjectOpenHashMap<>();

    protected Map<Long, DummyBossBar> dummyBossBars = new Long2ObjectLinkedOpenHashMap<>();

    private AsyncTask preLoginEventTask = null;
    protected boolean shouldLogin = false;

    public long lastSkinChange;

    protected double lastRightClickTime = 0.0;
    protected Vector3 lastRightClickPos = null;

    private int timeSinceRest;

    public int getStartActionTick() {
        return startAction;
    }

    public void startAction() {
        this.startAction = this.server.getTick();
    }

    public void stopAction() {
        this.startAction = -1;
    }

    public TranslationContainer getLeaveMessage() {
        return new TranslationContainer(TextFormat.YELLOW + "%multiplayer.player.left", this.getDisplayName());
    }

    @Deprecated
    public Long getClientId() {
        return randomClientId;
    }

    public Player getPlayer() {
        return this;
    }

    public AdventureSettings getAdventureSettings() {
        return adventureSettings;
    }

    public void setAdventureSettings(AdventureSettings adventureSettings) {
        this.adventureSettings = adventureSettings.clone(this);
        this.adventureSettings.update();
    }

    public void resetInAirTicks() {
        this.inAirTicks = 0;
    }

    @Deprecated
    public void setAllowFlight(boolean value) {
        this.getAdventureSettings().set(Type.ALLOW_FLIGHT, value);
        this.getAdventureSettings().update();
    }

    @Deprecated
    public boolean getAllowFlight() {
        return this.getAdventureSettings().get(Type.ALLOW_FLIGHT);
    }

    public void setAllowModifyWorld(boolean value) {
        this.getAdventureSettings().set(Type.WORLD_IMMUTABLE, !value);
        this.getAdventureSettings().set(Type.MINE, value);
        this.getAdventureSettings().set(Type.BUILD, value);
        this.getAdventureSettings().update();
    }

    public void setAllowInteract(boolean value) {
        setAllowInteract(value, value);
    }

    public void setAllowInteract(boolean value, boolean containers) {
        this.getAdventureSettings().set(Type.WORLD_IMMUTABLE, !value);
        this.getAdventureSettings().set(Type.DOORS_AND_SWITCHED, value);
        this.getAdventureSettings().set(Type.OPEN_CONTAINERS, containers);
        this.getAdventureSettings().update();
    }

    @Deprecated
    public void setAutoJump(boolean value) {
        this.getAdventureSettings().set(Type.AUTO_JUMP, value);
        this.getAdventureSettings().update();
    }

    @Deprecated
    public boolean hasAutoJump() {
        return this.getAdventureSettings().get(Type.AUTO_JUMP);
    }

    @Override
    public void spawnTo(Player player) {
        if (spawned && player.spawned && isAlive() && player.world == world && player.canSee(this) && !isSpectator()) {
            super.spawnTo(player);
        }
    }

    @Override
    public Server getServer() {
        return this.server;
    }

    public boolean canSee(Player player) {
        return !this.hiddenPlayers.containsKey(player.getUniqueId());
    }

    public void hidePlayer(Player player) {
        if (this == player) {
            return;
        }
        this.hiddenPlayers.put(player.getUniqueId(), player);
        player.despawnFrom(this);
    }

    public void showPlayer(Player player) {
        if (this == player) {
            return;
        }
        this.hiddenPlayers.remove(player.getUniqueId());
        if (player.isOnline()) {
            player.spawnTo(this);
        }
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return false;
    }

    @Override
    public void resetFallDistance() {
        super.resetFallDistance();
        if (inAirTicks != 0) {
            startAirTicks = 5;
        }
        inAirTicks = 0;
        highestPosition = position.y();
    }

    public boolean isOnline() {
        return this.connected && this.loggedIn;
    }

    public boolean isEnableClientCommand() {
        return this.enableClientCommand;
    }

    public void setEnableClientCommand(boolean enable) {
        this.enableClientCommand = enable;
        SetCommandsEnabledPacket pk = new SetCommandsEnabledPacket();
        pk.enabled = enable;
        this.dataPacket(pk);
    }

    public Player(SourceInterface interfaz, Long clientID, InetSocketAddress socketAddress) {
        super(null, new CompoundTag());
        this.interfaz = interfaz;
        this.socketAddress = socketAddress;
        this.clientID = clientID;
        networkSession = interfaz.getSession(socketAddress);
        server = Server.getInstance();
        lastBreak = -1;
        loaderId = Level.generateChunkLoaderId(this);
        chunksPerTick = server.getConfig("chunk-sending.per-tick", 4);
        spawnThreshold = server.getConfig("chunk-sending.spawn-threshold", 56);
        spawnPoint = null;
        gamemode = server.getGamemode();
        world = server.getDefaultLevel();
        viewDistance = server.getViewDistance();
        chunkRadius = viewDistance;
        //this.newPosition = new Vector3(0, 0, 0);
        boundingBox = new SimpleAxisAlignedBB(Vector3d.ZERO, Vector3d.ZERO);
        lastSkinChange = -1;

        playerUuid = null;
        rawUUID = null;

        creationTime = System.currentTimeMillis();
    }

    @Override
    protected void initEntity() {
        super.initEntity();

        addDefaultWindows();
    }

    public boolean isPlayer() {
        return true;
    }

    public boolean isConnected() {
        return connected;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
        if (spawned) {
            server.updatePlayerListData(getUniqueId(), getId(), getDisplayName(), getSkin(), getLoginChainData().getXUID());
        }
    }

    @Override
    public void setSkin(Skin skin) {
        super.setSkin(skin);
        if (spawned) {
            server.updatePlayerListData(getUniqueId(), getId(), getDisplayName(), skin, getLoginChainData().getXUID());
        }
    }

    public String getAddress() {
        return this.socketAddress.getAddress().getHostAddress();
    }

    public int getPort() {
        return this.socketAddress.getPort();
    }

    public InetSocketAddress getSocketAddress() {
        return this.socketAddress;
    }

    public Vector3d getNextPosition() {
        return newPosition != null? newPosition : position;
    }

    public boolean isSleeping() {
        return this.sleeping != null;
    }

    public int getInAirTicks() {
        return this.inAirTicks;
    }

    public boolean isUsingItem() {
        return this.getDataFlag(DATA_FLAGS, EntityFlags.ACTION) && this.startAction > -1;
    }

    public void setUsingItem(boolean value) {
        this.startAction = value ? this.server.getTick() : -1;
        this.setDataFlag(DATA_FLAGS, EntityFlags.ACTION, value);
    }

    public String getButtonText() {
        return this.buttonText;
    }

    public void setButtonText(String text) {
        if (this.buttonText.equals(text)) {
            return;
        }
        this.buttonText = text;
        this.setDataProperty(new StringEntityData(Entity.DATA_INTERACTIVE_TAG, this.buttonText));
    }

    public void unloadChunk(int x, int z) {
        this.unloadChunk(x, z, null);
    }

    public void unloadChunk(int x, int z, Level world) {
        world = world == null ? this.world : world;
        long index = Level.chunkHash(x, z);
        if (this.usedChunks.containsKey(index)) {
            for (Entity entity : world.getChunkEntities(x, z).values()) {
                if (entity != this) {
                    entity.despawnFrom(this);
                }
            }

            this.usedChunks.remove(index);
        }
        world.unregisterChunkLoader(this, x, z);
        this.loadQueue.remove(index);
    }

    public Point getSpawn() {
        return server.getDefaultLevel().getSpawnPoint();
    }

    public void sendChunk(int x, int z, DataPacket packet) {
        if (!this.connected) {
            return;
        }

        this.usedChunks.put(Level.chunkHash(x, z), Boolean.TRUE);
        this.chunkLoadCount++;

        this.dataPacket(packet);

        if (this.spawned) {
            for (Entity entity: world.getChunkEntities(x, z).values()) {
                if (this != entity && !entity.isRemoved() && entity.isAlive()) {
                    entity.spawnTo(this);
                }
            }
        }
    }

    public void sendChunk(int x, int z, int subChunkCount, byte[] payload) {
        if (!this.connected) {
            return;
        }

        this.usedChunks.put(Level.chunkHash(x, z), true);
        this.chunkLoadCount++;

        LevelChunkPacket pk = new LevelChunkPacket();
        pk.chunkX = x;
        pk.chunkZ = z;
        pk.subChunkCount = subChunkCount;
        pk.data = payload;

        this.dataPacket(pk);

        if (this.spawned) {
            for (Entity entity: world.getChunkEntities(x, z).values()) {
                if (this != entity && !entity.isRemoved() && entity.isAlive()) {
                    entity.spawnTo(this);
                }
            }
        }
    }

    protected void sendNextChunk() {
        if (!this.connected) {
            return;
        }

        if (!loadQueue.isEmpty()) {
            int count = 0;
            ObjectIterator<Long2ObjectMap.Entry<Boolean>> iter = loadQueue.long2ObjectEntrySet().fastIterator();
            while (iter.hasNext()) {
                Long2ObjectMap.Entry<Boolean> entry = iter.next();
                long index = entry.getLongKey();

                if (count >= this.chunksPerTick) {
                    break;
                }
                int chunkX = Level.getHashX(index);
                int chunkZ = Level.getHashZ(index);

                ++count;

                this.usedChunks.put(index, false);
                world.registerChunkLoader(this, chunkX, chunkZ, false);

                if (!world.populateChunk(chunkX, chunkZ)) {
                    if (this.spawned && this.teleportPosition == null) {
                        continue;
                    } else {
                        break;
                    }
                }

                iter.remove();

                PlayerChunkRequestEvent ev = new PlayerChunkRequestEvent(this, chunkX, chunkZ);
                this.server.getPluginManager().callEvent(ev);
                if (!ev.isCancelled()) {
                    world.requestChunk(chunkX, chunkZ, this);
                }
            }
        }
        if (this.chunkLoadCount >= this.spawnThreshold && !this.spawned && this.teleportPosition == null) {
            this.doFirstSpawn();
        }
    }

    protected void doFirstSpawn() {
        this.spawned = true;

        this.inventory.sendContents(this);
        this.inventory.sendArmorContents(this);
        this.offhandInventory.sendContents(this);
        this.setEnableClientCommand(true);

        SetTimePacket setTimePacket = new SetTimePacket();
        setTimePacket.time = world.getTime();
        this.dataPacket(setTimePacket);

        var pos = world.getSpawnPoint();

        PlayerRespawnEvent respawnEvent = new PlayerRespawnEvent(this, pos, true);

        server.getPluginManager().callEvent(respawnEvent);

        pos = respawnEvent.getRespawnPoint();

        this.sendPlayStatus(PlayStatusPacket.PLAYER_SPAWN);

        PlayerJoinEvent playerJoinEvent = new PlayerJoinEvent(this,
                new TranslationContainer(TextFormat.YELLOW + "%multiplayer.player.joined", new String[]{
                        this.getDisplayName()
                })
        );

        this.server.getPluginManager().callEvent(playerJoinEvent);

        this.noDamageTicks = 60;

        this.getServer().sendRecipeList(this);


        for (long index : this.usedChunks.keySet()) {
            int chunkX = Level.getHashX(index);
            int chunkZ = Level.getHashZ(index);
            for (Entity entity : world.getChunkEntities(chunkX, chunkZ).values()) {
                if (this != entity && !entity.isRemoved() && entity.isAlive()) {
                    entity.spawnTo(this);
                }
            }
        }

        int experience = this.getExperience();
        if (experience != 0) {
            this.sendExperience(experience);
        }

        int level = this.getExperienceLevel();
        if (level != 0) {
            this.sendExperienceLevel(this.getExperienceLevel());
        }

        teleport(pos);

        if (!isSpectator()) spawnToAll();

        world.sendWeather(this);

        //FoodLevel
        PlayerFood food = this.getFoodData();
        if (food.getLevel() != food.getMaxLevel()) {
            food.sendFoodLevel();
        }

        if (this.getHealth() < 1) {
            this.respawn();
        }

    }

    protected boolean orderChunks() {
        if (!this.connected) {
            return false;
        }

        this.nextChunkOrderRun = 200;

        loadQueue.clear();
        Long2ObjectOpenHashMap<Boolean> lastChunk = new Long2ObjectOpenHashMap<>(this.usedChunks);

        int centerX = getChunkX();
        int centerZ = getChunkZ();

        int radius = spawned ? this.chunkRadius : (int) Math.ceil(Math.sqrt(spawnThreshold));
        int radiusSqr = radius * radius;



        long index;
        for (int x = 0; x <= radius; x++) {
            int xx = x * x;
            for (int z = 0; z <= x; z++) {
                int distanceSqr = xx + z * z;
                if (distanceSqr > radiusSqr) continue;

                /* Top right quadrant */
                if(this.usedChunks.get(index = Level.chunkHash(centerX + x, centerZ + z)) != Boolean.TRUE) {
                    this.loadQueue.put(index, Boolean.TRUE);
                }
                lastChunk.remove(index);
                /* Top left quadrant */
                if(this.usedChunks.get(index = Level.chunkHash(centerX - x - 1, centerZ + z)) != Boolean.TRUE) {
                    this.loadQueue.put(index, Boolean.TRUE);
                }
                lastChunk.remove(index);
                /* Bottom right quadrant */
                if(this.usedChunks.get(index = Level.chunkHash(centerX + x, centerZ - z - 1)) != Boolean.TRUE) {
                    this.loadQueue.put(index, Boolean.TRUE);
                }
                lastChunk.remove(index);
                /* Bottom left quadrant */
                if(this.usedChunks.get(index = Level.chunkHash(centerX - x - 1, centerZ - z - 1)) != Boolean.TRUE) {
                    this.loadQueue.put(index, Boolean.TRUE);
                }
                lastChunk.remove(index);
                if(x != z){
                    /* Top right quadrant mirror */
                    if(this.usedChunks.get(index = Level.chunkHash(centerX + z, centerZ + x)) != Boolean.TRUE) {
                        this.loadQueue.put(index, Boolean.TRUE);
                    }
                    lastChunk.remove(index);
                    /* Top left quadrant mirror */
                    if(this.usedChunks.get(index = Level.chunkHash(centerX - z - 1, centerZ + x)) != Boolean.TRUE) {
                        this.loadQueue.put(index, Boolean.TRUE);
                    }
                    lastChunk.remove(index);
                    /* Bottom right quadrant mirror */
                    if(this.usedChunks.get(index = Level.chunkHash(centerX + z, centerZ - x - 1)) != Boolean.TRUE) {
                        this.loadQueue.put(index, Boolean.TRUE);
                    }
                    lastChunk.remove(index);
                    /* Bottom left quadrant mirror */
                    if(this.usedChunks.get(index = Level.chunkHash(centerX - z - 1, centerZ - x - 1)) != Boolean.TRUE) {
                        this.loadQueue.put(index, Boolean.TRUE);
                    }
                    lastChunk.remove(index);
                }
            }
        }

        LongIterator keys = lastChunk.keySet().iterator();
        while (keys.hasNext()) {
            index = keys.nextLong();
            this.unloadChunk(Level.getHashX(index), Level.getHashZ(index));
        }

        if (!loadQueue.isEmpty()) {
            NetworkChunkPublisherUpdatePacket packet = new NetworkChunkPublisherUpdatePacket();
            packet.position = position.toInt();
            packet.radius = viewDistance << 4;
            this.dataPacket(packet);
        }
        return true;
    }

    @Deprecated
    public boolean batchDataPacket(DataPacket packet) {
        return this.dataPacket(packet);
    }

    /**
     * 0 is true
     * -1 is false
     * other is identifer
     * @param packet packet to send
     * @return packet successfully sent
     */
    public boolean dataPacket(DataPacket packet) {
        if (!this.connected) {
            return false;
        }

        DataPacketSendEvent ev = new DataPacketSendEvent(this, packet);
        this.server.getPluginManager().callEvent(ev);
        if (ev.isCancelled()) {
            return false;
        }

        if (log.isTraceEnabled() && !server.isIgnoredPacket(packet.getClass())) {
            log.trace("Outbound {}: {}", this.getName(), packet);
        }

        this.networkSession.sendPacket(packet);
        return true;
    }

    @Deprecated
    public int dataPacket(DataPacket packet, boolean needACK) {
        return dataPacket(packet) ? 1 : 0;
    }

    /**
     * 0 is true
     * -1 is false
     * other is identifer
     * @param packet packet to send
     * @return packet successfully sent
     */
    @Deprecated
    public boolean directDataPacket(DataPacket packet) {
        return this.dataPacket(packet);
    }

    @Deprecated
    public int directDataPacket(DataPacket packet, boolean needACK) {
        return this.dataPacket(packet) ? 1 : 0;
    }

    public void forceDataPacket(DataPacket packet, Runnable callback) {
        this.networkSession.sendImmediatePacket(packet, (callback == null ? () -> {} : callback));
    }

    public int getPing() {
        return this.interfaz.getNetworkLatency(this);
    }

    public boolean sleepOn(Vector3i pos) {
        if (!this.isOnline()) {
            return false;
        }

        for (Entity p : world.getNearbyEntities(this.boundingBox.grow(2, 1, 2), this)) {
            if (p instanceof Player pl) {
                if (pl.sleeping != null && pos.toDouble().distance(pl.sleeping) <= 0.1) {
                    return false;
                }
            }
        }

        PlayerBedEnterEvent ev;
        this.server.getPluginManager().callEvent(ev = new PlayerBedEnterEvent(this, world.getBlock(pos)));
        if (ev.isCancelled()) {
            return false;
        }

        this.sleeping = pos.toDouble();
        this.teleport(Point.of(pos.toDouble().add(0.5, 0.5, 0.5), yaw, pitch, headYaw));

        this.setDataProperty(new Vector3iEntityData(DATA_PLAYER_BED_POSITION, pos));
        this.setDataFlag(DATA_PLAYER_FLAGS, DATA_PLAYER_FLAG_SLEEP, true);

        world.sleepTicks = 60;

        this.timeSinceRest = 0;

        return true;
    }

    public void setSpawn(Point point) {
        this.spawnPoint = point;
        var pos = point.getPosition();

        var pk = new SetSpawnPositionPacket();
        pk.spawnType = SetSpawnPositionPacket.TYPE_PLAYER_SPAWN;
        pk.position = pos.toInt();
        dataPacket(pk);
    }

    public void stopSleep() {
        if (this.sleeping != null) {
            this.server.getPluginManager().callEvent(new PlayerBedLeaveEvent(this, world.getBlock(this.sleeping.toInt())));

            this.sleeping = null;
            this.setDataProperty(new Vector3iEntityData(DATA_PLAYER_BED_POSITION, Vector3i.ZERO));
            this.setDataFlag(DATA_PLAYER_FLAGS, DATA_PLAYER_FLAG_SLEEP, false);


            world.sleepTicks = 0;

            AnimatePacket pk = new AnimatePacket();
            pk.eid = getId();
            pk.action = AnimatePacket.Action.WAKE_UP;
            this.dataPacket(pk);
        }
    }

    public int getGamemode() {
        return gamemode;
    }

    /**
     * Returns a client-friendly gamemode of the specified real gamemode
     * This function takes care of handling gamemodes known to MCPE (as of 1.1.0.3, that includes Survival, Creative and Adventure)
     * <p>
     * TODO: remove this when Spectator Mode gets added properly to MCPE
     */
    private static int getClientFriendlyGamemode(int gamemode) {
        gamemode &= 0x03;
        if (gamemode == Player.SPECTATOR) {
            return Player.CREATIVE;
        }
        return gamemode;
    }

    public boolean setGamemode(int gamemode) {
        return this.setGamemode(gamemode, false, null);
    }

    public boolean setGamemode(int gamemode, boolean clientSide) {
        return this.setGamemode(gamemode, clientSide, null);
    }

    public boolean setGamemode(int gamemode, boolean clientSide, AdventureSettings newSettings) {
        if (gamemode < 0 || gamemode > 3 || this.gamemode == gamemode) {
            return false;
        }

        if (newSettings == null) {
            newSettings = this.getAdventureSettings().clone(this);
            newSettings.set(Type.WORLD_IMMUTABLE, (gamemode & 0x02) > 0);
            newSettings.set(Type.MINE, (gamemode & 0x02) <= 0);
            newSettings.set(Type.BUILD, (gamemode & 0x02) <= 0);
            newSettings.set(Type.NO_PVM, gamemode == SPECTATOR);
            newSettings.set(Type.ALLOW_FLIGHT, (gamemode & 0x01) > 0);
            newSettings.set(Type.NO_CLIP, gamemode == SPECTATOR);
            if (gamemode == SPECTATOR) {
                newSettings.set(Type.FLYING, true);
            } else if ((gamemode & 0x1) == 0) {
                newSettings.set(Type.FLYING, false);
            }
        }

        PlayerGameModeChangeEvent ev;
        this.server.getPluginManager().callEvent(ev = new PlayerGameModeChangeEvent(this, gamemode, newSettings));

        if (ev.isCancelled()) {
            return false;
        }

        this.gamemode = gamemode;

        if (this.isSpectator()) {
            this.keepMovement = true;
            this.onGround = false;
            this.despawnFromAll();
        } else {
            this.keepMovement = false;
            this.spawnToAll();
        }

        getNbt().putInt("playerGameType", this.gamemode);

        if (!clientSide) {
            SetPlayerGameTypePacket pk = new SetPlayerGameTypePacket();
            pk.gamemode = getClientFriendlyGamemode(gamemode);
            this.dataPacket(pk);
        }

        this.setAdventureSettings(ev.getNewAdventureSettings());

        if (this.isSpectator()) {
            this.teleport(getPoint());
            this.setDataFlag(DATA_FLAGS, EntityFlags.SILENT, true);
            this.setDataFlag(DATA_FLAGS, EntityFlags.HAS_COLLISION, false);
        } else {
            this.setDataFlag(DATA_FLAGS, EntityFlags.SILENT, false);
            this.setDataFlag(DATA_FLAGS, EntityFlags.HAS_COLLISION, true);
        }

        this.resetFallDistance();

        this.inventory.sendContents(this);
        this.inventory.sendHeldItem(this.hasSpawned.values());
        this.offhandInventory.sendContents(this);
        this.offhandInventory.sendContents(this.getViewers().values());

        this.inventory.sendCreativeContents();
        return true;
    }

    @Deprecated
    public void sendSettings() {
        this.getAdventureSettings().update();
    }

    public boolean isSurvival() {
        return this.gamemode == SURVIVAL;
    }

    public boolean isCreative() {
        return this.gamemode == CREATIVE;
    }

    public boolean isSpectator() {
        return this.gamemode == SPECTATOR;
    }

    public boolean isAdventure() {
        return this.gamemode == ADVENTURE;
    }

    @Override
    public Item[] getDrops() {
        if (!this.isCreative() && !this.isSpectator()) {
            return super.getDrops();
        }

        return new Item[0];
    }

    @Override
    public boolean fastMove(Vector3d delta) {
        if (Vector3d.ZERO.equals(delta)) return true;

        AxisAlignedBB newBB = this.boundingBox.getOffsetBoundingBox(delta);

        if (this.isSpectator() || server.getAllowFlight() || !world.hasCollision(this, newBB.shrink(0, this.getStepHeight(), 0), false)) {
            this.boundingBox = newBB;
        }

        position = new Vector3d(
                (boundingBox.getMinX() + boundingBox.getMaxX()) / 2,
                boundingBox.getMinY() - ySize,
                (boundingBox.getMinZ() + boundingBox.getMaxZ()) / 2
        );

        this.checkChunks();

        if (!this.isSpectator()) {
            if (!this.onGround || delta.y() != 0) {
                AxisAlignedBB bb = this.boundingBox.clone();
                bb.setMinY(bb.getMinY() - 0.75);

                this.onGround = world.getCollisionBlocks(bb).length > 0;
            }
            this.isCollided = this.onGround;
            this.updateFallState(this.onGround);
        }
        return true;
    }

    @Override
    protected void checkGroundState(double movX, double movY, double movZ, double dx, double dy, double dz) {
        if (!this.onGround || movX != 0 || movY != 0 || movZ != 0) {
            boolean onGround = false;

            AxisAlignedBB bb = this.boundingBox.clone();
            bb.setMaxY(bb.getMinY() + 0.5);
            bb.setMinY(bb.getMinY() - 1);

            AxisAlignedBB realBB = this.boundingBox.clone();
            realBB.setMaxY(realBB.getMinY() + 0.1);
            realBB.setMinY(realBB.getMinY() - 0.2);

            int minX = FastMath.floorDouble(bb.getMinX());
            int minY = FastMath.floorDouble(bb.getMinY());
            int minZ = FastMath.floorDouble(bb.getMinZ());
            int maxX = FastMath.ceilDouble(bb.getMaxX());
            int maxY = FastMath.ceilDouble(bb.getMaxY());
            int maxZ = FastMath.ceilDouble(bb.getMaxZ());

            for (int z = minZ; z <= maxZ; ++z) {
                for (int x = minX; x <= maxX; ++x) {
                    for (int y = minY; y <= maxY; ++y) {
                        Block block = world.getBlock(x, y, z);

                        if (!block.canPassThrough() && block.collidesWithBB(realBB)) {
                            onGround = true;
                            break;
                        }
                    }
                }
            }

            this.onGround = onGround;
        }

        this.isCollided = this.onGround;
    }

    @Override
    protected void checkBlockCollision() {
        if (this.isSpectator()) {
            if (this.blocksAround == null) {
                this.blocksAround = new ArrayList<>();
            }
            if (this.collisionBlocks == null) {
                this.collisionBlocks = new ArrayList<>();
            }
            return;
        }

        boolean portal = false;

        for (Block block : this.getCollisionBlocks()) {

            block.onEntityCollide(this);
        }
    }

    protected void checkNearEntities() {
        for (Entity entity : world.getNearbyEntities(this.boundingBox.grow(1, 0.5, 1), this)) {
            entity.scheduleUpdate();

            if (!entity.isAlive() || !this.isAlive()) {
                continue;
            }

            this.pickupEntity(entity, true);
        }
    }

    protected void handleMovement(Vector3d clientPos) {
        if (!this.isAlive() || !this.spawned || this.teleportPosition != null || this.isSleeping()) {
            return;
        }

        boolean invalidMotion = false;
        double distance = clientPos.distanceSquared(position);
        if (!world.isChunkGenerated(clientPos.floorX() >> 4, clientPos.floorZ() >> 4)) {
            invalidMotion = true;
            this.nextChunkOrderRun = 0;
        }

        if (invalidMotion) {
            revertClientMotion(getPoint());
            return;
        }

        Vector3d diffVec = clientPos.sub(position);

        // Client likes to clip into few blocks like stairs or slabs
        // This should help reduce the server mis-prediction at least a bit
        diffVec = diffVec.add(0, ySize * (1 - 0.4D), 0);
        fastMove(diffVec);

        Vector3d corr = position.sub(clientPos);

        double yS = this.getStepHeight() + this.ySize;
        if (corr.y() >= -yS || corr.y() <= yS) {
            corr = new Vector3d(corr.x(), 0, corr.z());
        }

        if (this.checkMovement && (Math.abs(corr.x()) > 0.5 || Math.abs(corr.y()) > 0.5 || Math.abs(corr.z()) > 0.5) &&
                this.riding == null && !this.hasEffect(Effect.LEVITATION) && !this.hasEffect(Effect.SLOW_FALLING)) {

            if (!invalidMotion) {
                position = clientPos;
                double radius = this.getWidth() / 2;
                boundingBox.setBounds(position.sub(radius, radius, radius), position.add(radius, getHeight(), radius));
            }
        }

        if (invalidMotion) {
            revertClientMotion(getPoint());
            return;
        }

        var source = Point.of(lastPosition, lastYaw, lastPitch, lastHeadYaw);
        var target = getPoint();
        var pos = target.getPosition();
        double delta = FastMath.square(lastPosition.x() - pos.x()) +
                FastMath.square(lastPosition.y() - pos.y()) +
                FastMath.square(lastPosition.y() - pos.y());
        double deltaAngle = Math.abs(lastYaw - target.getYaw()) + Math.abs(lastPitch - target.getPitch());

        if (delta > 0.0005 || deltaAngle > 1) {
            boolean isFirst = firstMove;
            firstMove = false;

            lastPosition = target.getPosition();
            lastYaw = target.getYaw();
            lastPitch = target.getPitch();

            if (!isFirst) {
                List<Block> blocksAround = null;
                if (this.blocksAround != null) {
                    blocksAround = new ObjectArrayList<>(this.blocksAround);
                }
                List<Block> collidingBlocks = null;
                if (this.collisionBlocks != null) {
                    collidingBlocks = new ObjectArrayList<>(this.collisionBlocks);
                }

                var event = new PlayerMoveEvent(this, source, target);
                this.blocksAround = null;
                collisionBlocks = null;
                server.getPluginManager().callEvent(event);

                if (!event.isCancelled()) {
                    if (!target.equals(event.getTo())) {
                        teleport(event.getTo());
                    } else {
                        addMovement(position, this.yaw, this.pitch, this.yaw);
                    }
                } else {
                    this.blocksAround = blocksAround;
                    this.collisionBlocks = collidingBlocks;
                }
            }

            speed = source.getPosition().sub(target.getPosition());
        } else {
            speed = Vector3d.ZERO;
        }

        if ((this.isFoodEnabled() || this.getServer().getDifficulty() == 0) && distance >= 0.05) {
            double jump = 0;
            double swimming = this.isInsideOfWater() ? 0.015 * distance : 0;
            double distance2 = distance;
            if (swimming != 0) distance2 = 0;
            if (this.isSprinting()) {
                if (this.inAirTicks == 3 && swimming == 0) {
                    jump = 0.2;
                }
                this.getFoodData().updateFoodExpLevel(0.1 * distance2 + jump + swimming);
            } else {
                if (this.inAirTicks == 3 && swimming == 0) {
                    jump = 0.05;
                }
                this.getFoodData().updateFoodExpLevel(jump + swimming);
            }
        }

        this.forceMovement = null;
        if (distance != 0 && this.nextChunkOrderRun > 20) {
            this.nextChunkOrderRun = 20;
        }
        this.resetClientMovement();
    }

    protected void resetClientMovement() {
        this.newPosition = null;
    }

    protected void revertClientMotion(Point original) {
        lastPosition = original.getPosition();
        lastYaw = original.getYaw();
        lastPitch = original.getPitch();

        Vector3d syncPos = original.getPosition().add(0, 0.00001, 0);
        sendPosition(syncPos, original.getYaw(), original.getPitch(), MovePlayerPacket.MODE_RESET);
        forceMovement = syncPos;

        speed = Vector3d.ZERO;
    }

    @Override
    public double getStepHeight() {
        return 0.6f;
    }

    @Override
    public void addMovement(Vector3d vec, double yaw, double pitch, double headYaw) {
        sendPosition(vec, yaw, pitch, MovePlayerPacket.MODE_NORMAL, this.getViewers().values().toArray(new Player[0]));
    }

    @Override
    public boolean setMotion(Vector3d motion) {
        if (super.setMotion(motion)) {
            if (this.chunk != null) {
                addMotion(this.motion);  //Send to others

                var pk = new SetEntityMotionPacket();
                pk.eid = getId();
                pk.motion = motion.toFloat();
                this.dataPacket(pk);  //Send to self
            }

            if (this.motion.y() > 0) {
                //todo: check this
                this.startAirTicks = (int) ((-(Math.log(this.getGravity() / (this.getGravity() + this.getDrag() * this.motion.y()))) / this.getDrag()) * 2 + 5);
            }

            return true;
        }

        return false;
    }

    public void sendAttributes() {
        UpdateAttributesPacket pk = new UpdateAttributesPacket();
        pk.entityId = this.getId();
        pk.entries = new Attribute.Entry[]{
                Attributes.MAX_HEALTH.withValue(getMaxHealth()),
                Attributes.HUNGER.withValue(getFoodData().getLevel()),
                Attributes.MOVEMENT_SPEED.withValue(getMovementSpeed()),
                Attributes.EXPERIENCE_LEVEL.withValue(getExperienceLevel()),
                Attributes.EXPERIENCE.withValue(((float) this.getExperience()) / calculateRequireExperience(this.getExperienceLevel()))
        };

        dataPacket(pk);
    }

    @Override
    public boolean onUpdate(int currentTick) {
        if (!this.loggedIn) {
            return false;
        }

        int tickDiff = currentTick - this.lastUpdate;

        if (tickDiff <= 0) {
            return true;
        }

        this.messageCounter = 2;

        this.lastUpdate = currentTick;

        if (!this.isAlive() && this.spawned) {
            ++this.deadTicks;
            if (this.deadTicks >= 10) {
                this.despawnFromAll();
            }
            return true;
        }

        if (this.spawned) {
            while (!this.clientMovements.isEmpty()) {
                this.handleMovement(this.clientMovements.poll());
            }

            if (!this.isSpectator()) {
                this.checkNearEntities();
            }

            this.entityBaseTick(tickDiff);

            if (this.getServer().getDifficulty() == 0 && world.getGameRules().getBoolean(GameRule.NATURAL_REGENERATION)) {
                if (this.getHealth() < this.getMaxHealth() && this.ticksLived % 20 == 0) {
                    this.heal(1);
                }

                PlayerFood foodData = this.getFoodData();

                if (foodData.getLevel() < 20 && this.ticksLived % 10 == 0) {
                    foodData.addFoodLevel(1, 0);
                }
            }

            if (this.isOnFire() && this.lastUpdate % 10 == 0) {
                if (this.isCreative() && !this.isInsideOfFire()) {
                    this.extinguish();
                } else if (world.isRaining()) {
                    if (world.canBlockSeeSky(new Vector3(position))) {
                        this.extinguish();
                    }
                }
            }

            if (!this.isSpectator() && this.speed != null) {
                if (this.onGround) {
                    if (this.inAirTicks != 0) {
                        this.startAirTicks = 5;
                    }
                    this.inAirTicks = 0;
                    this.highestPosition = position.y();
                } else {
                    if (this.checkMovement && !this.isGliding() && !server.getAllowFlight() && !this.getAdventureSettings().get(Type.ALLOW_FLIGHT) && this.inAirTicks > 20 && !this.isSleeping() && !this.isImmobile() && !this.isSwimming() && this.riding == null && !this.hasEffect(Effect.LEVITATION) && !this.hasEffect(Effect.SLOW_FALLING)) {
                        double expectedVelocity = (-this.getGravity()) / ((double) this.getDrag()) - ((-this.getGravity()) / ((double) this.getDrag())) * Math.exp(-((double) this.getDrag()) * ((double) (this.inAirTicks - this.startAirTicks)));
                        double diff = (this.speed.y() - expectedVelocity) * (this.speed.y() - expectedVelocity);

                        int block = world.getBlockIdAt(position.floorX(), position.floorY(), position.floorZ());
                        boolean ignore = false;

                        if (!this.hasEffect(Effect.JUMP) && diff > 0.6 && expectedVelocity < this.speed.y() && !ignore) {
                            if (this.inAirTicks < 150) {
                                this.setMotion(new Vector3d(0, expectedVelocity, 0));
                            } else if (this.kick(PlayerKickEvent.Reason.FLYING_DISABLED, "Flying is not enabled on this server")) {
                                return false;
                            }
                        }
                        if (ignore) {
                            this.resetFallDistance();
                        }
                    }

                    if (position.y() > highestPosition) {
                        this.highestPosition = position.y();
                    }

                    if (this.isGliding()) this.resetFallDistance();

                    ++this.inAirTicks;

                }

                if (this.isSurvival() || this.isAdventure()) {
                    if (this.getFoodData() != null) this.getFoodData().update(tickDiff);
                }
            }

            if (!this.isSleeping()) {
                this.timeSinceRest++;
            }
        }

        this.checkTeleportPosition();

        if (this.spawned && this.dummyBossBars.size() > 0 && currentTick % 100 == 0) {
            this.dummyBossBars.values().forEach(DummyBossBar::updateBossEntityPosition);
        }

        return true;
    }

    public void checkNetwork() {
        if (!this.isOnline()) {
            return;
        }

        if (this.nextChunkOrderRun-- <= 0 || this.chunk == null) {
            this.orderChunks();
        }

        if (!this.loadQueue.isEmpty() || !this.spawned) {
            this.sendNextChunk();
        }
    }

    public boolean canInteract(Vector3d pos, double maxDistance) {
        return this.canInteract(pos, maxDistance, 6.0);
    }

    public boolean canInteract(Vector3d pos, double maxDistance, double maxDiff) {
        if (pos.distanceSquared(pos) > maxDistance * maxDistance) {
            return false;
        }

        var dV = this.getDirectionPlane();
        double dot = dV.dot(position.x(), position.z());
        double dot1 = dV.dot(pos.x(), pos.z());
        return (dot1 - dot) >= -maxDiff;
    }

    protected void processLogin() {
        Player oldPlayer = null;
        for (var p: server.getPlayerManager().getPlayers()) {
            if (p != this && p.getName() != null && p.getName().equalsIgnoreCase(this.getName()) ||
                    this.getUniqueId().equals(p.getUniqueId())) {
                oldPlayer = p;
                break;
            }
        }
        CompoundTag nbt;
        if (oldPlayer != null) {
            oldPlayer.saveNBT();
            nbt = oldPlayer.getNbt();
            oldPlayer.remove("", "disconnectionScreen.loggedinOtherLocation");
        } else {
            File legacyDataFile = new File(server.getDataPath() + "players/" + this.username.toLowerCase() + ".dat");
            File dataFile = new File(server.getDataPath() + "players/" + this.playerUuid.toString() + ".dat");
            if (legacyDataFile.exists() && !dataFile.exists()) {
                nbt = this.server.getOfflinePlayerData(this.username, false);

                if (!legacyDataFile.delete()) {
                    log.warn("Could not delete legacy player data for {}", this.username);
                }
            } else {
                nbt = this.server.getOfflinePlayerData(this.playerUuid, true);
            }
        }

        if (nbt == null) {
            this.remove(this.getLeaveMessage(), "Invalid data");
            return;
        }

        if (loginChainData.isXboxAuthed() && server.getPropertyBoolean("xbox-auth") || !server.getPropertyBoolean("xbox-auth")) {
            server.updateName(this.playerUuid, this.username);
        }

        this.playedBefore = (nbt.getLong("lastPlayed") - nbt.getLong("firstPlayed")) > 1;


        nbt.putString("NameTag", this.username);

        int exp = nbt.getInt("EXP");
        int expLevel = nbt.getInt("expLevel");
        this.setExperience(exp, expLevel);

        this.gamemode = nbt.getInt("playerGameType") & 0x03;
        if (this.server.getForceGamemode()) {
            this.gamemode = this.server.getGamemode();
            nbt.putInt("playerGameType", this.gamemode);
        }

        this.adventureSettings = new AdventureSettings(this)
                .set(Type.WORLD_IMMUTABLE, isAdventure() || isSpectator())
                .set(Type.MINE, !isAdventure() && !isSpectator())
                .set(Type.BUILD, !isAdventure() && !isSpectator())
                .set(Type.NO_PVM, this.isSpectator())
                .set(Type.AUTO_JUMP, true)
                .set(Type.ALLOW_FLIGHT, isCreative() || isSpectator())
                .set(Type.NO_CLIP, isSpectator())
                .set(Type.FLYING, isSpectator());

        Level level;
        if ((level = this.server.getLevelByName(nbt.getString("Level"))) == null) {
            world = server.getDefaultLevel();
            nbt.putString("Level", world.getName());
            nbt.getList("Pos", DoubleTag.class)
                    .add(new DoubleTag("0", world.getSpawnPoint().getPosition().x()))
                    .add(new DoubleTag("1", world.getSpawnPoint().getPosition().y()))
                    .add(new DoubleTag("2", world.getSpawnPoint().getPosition().z()));
        } else {
            world = level;
        }

        for (Tag achievement : nbt.getCompound("Achievements").getAllTags()) {
            if (!(achievement instanceof ByteTag)) {
                continue;
            }

            if (((ByteTag) achievement).getData() > 0) {
                this.achievements.add(achievement.getName());
            }
        }

        nbt.putLong("lastPlayed", System.currentTimeMillis() / 1000);

        UUID uuid = getUniqueId();
        nbt.putLong("UUIDLeast", uuid.getLeastSignificantBits());
        nbt.putLong("UUIDMost", uuid.getMostSignificantBits());

        if (this.server.getAutoSave()) {
            this.server.saveOfflinePlayerData(this.playerUuid, nbt, true);
        }

        this.sendPlayStatus(PlayStatusPacket.LOGIN_SUCCESS);
        this.server.onPlayerLogin(this);

        ListTag<DoubleTag> posList = nbt.getList("Pos", DoubleTag.class);

        super.init(world.getChunk((int) posList.get(0).data >> 4, (int) posList.get(2).data >> 4, true), nbt);

        if (!getNbt().contains("foodLevel")) {
            getNbt().putInt("foodLevel", 20);
        }
        int foodLevel = getNbt().getInt("foodLevel");
        if (!getNbt().contains("foodSaturationLevel")) {
            getNbt().putFloat("foodSaturationLevel", 20);
        }
        float foodSaturationLevel = getNbt().getFloat("foodSaturationLevel");
        this.foodData = new PlayerFood(this, foodLevel, foodSaturationLevel);

        if (this.isSpectator()) {
            this.keepMovement = true;
            this.onGround = false;
        }

        this.forceMovement = this.teleportPosition = this.getPosition();

        if (!getNbt().contains("TimeSinceRest")) {
            getNbt().putInt("TimeSinceRest", 0);
        }
        this.timeSinceRest = getNbt().getInt("TimeSinceRest");

        ResourcePacksInfoPacket infoPacket = new ResourcePacksInfoPacket();
        infoPacket.resourcePackEntries = this.server.getResourcePackManager().getResourceStack();
        infoPacket.mustAccept = this.server.getForceResources();
        this.dataPacket(infoPacket);
    }

    protected void completeLoginSequence() {
        PlayerLoginEvent ev;
        this.server.getPluginManager().callEvent(ev = new PlayerLoginEvent(this, "Plugin reason"));
        if (ev.isCancelled()) {
            this.remove(this.getLeaveMessage(), ev.getKickMessage());
            return;
        }

        spawnPoint = this.getSpawn();

        var pk = new StartGamePacket();
        pk.entityUniqueId = getId();
        pk.entityRuntimeId = getId();
        pk.playerGamemode = getClientFriendlyGamemode(this.gamemode);
        pk.position = position.toFloat();
        pk.yaw = (float) this.yaw;
        pk.pitch = (float) this.pitch;
        pk.seed = -1;
        pk.dimension = /*(byte) (this.level.getDimension() & 0xff)*/0;
        pk.worldGamemode = getClientFriendlyGamemode(this.gamemode);
        pk.difficulty = this.server.getDifficulty();
        pk.spawn = spawnPoint.getPosition().toInt();
        pk.hasAchievementsDisabled = true;
        pk.dayCycleStopTime = -1;
        pk.rainLevel = 0;
        pk.lightningLevel = 0;
        pk.commandsEnabled = this.isEnableClientCommand();
        pk.gameRules = world.getGameRules();
        pk.levelId = "";
        pk.worldName = this.getServer().getNetwork().getName();
        pk.generator = 1; //0 old, 1 infinite, 2 flat
        pk.isMovementServerAuthoritative = true;
        dataPacket(pk);

        dataPacket(new BiomeDefinitionListPacket());
        dataPacket(new AvailableEntityIdentifiersPacket());
        inventory.sendCreativeContents();
        getAdventureSettings().update();

        sendAttributes();
        sendPotionEffects(this);

        if (isSpectator()) {
            setDataFlag(DATA_FLAGS, EntityFlags.SILENT, true);
            setDataFlag(DATA_FLAGS, EntityFlags.HAS_COLLISION, false);
        }
        sendData(this);

        loggedIn = true;

        world.sendTime(this);

        sendAttributes();
        setNameTagVisible(true);
        setNameTagAlwaysVisible(true);
        setCanClimb(true);

        this.server.getLogger().info(this.getServer().getLanguage().translateString("nukkit.player.logIn",
                TextFormat.AQUA + this.username + TextFormat.WHITE,
                this.getAddress(),
                String.valueOf(this.getPort()),
                String.valueOf(getId()),
                world.getName(),
                String.valueOf(FastMath.round(position.x(), 4)),
                String.valueOf(FastMath.round(position.y(), 4)),
                String.valueOf(FastMath.round(position.z(), 4))));

        this.server.getPlayerManager().addPlayer(this);
        this.server.onPlayerCompleteLoginSequence(this);
    }

    public void handleDataPacket(DataPacket packet) {
        if (!connected) {
            return;
        }

        if (!verified && packet.pid() != ProtocolInfo.LOGIN_PACKET && packet.pid() != ProtocolInfo.BATCH_PACKET && packet.pid() != ProtocolInfo.REQUEST_NETWORK_SETTINGS_PACKET) {
            server.getLogger().warning("Ignoring " + packet.getClass().getSimpleName() + " from " + getAddress() + " due to player not verified yet");
            if (unverifiedPackets++ > 100) {
                this.remove("", "Too many failed login attempts");
            }
            return;
        }

        DataPacketReceiveEvent ev = new DataPacketReceiveEvent(this, packet);
        this.server.getPluginManager().callEvent(ev);
        if (ev.isCancelled()) {
            return;
        }

        if (packet.pid() == ProtocolInfo.BATCH_PACKET) {
            this.server.getNetwork().processBatch((BatchPacket) packet, this);
            return;
        }

        if (log.isTraceEnabled() && !server.isIgnoredPacket(packet.getClass())) {
            log.trace("Inbound {}: {}", this.getName(), packet);
        }

        BlockFace face;

        packetswitch:
        switch (packet.pid()) {
            case ProtocolInfo.REQUEST_NETWORK_SETTINGS_PACKET:
                if (this.loggedIn) {
                    break;
                }

                int protocolVersion = ((RequestNetworkSettingsPacket) packet).protocolVersion;

                if (!ProtocolInfo.SUPPORTED_PROTOCOLS.contains(protocolVersion)) {
                    String message;
                    if (protocolVersion < ProtocolInfo.CURRENT_PROTOCOL) {
                        message = "disconnectionScreen.outdatedClient";
                    } else {
                        message = "disconnectionScreen.outdatedServer";
                    }
                    this.remove("", message, true);
                    break;
                }

                NetworkSettingsPacket settingsPacket = new NetworkSettingsPacket();
                settingsPacket.compressionAlgorithm = PacketCompressionAlgorithm.ZLIB;
                settingsPacket.compressionThreshold = 1; // compress everything
                this.forceDataPacket(settingsPacket, () -> {
                    this.networkSession.setCompression(CompressionProvider.ZLIB);
                });
                break;
            case ProtocolInfo.LOGIN_PACKET:
                if (this.loggedIn) {
                    break;
                }

                LoginPacket loginPacket = (LoginPacket) packet;
                this.username = TextFormat.clean(loginPacket.username);
                this.displayName = this.username;
                this.iusername = this.username.toLowerCase();

                this.setDataProperty(new StringEntityData(DATA_NAMETAG, this.username), false);

                this.loginChainData = ClientChainData.read(loginPacket);

                if (!loginChainData.isXboxAuthed() && server.getPropertyBoolean("xbox-auth")) {
                    this.remove("", "disconnectionScreen.notAuthenticated");
                    break;
                }

                if (this.server.getPlayerManager().getPlayers().size() >= this.server.getMaxPlayers() && this.kick(PlayerKickEvent.Reason.SERVER_FULL, "disconnectionScreen.serverFull", false)) {
                    break;
                }

                this.randomClientId = loginPacket.clientId;

                this.playerUuid = loginPacket.clientUUID;
                this.rawUUID = Binary.writeUUID(this.playerUuid);

                boolean valid = true;
                int len = loginPacket.username.length();
                if (len > 16 || len < 3) {
                    valid = false;
                }

                for (int i = 0; i < len && valid; i++) {
                    char c = loginPacket.username.charAt(i);
                    if ((c >= 'a' && c <= 'z') ||
                            (c >= 'A' && c <= 'Z') ||
                            (c >= '0' && c <= '9') ||
                            c == '_' || c == ' '
                    ) {
                        continue;
                    }

                    valid = false;
                    break;
                }

                if (!valid || Objects.equals(this.iusername, "rcon") || Objects.equals(this.iusername, "console")) {
                    this.remove("", "disconnectionScreen.invalidName");

                    break;
                }

                if (!loginPacket.skin.isValid()) {
                    this.remove("", "disconnectionScreen.invalidSkin");
                    break;
                } else {
                    this.setSkin(loginPacket.skin);
                }

                PlayerPreLoginEvent playerPreLoginEvent;
                this.server.getPluginManager().callEvent(playerPreLoginEvent = new PlayerPreLoginEvent(this, "Plugin reason"));
                if (playerPreLoginEvent.isCancelled()) {
                    this.remove("", playerPreLoginEvent.getKickMessage());

                    break;
                }

                Player playerInstance = this;
                this.verified = true;

                this.preLoginEventTask = new AsyncTask() {
                    private PlayerAsyncPreLoginEvent event;

                    @Override
                    public void onRun() {
                        this.event = new PlayerAsyncPreLoginEvent(username, playerUuid, loginChainData, playerInstance.getSkin(), playerInstance.getAddress(), playerInstance.getPort());
                        server.getPluginManager().callEvent(this.event);
                    }

                    @Override
                    public void onCompletion(Server server) {
                        if (playerInstance.isRemoved()) {
                            return;
                        }

                        if (this.event.getLoginResult() == LoginResult.KICK) {
                            playerInstance.remove(this.event.getKickMessage(), this.event.getKickMessage());
                        } else if (playerInstance.shouldLogin) {
                            playerInstance.setSkin(this.event.getSkin());
                            playerInstance.completeLoginSequence();
                            for (Consumer<Server> action : this.event.getScheduledActions()) {
                                action.accept(server);
                            }
                        }
                    }
                };

                this.server.getScheduler().scheduleAsyncTask(this.preLoginEventTask);
                this.processLogin();
                break;
            case ProtocolInfo.RESOURCE_PACK_CLIENT_RESPONSE_PACKET:
                ResourcePackClientResponsePacket responsePacket = (ResourcePackClientResponsePacket) packet;
                switch (responsePacket.responseStatus) {
                    case ResourcePackClientResponsePacket.STATUS_REFUSED:
                        this.remove("", "disconnectionScreen.noReason");
                        break;
                    case ResourcePackClientResponsePacket.STATUS_SEND_PACKS:
                        for (ResourcePackClientResponsePacket.Entry entry : responsePacket.packEntries) {
                            ResourcePack resourcePack = this.server.getResourcePackManager().getPackById(entry.uuid);
                            if (resourcePack == null) {
                                this.remove("", "disconnectionScreen.resourcePack");
                                break;
                            }

                            ResourcePackDataInfoPacket dataInfoPacket = new ResourcePackDataInfoPacket();
                            dataInfoPacket.packId = resourcePack.getPackId();
                            dataInfoPacket.maxChunkSize = RESOURCE_PACK_CHUNK_SIZE;
                            dataInfoPacket.chunkCount = FastMath.ceil(resourcePack.getPackSize() / (float) RESOURCE_PACK_CHUNK_SIZE);
                            dataInfoPacket.compressedPackSize = resourcePack.getPackSize();
                            dataInfoPacket.sha256 = resourcePack.getSha256();
                            this.dataPacket(dataInfoPacket);
                        }
                        break;
                    case ResourcePackClientResponsePacket.STATUS_HAVE_ALL_PACKS:
                        ResourcePackStackPacket stackPacket = new ResourcePackStackPacket();
                        stackPacket.mustAccept = this.server.getForceResources();
                        stackPacket.resourcePackStack = this.server.getResourcePackManager().getResourceStack();
                        this.dataPacket(stackPacket);
                        break;
                    case ResourcePackClientResponsePacket.STATUS_COMPLETED:
                        this.shouldLogin = true;

                        if (this.preLoginEventTask.isFinished()) {
                            this.preLoginEventTask.onCompletion(server);
                        }
                        break;
                }
                break;
            case ProtocolInfo.RESOURCE_PACK_CHUNK_REQUEST_PACKET:
                ResourcePackChunkRequestPacket requestPacket = (ResourcePackChunkRequestPacket) packet;
                ResourcePack resourcePack = this.server.getResourcePackManager().getPackById(requestPacket.packId);
                if (resourcePack == null) {
                    this.remove("", "disconnectionScreen.resourcePack");
                    break;
                }

                ResourcePackChunkDataPacket dataPacket = new ResourcePackChunkDataPacket();
                dataPacket.packId = resourcePack.getPackId();
                dataPacket.chunkIndex = requestPacket.chunkIndex;
                dataPacket.data = resourcePack.getPackChunk(RESOURCE_PACK_CHUNK_SIZE * requestPacket.chunkIndex, RESOURCE_PACK_CHUNK_SIZE);
                dataPacket.progress = (long) RESOURCE_PACK_CHUNK_SIZE * requestPacket.chunkIndex;
                this.dataPacket(dataPacket);
                break;
            case ProtocolInfo.SET_LOCAL_PLAYER_AS_INITIALIZED_PACKET:
                if (this.locallyInitialized) {
                    break;
                }
                this.locallyInitialized = true;
                PlayerLocallyInitializedEvent locallyInitializedEvent = new PlayerLocallyInitializedEvent(this);
                this.server.getPluginManager().callEvent(locallyInitializedEvent);
                break;
            case ProtocolInfo.PLAYER_SKIN_PACKET:
                PlayerSkinPacket skinPacket = (PlayerSkinPacket) packet;
                Skin skin = skinPacket.skin;

                if (!skin.isValid()) {
                    this.getServer().getLogger().debug(username + ": PlayerSkinPacket with invalid skin");
                    break;
                }

                PlayerChangeSkinEvent playerChangeSkinEvent = new PlayerChangeSkinEvent(this, skin);
                playerChangeSkinEvent.setCancelled(TimeUnit.SECONDS.toMillis(this.server.getPlayerSkinChangeCooldown()) > System.currentTimeMillis() - this.lastSkinChange);
                this.server.getPluginManager().callEvent(playerChangeSkinEvent);
                if (!playerChangeSkinEvent.isCancelled()) {
                    this.lastSkinChange = System.currentTimeMillis();
                    this.setSkin(skin);
                }

                break;
            case ProtocolInfo.PACKET_VIOLATION_WARNING_PACKET:
                log.warn("Violation warning from {}: {}", this.getName(), packet.toString());
                break;
            case ProtocolInfo.EMOTE_PACKET:
                if (!this.spawned) {
                    return;
                }
                EmotePacket emotePacket = (EmotePacket) packet;
                if (emotePacket.runtimeId != getId()) {
                    server.getLogger().warning(this.username + " sent EmotePacket with invalid entity id: " + emotePacket.runtimeId + " != " + getId());
                    return;
                }
                for (Player viewer : this.getViewers().values()) {
                    viewer.dataPacket(emotePacket);
                }
                return;
            case ProtocolInfo.PLAYER_INPUT_PACKET:
                if (!this.isAlive() || !this.spawned) {
                    break;
                }
                PlayerInputPacket ipk = (PlayerInputPacket) packet;
                break;
            case ProtocolInfo.MOVE_PLAYER_PACKET:
                if (this.teleportPosition != null) {
                    break;
                }

                MovePlayerPacket movePlayerPacket = (MovePlayerPacket) packet;
                Vector3d newPos = movePlayerPacket.position.sub(0, getEyeHeight(), 0).toDouble();

                double dis = newPos.distanceSquared(position);
                if (dis == 0 && movePlayerPacket.yaw % 360 == this.yaw && movePlayerPacket.pitch % 360 == this.pitch) {
                    break;
                }

                if (dis > 100) {
                    this.sendPosition(position, movePlayerPacket.yaw, movePlayerPacket.pitch, MovePlayerPacket.MODE_RESET);
                    break;
                }

                boolean revert = false;
                if (!this.isAlive() || !this.spawned) {
                    revert = true;
                    this.forceMovement = position;
                }

                if (this.forceMovement != null && (newPos.distanceSquared(this.forceMovement) > 0.1 || revert)) {
                    this.sendPosition(this.forceMovement, movePlayerPacket.yaw, movePlayerPacket.pitch, MovePlayerPacket.MODE_RESET);
                } else {

                    movePlayerPacket.yaw %= 360;
                    movePlayerPacket.pitch %= 360;

                    if (movePlayerPacket.yaw < 0) {
                        movePlayerPacket.yaw += 360;
                    }

                    this.setRotation(movePlayerPacket.yaw, movePlayerPacket.pitch);
                    this.newPosition = newPos;
                    this.forceMovement = null;
                }
                break;
            case ProtocolInfo.PLAYER_AUTH_INPUT_PACKET:
                PlayerAuthInputPacket authPacket = (PlayerAuthInputPacket) packet;

                if (!authPacket.blockActionData.isEmpty()) {
                    for (PlayerBlockActionData action : authPacket.blockActionData.values()) {
                        var blockPos = action.getPosition();
                        var blockFace = BlockFace.fromIndex(action.getFacing());

                        if (this.lastBlockAction != null && this.lastBlockAction.getAction() == PlayerActionType.PREDICT_DESTROY_BLOCK &&
                                action.getAction() == PlayerActionType.CONTINUE_DESTROY_BLOCK) {
                            this.onBlockBreakStart(blockPos, blockFace);
                        }

                        var lastBreakPos = this.lastBlockAction == null ? null : this.lastBlockAction.getPosition();
                        if (lastBreakPos != null && (lastBreakPos.x() != blockPos.x() ||
                                lastBreakPos.y() != blockPos.y() || lastBreakPos.z() != blockPos.z())) {
                            this.onBlockBreakAbort(lastBreakPos, BlockFace.DOWN);
                            this.onBlockBreakStart(blockPos, blockFace);
                        }

                        switch (action.getAction()) {
                            case START_DESTROY_BLOCK -> onBlockBreakStart(blockPos, blockFace);
                            case ABORT_DESTROY_BLOCK, STOP_DESTROY_BLOCK -> onBlockBreakAbort(blockPos, blockFace);
                            case CONTINUE_DESTROY_BLOCK -> onBlockBreakContinue(blockPos, blockFace);
                            case PREDICT_DESTROY_BLOCK -> {

                                onBlockBreakAbort(blockPos, blockFace);
                                onBlockBreakComplete(blockPos , blockFace);
                            }
                        }
                        this.lastBlockAction = action;
                    }
                }

                if (this.teleportPosition != null) {
                    break;
                }

                if (authPacket.inputData.contains(AuthInputAction.START_SPRINTING)) {
                    PlayerToggleSprintEvent event = new PlayerToggleSprintEvent(this, true);
                    this.server.getPluginManager().callEvent(event);
                    if (event.isCancelled()) {
                        this.sendData(this);
                    } else {
                        this.setSprinting(true);
                    }
                }

                if (authPacket.inputData.contains(AuthInputAction.STOP_SPRINTING)) {
                    PlayerToggleSprintEvent event = new PlayerToggleSprintEvent(this, false);
                    this.server.getPluginManager().callEvent(event);
                    if (event.isCancelled()) {
                        this.sendData(this);
                    } else {
                        this.setSprinting(false);
                    }
                }

                if (authPacket.inputData.contains(AuthInputAction.START_SNEAKING)) {
                    PlayerToggleSneakEvent event = new PlayerToggleSneakEvent(this, true);
                    this.server.getPluginManager().callEvent(event);
                    if (event.isCancelled()) {
                        this.sendData(this);
                    } else {
                        this.setSneaking(true);
                    }
                }

                if (authPacket.inputData.contains(AuthInputAction.STOP_SNEAKING)) {
                    PlayerToggleSneakEvent event = new PlayerToggleSneakEvent(this, false);
                    this.server.getPluginManager().callEvent(event);
                    if (event.isCancelled()) {
                        this.sendData(this);
                    } else {
                        this.setSneaking(false);
                    }
                }

                if (authPacket.inputData.contains(AuthInputAction.START_JUMPING)) {
                    PlayerJumpEvent playerJumpEvent = new PlayerJumpEvent(this);
                    this.server.getPluginManager().callEvent(playerJumpEvent);
                }

                if (authPacket.inputData.contains(AuthInputAction.START_GLIDING)) {
                    PlayerToggleGlideEvent playerToggleGlideEvent = new PlayerToggleGlideEvent(this, true);
                    this.server.getPluginManager().callEvent(playerToggleGlideEvent);
                    if (playerToggleGlideEvent.isCancelled()) {
                        this.sendData(this);
                    } else {
                        this.setGliding(true);
                    }
                }

                if (authPacket.inputData.contains(AuthInputAction.STOP_GLIDING)) {
                    PlayerToggleGlideEvent playerToggleGlideEvent = new PlayerToggleGlideEvent(this, false);
                    this.server.getPluginManager().callEvent(playerToggleGlideEvent);
                    if (playerToggleGlideEvent.isCancelled()) {
                        this.sendData(this);
                    } else {
                        this.setGliding(false);
                    }
                }

                if (authPacket.inputData.contains(AuthInputAction.START_SWIMMING)) {
                    PlayerToggleSwimEvent ptse = new PlayerToggleSwimEvent(this, true);
                    this.server.getPluginManager().callEvent(ptse);
                    if (ptse.isCancelled()) {
                        this.sendData(this);
                    } else {
                        this.setSwimming(true);
                    }
                }

                if (authPacket.inputData.contains(AuthInputAction.STOP_SWIMMING)) {
                    PlayerToggleSwimEvent ptse = new PlayerToggleSwimEvent(this, false);
                    this.server.getPluginManager().callEvent(ptse);
                    if (ptse.isCancelled()) {
                        this.sendData(this);
                    } else {
                        this.setSwimming(false);
                    }
                }

                Vector3d clientPosition = authPacket.position.toDouble().sub(0, this.getEyeHeight(), 0);

                double distSqrt = clientPosition.distanceSquared(position);
                if (distSqrt == 0.0 && authPacket.yaw % 360 == this.yaw && authPacket.pitch % 360 == this.pitch) {
                    break;
                }

                if (distSqrt > 100) {
                    this.sendPosition(position, authPacket.yaw, authPacket.pitch, MovePlayerPacket.MODE_RESET);
                    break;
                }

                boolean revertMotion = false;
                if (!this.isAlive() || !this.spawned) {
                    revertMotion = true;
                    this.forceMovement = position;
                }

                if (this.forceMovement != null && (clientPosition.distanceSquared(this.forceMovement) > 0.1 || revertMotion)) {
                    this.sendPosition(this.forceMovement, authPacket.yaw, authPacket.pitch, MovePlayerPacket.MODE_RESET);
                } else {
                    float yaw = authPacket.yaw % 360;
                    float pitch = authPacket.pitch % 360;
                    if (yaw < 0) {
                        yaw += 360;
                    }

                    this.setRotation(yaw, pitch);
                    this.newPosition = clientPosition;
                    this.clientMovements.offer(clientPosition);
                    this.forceMovement = null;
                }
                break;
            case ProtocolInfo.MOVE_ENTITY_ABSOLUTE_PACKET:
                MoveEntityAbsolutePacket moveEntityAbsolutePacket = (MoveEntityAbsolutePacket) packet;
                if (this.riding == null || this.riding.getId() != moveEntityAbsolutePacket.eid || !this.riding.isControlling(this)) {
                    break;
                }
                break;
            case ProtocolInfo.REQUEST_ABILITY_PACKET:
                RequestAbilityPacket abilityPacket = (RequestAbilityPacket) packet;

                PlayerAbility ability = abilityPacket.getAbility();
                if (ability != PlayerAbility.FLYING) {
                    this.server.getLogger().info("[" + this.getName() + "] has tried to trigger " + ability + " ability " + (abilityPacket.isBoolValue() ? "on" : "off"));
                    return;
                }

                if (!server.getAllowFlight() && abilityPacket.isBoolValue() && !this.getAdventureSettings().get(Type.ALLOW_FLIGHT)) {
                    this.kick(PlayerKickEvent.Reason.FLYING_DISABLED, "Flying is not enabled on this server");
                    break;
                }

                PlayerToggleFlightEvent playerToggleFlightEvent = new PlayerToggleFlightEvent(this, abilityPacket.isBoolValue());
                if (this.isSpectator()) {
                    playerToggleFlightEvent.setCancelled();
                }
                this.server.getPluginManager().callEvent(playerToggleFlightEvent);
                if (playerToggleFlightEvent.isCancelled()) {
                    this.getAdventureSettings().update();
                } else {
                    this.getAdventureSettings().set(Type.FLYING, playerToggleFlightEvent.isFlying());
                }
                break;
            case ProtocolInfo.MOB_EQUIPMENT_PACKET:
                if (!this.spawned || !this.isAlive()) {
                    break;
                }

                MobEquipmentPacket mobEquipmentPacket = (MobEquipmentPacket) packet;

                Inventory inv = this.getWindowById(mobEquipmentPacket.windowId);

                if (inv == null) {
                    this.server.getLogger().debug(this.getName() + " has no open container with window ID " + mobEquipmentPacket.windowId);
                    return;
                }

                Item item = inv.getItem(mobEquipmentPacket.hotbarSlot);

                if (!item.equals(mobEquipmentPacket.item)) {
                    this.server.getLogger().debug(this.getName() + " tried to equip " + mobEquipmentPacket.item + " but have " + item + " in target slot");
                    inv.sendContents(this);
                    return;
                }

                if (inv instanceof PlayerInventory) {
                    ((PlayerInventory) inv).equipItem(mobEquipmentPacket.hotbarSlot);
                }

                this.setDataFlag(Player.DATA_FLAGS, EntityFlags.ACTION, false);

                break;
            case ProtocolInfo.PLAYER_ACTION_PACKET:
                PlayerActionPacket playerActionPacket = (PlayerActionPacket) packet;
                if (!this.spawned || !this.isAlive() && playerActionPacket.action != PlayerActionPacket.ACTION_RESPAWN) {
                    break;
                }

                switch (playerActionPacket.action) {
                    case PlayerActionPacket.ACTION_STOP_SLEEPING:
                        this.stopSleep();
                        break;
                    case PlayerActionPacket.ACTION_RESPAWN:
                        if (!this.spawned || this.isAlive() || !this.isOnline()) {
                            break;
                        }
                        this.respawn();
                        break;
                    case PlayerActionPacket.ACTION_DIMENSION_CHANGE_ACK:
                        this.sendPosition(position, this.yaw, this.pitch, MovePlayerPacket.MODE_RESET);
                        break;
                }

                this.setUsingItem(false);
                break;
            case ProtocolInfo.MOB_ARMOR_EQUIPMENT_PACKET:
                break;
            case ProtocolInfo.MODAL_FORM_RESPONSE_PACKET:
                if (!this.spawned || !this.isAlive()) {
                    break;
                }

                ModalFormResponsePacket modalFormPacket = (ModalFormResponsePacket) packet;

                if (formWindows.containsKey(modalFormPacket.formId)) {
                    FormWindow window = formWindows.remove(modalFormPacket.formId);
                    window.setResponse(modalFormPacket.data.trim());

                    for (FormResponseHandler handler : window.getHandlers()) {
                        handler.handle(this, modalFormPacket.formId);
                    }

                    PlayerFormRespondedEvent event = new PlayerFormRespondedEvent(this, modalFormPacket.formId, window);
                    getServer().getPluginManager().callEvent(event);
                } else if (serverSettings.containsKey(modalFormPacket.formId)) {
                    FormWindow window = serverSettings.get(modalFormPacket.formId);
                    window.setResponse(modalFormPacket.data.trim());

                    for (FormResponseHandler handler : window.getHandlers()) {
                        handler.handle(this, modalFormPacket.formId);
                    }

                    PlayerSettingsRespondedEvent event = new PlayerSettingsRespondedEvent(this, modalFormPacket.formId, window);
                    getServer().getPluginManager().callEvent(event);

                    //Set back new settings if not been cancelled
                    if (!event.isCancelled() && window instanceof FormWindowCustom)
                        ((FormWindowCustom) window).setElementsFromResponse();
                }

                break;

            case ProtocolInfo.INTERACT_PACKET:
                if (!this.spawned || !this.isAlive()) {
                    break;
                }

                InteractPacket interactPacket = (InteractPacket) packet;

                if (interactPacket.target == 0 && interactPacket.action == InteractPacket.ACTION_MOUSEOVER) {
                    this.setButtonText("");
                    break;
                }

                Entity targetEntity = interactPacket.target == this.getId() ? this : world.getEntity(interactPacket.target);

                if (targetEntity == null || !this.isAlive() || !targetEntity.isAlive()) {
                    break;
                }

                if (targetEntity instanceof EntityItem) {
                    this.kick(PlayerKickEvent.Reason.INVALID_PVE, "Attempting to interact with an invalid entity");
                    this.server.getLogger().warning(this.getServer().getLanguage().translateString("nukkit.player.invalidEntity", this.getName()));
                    break;
                }

                switch (interactPacket.action) {
                    case InteractPacket.ACTION_MOUSEOVER:
                        String buttonText = "";
                        this.setButtonText(buttonText);

                        this.getServer().getPluginManager().callEvent(new PlayerMouseOverEntityEvent(this, targetEntity));
                        break;
                    case InteractPacket.ACTION_VEHICLE_EXIT:
                        break;
                    case InteractPacket.ACTION_OPEN_INVENTORY:
                        if (targetEntity != this) {
                            break;
                        }
                        if (!this.inventoryOpen) {
                            this.inventory.open(this);
                            this.inventoryOpen = true;
                        }
                        break;
                }
                break;
            case ProtocolInfo.BLOCK_PICK_REQUEST_PACKET:
                var pickRequestPacket = (BlockPickRequestPacket) packet;
                var pos = pickRequestPacket.position;
                Block block = world.getBlock(pos.x(), pos.y(), pos.z(), false);
                if (block.getPosition().toDouble().distanceSquared(position) > 1000) {
                    this.getServer().getLogger().debug(username + ": Block pick request for a block too far away");
                    return;
                }
                item = block.toItem();

                if (pickRequestPacket.addUserData) {
                    var blockEntity = world.getBlockEntity(pickRequestPacket.position);
                    if (blockEntity != null) {
                        CompoundTag nbt = blockEntity.getSaveData()
                                .remove("id")
                                .remove("x")
                                .remove("y")
                                .remove("z");

                        item.setCustomBlockData(nbt);
                        item.setLore("+(DATA)");
                    }
                }

                PlayerBlockPickEvent pickEvent = new PlayerBlockPickEvent(this, block, item);
                if (this.isSpectator()) {
                    log.debug("Got block-pick request from " + this.getName() + " when in spectator mode");
                    pickEvent.setCancelled();
                }

                this.server.getPluginManager().callEvent(pickEvent);

                if (!pickEvent.isCancelled()) {
                    boolean itemExists = false;
                    int itemSlot = -1;
                    for (int slot = 0; slot < this.inventory.getSize(); slot++) {
                        if (this.inventory.getItem(slot).equals(pickEvent.getItem())) {
                            if (slot < this.inventory.getHotbarSize()) {
                                this.inventory.setHeldItemSlot(slot);
                            } else {
                                itemSlot = slot;
                            }
                            itemExists = true;
                            break;
                        }
                    }

                    for (int slot = 0; slot < this.inventory.getHotbarSize(); slot++) {
                        if (this.inventory.getItem(slot).isNull()) {
                            if (!itemExists && this.isCreative()) {
                                this.inventory.setHeldItemSlot(slot);
                                this.inventory.setItemInHand(pickEvent.getItem());
                                break packetswitch;
                            } else if (itemSlot > -1) {
                                this.inventory.setHeldItemSlot(slot);
                                this.inventory.setItemInHand(this.inventory.getItem(itemSlot));
                                this.inventory.clear(itemSlot, true);
                                break packetswitch;
                            }
                        }
                    }

                    if (!itemExists && this.isCreative()) {
                        Item itemInHand = this.inventory.getItemInHand();
                        this.inventory.setItemInHand(pickEvent.getItem());
                        if (!this.inventory.isFull()) {
                            for (int slot = 0; slot < this.inventory.getSize(); slot++) {
                                if (this.inventory.getItem(slot).isNull()) {
                                    this.inventory.setItem(slot, itemInHand);
                                    break;
                                }
                            }
                        }
                    } else if (itemSlot > -1) {
                        Item itemInHand = this.inventory.getItemInHand();
                        this.inventory.setItemInHand(this.inventory.getItem(itemSlot));
                        this.inventory.setItem(itemSlot, itemInHand);
                    }
                }
                break;
            case ProtocolInfo.ANIMATE_PACKET:
                if (!this.spawned || !this.isAlive()) {
                    break;
                }

                AnimatePacket animatePacket = (AnimatePacket) packet;
                PlayerAnimationEvent animationEvent = new PlayerAnimationEvent(this, animatePacket.action);

                // prevent client send illegal packet to server and broadcast to other client and make other client crash
                if (animatePacket.action == null // illegal action id
                        || animatePacket.action == AnimatePacket.Action.WAKE_UP // these actions are only for server to client
                        || animatePacket.action == AnimatePacket.Action.CRITICAL_HIT
                        || animatePacket.action == AnimatePacket.Action.MAGIC_CRITICAL_HIT) {
                    break; // maybe we should cancel the event here? but if client send too many packets, server will lag
                }

                this.server.getPluginManager().callEvent(animationEvent);
                if (animationEvent.isCancelled()) {
                    break;
                }

                AnimatePacket.Action animation = animationEvent.getAnimationType();

                switch (animation) {
                    case ROW_RIGHT:
                    case ROW_LEFT:
                        break;
                }

                animatePacket.eid = this.getId();
                animatePacket.action = animationEvent.getAnimationType();
                Server.broadcastPacket(this.getViewers().values(), animatePacket);
                break;
            case ProtocolInfo.SET_HEALTH_PACKET:
                //use UpdateAttributePacket instead
                break;

            case ProtocolInfo.ENTITY_EVENT_PACKET:
                if (!this.spawned || !this.isAlive()) {
                    break;
                }
                EntityEventPacket entityEventPacket = (EntityEventPacket) packet;
                if (entityEventPacket.event != EntityEventPacket.ENCHANT)
                    this.craftingType = CRAFTING_SMALL;
                //this.resetCraftingGridType();


                if (entityEventPacket.event == EntityEventPacket.EATING_ITEM) {
                    if (entityEventPacket.data == 0 || entityEventPacket.eid != getId()) {
                        break;
                    }

                    entityEventPacket.eid = getId();
                    entityEventPacket.isEncoded = false;

                    this.dataPacket(entityEventPacket);
                    Server.broadcastPacket(this.getViewers().values(), entityEventPacket);
                } else if (entityEventPacket.event == EntityEventPacket.ENCHANT) {
                    if (entityEventPacket.eid != getId()) {
                        break;
                    }

                    Inventory inventory = this.getWindowById(ANVIL_WINDOW_ID);
                    if (inventory instanceof AnvilInventory) {
                        ((AnvilInventory) inventory).setCost(-entityEventPacket.data);
                    }
                }
                break;
            case ProtocolInfo.COMMAND_REQUEST_PACKET:
                if (!this.spawned || !this.isAlive()) {
                    break;
                }
                this.craftingType = CRAFTING_SMALL;
                CommandRequestPacket commandRequestPacket = (CommandRequestPacket) packet;
                PlayerCommandPreprocessEvent playerCommandPreprocessEvent = new PlayerCommandPreprocessEvent(this, commandRequestPacket.command);
                this.server.getPluginManager().callEvent(playerCommandPreprocessEvent);
                if (playerCommandPreprocessEvent.isCancelled()) {
                    break;
                }
                break;
            case ProtocolInfo.TEXT_PACKET:
                if (!this.spawned || !this.isAlive()) {
                    break;
                }

                TextPacket textPacket = (TextPacket) packet;

                if (textPacket.type == TextPacket.TYPE_CHAT) {
                    String chatMessage = textPacket.message;
                    int breakLine = chatMessage.indexOf('\n');
                    // Chat messages shouldn't contain break lines so ignore text afterwards
                    if (breakLine != -1) {
                        chatMessage = chatMessage.substring(0, breakLine);
                    }
                    this.chat(chatMessage);
                }
                break;
            case ProtocolInfo.CONTAINER_CLOSE_PACKET:
                ContainerClosePacket containerClosePacket = (ContainerClosePacket) packet;
                if (!this.spawned || containerClosePacket.windowId == ContainerIds.INVENTORY && !inventoryOpen) {
                    break;
                }

                if (this.windowIndex.containsKey(containerClosePacket.windowId)) {
                    this.server.getPluginManager().callEvent(new InventoryCloseEvent(this.windowIndex.get(containerClosePacket.windowId), this));
                    if (containerClosePacket.windowId == ContainerIds.INVENTORY) this.inventoryOpen = false;
                    this.closingWindowId = containerClosePacket.windowId;
                    this.removeWindow(this.windowIndex.get(containerClosePacket.windowId), true);
                    this.closingWindowId = Integer.MIN_VALUE;
                }
                if (containerClosePacket.windowId == -1) {
                    this.craftingType = CRAFTING_SMALL;
                    this.resetCraftingGridType();
                    this.addWindow(this.craftingGrid, ContainerIds.NONE);
                    ContainerClosePacket pk = new ContainerClosePacket();
                    pk.wasServerInitiated = false;
                    pk.windowId = -1;
                    this.dataPacket(pk);
                }
                break;
            case ProtocolInfo.CRAFTING_EVENT_PACKET:
                break;
            case ProtocolInfo.BLOCK_ENTITY_DATA_PACKET:
                if (!this.spawned || !this.isAlive()) {
                    break;
                }

                BlockEntityDataPacket blockEntityDataPacket = (BlockEntityDataPacket) packet;
                this.craftingType = CRAFTING_SMALL;
                this.resetCraftingGridType();

                var vec = blockEntityDataPacket.position;
                if (vec.toDouble().distanceSquared(position) > 10000) {
                    break;
                }

                var t = world.getBlockEntity(vec);
                if (t.getType() instanceof SpawnableBlockEntityType type) {
                    CompoundTag nbt;
                    try {
                        nbt = NBTIO.read(blockEntityDataPacket.nbt, ByteOrder.LITTLE_ENDIAN, true);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    if (!type.tryUpdateNbtByPlayer(this, nbt)) {
                        type.spawnTo(this);
                    }
                }
                break;
            case ProtocolInfo.REQUEST_CHUNK_RADIUS_PACKET:
                RequestChunkRadiusPacket requestChunkRadiusPacket = (RequestChunkRadiusPacket) packet;
                ChunkRadiusUpdatedPacket chunkRadiusUpdatePacket = new ChunkRadiusUpdatedPacket();
                this.chunkRadius = Math.max(3, Math.min(requestChunkRadiusPacket.radius, this.viewDistance));
                chunkRadiusUpdatePacket.radius = this.chunkRadius;
                this.dataPacket(chunkRadiusUpdatePacket);
                break;
            case ProtocolInfo.SET_PLAYER_GAME_TYPE_PACKET:
                SetPlayerGameTypePacket setPlayerGameTypePacket = (SetPlayerGameTypePacket) packet;
                if (setPlayerGameTypePacket.gamemode != this.gamemode) {
                    this.setGamemode(setPlayerGameTypePacket.gamemode, true);
                }
                break;
            case ProtocolInfo.ITEM_FRAME_DROP_ITEM_PACKET:
                ItemFrameDropItemPacket itemFrameDropItemPacket = (ItemFrameDropItemPacket) packet;
                var vector3 = itemFrameDropItemPacket.position;
                if (vector3.toDouble().distanceSquared(position) < 1000) {
                    world.getBlockEntity(vector3);
                }
                break;
            case ProtocolInfo.MAP_INFO_REQUEST_PACKET:
                break;
            case ProtocolInfo.LEVEL_SOUND_EVENT_PACKET_V1:
            case ProtocolInfo.LEVEL_SOUND_EVENT_PACKET_V2:
            case ProtocolInfo.LEVEL_SOUND_EVENT_PACKET:
                if (!this.isSpectator()) {
                    world.addChunkPacket(this.getChunkX(), this.getChunkZ(), packet);
                }
                break;
            case ProtocolInfo.INVENTORY_TRANSACTION_PACKET:
                if (this.isSpectator()) {
                    this.sendAllInventories();
                    break;
                }

                InventoryTransactionPacket transactionPacket = (InventoryTransactionPacket) packet;

                List<InventoryAction> actions = new ArrayList<>();
                for (NetworkInventoryAction networkInventoryAction : transactionPacket.actions) {
                    InventoryAction a = networkInventoryAction.createInventoryAction(this);

                    if (a == null) {
                        this.getServer().getLogger().debug("Unmatched inventory action from " + this.getName() + ": " + networkInventoryAction);
                        this.sendAllInventories();
                        break packetswitch;
                    }

                    actions.add(a);
                }

                if (transactionPacket.isCraftingPart) {
                    if (this.craftingTransaction == null) {
                        this.craftingTransaction = new CraftingTransaction(this, actions);
                    } else {
                        for (InventoryAction action : actions) {
                            this.craftingTransaction.addAction(action);
                        }
                    }

                    if (this.craftingTransaction.getPrimaryOutput() != null && this.craftingTransaction.canExecute()) {
                        //we get the actions for this in several packets, so we can't execute it until we get the result

                        this.craftingTransaction.execute();
                        this.craftingTransaction = null;
                    }

                    return;
                } else if (transactionPacket.isEnchantingPart) {
                    if (this.enchantTransaction == null) {
                        this.enchantTransaction = new EnchantTransaction(this, actions);
                    } else {
                        for (InventoryAction action : actions) {
                            this.enchantTransaction.addAction(action);
                        }
                    }
                    if (this.enchantTransaction.canExecute()) {
                        this.enchantTransaction.execute();
                        this.enchantTransaction = null;
                    }
                    return;
                } else if (transactionPacket.isRepairItemPart) {
                    if (this.repairItemTransaction == null) {
                        this.repairItemTransaction = new RepairItemTransaction(this, actions);
                    } else {
                        for (InventoryAction action : actions) {
                            this.repairItemTransaction.addAction(action);
                        }
                    }
                    if (this.repairItemTransaction.canExecute()) {
                        this.repairItemTransaction.execute();
                        this.repairItemTransaction = null;
                    }
                    return;
                } else if (this.craftingTransaction != null) {
                    if (craftingTransaction.checkForCraftingPart(actions)) {
                        for (InventoryAction action : actions) {
                            craftingTransaction.addAction(action);
                        }
                        return;
                    } else {
                        this.server.getLogger().debug("Got unexpected normal inventory action with incomplete crafting transaction from " + this.getName() + ", refusing to execute crafting");
                        this.removeAllWindows(false);
                        this.sendAllInventories();
                        this.craftingTransaction = null;
                    }
                } else if (this.enchantTransaction != null) {
                    if (enchantTransaction.checkForEnchantPart(actions)) {
                        for (InventoryAction action : actions) {
                            enchantTransaction.addAction(action);
                        }
                        return;
                    } else {
                        this.server.getLogger().debug("Got unexpected normal inventory action with incomplete enchanting transaction from " + this.getName() + ", refusing to execute enchant " + transactionPacket.toString());
                        this.removeAllWindows(false);
                        this.sendAllInventories();
                        this.enchantTransaction = null;
                    }
                } else if (this.repairItemTransaction != null) {
                    if (RepairItemTransaction.checkForRepairItemPart(actions)) {
                        for (InventoryAction action : actions) {
                            this.repairItemTransaction.addAction(action);
                        }
                        return;
                    } else {
                        this.server.getLogger().debug("Got unexpected normal inventory action with incomplete repair item transaction from " + this.getName() + ", refusing to execute repair item " + transactionPacket.toString());
                        this.removeAllWindows(false);
                        this.sendAllInventories();
                        this.repairItemTransaction = null;
                    }
                }

                switch (transactionPacket.transactionType) {
                    case InventoryTransactionPacket.TYPE_NORMAL:
                        InventoryTransaction transaction = new InventoryTransaction(this, actions);

                        if (!transaction.execute()) {
                            this.server.getLogger().debug("Failed to execute inventory transaction from " + this.getName() + " with actions: " + Arrays.toString(transactionPacket.actions));
                            break packetswitch; //oops!
                        }

                        //TODO: fix achievement for getting iron from furnace

                        break packetswitch;
                    case InventoryTransactionPacket.TYPE_MISMATCH:
                        if (transactionPacket.actions.length > 0) {
                            this.server.getLogger().debug("Expected 0 actions for mismatch, got " + transactionPacket.actions.length + ", " + Arrays.toString(transactionPacket.actions));
                        }
                        this.sendAllInventories();

                        break packetswitch;
                    case InventoryTransactionPacket.TYPE_USE_ITEM:
                        UseItemData useItemData = (UseItemData) transactionPacket.transactionData;

                        var blockVector = useItemData.blockPos;
                        face = useItemData.face;

                        int type = useItemData.actionType;
                        switch (type) {
                            case InventoryTransactionPacket.USE_ITEM_ACTION_CLICK_BLOCK:
                                // Remove if client bug is ever fixed
                                boolean spamBug = (lastRightClickPos != null && System.currentTimeMillis() - lastRightClickTime < 100.0 && blockVector.toDouble().distanceSquared(lastRightClickPos.toNewVector()) < 0.00001);
                                lastRightClickPos = new Vector3(blockVector.toDouble());
                                lastRightClickTime = System.currentTimeMillis();
                                if (spamBug) {
                                    return;
                                }

                                this.setDataFlag(DATA_FLAGS, EntityFlags.ACTION, false);

                                if (this.canInteract(blockVector.toDouble().add(0.5, 0.5, 0.5), isCreative()? 13 : 7)) {
                                    if (this.isCreative()) {
                                        Item i = inventory.getItemInHand();
                                        if (world.useItemOn(blockVector, i, face, useItemData.clickPos.x(), useItemData.clickPos.y(), useItemData.clickPos.z(), this) != null) {
                                            break packetswitch;
                                        }
                                    } else if (inventory.getItemInHand().equals(useItemData.itemInHand)) {
                                        Item i = inventory.getItemInHand();
                                        Item oldItem = i.clone();
                                        //TODO: Implement adventure mode checks
                                        if ((i = world.useItemOn(blockVector, i, face, useItemData.clickPos.x(), useItemData.clickPos.y(), useItemData.clickPos.z(), this)) != null) {
                                            if (!i.equals(oldItem) || i.getCount() != oldItem.getCount()) {
                                                if (oldItem.getId() == i.getId() || i.getId() == 0) {
                                                    inventory.setItemInHand(i);
                                                } else {
                                                    server.getLogger().debug("Tried to set item " + i.getId() + " but " + this.username + " had item " + oldItem.getId() + " in their hand slot");
                                                }
                                                inventory.sendHeldItem(this.getViewers().values());
                                            }
                                            break packetswitch;
                                        }
                                    }
                                }

                                inventory.sendHeldItem(this);

                                if (blockVector.toDouble().distanceSquared(position) > 10000) {
                                    break packetswitch;
                                }

                                Block target = world.getBlock(blockVector);
                                block = target.getSide(face);

                                world.sendBlocks(new Player[]{this}, new Vector3i[]{target.getPosition(), block.getPosition()}, UpdateBlockPacket.FLAG_ALL_PRIORITY);
                                break packetswitch;
                            case InventoryTransactionPacket.USE_ITEM_ACTION_BREAK_BLOCK:
                                if (!this.spawned || !this.isAlive()) {
                                    break packetswitch;
                                }

                                System.out.println("USE_ITEM_ACTION_BREAK_BLOCK");

                                this.resetCraftingGridType();

                                Item i = this.getInventory().getItemInHand();

                                Item oldItem = i.clone();

                                if (this.canInteract(blockVector.toDouble().add(0.5, 0.5, 0.5), this.isCreative() ? 13 : 7) && (i = world.useBreakOn(blockVector, face, i, this, true)) != null) {
                                    if (this.isSurvival()) {
                                        this.getFoodData().updateFoodExpLevel(0.005);
                                        if (!i.equals(oldItem) || i.getCount() != oldItem.getCount()) {
                                            if (oldItem.getId() == i.getId() || i.getId() == 0) {
                                                inventory.setItemInHand(i);
                                            } else {
                                                server.getLogger().debug("Tried to set item " + i.getId() + " but " + this.username + " had item " + oldItem.getId() + " in their hand slot");
                                            }
                                            inventory.sendHeldItem(this.getViewers().values());
                                        }
                                    }
                                    break packetswitch;
                                }

                                inventory.sendContents(this);
                                inventory.sendHeldItem(this);

                                if (blockVector.toDouble().distanceSquared(position) < 10000) {
                                    target = world.getBlock(blockVector);
                                    world.sendBlocks(new Player[]{this}, new Vector3i[]{target.getPosition()}, UpdateBlockPacket.FLAG_ALL_PRIORITY);

                                    var blockEntity = world.getBlockEntity(blockVector);
                                    if (blockEntity.getType() instanceof SpawnableBlockEntityType $type) {
                                        $type.spawnTo(this);
                                    }
                                }

                                break packetswitch;
                            case InventoryTransactionPacket.USE_ITEM_ACTION_CLICK_AIR:
                                Vector3d directionVector = this.getDirectionVector();

                                if (this.isCreative()) {
                                    item = this.inventory.getItemInHand();
                                } else if (!this.inventory.getItemInHand().equals(useItemData.itemInHand)) {
                                    this.inventory.sendHeldItem(this);
                                    break packetswitch;
                                } else {
                                    item = this.inventory.getItemInHand();
                                }

                                PlayerInteractEvent interactEvent = new PlayerInteractEvent(this, item, directionVector, face, Action.RIGHT_CLICK_AIR);

                                this.server.getPluginManager().callEvent(interactEvent);

                                if (interactEvent.isCancelled()) {
                                    this.inventory.sendHeldItem(this);
                                    break packetswitch;
                                }

                                if (item.onClickAir(this, new Vector3(directionVector))) {
                                    if (!this.isCreative()) {
                                        if (item.getId() == 0 || this.inventory.getItemInHand().getId() == item.getId()) {
                                            this.inventory.setItemInHand(item);
                                        } else {
                                            server.getLogger().debug("Tried to set item " + item.getId() + " but " + this.username + " had item " + this.inventory.getItemInHand().getId() + " in their hand slot");
                                        }
                                    }

                                    if (!this.isUsingItem()) {
                                        this.setUsingItem(true);
                                        break packetswitch;
                                    }

                                    // Used item
                                    int ticksUsed = this.server.getTick() - this.startAction;
                                    this.setUsingItem(false);

                                    if (!item.onUse(this, ticksUsed)) {
                                        this.inventory.sendContents(this);
                                    }
                                }

                                break packetswitch;
                            default:
                                //unknown
                                break;
                        }
                        break;
                    case InventoryTransactionPacket.TYPE_USE_ITEM_ON_ENTITY:
                        UseItemOnEntityData useItemOnEntityData = (UseItemOnEntityData) transactionPacket.transactionData;

                        Entity target = world.getEntity(useItemOnEntityData.entityRuntimeId);
                        if (target == null) {
                            return;
                        }

                        type = useItemOnEntityData.actionType;

                        if (!useItemOnEntityData.itemInHand.equalsExact(this.inventory.getItemInHand())) {
                            this.inventory.sendHeldItem(this);
                        }

                        item = this.inventory.getItemInHand();

                        switch (type) {
                            case InventoryTransactionPacket.USE_ITEM_ON_ENTITY_ACTION_INTERACT:
                                PlayerInteractEntityEvent playerInteractEntityEvent = new PlayerInteractEntityEvent(this, target, item, useItemOnEntityData.clickPos);
                                if (this.isSpectator()) playerInteractEntityEvent.setCancelled();
                                getServer().getPluginManager().callEvent(playerInteractEntityEvent);

                                if (playerInteractEntityEvent.isCancelled()) {
                                    break;
                                }
                                if (target.onInteract(this, item, useItemOnEntityData.clickPos) && this.isSurvival()) {
                                    if (item.isTool()) {
                                        if (item.useOn(target) && item.getDamage() >= item.getMaxDurability()) {
                                            item = new ItemBlock(Block.get(BlockID.AIR));
                                        }
                                    } else {
                                        if (item.count > 1) {
                                            item.count--;
                                        } else {
                                            item = new ItemBlock(Block.get(BlockID.AIR));
                                        }
                                    }

                                    if (item.getId() == 0 || this.inventory.getItemInHand().getId() == item.getId()) {
                                        this.inventory.setItemInHand(item);
                                    } else {
                                        server.getLogger().debug("Tried to set item " + item.getId() + " but " + this.username + " had item " + this.inventory.getItemInHand().getId() + " in their hand slot");
                                    }
                                }
                                break;
                            case InventoryTransactionPacket.USE_ITEM_ON_ENTITY_ACTION_ATTACK:
                                if (!this.canInteract(target.getPosition(), isCreative() ? 8 : 5)) {
                                    break;
                                } else if (target instanceof Player) {
                                    if ((((Player) target).getGamemode() & 0x01) > 0) {
                                        break;
                                    } else if (!this.server.getPropertyBoolean("pvp")) {
                                        break;
                                    }
                                }

                                Enchantment[] enchantments = item.getEnchantments();

                                float itemDamage = item.getAttackDamage();
                                for (Enchantment enchantment : enchantments) {
                                    itemDamage += enchantment.getDamageBonus(target);
                                }

                                Map<DamageModifier, Float> damage = new EnumMap<>(DamageModifier.class);
                                damage.put(DamageModifier.BASE, itemDamage);

                                float knockBack = 0.3f;
                                Enchantment knockBackEnchantment = item.getEnchantment(Enchantment.ID_KNOCKBACK);
                                if (knockBackEnchantment != null) {
                                    knockBack += knockBackEnchantment.getLevel() * 0.1f;
                                }

                                EntityDamageByEntityEvent entityDamageByEntityEvent = new EntityDamageByEntityEvent(this, target, DamageCause.ENTITY_ATTACK, damage, knockBack, enchantments);
                                if (this.isSpectator()) entityDamageByEntityEvent.setCancelled();
                                if ((target instanceof Player) && !world.getGameRules().getBoolean(GameRule.PVP)) {
                                    entityDamageByEntityEvent.setCancelled();
                                }

                                if (!target.attack(entityDamageByEntityEvent)) {
                                    if (item.isTool() && this.isSurvival()) {
                                        this.inventory.sendContents(this);
                                    }
                                    break;
                                }

                                for (Enchantment enchantment : item.getEnchantments()) {
                                    enchantment.doPostAttack(this, target);
                                }

                                if (item.isTool() && (this.isSurvival() || this.isAdventure())) {
                                    if (item.useOn(target) && item.getDamage() >= item.getMaxDurability()) {
                                        this.inventory.setItemInHand(Item.get(0));
                                    } else {
                                        if (item.getId() == 0 || this.inventory.getItemInHand().getId() == item.getId()) {
                                            this.inventory.setItemInHand(item);
                                        } else {
                                            server.getLogger().debug("Tried to set item " + item.getId() + " but " + this.username + " had item " + this.inventory.getItemInHand().getId() + " in their hand slot");
                                        }
                                    }
                                }
                                return;
                            default:
                                break; //unknown
                        }

                        break;
                    case InventoryTransactionPacket.TYPE_RELEASE_ITEM:
                        if (this.isSpectator()) {
                            this.sendAllInventories();
                            break packetswitch;
                        }
                        ReleaseItemData releaseItemData = (ReleaseItemData) transactionPacket.transactionData;

                        try {
                            type = releaseItemData.actionType;
                            switch (type) {
                                case InventoryTransactionPacket.RELEASE_ITEM_ACTION_RELEASE:
                                    if (this.isUsingItem()) {
                                        item = this.inventory.getItemInHand();

                                        int ticksUsed = this.server.getTick() - this.startAction;
                                        if (!item.onRelease(this, ticksUsed)) {
                                            this.inventory.sendContents(this);
                                        }

                                        this.setUsingItem(false);
                                    } else {
                                        this.inventory.sendContents(this);
                                    }
                                    return;
                                case InventoryTransactionPacket.RELEASE_ITEM_ACTION_CONSUME:
                                    log.debug("Unexpected release item action consume from {}", this::getName);
                                    return;
                                default:
                                    break;
                            }
                        } finally {
                            this.setUsingItem(false);
                        }
                        break;
                    default:
                        this.inventory.sendContents(this);
                        break;
                }
                break;
            case ProtocolInfo.PLAYER_HOTBAR_PACKET:
                PlayerHotbarPacket hotbarPacket = (PlayerHotbarPacket) packet;

                if (hotbarPacket.windowId != ContainerIds.INVENTORY) {
                    return; //In PE this should never happen
                }

                this.inventory.equipItem(hotbarPacket.selectedHotbarSlot);
                break;
            case ProtocolInfo.SERVER_SETTINGS_REQUEST_PACKET:
                PlayerServerSettingsRequestEvent settingsRequestEvent = new PlayerServerSettingsRequestEvent(this, new HashMap<>(this.serverSettings));
                this.getServer().getPluginManager().callEvent(settingsRequestEvent);

                if (!settingsRequestEvent.isCancelled()) {
                    settingsRequestEvent.getSettings().forEach((id, window) -> {
                        ServerSettingsResponsePacket re = new ServerSettingsResponsePacket();
                        re.formId = id;
                        re.data = window.getJSONData();
                        this.dataPacket(re);
                    });
                }
                break;
            case ProtocolInfo.RESPAWN_PACKET:
                if (this.isAlive()) {
                    break;
                }
                RespawnPacket respawnPacket = (RespawnPacket) packet;
                if (respawnPacket.respawnState == RespawnPacket.STATE_CLIENT_READY_TO_SPAWN) {
                    RespawnPacket respawn1 = new RespawnPacket();
                    respawn1.position = position.toFloat();
                    respawn1.respawnState = RespawnPacket.STATE_READY_TO_SPAWN;
                    this.dataPacket(respawn1);
                }
                break;
            case ProtocolInfo.BOOK_EDIT_PACKET:
                BookEditPacket bookEditPacket = (BookEditPacket) packet;
                Item oldBook = this.inventory.getItem(bookEditPacket.inventorySlot);

                if (bookEditPacket.text != null && bookEditPacket.text.length() > 256) {
                    this.getServer().getLogger().debug(username + ": BookEditPacket with too long text");
                    return;
                }

                Item newBook = oldBook.clone();
                boolean success;
                return;
        }

    }

    private void onBlockBreakContinue(Vector3i pos, BlockFace face) {
        if (this.isBreakingBlock()) {
            Block block = world.getBlock(pos, false);
            world.addParticle(new PunchBlockParticle(new Vector3(pos.toDouble()), block, face));
        }
    }

    private void onBlockBreakStart(Vector3i pos, BlockFace face) {
        long currentBreak = System.currentTimeMillis();
        // HACK: Client spams multiple left clicks so we need to skip them.
        if ((this.lastBreakPosition.equals(pos) && (currentBreak - this.lastBreak) < 10) || pos.toDouble().distanceSquared(position) > 100) {
            return;
        }

        Block target = world.getBlock(pos);
        var playerInteractEvent = new PlayerInteractEvent(this, this.inventory.getItemInHand(), target, face, target.getId() == 0 ? Action.LEFT_CLICK_AIR : Action.LEFT_CLICK_BLOCK);
        this.getServer().getPluginManager().callEvent(playerInteractEvent);
        if (playerInteractEvent.isCancelled()) {
            this.inventory.sendHeldItem(this);
            return;
        }

        Block block = target.getSide(face);

        if (!this.isCreative()) {
            double breakTime = Math.ceil(target.getBreakTime(this.inventory.getItemInHand(), this) * 20);
            if (breakTime > 0) {
                LevelEventPacket pk = new LevelEventPacket();
                pk.evid = LevelEventPacket.EVENT_BLOCK_START_BREAK;
                pk.x = (float) pos.x();
                pk.y = (float) pos.y();
                pk.z = (float) pos.z();
                pk.data = (int) (65535 / breakTime);
                world.addChunkPacket(pos.x() >> 4, pos.z() >> 4, pk);
            }
        }

        this.breakingBlock = target;
        this.lastBreak = currentBreak;
        this.lastBreakPosition = pos;
    }

    private void onBlockBreakAbort(Vector3i pos, BlockFace face) {
        if (pos.toDouble().distanceSquared(position) < 100) {
            LevelEventPacket pk = new LevelEventPacket();
            pk.evid = LevelEventPacket.EVENT_BLOCK_STOP_BREAK;
            pk.x = (float) pos.x();
            pk.y = (float) pos.y();
            pk.z = (float) pos.z();
            pk.data = 0;
            world.addChunkPacket(pos.x() >> 4, pos.z() >> 4, pk);
        }

        breakingBlock = null;
    }

    private void onBlockBreakComplete(Vector3i blockPos, BlockFace face) {
        if (!this.spawned || !this.isAlive()) {
            return;
        }

        this.resetCraftingGridType();

        Item handItem = this.getInventory().getItemInHand();
        Item clone = handItem.clone();

        boolean canInteract = this.canInteract(blockPos.toDouble().add(0.5, 0.5, 0.5), this.isCreative() ? 13 : 7);
        if (canInteract) {
            handItem = world.useBreakOn(blockPos, face, handItem, this, true);
            if (handItem != null && this.isSurvival()) {
                this.getFoodData().updateFoodExpLevel(0.005);
                if (handItem.equals(clone) && handItem.getCount() == clone.getCount()) {
                    return;
                }

                if (clone.getId() == handItem.getId() || handItem.getId() == 0) {
                    inventory.setItemInHand(handItem);
                } else {
                    server.getLogger().debug("Tried to set item " + handItem.getId() + " but " + this.username + " had item " + clone.getId() + " in their hand slot");
                }
                inventory.sendHeldItem(this.getViewers().values());
            }
            return;
        }

        inventory.sendContents(this);
        inventory.sendHeldItem(this);

        if (blockPos.toDouble().distanceSquared(position) < 100) {
            Block target = world.getBlock(blockPos);
            world.sendBlocks(new Player[]{this}, new Vector3i[]{target.getPosition()}, UpdateBlockPacket.FLAG_ALL_PRIORITY);

            var blockEntity = world.getBlockEntity(blockPos);
            if (blockEntity.getType() instanceof SpawnableBlockEntityType type) {
                type.spawnTo(this);
            }
        }
    }

    /**
     * Sends a chat message as this player. If the message begins with a / (forward-slash) it will be treated
     * as a command.
     * @param message message to send
     * @return successful
     */
    public boolean chat(String message) {
        if (!this.spawned || !this.isAlive()) {
            return false;
        }

        this.resetCraftingGridType();
        this.craftingType = CRAFTING_SMALL;

        if (this.removeFormat) {
            message = TextFormat.clean(message, true);
        }

        for (String msg : message.split("\n")) {
            if (!msg.trim().isEmpty() && msg.length() <= 512 && this.messageCounter-- > 0) {
                PlayerChatEvent chatEvent = new PlayerChatEvent(this, msg);
                this.server.getPluginManager().callEvent(chatEvent);
                if (!chatEvent.isCancelled()) {
                    this.server.broadcastMessage(this.getServer().getLanguage().translateString(chatEvent.getFormat(), new String[]{chatEvent.getPlayer().getDisplayName(), chatEvent.getMessage()}), chatEvent.getRecipients());
                }
            }
        }

        return true;
    }

    public boolean kick() {
        return this.kick("");
    }

    public boolean kick(String reason, boolean isAdmin) {
        return this.kick(PlayerKickEvent.Reason.UNKNOWN, reason, isAdmin);
    }

    public boolean kick(String reason) {
        return kick(PlayerKickEvent.Reason.UNKNOWN, reason);
    }

    public boolean kick(PlayerKickEvent.Reason reason) {
        return this.kick(reason, true);
    }

    public boolean kick(PlayerKickEvent.Reason reason, String reasonString) {
        return this.kick(reason, reasonString, true);
    }

    public boolean kick(PlayerKickEvent.Reason reason, boolean isAdmin) {
        return this.kick(reason, reason.toString(), isAdmin);
    }

    public boolean kick(PlayerKickEvent.Reason reason, String reasonString, boolean isAdmin) {
        PlayerKickEvent ev;
        this.server.getPluginManager().callEvent(ev = new PlayerKickEvent(this, reason, this.getLeaveMessage()));
        if (!ev.isCancelled()) {
            String message;
            if (isAdmin) {
                message = reasonString;
            } else {
                if (reasonString.isEmpty()) {
                    message = "disconnectionScreen.noReason";
                } else {
                    message = reasonString;
                }
            }

            this.remove(ev.getQuitMessage(), message);

            return true;
        }

        return false;
    }

    public void setViewDistance(int distance) {
        this.chunkRadius = distance;

        ChunkRadiusUpdatedPacket pk = new ChunkRadiusUpdatedPacket();
        pk.radius = distance;

        this.dataPacket(pk);
    }

    public int getViewDistance() {
        return this.chunkRadius;
    }

    public void sendMessage(String message) {
        TextPacket pk = new TextPacket();
        pk.type = TextPacket.TYPE_RAW;
        pk.message = this.server.getLanguage().translateString(message);
        this.dataPacket(pk);
    }

    public void sendMessage(TextContainer message) {
        if (message instanceof TranslationContainer) {
            this.sendTranslation(message.getText(), ((TranslationContainer) message).getParameters());
            return;
        }
        this.sendMessage(message.getText());
    }

    public void sendTranslation(String message) {
        this.sendTranslation(message, new String[0]);
    }

    public void sendTranslation(String message, String[] parameters) {
        TextPacket pk = new TextPacket();
        if (!this.server.isLanguageForced()) {
            pk.type = TextPacket.TYPE_TRANSLATION;
            pk.message = this.server.getLanguage().translateString(message, parameters, "nukkit.");
            for (int i = 0; i < parameters.length; i++) {
                parameters[i] = this.server.getLanguage().translateString(parameters[i], parameters, "nukkit.");

            }
            pk.parameters = parameters;
        } else {
            pk.type = TextPacket.TYPE_RAW;
            pk.message = this.server.getLanguage().translateString(message, parameters);
        }
        this.dataPacket(pk);
    }

    public void sendChat(String message) {
        this.sendChat("", message);
    }

    public void sendChat(String source, String message) {
        TextPacket pk = new TextPacket();
        pk.type = TextPacket.TYPE_CHAT;
        pk.source = source;
        pk.message = this.server.getLanguage().translateString(message);
        this.dataPacket(pk);
    }

    public void sendPopup(String message) {
        this.sendPopup(message, "");
    }

    public void sendPopup(String message, String subtitle) {
        TextPacket pk = new TextPacket();
        pk.type = TextPacket.TYPE_POPUP;
        pk.message = message;
        this.dataPacket(pk);
    }

    public void sendTip(String message) {
        TextPacket pk = new TextPacket();
        pk.type = TextPacket.TYPE_TIP;
        pk.message = message;
        this.dataPacket(pk);
    }

    public void clearTitle() {
        SetTitlePacket pk = new SetTitlePacket();
        pk.type = SetTitlePacket.TYPE_CLEAR;
        this.dataPacket(pk);
    }

    /**
     * Resets both title animation times and subtitle for the next shown title
     */
    public void resetTitleSettings() {
        SetTitlePacket pk = new SetTitlePacket();
        pk.type = SetTitlePacket.TYPE_RESET;
        this.dataPacket(pk);
    }

    public void setSubtitle(String subtitle) {
        SetTitlePacket pk = new SetTitlePacket();
        pk.type = SetTitlePacket.TYPE_SUBTITLE;
        pk.text = subtitle;
        this.dataPacket(pk);
    }

    public void setTitleAnimationTimes(int fadein, int duration, int fadeout) {
        SetTitlePacket pk = new SetTitlePacket();
        pk.type = SetTitlePacket.TYPE_ANIMATION_TIMES;
        pk.fadeInTime = fadein;
        pk.stayTime = duration;
        pk.fadeOutTime = fadeout;
        this.dataPacket(pk);
    }


    private void setTitle(String text) {
        SetTitlePacket packet = new SetTitlePacket();
        packet.text = text;
        packet.type = SetTitlePacket.TYPE_TITLE;
        this.dataPacket(packet);
    }

    public void sendTitle(String title) {
        this.sendTitle(title, null, 20, 20, 5);
    }

    public void sendTitle(String title, String subtitle) {
        this.sendTitle(title, subtitle, 20, 20, 5);
    }

    public void sendTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        this.setTitleAnimationTimes(fadeIn, stay, fadeOut);
        if (!Strings.isNullOrEmpty(subtitle)) {
            this.setSubtitle(subtitle);
        }
        // title won't send if an empty string is used.
        this.setTitle(Strings.isNullOrEmpty(title) ? " " : title);
    }

    public void sendActionBar(String title) {
        this.sendActionBar(title, 1, 0, 1);
    }

    public void sendActionBar(String title, int fadein, int duration, int fadeout) {
        SetTitlePacket pk = new SetTitlePacket();
        pk.type = SetTitlePacket.TYPE_ACTION_BAR;
        pk.text = title;
        pk.fadeInTime = fadein;
        pk.stayTime = duration;
        pk.fadeOutTime = fadeout;
        this.dataPacket(pk);
    }

    public void sendToast(String title, String content) {
        ToastRequestPacket pk = new ToastRequestPacket();
        pk.title = title;
        pk.content = content;
        this.dataPacket(pk);
    }

    @Override
    public void remove() {
        remove("");
    }

    public void remove(String message) {
        remove(message, "generic");
    }

    public void remove(String message, String reason) {
        remove(message, reason, true);
    }

    public void remove(String message, String reason, boolean notify) {
        remove(new TextContainer(message), reason, notify);
    }

    public void remove(TextContainer message) {
        remove(message, "generic");
    }

    public void remove(TextContainer message, String reason) {
        remove(message, reason, true);
    }

    public void remove(TextContainer message, String reason, boolean notify) {
        if (this.connected && !removed) {
            if (notify && reason.length() > 0) {
                DisconnectPacket pk = new DisconnectPacket();
                pk.message = reason;
                this.forceDataPacket(pk, null);
            }

            this.connected = false;
            PlayerQuitEvent ev = null;
            if (this.getName() != null && this.getName().length() > 0) {
                this.server.getPluginManager().callEvent(ev = new PlayerQuitEvent(this, message, true, reason));
                if (this.loggedIn && ev.getAutoSave()) {
                    this.save();
                }
            }

            for (var player: server.getPlayerManager().getPlayers()) {
                if (!player.canSee(this)) {
                    player.showPlayer(this);
                }
            }

            this.hiddenPlayers.clear();

            this.removeAllWindows(true);

            for (long index : new ArrayList<>(this.usedChunks.keySet())) {
                int chunkX = Level.getHashX(index);
                int chunkZ = Level.getHashZ(index);
                world.unregisterChunkLoader(this, chunkX, chunkZ);
                this.usedChunks.remove(index);

                for (Entity entity : world.getChunkEntities(chunkX, chunkZ).values()) {
                    if (entity != this) {
                        entity.getViewers().remove(getLoaderId());
                    }
                }
            }

            super.remove();

            this.interfaz.close(this, notify ? reason : "");

            if (this.loggedIn) {
                this.server.getPlayerManager().removePlayer(this);
            }

            this.loggedIn = false;

            this.spawned = false;
            this.server.getLogger().info(this.getServer().getLanguage().translateString("nukkit.player.logOut",
                    TextFormat.AQUA + (this.getName() == null ? "" : this.getName()) + TextFormat.WHITE,
                    this.getAddress(),
                    String.valueOf(this.getPort()),
                    this.getServer().getLanguage().translateString(reason)));
            this.windows.clear();
            this.usedChunks.clear();
            this.loadQueue.clear();
            this.hasSpawned.clear();
            this.spawnPoint = null;

            this.riding = null;
        }

        if (this.inventory != null) {
            this.inventory = null;
        }

        this.chunk = null;

        this.server.getPlayerManager().removePlayerConnection(this);
    }

    public void save() {
        this.save(false);
    }

    public void save(boolean async) {
        if (removed) {
            throw new IllegalStateException("Tried to save closed player");
        }

        super.saveNBT();
    }

    public String getName() {
        return this.username;
    }

    @Override
    public void kill() {
        if (!this.spawned) {
            return;
        }

        boolean showMessages = world.getGameRules().getBoolean(GameRule.SHOW_DEATH_MESSAGES);
        String message = "";
        List<String> params = new ArrayList<>();
        EntityDamageEvent cause = this.getLastDamageCause();

        if (showMessages) {
            params.add(this.getDisplayName());

            switch (cause == null ? DamageCause.CUSTOM : cause.getCause()) {
                case ENTITY_ATTACK:
                    if (cause instanceof EntityDamageByEntityEvent) {
                        Entity e = ((EntityDamageByEntityEvent) cause).getDamager();
                        killer = e;
                        if (e instanceof Player) {
                            message = "death.attack.player";
                            params.add(((Player) e).getDisplayName());
                            break;
                        } else if (e instanceof EntityLiving) {
                            message = "death.attack.mob";
                            params.add(!Objects.equals(e.getNameTag(), "") ? e.getNameTag() : e.getName());
                            break;
                        } else {
                            params.add("Unknown");
                        }
                    }
                    break;
                case PROJECTILE:
                    if (cause instanceof EntityDamageByEntityEvent) {
                        Entity e = ((EntityDamageByEntityEvent) cause).getDamager();
                        killer = e;
                        if (e instanceof Player) {
                            message = "death.attack.arrow";
                            params.add(((Player) e).getDisplayName());
                        } else if (e instanceof EntityLiving) {
                            message = "death.attack.arrow";
                            params.add(!Objects.equals(e.getNameTag(), "") ? e.getNameTag() : e.getName());
                            break;
                        } else {
                            params.add("Unknown");
                        }
                    }
                    break;
                case VOID:
                    message = "death.attack.outOfWorld";
                    break;
                case FALL:
                    if (cause.getFinalDamage() > 2) {
                        message = "death.fell.accident.generic";
                        break;
                    }
                    message = "death.attack.fall";
                    break;

                case SUFFOCATION:
                    message = "death.attack.inWall";
                    break;

                case LAVA:
                    message = "death.attack.lava";
                    break;

                case FIRE:
                    message = "death.attack.onFire";
                    break;

                case FIRE_TICK:
                    message = "death.attack.inFire";
                    break;

                case DROWNING:
                    message = "death.attack.drown";
                    break;

                case CONTACT:
                    if (cause instanceof EntityDamageByBlockEvent) {
                        int id = ((EntityDamageByBlockEvent) cause).getDamager().getId();
                    }
                    break;

                case BLOCK_EXPLOSION:
                case ENTITY_EXPLOSION:
                    if (cause instanceof EntityDamageByEntityEvent) {
                        Entity e = ((EntityDamageByEntityEvent) cause).getDamager();
                        killer = e;
                        if (e instanceof Player) {
                            message = "death.attack.explosion.player";
                            params.add(((Player) e).getDisplayName());
                        } else if (e instanceof EntityLiving) {
                            message = "death.attack.explosion.player";
                            params.add(!Objects.equals(e.getNameTag(), "") ? e.getNameTag() : e.getName());
                            break;
                        } else {
                            message = "death.attack.explosion";
                        }
                    } else {
                        message = "death.attack.explosion";
                    }
                    break;
                case MAGIC:
                    message = "death.attack.magic";
                    break;
                case LIGHTNING:
                    message = "death.attack.lightningBolt";
                    break;
                case HUNGER:
                    message = "death.attack.starve";
                    break;
                default:
                    message = "death.attack.generic";
                    break;
            }
        }

        PlayerDeathEvent ev = new PlayerDeathEvent(this, this.getDrops(), new TranslationContainer(message, params.toArray(new String[0])), this.expLevel);
        ev.setKeepExperience(world.gameRules.getBoolean(GameRule.KEEP_INVENTORY));
        ev.setKeepInventory(ev.getKeepExperience());

        this.server.getPluginManager().callEvent(ev);

        if (!ev.isCancelled()) {

            this.health = 0;
            this.extinguish();
            this.scheduleUpdate();

            if (!ev.getKeepInventory() && world.getGameRules().getBoolean(GameRule.DO_ENTITY_DROPS)) {
                for (Item item : ev.getDrops()) {
                    if (!item.hasEnchantment(Enchantment.ID_VANISHING_CURSE)) {
                        world.dropItem(new Vector3(position), item, null, true, 40);
                    }
                }

                if (this.inventory != null) {
                    this.inventory.clearAll();
                }
                if (this.offhandInventory != null) {
                    this.offhandInventory.clearAll();
                }
            }

            if (!ev.getKeepExperience() && world.getGameRules().getBoolean(GameRule.DO_ENTITY_DROPS)) {
                if (this.isSurvival() || this.isAdventure()) {
                    int exp = ev.getExperience() * 7;
                    if (exp > 100) exp = 100;
                    world.dropExpOrb(new Vector3(position), exp);
                }
                this.setExperience(0, 0);
            }

            this.timeSinceRest = 0;

            RespawnPacket pk = new RespawnPacket();
            var point = this.getSpawn();
            pk.position = point.getPosition().toFloat();
            pk.respawnState = RespawnPacket.STATE_SEARCHING_FOR_SPAWN;

            this.dataPacket(pk);
        }
    }

    protected void respawn() {
        craftingType = CRAFTING_SMALL;
        resetCraftingGridType();

        PlayerRespawnEvent playerRespawnEvent = new PlayerRespawnEvent(this, getSpawn());
        server.getPluginManager().callEvent(playerRespawnEvent);

        var respawnPoint = playerRespawnEvent.getRespawnPoint();

        sendExperience();
        sendExperienceLevel();

        setSprinting(false);
        setSneaking(false);

        setDataProperty(new ShortEntityData(Player.DATA_AIR, 400), false);
        deadTicks = 0;
        noDamageTicks = 60;

        removeAllEffects();
        setHealth(this.getMaxHealth());
        getFoodData().setLevel(20, 20);

        sendData(this);

        setMovementSpeed(DEFAULT_SPEED);

        getAdventureSettings().update();
        inventory.sendContents(this);
        inventory.sendArmorContents(this);
        offhandInventory.sendContents(this);

        teleport(respawnPoint);
        spawnToAll();
        scheduleUpdate();
    }

    @Override
    public void setHealth(float health) {
        if (health < 1) {
            health = 0;
        }

        super.setHealth(health);
        //TODO: Remove it in future! This a hack to solve the client-side absorption bug! WFT Mojang (Half a yellow heart cannot be shown, we can test it in local gaming)
        var attribute = Attributes.MAX_HEALTH.withValue(
                health > 0 ? (health < getMaxHealth() ? health : getMaxHealth()) : 0,
                getAbsorption() % 2 != 0 ? getMaxHealth() + 1 : getMaxHealth()
        );

        if (!spawned) return;

        var pk = new UpdateAttributesPacket();
        pk.entries = new Attribute.Entry[]{ attribute };
        pk.entityId = getId();
        dataPacket(pk);

    }

    @Override
    public void setMaxHealth(int maxHealth) {
        super.setMaxHealth(maxHealth);

        if (!spawned) return;

        var attribute = Attributes.MAX_HEALTH.withValue(
                health > 0 ? (health < getMaxHealth() ? health : getMaxHealth()) : 0,
                getAbsorption() % 2 != 0 ? getMaxHealth() + 1 : getMaxHealth()
        );

        var pk = new UpdateAttributesPacket();
        pk.entries = new Attribute.Entry[]{ attribute };
        pk.entityId = getId();
        dataPacket(pk);
    }

    public int getExperience() {
        return this.exp;
    }

    public int getExperienceLevel() {
        return this.expLevel;
    }

    public void addExperience(int add) {
        if (add == 0) return;
        int now = this.getExperience();
        int added = now + add;
        int level = this.getExperienceLevel();
        int most = calculateRequireExperience(level);
        while (added >= most) {  //Level Up!
            added = added - most;
            level++;
            most = calculateRequireExperience(level);
        }
        this.setExperience(added, level);
    }

    public static int calculateRequireExperience(int level) {
        if (level >= 30) {
            return 112 + (level - 30) * 9;
        } else if (level >= 15) {
            return 37 + (level - 15) * 5;
        } else {
            return 7 + level * 2;
        }
    }

    public void setExperience(int exp) {
        setExperience(exp, this.getExperienceLevel());
    }

    //todo something on performance, lots of exp orbs then lots of packets, could crash client

    public void setExperience(int exp, int level) {
        PlayerExperienceChangeEvent ev = new PlayerExperienceChangeEvent(this, this.exp, this.expLevel, exp, level);
        this.server.getPluginManager().callEvent(ev);

        if (ev.isCancelled()) {
            return;
        }

        this.exp = ev.getNewExperience();
        this.expLevel = ev.getNewExperienceLevel();

        this.sendExperienceLevel(this.expLevel);
        this.sendExperience(this.exp);
    }

    public void sendExperience() {
        sendExperience(this.getExperience());
    }

    public void sendExperience(int exp) {
        if (!spawned) return;

        float percent = ((float) exp) / calculateRequireExperience(this.getExperienceLevel());
        percent = Math.max(0f, Math.min(1f, percent));

        setAttribute(Attributes.EXPERIENCE, percent);
    }

    public void sendExperienceLevel() {
        sendExperienceLevel(this.getExperienceLevel());
    }

    public void sendExperienceLevel(int level) {
        if (!spawned) return;

        setAttribute(Attributes.EXPERIENCE_LEVEL, level);
    }

    public void setAttribute(Attribute attribute, float value) {
        var pk = new UpdateAttributesPacket();
        pk.entries = new Attribute.Entry[]{ new Attribute.Entry(attribute, value) };
        pk.entityId = getId();

        dataPacket(pk);
    }

    @Override
    public void setMovementSpeed(float speed) {
        setMovementSpeed(speed, true);
    }

    public void setMovementSpeed(float speed, boolean send) {
        super.setMovementSpeed(speed);
        if (this.spawned && send) {
            this.sendMovementSpeed(speed);
        }
    }

    public void sendMovementSpeed(float speed){
        setAttribute(Attributes.MOVEMENT_SPEED, speed);
    }

    public Entity getKiller() {
        return killer;
    }

    @Override
    public boolean attack(EntityDamageEvent source) {
        if (!this.isAlive()) {
            return false;
        }

        if (this.isSpectator() || (this.isCreative() && source.getCause() != DamageCause.SUICIDE)) {
            //source.setCancelled();
            return false;
        } else if (this.getAdventureSettings().get(Type.ALLOW_FLIGHT) && source.getCause() == DamageCause.FALL) {
            //source.setCancelled();
            return false;
        } else if (source.getCause() == DamageCause.FALL) {
        }

        if (super.attack(source)) { //!source.isCancelled()
            if (this.getLastDamageCause() == source && this.spawned) {
                if (source instanceof EntityDamageByEntityEvent) {
                    Entity damager = ((EntityDamageByEntityEvent) source).getDamager();
                    if (damager instanceof Player) {
                        ((Player) damager).getFoodData().updateFoodExpLevel(0.1);
                    }
                }
                EntityEventPacket pk = new EntityEventPacket();
                pk.eid = getId();
                pk.event = EntityEventPacket.HURT_ANIMATION;
                this.dataPacket(pk);
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Drops an item on the ground in front of the player. Returns if the item drop was successful.
     *
     * @param item to drop
     * @return bool if the item was dropped or if the item was null
     */
    public boolean dropItem(Item item) {
        if (!this.spawned || !this.isAlive()) {
            return false;
        }

        if (item.isNull()) {
            this.server.getLogger().debug(this.getName() + " attempted to drop a null item (" + item + ")");
            return true;
        }

        Vector3d motion = this.getDirectionVector().mul(0.4);

        world.dropItem(new Vector3(position.add(0, 1.3, 0)), item, new Vector3(motion), 40);

        this.setDataFlag(DATA_FLAGS, EntityFlags.ACTION, false);
        return true;
    }

    /**
     * Drops an item on the ground in front of the player. Returns the dropped item.
     *
     * @param item to drop
     * @return EntityItem if the item was dropped or null if the item was null
     */
    public EntityItem dropAndGetItem(Item item) {
        if (!this.spawned || !this.isAlive()) {
            return null;
        }

        if (item.isNull()) {
            this.server.getLogger().debug(this.getName() + " attempted to drop a null item (" + item + ")");
            return null;
        }

        Vector3d motion = this.getDirectionVector().mul(0.4);

        this.setDataFlag(DATA_FLAGS, EntityFlags.ACTION, false);

        return world.dropAndGetItem(new Vector3(position.add(0, 1.3, 0)), item, new Vector3(motion), 40);
    }

    public void sendPosition(Vector3d pos) {
        this.sendPosition(pos, this.yaw);
    }

    public void sendPosition(Vector3d pos, double yaw) {
        this.sendPosition(pos, yaw, this.pitch);
    }

    public void sendPosition(Vector3d pos, double yaw, double pitch) {
        this.sendPosition(pos, yaw, pitch, MovePlayerPacket.MODE_NORMAL);
    }

    public void sendPosition(Vector3d pos, double yaw, double pitch, int mode) {
        this.sendPosition(pos, yaw, pitch, mode, null);
    }

    public void sendPosition(Vector3d pos, double yaw, double pitch, int mode, Player[] targets) {
        var pk = new MovePlayerPacket();
        pk.eid = this.getId();
        pk.position = pos.add(0, getEyeHeight(), 0).toFloat();
        pk.headYaw = (float) yaw;
        pk.pitch = (float) pitch;
        pk.yaw = (float) yaw;
        pk.mode = mode;

        if (riding != null) {
            pk.ridingEid = this.riding.getId();
            pk.mode = MovePlayerPacket.MODE_PITCH;
        }

        if (targets != null) {
            Server.broadcastPacket(targets, pk);
        } else {
            dataPacket(pk);
        }
    }

    @Override
    protected void checkChunks() {
        if (this.chunk == null || (this.chunk.getX() != getChunkX() || this.chunk.getZ() != getChunkZ())) {
            if (this.chunk != null) {
                this.chunk.removeEntity(this);
            }
            this.chunk = world.getChunk(getChunkX(), getChunkZ(), true);

            if (!this.justCreated) {
                Map<Integer, Player> newChunk = world.getChunkPlayers(getChunkX(), getChunkZ());
                newChunk.remove(this.getLoaderId());

                //List<Player> reload = new ArrayList<>();
                for (Player player : new ArrayList<>(this.hasSpawned.values())) {
                    if (!newChunk.containsKey(player.getLoaderId())) {
                        this.despawnFrom(player);
                    } else {
                        newChunk.remove(player.getLoaderId());
                        //reload.add(player);
                    }
                }

                for (Player player : newChunk.values()) {
                    this.spawnTo(player);
                }
            }

            if (this.chunk == null) {
                return;
            }

            this.chunk.addEntity(this);
        }
    }

    protected boolean checkTeleportPosition() {
        if (teleportPosition != null) {
            int chunkX = teleportPosition.floorX() >> 4;
            int chunkZ = teleportPosition.floorZ() >> 4;

            for (int X = -1; X <= 1; ++X) {
                for (int Z = -1; Z <= 1; ++Z) {
                    long index = Level.chunkHash(chunkX + X, chunkZ + Z);
                    if (!this.usedChunks.containsKey(index) || !this.usedChunks.get(index)) {
                        return false;
                    }
                }
            }

            this.spawnToAll();
            this.forceMovement = this.teleportPosition;
            this.teleportPosition = null;
            return true;
        }

        return false;
    }

    protected void sendPlayStatus(int status) {
        sendPlayStatus(status, false);
    }

    protected void sendPlayStatus(int status, boolean immediate) {
        PlayStatusPacket pk = new PlayStatusPacket();
        pk.status = status;

        this.dataPacket(pk);
    }

    @Override
    public boolean teleport(Point point) {
        if (!isOnline()) return false;

        Point from = getPoint();
        Point to = point;

        PlayerTeleportEvent event = new PlayerTeleportEvent(this, from, to);
        server.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;
        to = event.getTo();

        //TODO Remove it! A hack to solve the client-side teleporting bug! (inside into the block)
        var pos = to.getPosition();
        if (super.teleport(pos.y() == pos.floorY()? to.withPosition(pos.add(0, 0.00001, 0)) : to)) {
            removeAllWindows();

            teleportPosition = position;
            forceMovement = this.teleportPosition;
            sendPosition(position, this.yaw, this.pitch, MovePlayerPacket.MODE_TELEPORT);

            checkTeleportPosition();

            resetFallDistance();
            nextChunkOrderRun = 0;
            resetClientMovement();

            getDummyBossBars().values().forEach(DummyBossBar::reshow);
            world.sendWeather(this);
            world.sendTime(this);
            return true;
        }

        return false;
    }

    protected void forceSendEmptyChunks() {
        int chunkPositionX = getChunkX();
        int chunkPositionZ = getChunkZ();
        for (int x = -chunkRadius; x < chunkRadius; x++) {
            for (int z = -chunkRadius; z < chunkRadius; z++) {
                LevelChunkPacket chunk = new LevelChunkPacket();
                chunk.chunkX = chunkPositionX + x;
                chunk.chunkZ = chunkPositionZ + z;
                chunk.data = new byte[0];
                this.dataPacket(chunk);
            }
        }
    }

    public int showFormWindow(FormWindow window) {
        return showFormWindow(window, this.formWindowCount++);
    }

    public int showFormWindow(FormWindow window, int id) {
        ModalFormRequestPacket packet = new ModalFormRequestPacket();
        packet.formId = id;
        packet.data = window.getJSONData();
        this.formWindows.put(packet.formId, window);

        this.dataPacket(packet);
        return id;
    }

    public int addServerSettings(FormWindow window) {
        int id = this.formWindowCount++;

        this.serverSettings.put(id, window);
        return id;
    }

    public long createBossBar(String text, int length) {
        DummyBossBar bossBar = new DummyBossBar.Builder(this).text(text).length(length).build();
        return this.createBossBar(bossBar);
    }

    public long createBossBar(DummyBossBar dummyBossBar) {
        this.dummyBossBars.put(dummyBossBar.getBossBarId(), dummyBossBar);
        dummyBossBar.create();
        return dummyBossBar.getBossBarId();
    }

    public DummyBossBar getDummyBossBar(long bossBarId) {
        return this.dummyBossBars.getOrDefault(bossBarId, null);
    }

    public Map<Long, DummyBossBar> getDummyBossBars() {
        return dummyBossBars;
    }

    public void updateBossBar(String text, int length, long bossBarId) {
        if (this.dummyBossBars.containsKey(bossBarId)) {
            DummyBossBar bossBar = this.dummyBossBars.get(bossBarId);
            bossBar.setText(text);
            bossBar.setLength(length);
        }
    }

    public void removeBossBar(long bossBarId) {
        if (this.dummyBossBars.containsKey(bossBarId)) {
            this.dummyBossBars.get(bossBarId).destroy();
            this.dummyBossBars.remove(bossBarId);
        }
    }

    public int getWindowId(Inventory inventory) {
        if (this.windows.containsKey(inventory)) {
            return this.windows.get(inventory);
        }

        return -1;
    }

    public Inventory getWindowById(int id) {
        return this.windowIndex.get(id);
    }

    public int addWindow(Inventory inventory) {
        return this.addWindow(inventory, null);
    }

    public int addWindow(Inventory inventory, Integer forceId) {
        return addWindow(inventory, forceId, false);
    }

    public int addWindow(Inventory inventory, Integer forceId, boolean isPermanent) {
        return addWindow(inventory, forceId, isPermanent, false);
    }

    public int addWindow(Inventory inventory, Integer forceId, boolean isPermanent, boolean alwaysOpen) {
        if (this.windows.containsKey(inventory)) {
            return this.windows.get(inventory);
        }
        int cnt;
        if (forceId == null) {
            this.windowCnt = cnt = Math.max(4, ++this.windowCnt % 99);
        } else {
            cnt = forceId;
        }
        this.windows.forcePut(inventory, cnt);

        if (isPermanent) {
            this.permanentWindows.add(cnt);
        }

        if (this.spawned && inventory.open(this)) {
            return cnt;
        } else if (!alwaysOpen) {
            this.removeWindow(inventory);

            return -1;
        } else {
            inventory.getViewers().add(this);
        }

        return cnt;
    }

    public Optional<Inventory> getTopWindow() {
        for (Entry<Inventory, Integer> entry : this.windows.entrySet()) {
            if (!this.permanentWindows.contains(entry.getValue())) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    public void removeWindow(Inventory inventory) {
        this.removeWindow(inventory, false);
    }

    protected void removeWindow(Inventory inventory, boolean isResponse) {
        inventory.close(this);
        if (isResponse && !this.permanentWindows.contains(this.getWindowId(inventory)))
            this.windows.remove(inventory);
    }

    public void sendAllInventories() {
        for (Inventory inv : this.windows.keySet()) {
            inv.sendContents(this);

            if (inv instanceof PlayerInventory) {
                ((PlayerInventory) inv).sendArmorContents(this);
            }
        }
    }

    protected void addDefaultWindows() {
        this.addWindow(this.getInventory(), ContainerIds.INVENTORY, true, true);

        this.playerUIInventory = new PlayerUIInventory(this);
        this.addWindow(this.playerUIInventory, ContainerIds.UI, true);
        this.addWindow(this.offhandInventory, ContainerIds.OFFHAND, true, true);

        this.craftingGrid = this.playerUIInventory.getCraftingGrid();
        this.addWindow(this.craftingGrid, ContainerIds.NONE);

        //TODO: more windows
    }

    public PlayerUIInventory getUIInventory() {
        return playerUIInventory;
    }

    public PlayerCursorInventory getCursorInventory() {
        return this.playerUIInventory.getCursorInventory();
    }

    public CraftingGrid getCraftingGrid() {
        return this.craftingGrid;
    }

    public void setCraftingGrid(CraftingGrid grid) {
        this.craftingGrid = grid;
        this.addWindow(grid, ContainerIds.NONE);
    }

    public void resetCraftingGridType() {
        if (this.craftingGrid != null) {
            Item[] drops = this.inventory.addItem(this.craftingGrid.getContents().values().toArray(new Item[0]));

            if (drops.length > 0) {
                for (Item drop : drops) {
                    this.dropItem(drop);
                }
            }

            drops = this.inventory.addItem(this.getCursorInventory().getItem(0));
            if (drops.length > 0) {
                for (Item drop : drops) {
                    this.dropItem(drop);
                }
            }

            this.playerUIInventory.clearAll();

            if (this.craftingGrid instanceof BigCraftingGrid) {
                this.craftingGrid = this.playerUIInventory.getCraftingGrid();
                this.addWindow(this.craftingGrid, ContainerIds.NONE);
//
//                ContainerClosePacket pk = new ContainerClosePacket(); //be sure, big crafting is really closed
//                pk.windowId = ContainerIds.NONE;
//                this.dataPacket(pk);
            }

            this.craftingType = CRAFTING_SMALL;
        }
    }

    public void removeAllWindows() {
        removeAllWindows(false);
    }

    public void removeAllWindows(boolean permanent) {
        for (Entry<Integer, Inventory> entry : new ArrayList<>(this.windowIndex.entrySet())) {
            if (!permanent && this.permanentWindows.contains(entry.getKey())) {
                continue;
            }
            this.removeWindow(entry.getValue());
        }
    }

    public int getClosingWindowId() {
        return this.closingWindowId;
    }

    @Override
    public void setMetadata(String metadataKey, MetadataValue newMetadataValue) {
        this.server.getPlayerMetadata().setMetadata(this, metadataKey, newMetadataValue);
    }

    @Override
    public List<MetadataValue> getMetadata(String metadataKey) {
        return this.server.getPlayerMetadata().getMetadata(this, metadataKey);
    }

    @Override
    public boolean hasMetadata(String metadataKey) {
        return this.server.getPlayerMetadata().hasMetadata(this, metadataKey);
    }

    @Override
    public void removeMetadata(String metadataKey, Plugin owningPlugin) {
        this.server.getPlayerMetadata().removeMetadata(this, metadataKey, owningPlugin);
    }

    @Override
    public void onChunkChanged(FullChunk chunk) {
        this.usedChunks.remove(Level.chunkHash(chunk.getX(), chunk.getZ()));
    }

    @Override
    public void onChunkLoaded(FullChunk chunk) {

    }

    @Override
    public void onChunkPopulated(FullChunk chunk) {

    }

    @Override
    public void onChunkUnloaded(FullChunk chunk) {

    }

    @Override
    public void onBlockChanged(Vector3i block) {

    }

    @Override
    public int getLoaderId() {
        return this.loaderId;
    }

    @Override
    public boolean isLoaderActive() {
        return this.isConnected();
    }


    public static BatchPacket getChunkCacheFromData(int chunkX, int chunkZ, int subChunkCount, byte[] payload) {
        LevelChunkPacket pk = new LevelChunkPacket();
        pk.chunkX = chunkX;
        pk.chunkZ = chunkZ;
        pk.subChunkCount = subChunkCount;
        pk.data = payload;
        pk.encode();

        BatchPacket batch = new BatchPacket();
        byte[][] batchPayload = new byte[2][];
        byte[] buf = pk.getBuffer();
        batchPayload[0] = Binary.writeUnsignedVarInt(buf.length);
        batchPayload[1] = buf;
        byte[] data = Binary.appendBytes(batchPayload);
        try {
            batch.payload = Network.deflateRaw(data, Server.getInstance().networkCompressionLevel);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return batch;
    }

    private boolean foodEnabled = true;

    public boolean isFoodEnabled() {
        return !(this.isCreative() || this.isSpectator()) && this.foodEnabled;
    }

    public void setFoodEnabled(boolean foodEnabled) {
        this.foodEnabled = foodEnabled;
    }

    public PlayerFood getFoodData() {
        return this.foodData;
    }

    //todo a lot on dimension

    private void setDimension(int dimension) {
        ChangeDimensionPacket pk = new ChangeDimensionPacket();
        pk.dimension = dimension;
        pk.position = position.toFloat();
        dataPacket(pk);
    }

    @Override
    public boolean setWorld(Level world) {
        Level oldWorld = this.world;
        if (super.setWorld(world)) {
            SetSpawnPositionPacket spawnPosition = new SetSpawnPositionPacket();
            spawnPosition.spawnType = SetSpawnPositionPacket.TYPE_WORLD_SPAWN;
            var spawn = world.getSpawnPoint();
            spawnPosition.position = spawn.getPosition().toInt();
            this.dataPacket(spawnPosition);

            // Remove old chunks
            for (long index : new ArrayList<>(this.usedChunks.keySet())) {
                int chunkX = Level.getHashX(index);
                int chunkZ = Level.getHashZ(index);
                this.unloadChunk(chunkX, chunkZ, oldWorld);
            }
            this.usedChunks.clear();

            SetTimePacket setTime = new SetTimePacket();
            setTime.time = world.getTime();
            this.dataPacket(setTime);

            GameRulesChangedPacket gameRulesChanged = new GameRulesChangedPacket();
            gameRulesChanged.gameRules = world.getGameRules();
            this.dataPacket(gameRulesChanged);
            return true;
        }

        return false;
    }

    public void setCheckMovement(boolean checkMovement) {
        this.checkMovement = checkMovement;
    }

    public boolean isCheckingMovement() {
        return this.checkMovement;
    }

    public synchronized void setLocale(Locale locale) {
        this.locale.set(locale);
    }

    public synchronized Locale getLocale() {
        return this.locale.get();
    }

    @Override
    public void setSprinting(boolean value) {
        if (isSprinting() != value) {
            super.setSprinting(value);

            if(this.hasEffect(Effect.SPEED)) {
                float movementSpeed = this.getMovementSpeed();
                this.sendMovementSpeed(value ? movementSpeed * 1.3f : movementSpeed);
            }
        }
    }

    public void transfer(InetSocketAddress address) {
        TransferPacket pk = new TransferPacket();
        pk.address = address.getAddress().getHostAddress();
        pk.port = address.getPort();
        this.dataPacket(pk);
    }

    public LoginChainData getLoginChainData() {
        return this.loginChainData;
    }

    public boolean pickupEntity(Entity entity, boolean near) {
        if (!this.spawned || !this.isAlive() || !this.isOnline() || this.isSpectator() || entity.isRemoved()) {
            return false;
        }

        int tick = this.getServer().getTick();
        return false;
    }

    @Override
    public int hashCode() {
        if ((this.hash == 0) || (this.hash == 485)) {
            this.hash = (485 + (getUniqueId() != null ? getUniqueId().hashCode() : 0));
        }

        return this.hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Player)) {
            return false;
        }
        Player other = (Player) obj;
        return Objects.equals(this.getUniqueId(), other.getUniqueId()) && this.getId() == other.getId();
    }

    public boolean isBreakingBlock() {
        return this.breakingBlock != null;
    }

    /**
     * Show a window of a XBOX account's profile
     * @param xuid XUID
     */
    public void showXboxProfile(String xuid) {
        ShowProfilePacket pk = new ShowProfilePacket();
        pk.xuid = xuid;
        this.dataPacket(pk);
    }

    @Override
    public String toString() {
        return "Player(name='" + getName() +
                "', location=" + super.toString() +
                ')';
    }

    public NetworkPlayerSession getNetworkSession() {
        return this.networkSession;
    }
}
