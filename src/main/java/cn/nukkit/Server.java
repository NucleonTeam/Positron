package cn.nukkit;

import cn.nukkit.block.Block;
import cn.nukkit.console.NukkitConsole;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.entity.data.Skin;
import cn.nukkit.entity.item.*;
import cn.nukkit.entity.weather.EntityLightning;
import cn.nukkit.event.HandlerList;
import cn.nukkit.event.level.LevelInitEvent;
import cn.nukkit.event.level.LevelLoadEvent;
import cn.nukkit.event.server.BatchPacketsEvent;
import cn.nukkit.event.server.PlayerDataSerializeEvent;
import cn.nukkit.event.server.QueryRegenerateEvent;
import cn.nukkit.event.server.ServerStopEvent;
import cn.nukkit.inventory.CraftingManager;
import cn.nukkit.item.Item;
import cn.nukkit.item.RuntimeItems;
import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.lang.BaseLang;
import cn.nukkit.level.GlobalBlockPalette;
import cn.nukkit.level.Level;
import cn.nukkit.level.biome.EnumBiome;
import cn.nukkit.level.format.LevelProvider;
import cn.nukkit.level.format.LevelProviderManager;
import cn.nukkit.level.format.anvil.Anvil;
import cn.nukkit.level.generator.Flat;
import cn.nukkit.level.generator.Generator;
import cn.nukkit.metadata.EntityMetadataStore;
import cn.nukkit.metadata.LevelMetadataStore;
import cn.nukkit.metadata.PlayerMetadataStore;
import cn.nukkit.nbt.NBTIO;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.DoubleTag;
import cn.nukkit.nbt.tag.FloatTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.network.CompressBatchedTask;
import cn.nukkit.network.Network;
import cn.nukkit.network.RakNetInterface;
import cn.nukkit.network.SourceInterface;
import cn.nukkit.network.protocol.BatchPacket;
import cn.nukkit.network.protocol.DataPacket;
import cn.nukkit.network.protocol.PlayerListPacket;
import cn.nukkit.network.protocol.ProtocolInfo;
import cn.nukkit.network.query.QueryHandler;
import cn.nukkit.plugin.JavaPluginLoader;
import cn.nukkit.plugin.Plugin;
import cn.nukkit.plugin.PluginLoadOrder;
import cn.nukkit.plugin.PluginManager;
import cn.nukkit.plugin.service.NKServiceManager;
import cn.nukkit.plugin.service.ServiceManager;
import cn.nukkit.potion.Effect;
import cn.nukkit.potion.Potion;
import cn.nukkit.resourcepacks.ResourcePackManager;
import cn.nukkit.scheduler.ServerScheduler;
import cn.nukkit.scheduler.Task;
import cn.nukkit.utils.*;
import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.impl.Iq80DBFactory;
import ru.mc_positron.math.FastMath;
import ru.mc_positron.player.PlayerManager;
import ru.mc_positron.player.PositronPlayerManager;
import ru.mc_positron.registry.Registry;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

@Log4j2
public class Server {

    private static final long START_TIME = System.currentTimeMillis();
    public static int DEBUG_LEVEL = 1;
    private static Server instance = null;
    private final AtomicBoolean isRunning = new AtomicBoolean(true);
    private boolean hasStopped = false;
    private final PluginManager pluginManager;
    private final ServerScheduler scheduler;
    private int tickCounter;
    private long nextTick;
    private final float[] tickAverage = {20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20};
    private final float[] useAverage = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private float maxTick = 20;
    private float maxUse = 0;
    private int sendUsageTicker = 0;
    private final NukkitConsole console;
    private final ConsoleThread consoleThread;
    private final CraftingManager craftingManager;
    private final ResourcePackManager resourcePackManager;
    private final int maxPlayers;
    private boolean autoSave = true;
    private final EntityMetadataStore entityMetadata;
    private final PlayerMetadataStore playerMetadata;
    private final LevelMetadataStore levelMetadata;
    private final Network network;
    private final boolean networkCompressionAsync;
    public int networkCompressionLevel;
    private final boolean autoTickRate;
    private final int autoTickRateLimit;
    private final boolean alwaysTickPlayers;
    private final int baseTickRate;
    private Boolean getAllowFlight = null;
    private int difficulty = Integer.MAX_VALUE;
    private int defaultGamemode = Integer.MAX_VALUE;
    private int autoSaveTicker = 0;
    private int autoSaveTicks = 6000;
    private final BaseLang baseLang;
    private final boolean forceLanguage;
    private final String dataPath;
    private QueryHandler queryHandler;
    private QueryRegenerateEvent queryRegenerateEvent;
    private final Config properties;
    private final Config config;
    private final Map<Integer, Level> levels = new HashMap<>() {
        public Level put(Integer key, Level value) {
            Level result = super.put(key, value);
            levelArray = levels.values().toArray(new Level[0]);
            return result;
        }

        public boolean remove(Object key, Object value) {
            boolean result = super.remove(key, value);
            levelArray = levels.values().toArray(new Level[0]);
            return result;
        }

        public Level remove(Object key) {
            Level result = super.remove(key);
            levelArray = levels.values().toArray(new Level[0]);
            return result;
        }
    };
    private Level[] levelArray = new Level[0];
    private final ServiceManager serviceManager = new NKServiceManager();
    private Level defaultLevel = null;
    private final Thread currentThread;
    private Watchdog watchdog;
    private final DB nameLookup;
    private final PlayerDataSerializer playerDataSerializer;
    private final Set<String> ignoredPackets = new HashSet<>();

    @Getter private final PlayerManager playerManager = new PositronPlayerManager(this);

    public static Server init() {
        var path = System.getProperty("user.dir") + "/";
        var dataPath = System.getProperty("user.dir") + "/";
        var pluginPath = dataPath + "plugins";

        return new Server(path, dataPath, pluginPath, null);
    }

