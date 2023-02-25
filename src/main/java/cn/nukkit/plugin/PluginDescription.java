package cn.nukkit.plugin;

import cn.nukkit.utils.PluginException;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.*;

public class PluginDescription {

    private String name;
    private String main;
    private List<String> api;
    private List<String> depend = new ArrayList<>();
    private List<String> softDepend = new ArrayList<>();
    private List<String> loadBefore = new ArrayList<>();
    private String version;
    private Map<String, Object> commands = new HashMap<>();
    private String description;
    private final List<String> authors = new ArrayList<>();
    private String website;
    private String prefix;
    private PluginLoadOrder order = PluginLoadOrder.POSTWORLD;

    public PluginDescription(Map<String, Object> yamlMap) {
        this.loadMap(yamlMap);
    }

    public PluginDescription(String yamlString) {
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(dumperOptions);
        this.loadMap(yaml.loadAs(yamlString, LinkedHashMap.class));
    }

    private void loadMap(Map<String, Object> plugin) throws PluginException {
        this.name = ((String) plugin.get("name")).replaceAll("[^A-Za-z0-9 _.-]", "");
        if (this.name.equals("")) {
            throw new PluginException("Invalid PluginDescription name");
        }
        this.name = this.name.replace(" ", "_");
        this.version = String.valueOf(plugin.get("version"));
        this.main = (String) plugin.get("main");
        Object api = plugin.get("api");
        if (api instanceof List) {
            this.api = (List<String>) api;
        } else {
            List<String> list = new ArrayList<>();
            list.add((String) api);
            this.api = list;
        }
        if (this.main.startsWith("cn.nukkit.")) {
            throw new PluginException("Invalid PluginDescription main, cannot start within the cn.nukkit. package");
        }

        if (plugin.containsKey("commands") && plugin.get("commands") instanceof Map) {
            this.commands = (Map<String, Object>) plugin.get("commands");
        }

        if (plugin.containsKey("depend")) {
            this.depend = (List<String>) plugin.get("depend");
        }

        if (plugin.containsKey("softdepend")) {
            this.softDepend = (List<String>) plugin.get("softdepend");
        }

        if (plugin.containsKey("loadbefore")) {
            this.loadBefore = (List<String>) plugin.get("loadbefore");
        }

        if (plugin.containsKey("website")) {
            this.website = (String) plugin.get("website");
        }

        if (plugin.containsKey("description")) {
            this.description = (String) plugin.get("description");
        }

        if (plugin.containsKey("prefix")) {
            this.prefix = (String) plugin.get("prefix");
        }

        if (plugin.containsKey("load")) {
            String order = (String) plugin.get("load");
            try {
                this.order = PluginLoadOrder.valueOf(order);
            } catch (Exception e) {
                throw new PluginException("Invalid PluginDescription load");
            }
        }

        if (plugin.containsKey("author")) {
            this.authors.add((String) plugin.get("author"));
        }

        if (plugin.containsKey("authors")) {
            this.authors.addAll((Collection<? extends String>) plugin.get("authors"));
        }
    }

    /**
     * 返回这个插件完整的名字。<br>
     * Returns the full name of this plugin.
     *
     * <p>一个插件完整的名字由{@code 名字+" v"+版本号}组成。比如：<br>
     * A full name of a plugin is composed by {@code name+" v"+version}.for example:</p>
     * <p>{@code HelloWorld v1.0.0}</p>
     *
     * @return 这个插件完整的名字。<br>The full name of this plugin.
     * @see PluginDescription
     * @since Nukkit 1.0 | Nukkit API 1.0.0
     */
    public String getFullName() {
        return this.name + " v" + this.version;
    }

    /**
     * 返回这个插件支持的Nukkit API版本列表。<br>
     * Returns all Nukkit API versions this plugin supports.
     *
     * @return 这个插件支持的Nukkit API版本列表。<br>A list of all Nukkit API versions String this plugin supports.
     * @see PluginDescription
     * @since Nukkit 1.0 | Nukkit API 1.0.0
     */
    public List<String> getCompatibleAPIs() {
        return api;
    }

    /**
     * 返回这个插件的作者列表。<br>
     * Returns all the authors of this plugin.
     *
     * @return 这个插件的作者列表。<br>A list of all authors of this plugin.
     * @see PluginDescription
     * @since Nukkit 1.0 | Nukkit API 1.0.0
     */
    public List<String> getAuthors() {
        return authors;
    }

