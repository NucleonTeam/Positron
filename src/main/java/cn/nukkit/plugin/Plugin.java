package cn.nukkit.plugin;

import cn.nukkit.Server;
import cn.nukkit.utils.Config;

import java.io.File;
import java.io.InputStream;

public interface Plugin {

    void onLoad();

    void onEnable();

    boolean isEnabled();

    void onDisable();

    boolean isDisabled();

    File getDataFolder();

    PluginDescription getDescription();

    InputStream getResource(String filename);

    boolean saveResource(String filename);

    boolean saveResource(String filename, boolean replace);

    boolean saveResource(String filename, String outputName, boolean replace);

    Config getConfig();

    void saveConfig();

    void saveDefaultConfig();

    void reloadConfig();

    Server getServer();

    String getName();

    PluginLogger getLogger();

    PluginLoader getPluginLoader();

}