    private Server(final String filePath, String dataPath, String pluginPath, String predefinedLanguage) {
        var completeInitialization = Registry.init();

        Preconditions.checkState(instance == null, "Already initialized!");
        currentThread = Thread.currentThread(); // Saves the current thread instance as a reference, used in Server#isPrimaryThread()
        instance = this;

        if (!new File(dataPath + "worlds/").exists()) {
            new File(dataPath + "worlds/").mkdirs();
        }

        if (!new File(dataPath + "players/").exists()) {
            new File(dataPath + "players/").mkdirs();
        }

        if (!new File(pluginPath).exists()) {
            new File(pluginPath).mkdirs();
        }

        this.dataPath = new File(dataPath).getAbsolutePath() + "/";
        String pluginPath1 = new File(pluginPath).getAbsolutePath() + "/";

        this.console = new NukkitConsole(this);
        this.consoleThread = new ConsoleThread();
        this.consoleThread.start();

        this.playerDataSerializer = new DefaultPlayerDataSerializer(this);

        //todo: VersionString 现在不必要

        if (!new File(this.dataPath + "nukkit.yml").exists()) {
            this.getLogger().info(TextFormat.GREEN + "Welcome! Please choose a language first!");
            try {
                InputStream languageList = this.getClass().getClassLoader().getResourceAsStream("lang/language.list");
                if (languageList == null) {
                    throw new IllegalStateException("lang/language.list is missing. If you are running a development version, make sure you have run 'git submodule update --init'.");
                }
                String[] lines = Utils.readFile(languageList).split("\n");
                for (String line : lines) {
                    this.getLogger().info(line);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            String fallback = BaseLang.FALLBACK_LANGUAGE;
            String language = null;
            while (language == null) {
                String lang;
                if (predefinedLanguage != null) {
                    log.info("Trying to load language from predefined language: " + predefinedLanguage);
                    lang = predefinedLanguage;
                } else {
                    lang = this.console.readLine();
                }

                InputStream conf = this.getClass().getClassLoader().getResourceAsStream("lang/" + lang + "/lang.ini");
                if (conf != null) {
                    language = lang;
                } else if(predefinedLanguage != null) {
                    log.warn("No language found for predefined language: " + predefinedLanguage + ", please choose a valid language");
                    predefinedLanguage = null;
                }
            }

            InputStream advacedConf = this.getClass().getClassLoader().getResourceAsStream("lang/" + language + "/nukkit.yml");
            if (advacedConf == null) {
                advacedConf = this.getClass().getClassLoader().getResourceAsStream("lang/" + fallback + "/nukkit.yml");
            }

            try {
                Utils.writeFile(this.dataPath + "nukkit.yml", advacedConf);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }

        this.console.setExecutingCommands(true);

        log.info("Loading {} ...", TextFormat.GREEN + "nukkit.yml" + TextFormat.WHITE);
        this.config = new Config(this.dataPath + "nukkit.yml", Config.YAML);

        DEBUG_LEVEL = FastMath.clamp(this.getConfig("debug.level", 1), 1, 3);

        int logLevel = (DEBUG_LEVEL + 3) * 100;
        var currentLevel = Server.getLogLevel();
        for (var level: org.apache.logging.log4j.Level.values()) {
            if (level.intLevel() == logLevel && level.intLevel() > currentLevel.intLevel()) {
                Server.setLogLevel(level);
                break;
            }
        }

        ignoredPackets.addAll(getConfig().getStringList("debug.ignored-packets"));
        ignoredPackets.add("BatchPacket");

        log.info("Loading {} ...", TextFormat.GREEN + "server.properties" + TextFormat.WHITE);
        this.properties = new Config(this.dataPath + "server.properties", Config.PROPERTIES, new ConfigSection() {
            {
                put("motd", "A Nukkit Powered Server");
                put("sub-motd", "https://nukkitx.com");
                put("server-port", 19132);
                put("server-ip", "0.0.0.0");
                put("view-distance", 10);
                put("white-list", false);
                put("achievements", true);
                put("announce-player-achievements", true);
                put("spawn-protection", 16);
                put("max-players", 20);
                put("allow-flight", false);
                put("spawn-animals", true);
                put("spawn-mobs", true);
                put("gamemode", 0);
                put("force-gamemode", false);
                put("hardcore", false);
                put("pvp", true);
                put("difficulty", 1);
                put("generator-settings", "");
                put("level-name", "world");
                put("level-seed", "");
                put("level-type", "DEFAULT");
                put("allow-nether", true);
                put("enable-query", true);
                put("enable-rcon", false);
                put("rcon.password", Base64.getEncoder().encodeToString(UUID.randomUUID().toString().replace("-", "").getBytes()).substring(3, 13));
                put("auto-save", true);
                put("force-resources", false);
                put("xbox-auth", true);
            }
        });

        this.forceLanguage = this.getConfig("settings.force-language", false);
        this.baseLang = new BaseLang(this.getConfig("settings.language", BaseLang.FALLBACK_LANGUAGE));
        log.info(this.getLanguage().translateString("language.selected", new String[]{getLanguage().getName(), getLanguage().getLang()}));
        log.info(getLanguage().translateString("nukkit.server.start", TextFormat.AQUA + this.getVersion() + TextFormat.RESET));

        Object poolSize = this.getConfig("settings.async-workers", (Object) "auto");
        if (!(poolSize instanceof Integer)) {
            try {
                poolSize = Integer.valueOf((String) poolSize);
            } catch (Exception e) {
                poolSize = Math.max(Runtime.getRuntime().availableProcessors() + 1, 4);
            }
        }

        ServerScheduler.WORKERS = (int) poolSize;

        int networkZlibProvider = this.getConfig("network.zlib-provider", 2);
        Zlib.setProvider(networkZlibProvider);

        this.networkCompressionLevel = this.getConfig("network.compression-level", 7);
        this.networkCompressionAsync = this.getConfig("network.async-compression", true);

        this.autoTickRate = this.getConfig("level-settings.auto-tick-rate", true);
        this.autoTickRateLimit = this.getConfig("level-settings.auto-tick-rate-limit", 20);
        this.alwaysTickPlayers = this.getConfig("level-settings.always-tick-players", false);
        this.baseTickRate = this.getConfig("level-settings.base-tick-rate", 1);

        this.scheduler = new ServerScheduler();

        this.entityMetadata = new EntityMetadataStore();
        this.playerMetadata = new PlayerMetadataStore();
        this.levelMetadata = new LevelMetadataStore();

        this.maxPlayers = this.getPropertyInt("max-players", 20);
        this.setAutoSave(this.getPropertyBoolean("auto-save", true));

        if (this.getPropertyBoolean("hardcore", false) && this.getDifficulty() < 3) {
            this.setPropertyInt("difficulty", 3);
        }

        log.info(this.getLanguage().translateString("nukkit.server.networkStart", new String[]{this.getIp().equals("") ? "*" : this.getIp(), String.valueOf(this.getPort())}));

        this.network = new Network(this);
        this.network.setName(this.getMotd());
        this.network.setSubName(this.getSubMotd());

        log.info(this.getLanguage().translateString("nukkit.server.info", this.getName(), TextFormat.YELLOW + this.getNukkitVersion() + TextFormat.WHITE, TextFormat.AQUA + this.getCodename() + TextFormat.WHITE, this.getApiVersion()));
        log.info(this.getLanguage().translateString("nukkit.server.license", this.getName()));

        this.registerEntities();
        this.registerBlockEntities();
        pluginManager = new PluginManager(this);

        Block.init();
        Enchantment.init();
        RuntimeItems.init();
        Item.init();
        EnumBiome.values(); //load class, this also registers biomes
        Effect.init();
        Potion.init();
        GlobalBlockPalette.getOrCreateRuntimeId(0, 0); //Force it to load

        // Convert legacy data before plugins get the chance to mess with it.
        try {
            nameLookup = Iq80DBFactory.factory.open(new File(dataPath, "players"), new Options()
                    .createIfMissing(true)
                    .compressionType(CompressionType.ZLIB_RAW));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        convertLegacyPlayerData();

        this.craftingManager = new CraftingManager();
        this.resourcePackManager = new ResourcePackManager(new File(this.dataPath, "resource_packs"));

        this.pluginManager.registerInterface(JavaPluginLoader.class);

        this.queryRegenerateEvent = new QueryRegenerateEvent(this, 5);

        this.network.registerInterface(new RakNetInterface(this));

        this.pluginManager.loadPlugins(pluginPath1);

        this.enablePlugins(PluginLoadOrder.STARTUP);

        LevelProviderManager.addProvider(this, Anvil.class);

        Generator.addGenerator(Flat.class, "flat", Generator.TYPE_FLAT);
        //todo: add old generator and hell generator

        completeInitialization.accept(this);

        for (String name : this.getConfig("worlds", new HashMap<String, Object>()).keySet()) {
            if (!this.loadLevel(name)) {
                long seed;
                try {
                    seed = ((Integer) this.getConfig("worlds." + name + ".seed")).longValue();
                } catch (Exception e) {
                    seed = System.currentTimeMillis();
                }

                Map<String, Object> options = new HashMap<>();
                String[] opts = (this.getConfig("worlds." + name + ".generator", Generator.getGenerator("default").getSimpleName())).split(":");
                Class<? extends Generator> generator = Generator.getGenerator(opts[0]);
                if (opts.length > 1) {
                    StringBuilder preset = new StringBuilder();
                    for (int i = 1; i < opts.length; i++) {
                        preset.append(opts[i]).append(":");
                    }
                    preset = new StringBuilder(preset.substring(0, preset.length() - 1));

                    options.put("preset", preset.toString());
                }

                this.generateLevel(name, seed, generator, options);
            }
        }

        if (this.getDefaultLevel() == null) {
            String defaultName = this.getPropertyString("level-name", "world");
            if (defaultName == null || defaultName.trim().isEmpty()) {
                this.getLogger().warning("level-name cannot be null, using default");
                defaultName = "world";
                this.setPropertyString("level-name", defaultName);
            }

            if (!this.loadLevel(defaultName)) {
                long seed;
                String seedString = String.valueOf(this.getProperty("level-seed", System.currentTimeMillis()));
                try {
                    seed = Long.parseLong(seedString);
                } catch (NumberFormatException e) {
                    seed = seedString.hashCode();
                }
                this.generateLevel(defaultName, seed == 0 ? System.currentTimeMillis() : seed);
            }

            this.setDefaultLevel(this.getLevelByName(defaultName));
        }

        this.properties.save(true);

        if (this.getDefaultLevel() == null) {
            this.getLogger().emergency(this.getLanguage().translateString("nukkit.level.defaultError"));
            this.forceShutdown();

            return;
        }

        if (this.getConfig("ticks-per.autosave", 6000) > 0) {
            this.autoSaveTicks = this.getConfig("ticks-per.autosave", 6000);
        }

        this.enablePlugins(PluginLoadOrder.POSTWORLD);

        if (DEBUG_LEVEL < 2) {
            this.watchdog = new Watchdog(this, 60000);
            this.watchdog.start();
        }

        this.start();
    }

    public int broadcastMessage(String message, Collection<? extends Player> recipients) {
        for (Player recipient : recipients) {
            recipient.sendMessage(message);
        }

        return recipients.size();
    }

    public static void broadcastPacket(Collection<Player> players, DataPacket packet) {
        packet.tryEncode();

        for (Player player : players) {
            player.dataPacket(packet);
        }
    }

    public static void broadcastPacket(Player[] players, DataPacket packet) {
        packet.tryEncode();

        for (Player player : players) {
            player.dataPacket(packet);
        }
    }

    @Deprecated
    public void batchPackets(Player[] players, DataPacket[] packets) {
        this.batchPackets(players, packets, false);
    }

    @Deprecated
    public void batchPackets(Player[] players, DataPacket[] packets, boolean forceSync) {
        if (players == null || packets == null || players.length == 0 || packets.length == 0) {
            return;
        }

        BatchPacketsEvent ev = new BatchPacketsEvent(players, packets, forceSync);
        getPluginManager().callEvent(ev);
        if (ev.isCancelled()) {
            return;
        }

        byte[][] payload = new byte[packets.length * 2][];
        for (int i = 0; i < packets.length; i++) {
            DataPacket p = packets[i];
            int idx = i * 2;
            p.tryEncode();
            byte[] buf = p.getBuffer();
            payload[idx] = Binary.writeUnsignedVarInt(buf.length);
            payload[idx + 1] = buf;
            packets[i] = null;
        }

        List<InetSocketAddress> targets = new ArrayList<>();
        for (Player p : players) {
            if (p.isConnected()) {
                targets.add(p.getSocketAddress());
            }
        }

        if (!forceSync && this.networkCompressionAsync) {
            this.getScheduler().scheduleAsyncTask(new CompressBatchedTask(payload, targets, this.networkCompressionLevel));
        } else {
            try {
                byte[] data = Binary.appendBytes(payload);
                this.broadcastPacketsCallback(Network.deflateRaw(data, this.networkCompressionLevel), targets);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void broadcastPacketsCallback(byte[] data, List<InetSocketAddress> targets) {
        BatchPacket pk = new BatchPacket();
        pk.payload = data;

        targets.stream()
                .map(playerManager::getPlayerConnection)
                .filter(Objects::nonNull)
                .forEach(connection -> connection.dataPacket(pk));
    }

    public void enablePlugins(PluginLoadOrder type) {
        for (Plugin plugin : new ArrayList<>(this.pluginManager.getPlugins().values())) {
            if (!plugin.isEnabled() && type == plugin.getDescription().getOrder()) {
                this.enablePlugin(plugin);
            }
        }
    }

    public void enablePlugin(Plugin plugin) {
        this.pluginManager.enablePlugin(plugin);
    }

    public void shutdown() {
        isRunning.compareAndSet(true, false);
    }

    public void forceShutdown() {
        if (this.hasStopped) {
            return;
        }

        try {
            isRunning.compareAndSet(true, false);

            this.hasStopped = true;

            ServerStopEvent serverStopEvent = new ServerStopEvent();
            getPluginManager().callEvent(serverStopEvent);

            for (var connection: playerManager.getConnectedPlayers()) {
                connection.remove(
                        connection.getLeaveMessage(),
                        this.getConfig("settings.shutdown-message",
                        "Server closed")
                );
            }

            this.getLogger().debug("Disabling all plugins");
            this.pluginManager.disablePlugins();

            this.getLogger().debug("Removing event handlers");
            HandlerList.unregisterAll();

            this.getLogger().debug("Stopping all tasks");
            this.scheduler.cancelAllTasks();
            this.scheduler.mainThreadHeartbeat(Integer.MAX_VALUE);

            this.getLogger().debug("Unloading all levels");
            for (Level level : this.levelArray) {
                this.unloadLevel(level, true);
            }

            this.getLogger().debug("Closing console");
            this.consoleThread.interrupt();

            this.getLogger().debug("Stopping network interfaces");
            for (SourceInterface interfaz : this.network.getInterfaces()) {
                interfaz.shutdown();
                this.network.unregisterInterface(interfaz);
            }

            if (nameLookup != null) {
                nameLookup.close();
            }

            this.getLogger().debug("Disabling timings");
            if (this.watchdog != null) {
                this.watchdog.kill();
            }
            //todo other things
        } catch (Exception e) {
            log.fatal("Exception happened while shutting down, exiting the process", e);
            System.exit(1);
        }
    }

    public void start() {
        if (this.getPropertyBoolean("enable-query", true)) {
            this.queryHandler = new QueryHandler();
        }

        //todo send usage setting
        this.tickCounter = 0;

        log.info(this.getLanguage().translateString("nukkit.server.defaultGameMode", getGamemodeString(this.getGamemode())));

        log.info(this.getLanguage().translateString("nukkit.server.startFinished", String.valueOf((double) (System.currentTimeMillis() - START_TIME) / 1000)));

        this.tickProcessor();
        this.forceShutdown();
    }

    public void handlePacket(InetSocketAddress address, ByteBuf payload) {
        try {
            if (!payload.isReadable(3)) {
                return;
            }
            byte[] prefix = new byte[2];
            payload.readBytes(prefix);

            if (!Arrays.equals(prefix, new byte[]{(byte) 0xfe, (byte) 0xfd})) {
                return;
            }
            if (this.queryHandler != null) {
                this.queryHandler.handle(address, payload);
            }
        } catch (Exception e) {
            log.error("Error whilst handling packet", e);

            this.network.blockAddress(address.getAddress(), -1);
        }
    }

    private int lastLevelGC;

    public void tickProcessor() {
        this.nextTick = System.currentTimeMillis();
        try {
            while (this.isRunning.get()) {
                try {
                    this.tick();

                    long next = this.nextTick;
                    long current = System.currentTimeMillis();

                    if (next - 0.1 > current) {
                        long allocated = next - current - 1;

                        { // Instead of wasting time, do something potentially useful
                            int offset = 0;
                            for (int i = 0; i < levelArray.length; i++) {
                                offset = (i + lastLevelGC) % levelArray.length;
                                Level level = levelArray[offset];
                                level.doGarbageCollection(allocated - 1);
                                allocated = next - System.currentTimeMillis();
                                if (allocated <= 0) {
                                    break;
                                }
                            }
                            lastLevelGC = offset + 1;
                        }

                        if (allocated > 0) {
                            Thread.sleep(allocated, 900000);
                        }
                    }
                } catch (RuntimeException e) {
                    this.getLogger().logException(e);
                }
            }
        } catch (Throwable e) {
            log.fatal("Exception happened while ticking server", e);
            log.fatal(Utils.getAllThreadDumps());
        }
    }

    public void onPlayerCompleteLoginSequence(Player player) {
        this.sendFullPlayerListData(player);
    }

    public void onPlayerLogin(Player player) {

    }

    public void updatePlayerListData(UUID uuid, long entityId, String name, Skin skin, String xboxUserId) {
        this.updatePlayerListData(uuid, entityId, name, skin, xboxUserId, playerManager.getPlayers());
    }

    public void updatePlayerListData(UUID uuid, long entityId, String name, Skin skin, Player[] players) {
        this.updatePlayerListData(uuid, entityId, name, skin, "", players);
    }

    public void updatePlayerListData(UUID uuid, long entityId, String name, Skin skin, String xboxUserId, Player[] players) {
        PlayerListPacket pk = new PlayerListPacket();
        pk.type = PlayerListPacket.TYPE_ADD;
        pk.entries = new PlayerListPacket.Entry[]{new PlayerListPacket.Entry(uuid, entityId, name, skin, xboxUserId)};
        Server.broadcastPacket(players, pk);
    }

    public void updatePlayerListData(UUID uuid, long entityId, String name, Skin skin, String xboxUserId, Collection<Player> players) {
        this.updatePlayerListData(uuid, entityId, name, skin, xboxUserId, players.toArray(new Player[0]));
    }

    public void removePlayerListData(UUID uuid, Player player) {
        PlayerListPacket pk = new PlayerListPacket();
        pk.type = PlayerListPacket.TYPE_REMOVE;
        pk.entries = new PlayerListPacket.Entry[]{new PlayerListPacket.Entry(uuid)};
        player.dataPacket(pk);
    }

    public void sendFullPlayerListData(Player player) {
        PlayerListPacket pk = new PlayerListPacket();
        pk.type = PlayerListPacket.TYPE_ADD;
        pk.entries = playerManager.getPlayers().stream()
                .map(p -> new PlayerListPacket.Entry(
                        p.getUniqueId(),
                        p.getId(),
                        p.getDisplayName(),
                        p.getSkin(),
                        p.getLoginChainData().getXUID()))
                .toArray(PlayerListPacket.Entry[]::new);

        player.dataPacket(pk);
    }

    public void sendRecipeList(Player player) {
        player.dataPacket(CraftingManager.packet);
    }

    private void checkTickUpdates(int currentTick, long tickTime) {
        for (var p: playerManager.getConnectedPlayers()) {

            if (this.alwaysTickPlayers) {
                p.onUpdate(currentTick);
            }
        }

        //Do level ticks
        for (Level level : this.levelArray) {
            if (level.getTickRate() > this.baseTickRate && --level.tickRateCounter > 0) {
                continue;
            }

            try {
                long levelTime = System.currentTimeMillis();
                level.doTick(currentTick);
                int tickMs = (int) (System.currentTimeMillis() - levelTime);
                level.tickRateTime = tickMs;

                if (this.autoTickRate) {
                    if (tickMs < 50 && level.getTickRate() > this.baseTickRate) {
                        int r;
                        level.setTickRate(r = level.getTickRate() - 1);
                        if (r > this.baseTickRate) {
                            level.tickRateCounter = level.getTickRate();
                        }
                        this.getLogger().debug("Raising level \"" + level.getName() + "\" tick rate to " + level.getTickRate() + " ticks");
                    } else if (tickMs >= 50) {
                        if (level.getTickRate() == this.baseTickRate) {
                            level.setTickRate(Math.max(this.baseTickRate + 1, Math.min(this.autoTickRateLimit, tickMs / 50)));
                            this.getLogger().debug("Level \"" + level.getName() + "\" took " + FastMath.round(tickMs, 2) + "ms, setting tick rate to " + level.getTickRate() + " ticks");
                        } else if ((tickMs / level.getTickRate()) >= 50 && level.getTickRate() < this.autoTickRateLimit) {
                            level.setTickRate(level.getTickRate() + 1);
                            this.getLogger().debug("Level \"" + level.getName() + "\" took " + FastMath.round(tickMs, 2) + "ms, setting tick rate to " + level.getTickRate() + " ticks");
                        }
                        level.tickRateCounter = level.getTickRate();
                    }
                }
            } catch (Exception e) {
                log.error(this.getLanguage().translateString("nukkit.level.tickError",
                        new String[]{level.getFolderName(), Utils.getExceptionMessage(e)}));
            }
        }
    }

    public void doAutoSave() {
        if (this.getAutoSave()) {
            for (var player: playerManager.getConnectedPlayers()) {
                if (player.isOnline()) {
                    player.save(true);
                } else if (!player.isConnected()) {
                    playerManager.removePlayerConnection(player);
                }
            }

            for (Level level : this.levelArray) {
                level.save();
            }
        }
    }

    private boolean tick() {
        long tickTime = System.currentTimeMillis();

        // TODO
        long time = tickTime - this.nextTick;
        if (time < -25) {
            try {
                Thread.sleep(Math.max(5, -time - 25));
            } catch (InterruptedException e) {
                Server.getInstance().getLogger().logException(e);
            }
        }

        long tickTimeNano = System.nanoTime();
        if ((tickTime - this.nextTick) < -25) {
            return false;
        }

        ++this.tickCounter;

        this.network.processInterfaces();

        this.scheduler.mainThreadHeartbeat(this.tickCounter);

        this.checkTickUpdates(this.tickCounter, tickTime);

        for (var player: playerManager.getConnectedPlayers()) {
            player.checkNetwork();
        }

        if ((this.tickCounter & 0b1111) == 0) {
            this.network.resetStatistics();
            this.maxTick = 20;
            this.maxUse = 0;

            if ((this.tickCounter & 0b111111111) == 0) {
                try {
                    this.getPluginManager().callEvent(this.queryRegenerateEvent = new QueryRegenerateEvent(this, 5));
                    if (this.queryHandler != null) {
                        this.queryHandler.regenerateInfo();
                    }
                } catch (Exception e) {
                    log.error(e);
                }
            }

            this.getNetwork().updateName();
        }

        if (this.autoSave && ++this.autoSaveTicker >= this.autoSaveTicks) {
            this.autoSaveTicker = 0;
            this.doAutoSave();
        }

        if (this.sendUsageTicker > 0 && --this.sendUsageTicker == 0) {
            this.sendUsageTicker = 6000;
            //todo sendUsage
        }

        if (this.tickCounter % 100 == 0) {
            for (Level level : this.levelArray) {
                level.doChunkGarbageCollection();
            }
        }

        //long now = System.currentTimeMillis();
        long nowNano = System.nanoTime();
        //float tick = Math.min(20, 1000 / Math.max(1, now - tickTime));
        //float use = Math.min(1, (now - tickTime) / 50);

        float tick = (float) Math.min(20, 1000000000 / Math.max(1000000, ((double) nowNano - tickTimeNano)));
        float use = (float) Math.min(1, ((double) (nowNano - tickTimeNano)) / 50000000);

        if (this.maxTick > tick) {
            this.maxTick = tick;
        }

        if (this.maxUse < use) {
            this.maxUse = use;
        }

        System.arraycopy(this.tickAverage, 1, this.tickAverage, 0, this.tickAverage.length - 1);
        this.tickAverage[this.tickAverage.length - 1] = tick;

        System.arraycopy(this.useAverage, 1, this.useAverage, 0, this.useAverage.length - 1);
        this.useAverage[this.useAverage.length - 1] = use;

        if ((this.nextTick - tickTime) < -1000) {
            this.nextTick = tickTime;
        } else {
            this.nextTick += 50;
        }

        return true;
    }

    public long getNextTick() {
        return nextTick;
    }

    public QueryRegenerateEvent getQueryInformation() {
        return this.queryRegenerateEvent;
    }

    public String getName() {
        return "Nukkit";
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    public String getNukkitVersion() {
        return "1.0.0";
    }

    public String getCodename() {
        return "";
    }

    public String getVersion() {
        return ProtocolInfo.MINECRAFT_VERSION;
    }

    public String getApiVersion() {
        return "1.0.14";
    }

    public String getDataPath() {
        return dataPath;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public int getPort() {
        return this.getPropertyInt("server-port", 19132);
    }

    public int getViewDistance() {
        return this.getPropertyInt("view-distance", 10);
    }

    public String getIp() {
        return this.getPropertyString("server-ip", "0.0.0.0");
    }

    public boolean getAutoSave() {
        return this.autoSave;
    }

    public void setAutoSave(boolean autoSave) {
        this.autoSave = autoSave;
        for (Level level : this.levelArray) {
            level.setAutoSave(this.autoSave);
        }
    }

    public String getLevelType() {
        return this.getPropertyString("level-type", "DEFAULT");
    }

    public int getGamemode() {
        try {
            return this.getPropertyInt("gamemode", 0) & 0b11;
        } catch (NumberFormatException exception) {
            return getGamemodeFromString(this.getPropertyString("gamemode")) & 0b11;
        }
    }

    public boolean getForceGamemode() {
        return this.getPropertyBoolean("force-gamemode", false);
    }

    public static String getGamemodeString(int mode) {
        return getGamemodeString(mode, false);
    }

    public static String getGamemodeString(int mode, boolean direct) {
        switch (mode) {
            case Player.SURVIVAL:
                return direct ? "Survival" : "%gameMode.survival";
            case Player.CREATIVE:
                return direct ? "Creative" : "%gameMode.creative";
            case Player.ADVENTURE:
                return direct ? "Adventure" : "%gameMode.adventure";
            case Player.SPECTATOR:
                return direct ? "Spectator" : "%gameMode.spectator";
        }
        return "UNKNOWN";
    }

    public static int getGamemodeFromString(String str) {
        switch (str.trim().toLowerCase()) {
            case "0":
            case "survival":
            case "s":
                return Player.SURVIVAL;

            case "1":
            case "creative":
            case "c":
                return Player.CREATIVE;

            case "2":
            case "adventure":
            case "a":
                return Player.ADVENTURE;

            case "3":
            case "spectator":
            case "spc":
            case "view":
            case "v":
                return Player.SPECTATOR;
        }
        return -1;
    }

    public static int getDifficultyFromString(String str) {
        switch (str.trim().toLowerCase()) {
            case "0":
            case "peaceful":
            case "p":
                return 0;

            case "1":
            case "easy":
            case "e":
                return 1;

            case "2":
            case "normal":
            case "n":
                return 2;

            case "3":
            case "hard":
            case "h":
                return 3;
        }
        return -1;
    }

    public int getDifficulty() {
        if (this.difficulty == Integer.MAX_VALUE) {
            this.difficulty = getDifficultyFromString(this.getPropertyString("difficulty", "1"));
        }
        return this.difficulty;
    }

    public void setDifficulty(int difficulty) {
        int value = difficulty;
        if (value < 0) value = 0;
        if (value > 3) value = 3;
        this.difficulty = value;
        this.setPropertyInt("difficulty", value);
    }

    public boolean getAllowFlight() {
        if (getAllowFlight == null) {
            getAllowFlight = this.getPropertyBoolean("allow-flight", false);
        }
        return getAllowFlight;
    }

    public int getDefaultGamemode() {
        if (this.defaultGamemode == Integer.MAX_VALUE) {
            this.defaultGamemode = this.getGamemode();
        }
        return this.defaultGamemode;
    }

    public String getMotd() {
        return this.getPropertyString("motd", "A Nukkit Powered Server");
    }

    public String getSubMotd() {
        String subMotd = this.getPropertyString("sub-motd", "https://nukkitx.com");
        if (subMotd.isEmpty()) {
            subMotd = "https://nukkitx.com"; // The client doesn't allow empty sub-motd in 1.16.210
        }
        return subMotd;
    }

    public boolean getForceResources() {
        return this.getPropertyBoolean("force-resources", false);
    }

    public MainLogger getLogger() {
        return MainLogger.getLogger();
    }

    public EntityMetadataStore getEntityMetadata() {
        return entityMetadata;
    }

    public PlayerMetadataStore getPlayerMetadata() {
        return playerMetadata;
    }

    public LevelMetadataStore getLevelMetadata() {
        return levelMetadata;
    }

    public PluginManager getPluginManager() {
        return this.pluginManager;
    }

    public CraftingManager getCraftingManager() {
        return craftingManager;
    }

    public ResourcePackManager getResourcePackManager() {
        return resourcePackManager;
    }

    public ServerScheduler getScheduler() {
        return scheduler;
    }

    public int getTick() {
        return tickCounter;
    }

    public Optional<UUID> lookupName(String name) {
        byte[] nameBytes = name.toLowerCase().getBytes(StandardCharsets.UTF_8);
        byte[] uuidBytes = nameLookup.get(nameBytes);
        if (uuidBytes == null) {
            return Optional.empty();
        }

        if (uuidBytes.length != 16) {
            log.warn("Invalid uuid in name lookup database detected! Removing");
            nameLookup.delete(nameBytes);
            return Optional.empty();
        }

        ByteBuffer buffer = ByteBuffer.wrap(uuidBytes);
        return Optional.of(new UUID(buffer.getLong(), buffer.getLong()));
    }

    void updateName(UUID uuid, String name) {
        byte[] nameBytes = name.toLowerCase().getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());

        nameLookup.put(nameBytes, buffer.array());
    }

    public CompoundTag getOfflinePlayerData(UUID uuid, boolean create) {
        return getOfflinePlayerDataInternal(uuid.toString(), true, create);
    }

    @Deprecated
    public CompoundTag getOfflinePlayerData(String name) {
        return getOfflinePlayerData(name, false);
    }

    @Deprecated
    public CompoundTag getOfflinePlayerData(String name, boolean create) {
        Optional<UUID> uuid = lookupName(name);
        return getOfflinePlayerDataInternal(uuid.map(UUID::toString).orElse(name), true, create);
    }

    private CompoundTag getOfflinePlayerDataInternal(String name, boolean runEvent, boolean create) {
        Preconditions.checkNotNull(name, "name");

        PlayerDataSerializeEvent event = new PlayerDataSerializeEvent(name, playerDataSerializer);
        if (runEvent) {
            pluginManager.callEvent(event);
        }

        Optional<InputStream> dataStream = Optional.empty();
        try {
            dataStream = event.getSerializer().read(name, event.getUuid().orElse(null));
            if (dataStream.isPresent()) {
                return NBTIO.readCompressed(dataStream.get());
            }
        } catch (IOException e) {
            log.warn(this.getLanguage().translateString("nukkit.data.playerCorrupted", name));
            log.throwing(e);
        } finally {
            if (dataStream.isPresent()) {
                try {
                    dataStream.get().close();
                } catch (IOException e) {
                    log.throwing(e);
                }
            }
        }
        CompoundTag nbt = null;
        if (create) {
            if (this.shouldSavePlayerData()) {
                log.info(this.getLanguage().translateString("nukkit.data.playerNotFound", name));
            }
            var spawn = this.getDefaultLevel().getSpawnPoint();
            nbt = new CompoundTag()
                    .putLong("firstPlayed", System.currentTimeMillis() / 1000)
                    .putLong("lastPlayed", System.currentTimeMillis() / 1000)
                    .putList(new ListTag<DoubleTag>("Pos")
                            .add(new DoubleTag("0", spawn.getPosition().x()))
                            .add(new DoubleTag("1", spawn.getPosition().y()))
                            .add(new DoubleTag("2", spawn.getPosition().z())))
                    .putString("Level", this.getDefaultLevel().getName())
                    .putList(new ListTag<>("Inventory"))
                    .putCompound("Achievements", new CompoundTag())
                    .putInt("playerGameType", this.getGamemode())
                    .putList(new ListTag<DoubleTag>("Motion")
                            .add(new DoubleTag("0", 0))
                            .add(new DoubleTag("1", 0))
                            .add(new DoubleTag("2", 0)))
                    .putList(new ListTag<FloatTag>("Rotation")
                            .add(new FloatTag("0", 0))
                            .add(new FloatTag("1", 0)))
                    .putFloat("FallDistance", 0)
                    .putShort("Fire", 0)
                    .putShort("Air", 300)
                    .putBoolean("OnGround", true)
                    .putBoolean("Invulnerable", false);

            this.saveOfflinePlayerData(name, nbt, true, runEvent);
        }
        return nbt;
    }

    public void saveOfflinePlayerData(UUID uuid, CompoundTag tag, boolean async) {
        this.saveOfflinePlayerData(uuid.toString(), tag, async);
    }

    public void saveOfflinePlayerData(String name, CompoundTag tag, boolean async) {
        Optional<UUID> uuid = lookupName(name);
        saveOfflinePlayerData(uuid.map(UUID::toString).orElse(name), tag, async, true);
    }

    private void saveOfflinePlayerData(String name, CompoundTag tag, boolean async, boolean runEvent) {
        String nameLower = name.toLowerCase();
        if (this.shouldSavePlayerData()) {
            PlayerDataSerializeEvent event = new PlayerDataSerializeEvent(nameLower, playerDataSerializer);
            if (runEvent) {
                pluginManager.callEvent(event);
            }

            this.getScheduler().scheduleTask(new Task() {
                boolean hasRun = false;

                @Override
                public void onRun(int currentTick) {
                    this.onCancel();
                }

                //doing it like this ensures that the playerdata will be saved in a server shutdown
                @Override
                public void onCancel() {
                    if (!this.hasRun)    {
                        this.hasRun = true;
                        saveOfflinePlayerDataInternal(event.getSerializer(), tag, nameLower, event.getUuid().orElse(null));
                    }
                }
            }, async);
        }
    }

    private void saveOfflinePlayerDataInternal(PlayerDataSerializer serializer, CompoundTag tag, String name, UUID uuid) {
        try (OutputStream dataStream = serializer.write(name, uuid)) {
            NBTIO.writeGZIPCompressed(tag, dataStream, ByteOrder.BIG_ENDIAN);
        } catch (Exception e) {
            log.error(this.getLanguage().translateString("nukkit.data.saveError", name, e));
        }
    }

    private void convertLegacyPlayerData() {
        File dataDirectory = new File(getDataPath(), "players/");
        Pattern uuidPattern = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}.dat$");

        File[] files = dataDirectory.listFiles(file -> {
            String name = file.getName();
            return !uuidPattern.matcher(name).matches() && name.endsWith(".dat");
        });

        if (files == null) {
            return;
        }

        for (File legacyData : files) {
            String name = legacyData.getName();
            // Remove file extension
            name = name.substring(0, name.length() - 4);

            log.debug("Attempting legacy player data conversion for {}", name);

            CompoundTag tag = getOfflinePlayerDataInternal(name, false, false);

            if (tag == null || !tag.contains("UUIDLeast") || !tag.contains("UUIDMost")) {
                // No UUID so we cannot convert. Wait until player logs in.
                continue;
            }

            UUID uuid = new UUID(tag.getLong("UUIDMost"), tag.getLong("UUIDLeast"));
            if (!tag.contains("NameTag")) {
                tag.putString("NameTag", name);
            }

            if (new File(getDataPath() + "players/" + uuid.toString() + ".dat").exists()) {
                // We don't want to overwrite existing data.
                continue;
            }

            saveOfflinePlayerData(uuid.toString(), tag, false, false);

            // Add name to lookup table
            updateName(uuid, name);

            // Delete legacy data
            if (!legacyData.delete()) {
                log.warn("Unable to delete legacy data for {}", name);
            }
        }
    }

    public Map<Integer, Level> getLevels() {
        return levels;
    }

    public Level getDefaultLevel() {
        return defaultLevel;
    }

    public void setDefaultLevel(Level defaultLevel) {
        if (defaultLevel == null || (this.isLevelLoaded(defaultLevel.getFolderName()) && defaultLevel != this.defaultLevel)) {
            this.defaultLevel = defaultLevel;
        }
    }

    public boolean isLevelLoaded(String name) {
        return this.getLevelByName(name) != null;
    }

    public Level getLevel(int levelId) {
        if (this.levels.containsKey(levelId)) {
            return this.levels.get(levelId);
        }
        return null;
    }

    public Level getLevelByName(String name) {
        for (Level level : this.levelArray) {
            if (level.getFolderName().equalsIgnoreCase(name)) {
                return level;
            }
        }

        return null;
    }

    public boolean unloadLevel(Level level) {
        return this.unloadLevel(level, false);
    }

    public boolean unloadLevel(Level level, boolean forceUnload) {
        if (level == this.getDefaultLevel() && !forceUnload) {
            throw new IllegalStateException("The default level cannot be unloaded while running, please switch levels.");
        }

        return level.unload(forceUnload);

    }

    public boolean loadLevel(String name) {
        if (Objects.equals(name.trim(), "")) {
            throw new LevelException("Invalid empty level name");
        }
        if (this.isLevelLoaded(name)) {
            return true;
        } else if (!this.isLevelGenerated(name)) {
            log.warn(this.getLanguage().translateString("nukkit.level.notFound", name));

            return false;
        }

        String path;

        if (name.contains("/") || name.contains("\\")) {
            path = name;
        } else {
            path = this.getDataPath() + "worlds/" + name + "/";
        }

        Class<? extends LevelProvider> provider = LevelProviderManager.getProvider(path);

        if (provider == null) {
            log.error(this.getLanguage().translateString("nukkit.level.loadError", new String[]{name, "Unknown provider"}));

            return false;
        }

        Level level;
        try {
            level = new Level(this, name, path, provider);
        } catch (Exception e) {
            log.error(this.getLanguage().translateString("nukkit.level.loadError", new String[]{name, e.getMessage()}));
            return false;
        }

        this.levels.put(level.getId(), level);

        level.initLevel();

        this.getPluginManager().callEvent(new LevelLoadEvent(level));

        level.setTickRate(this.baseTickRate);

        return true;
    }

    public boolean generateLevel(String name) {
        return this.generateLevel(name, new java.util.Random().nextLong());
    }

    public boolean generateLevel(String name, long seed) {
        return this.generateLevel(name, seed, null);
    }

    public boolean generateLevel(String name, long seed, Class<? extends Generator> generator) {
        return this.generateLevel(name, seed, generator, new HashMap<>());
    }

    public boolean generateLevel(String name, long seed, Class<? extends Generator> generator, Map<String, Object> options) {
        return generateLevel(name, seed, generator, options, null);
    }

    public boolean generateLevel(String name, long seed, Class<? extends Generator> generator, Map<String, Object> options, Class<? extends LevelProvider> provider) {
        if (Objects.equals(name.trim(), "") || this.isLevelGenerated(name)) {
            return false;
        }

        if (!options.containsKey("preset")) {
            options.put("preset", this.getPropertyString("generator-settings", ""));
        }

        if (generator == null) {
            generator = Generator.getGenerator(this.getLevelType());
        }

        if (provider == null) {
            provider = LevelProviderManager.getProviderByName(this.getConfig().get("level-settings.default-format", "anvil"));
        }

        String path;

        if (name.contains("/") || name.contains("\\")) {
            path = name;
        } else {
            path = this.getDataPath() + "worlds/" + name + "/";
        }

        Level level;
        try {
            provider.getMethod("generate", String.class, String.class, long.class, Class.class, Map.class).invoke(null, path, name, seed, generator, options);

            level = new Level(this, name, path, provider);
            this.levels.put(level.getId(), level);

            level.initLevel();
            level.setTickRate(this.baseTickRate);
        } catch (Exception e) {
            log.error(this.getLanguage().translateString("nukkit.level.generationError", new String[]{name, Utils.getExceptionMessage(e)}));
            return false;
        }

        this.getPluginManager().callEvent(new LevelInitEvent(level));

        this.getPluginManager().callEvent(new LevelLoadEvent(level));
        return true;
    }

    public boolean isLevelGenerated(String name) {
        if (Objects.equals(name.trim(), "")) {
            return false;
        }

        if (this.getLevelByName(name) == null) {
            String path;

            if (name.contains("/") || name.contains("\\")) {
                path = name;
            } else {
                path = this.getDataPath() + "worlds/" + name + "/";
            }

            return LevelProviderManager.getProvider(path) != null;
        }

        return true;
    }

    public BaseLang getLanguage() {
        return baseLang;
    }

    public boolean isLanguageForced() {
        return forceLanguage;
    }

    public Network getNetwork() {
        return network;
    }

    //Revising later...
    public Config getConfig() {
        return this.config;
    }

    public <T> T getConfig(String variable) {
        return this.getConfig(variable, null);
    }

    @SuppressWarnings("unchecked")
    public <T> T getConfig(String variable, T defaultValue) {
        Object value = this.config.get(variable);
        return value == null ? defaultValue : (T) value;
    }

    public Config getProperties() {
        return this.properties;
    }

    public Object getProperty(String variable) {
        return this.getProperty(variable, null);
    }

    public Object getProperty(String variable, Object defaultValue) {
        return this.properties.exists(variable) ? this.properties.get(variable) : defaultValue;
    }

    public void setPropertyString(String variable, String value) {
        this.properties.set(variable, value);
        this.properties.save();
    }

    public String getPropertyString(String variable) {
        return this.getPropertyString(variable, null);
    }

    public String getPropertyString(String key, String defaultValue) {
        return this.properties.exists(key) ? this.properties.get(key).toString() : defaultValue;
    }

    public int getPropertyInt(String variable, Integer defaultValue) {
        return this.properties.exists(variable) ? (!this.properties.get(variable).equals("") ? Integer.parseInt(String.valueOf(this.properties.get(variable))) : defaultValue) : defaultValue;
    }

    public void setPropertyInt(String variable, int value) {
        this.properties.set(variable, value);
        this.properties.save();
    }

    public boolean getPropertyBoolean(String variable) {
        return this.getPropertyBoolean(variable, null);
    }

    public boolean getPropertyBoolean(String variable, Object defaultValue) {
        Object value = this.properties.exists(variable) ? this.properties.get(variable) : defaultValue;
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        switch (String.valueOf(value)) {
            case "on":
            case "true":
            case "1":
            case "yes":
                return true;
        }
        return false;
    }

    public ServiceManager getServiceManager() {
        return serviceManager;
    }

    public Map<String, List<String>> getCommandAliases() {
        Object section = this.getConfig("aliases");
        Map<String, List<String>> result = new LinkedHashMap<>();
        if (section instanceof Map) {
            for (Map.Entry entry : (Set<Map.Entry>) ((Map) section).entrySet()) {
                List<String> commands = new ArrayList<>();
                String key = (String) entry.getKey();
                Object value = entry.getValue();
                if (value instanceof List) {
                    commands.addAll((List<String>) value);
                } else {
                    commands.add((String) value);
                }

                result.put(key, commands);
            }
        }

        return result;

    }

    public boolean shouldSavePlayerData() {
        return this.getConfig("player.save-player-data", true);
    }

    public int getPlayerSkinChangeCooldown() {
        return this.getConfig("player.skin-change-cooldown", 30);
    }

    public final boolean isPrimaryThread() {
        return (Thread.currentThread() == currentThread);
    }

    public Thread getPrimaryThread() {
        return currentThread;
    }

    private void registerEntities() {
        Entity.registerEntity("Lightning", EntityLightning.class);
        Entity.registerEntity("FallingSand", EntityFallingBlock.class);
        Entity.registerEntity("Item", EntityItem.class);

        Entity.registerEntity("Human", EntityHuman.class, true);
    }

    private void registerBlockEntities() {

    }

    public boolean isIgnoredPacket(Class<? extends DataPacket> clazz) {
        return this.ignoredPackets.contains(clazz.getSimpleName());
    }

    public static Server getInstance() {
        return instance;
    }

    private class ConsoleThread extends Thread implements InterruptibleThread {

        @Override
        public void run() {
            console.start();
        }
    }

    public static void setLogLevel(org.apache.logging.log4j.Level level) {
        Preconditions.checkNotNull(level, "level");
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration log4jConfig = ctx.getConfiguration();
        LoggerConfig loggerConfig = log4jConfig.getLoggerConfig(org.apache.logging.log4j.LogManager.ROOT_LOGGER_NAME);
        loggerConfig.setLevel(level);
        ctx.updateLoggers();
    }

    public static org.apache.logging.log4j.Level getLogLevel() {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration log4jConfig = ctx.getConfiguration();
        LoggerConfig loggerConfig = log4jConfig.getLoggerConfig(org.apache.logging.log4j.LogManager.ROOT_LOGGER_NAME);
        return loggerConfig.getLevel();
    }
}