    /**
     * 返回这个插件的信息前缀。<br>
     * Returns the message title of this plugin.
     *
     * <p>插件的信息前缀在记录器记录信息时，会作为信息头衔使用。如果没有定义记录器，会使用插件的名字作为信息头衔。<br>
     * When a PluginLogger logs, the message title is used as the prefix of message. If prefix is undefined,
     * the plugin name will be used instead. </p>
     *
     * @return 这个插件的作信息前缀。如果没定义，返回{@code null}。<br>
     * The message title of this plugin, or{@code null} if undefined.
     * @see PluginLogger
     * @see PluginDescription
     * @since Nukkit 1.0 | Nukkit API 1.0.0
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * 返回这个插件定义的命令列表。<br>
     * Returns all the defined commands of this plugin.
     *
     * @return 这个插件定义的命令列表。<br>A map of all defined commands of this plugin.
     * @see PluginDescription
     * @since Nukkit 1.0 | Nukkit API 1.0.0
     */
    public Map<String, Object> getCommands() {
        return commands;
    }

    /**
     * 返回这个插件所依赖的插件名字。<br>
     * The names of the plugins what is depended by this plugin.
     *
     * Nukkit插件的依赖有这些注意事项：<br>Here are some note for Nukkit plugin depending:
     * <ul>
     * <li>一个插件不能依赖自己（否则会报错）。<br>A plugin can not depend on itself (or there will be an exception).</li>
     * <li>如果一个插件依赖另一个插件，那么必须要安装依赖的插件后才能加载这个插件。<br>
     * If a plugin relies on another one, the another one must be installed at the same time, or Nukkit
     * won't load this plugin.</li>
     * <li>当一个插件所依赖的插件不存在时，Nukkit不会加载这个插件，但是会提醒用户去安装所依赖的插件。<br>
     * When the required dependency plugin does not exists, Nukkit won't load this plugin, but will tell the
     * user that this dependency is required.</li>
     * </ul>
     *
     * <p>举个例子，如果A插件依赖于B插件，在没有安装B插件而安装A插件的情况下，Nukkit会阻止A插件的加载。
     * 只有在安装B插件前安装了它所依赖的A插件，Nukkit才会允许加载B插件。<br>
     * For example, there is a Plugin A which relies on Plugin B. If you installed A without installing B,
     * Nukkit won't load A because its dependency B is lost. Only when B is installed, A will be loaded
     * by Nukkit.</p>
     *
     * @return 插件名字列表的 {@code List}对象。<br>A {@code List} object carries the plugin names.
     * @see PluginDescription
     * @since Nukkit 1.0 | Nukkit API 1.0.0
     */
    public List<String> getDepend() {
        return depend;
    }

    /**
     * 返回这个插件的描述文字。<br>
     * Returns the description text of this plugin.
     *
     * @return 这个插件的描述文字。<br>The description text of this plugin.
     * @see PluginDescription
     * @since Nukkit 1.0 | Nukkit API 1.0.0
     */
    public String getDescription() {
        return description;
    }

    /**
     * TODO finish javadoc
     */
    public List<String> getLoadBefore() {
        return loadBefore;
    }

    /**
     * 返回这个插件的主类名。<br>
     * Returns the main class name of this plugin.
     *
     * <p>一个插件的加载都是从主类开始的。主类的名字在插件的配置文件中定义后可以通过这个函数返回。一个返回值例子：<br>
     * The load action of a Nukkit plugin begins from main class. The name of main class should be defined
     * in the plugin configuration, and it can be returned by this function. An example for return value: <br>
     * {@code "com.example.ExamplePlugin"}</p>
     *
     * @return 这个插件的主类名。<br>The main class name of this plugin.
     * @see PluginDescription
     * @since Nukkit 1.0 | Nukkit API 1.0.0
     */
    public String getMain() {
        return main;
    }

    /**
     * 返回这个插件的名字。<br>
     * Returns the name of this plugin.
     *
     * @return 这个插件的名字。<br>The name of this plugin.
     * @see PluginDescription
     * @since Nukkit 1.0 | Nukkit API 1.0.0
     */
    public String getName() {
        return name;
    }

    /**
     * 返回这个插件加载的顺序，即插件应该在什么时候加载。<br>
     * Returns the order the plugin loads, or when the plugin is loaded.
     *
     * @return 这个插件加载的顺序。<br>The order the plugin loads.
     * @see PluginDescription
     * @see PluginLoadOrder
     * @since Nukkit 1.0 | Nukkit API 1.0.0
     */
    public PluginLoadOrder getOrder() {
        return order;
    }

    /**
     * TODO finish javadoc
     */
    public List<String> getSoftDepend() {
        return softDepend;
    }

    /**
     * 返回这个插件的版本号。<br>
     * Returns the version string of this plugin.
     *
     * @return 这个插件的版本号。<br>The version string od this plugin.
     * @see PluginDescription
     * @since Nukkit 1.0 | Nukkit API 1.0.0
     */
    public String getVersion() {
        return version;
    }

    /**
     * 返回这个插件的网站。<br>
     * Returns the website of this plugin.
     *
     * @return 这个插件的网站。<br>The website of this plugin.
     * @see PluginDescription
     * @since Nukkit 1.0 | Nukkit API 1.0.0
     */
    public String getWebsite() {
        return website;
    }
}
