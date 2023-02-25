package cn.nukkit.command;

import cn.nukkit.Server;
import cn.nukkit.command.data.CommandParameter;
import cn.nukkit.command.defaults.*;
import cn.nukkit.command.simple.*;
import cn.nukkit.lang.TranslationContainer;
import cn.nukkit.utils.MainLogger;
import cn.nukkit.utils.TextFormat;
import cn.nukkit.utils.Utils;

import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class SimpleCommandMap implements CommandMap {
    protected final Map<String, Command> knownCommands = new HashMap<>();

    private final Server server;

    public SimpleCommandMap(Server server) {
        this.server = server;
        this.setDefaultCommands();
    }

    private void setDefaultCommands() {
        this.register("nukkit", new PluginsCommand("plugins"));
        this.register("nukkit", new HelpCommand("help"));
        this.register("nukkit", new StopCommand("stop"));
        this.register("nukkit", new StatusCommand("status"));
        this.register("nukkit", new GarbageCollectorCommand("gc"));
        this.register("nukkit", new TimingsCommand("timings"));
    }

    @Override
    public void registerAll(String fallbackPrefix, List<? extends Command> commands) {
        for (Command command : commands) {
            this.register(fallbackPrefix, command);
        }
    }

    @Override
    public boolean register(String fallbackPrefix, Command command) {
        return this.register(fallbackPrefix, command, null);
    }

    @Override
    public boolean register(String fallbackPrefix, Command command, String label) {
        if (label == null) {
            label = command.getName();
        }
        label = label.trim().toLowerCase();
        fallbackPrefix = fallbackPrefix.trim().toLowerCase();

        boolean registered = this.registerAlias(command, false, fallbackPrefix, label);

        List<String> aliases = new ArrayList<>(Arrays.asList(command.getAliases()));

        for (Iterator<String> iterator = aliases.iterator(); iterator.hasNext(); ) {
            String alias = iterator.next();
            if (!this.registerAlias(command, true, fallbackPrefix, alias)) {
                iterator.remove();
            }
        }
        command.setAliases(aliases.toArray(new String[0]));

        if (!registered) {
            command.setLabel(fallbackPrefix + ":" + label);
        }

        command.register(this);

        return registered;
    }

    @Override
    public void registerSimpleCommands(Object object) {
        for (Method method : object.getClass().getDeclaredMethods()) {
            cn.nukkit.command.simple.Command def = method.getAnnotation(cn.nukkit.command.simple.Command.class);
            if (def != null) {
                SimpleCommand sc = new SimpleCommand(object, method, def.name(), def.description(), def.usageMessage(), def.aliases());

                Arguments args = method.getAnnotation(Arguments.class);
                if (args != null) {
                    sc.setMaxArgs(args.max());
                    sc.setMinArgs(args.min());
                }

                CommandPermission perm = method.getAnnotation(CommandPermission.class);
                if (perm != null) {
                    sc.setPermission(perm.value());
                }

                if (method.isAnnotationPresent(ForbidConsole.class)) {
                    sc.setForbidConsole(true);
                }

                CommandParameters commandParameters = method.getAnnotation(CommandParameters.class);
                if (commandParameters != null) {
                    Map<String, CommandParameter[]> map = Arrays.stream(commandParameters.parameters())
                            .collect(Collectors.toMap(Parameters::name, parameters -> Arrays.stream(parameters.parameters())
                                    .map(parameter -> CommandParameter.newType(parameter.name(), parameter.optional(), parameter.type()))
                                    .distinct()
                                    .toArray(CommandParameter[]::new)));

                    sc.commandParameters.putAll(map);
                }

                this.register(def.name(), sc);
            }
        }
    }

    private boolean registerAlias(Command command, boolean isAlias, String fallbackPrefix, String label) {
        this.knownCommands.put(fallbackPrefix + ":" + label, command);

        //if you're registering a command alias that is already registered, then return false
        boolean alreadyRegistered = this.knownCommands.containsKey(label);
        Command existingCommand = this.knownCommands.get(label);
        boolean existingCommandIsNotVanilla = alreadyRegistered && !(existingCommand instanceof VanillaCommand);
        //basically, if we're an alias and it's already registered, or we're a vanilla command, then we can't override it
        if ((command instanceof VanillaCommand || isAlias) && alreadyRegistered && existingCommandIsNotVanilla) {
            return false;
        }

        //if you're registering a name (alias or label) which is identical to another command who's primary name is the same
        //so basically we can't override the main name of a command, but we can override aliases if we're not an alias

        //added the last statement which will allow us to override a VanillaCommand unconditionally
        if (alreadyRegistered && existingCommand.getLabel() != null && existingCommand.getLabel().equals(label) && existingCommandIsNotVanilla) {
            return false;
        }

        //you can now assume that the command is either uniquely named, or overriding another command's alias (and is not itself, an alias)

        if (!isAlias) {
            command.setLabel(label);
        }

        // Then we need to check if there isn't any command conflicts with vanilla commands
        ArrayList<String> toRemove = new ArrayList<>();

        for (Entry<String, Command> entry : knownCommands.entrySet()) {
            Command cmd = entry.getValue();
            if (cmd.getLabel().equalsIgnoreCase(command.getLabel()) && !cmd.equals(command)) { // If the new command conflicts... (But if it isn't the same command)
                if (cmd instanceof VanillaCommand) { // And if the old command is a vanilla command...
                    // Remove it!
                    toRemove.add(entry.getKey());
                }
            }
        }

        // Now we loop the toRemove list to remove the command conflicts from the knownCommands map
        for (String cmd : toRemove) {
            knownCommands.remove(cmd);
        }

        this.knownCommands.put(label, command);

        return true;
    }

    private ArrayList<String> parseArguments(String cmdLine) {
        StringBuilder sb = new StringBuilder(cmdLine);
        ArrayList<String> args = new ArrayList<>();
        boolean notQuoted = true;
        int start = 0;

        for (int i = 0; i < sb.length(); i++) {
            if (sb.charAt(i) == '\\') {
                sb.deleteCharAt(i);
                continue;
            }

            if (sb.charAt(i) == ' ' && notQuoted) {
                String arg = sb.substring(start, i);
                if (!arg.isEmpty()) {
                    args.add(arg);
                }
                start = i + 1;
            } else if (sb.charAt(i) == '"') {
                sb.deleteCharAt(i);
                --i;
                notQuoted = !notQuoted;
            }
        }

        String arg = sb.substring(start);
        if (!arg.isEmpty()) {
            args.add(arg);
        }
        return args;
    }

    @Override
    public boolean dispatch(CommandSender sender, String cmdLine) {
        ArrayList<String> parsed = parseArguments(cmdLine);
        if (parsed.size() == 0) {
            return false;
        }

        String sentCommandLabel = parsed.remove(0).toLowerCase();
        String[] args = parsed.toArray(new String[0]);
        Command target = this.getCommand(sentCommandLabel);

        if (target == null) {
            return false;
        }

        target.timing.startTiming();
        try {
            target.execute(sender, sentCommandLabel, args);
        } catch (Exception e) {
            sender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.generic.exception"));
            this.server.getLogger().critical(this.server.getLanguage().translateString("nukkit.command.exception", cmdLine, target.toString(), Utils.getExceptionMessage(e)));
            MainLogger logger = sender.getServer().getLogger();
            if (logger != null) {
                logger.logException(e);
            }
        }
        target.timing.stopTiming();

        return true;
    }

    @Override
    public void clearCommands() {
        for (Command command : this.knownCommands.values()) {
            command.unregister(this);
        }
        this.knownCommands.clear();
        this.setDefaultCommands();
    }

    @Override
    public Command getCommand(String name) {
        if (this.knownCommands.containsKey(name)) {
            return this.knownCommands.get(name);
        }
        return null;
    }

    public Map<String, Command> getCommands() {
        return knownCommands;
    }

    public void registerServerAliases() {
        Map<String, List<String>> values = this.server.getCommandAliases();
        for (Map.Entry<String, List<String>> entry : values.entrySet()) {
            String alias = entry.getKey();
            List<String> commandStrings = entry.getValue();
            if (alias.contains(" ") || alias.contains(":")) {
                this.server.getLogger().warning(this.server.getLanguage().translateString("nukkit.command.alias.illegal", alias));
                continue;
            }
            List<String> targets = new ArrayList<>();

            StringBuilder bad = new StringBuilder();

            for (String commandString : commandStrings) {
                String[] args = commandString.split(" ");
                Command command = this.getCommand(args[0]);

                if (command == null) {
                    if (bad.length() > 0) {
                        bad.append(", ");
                    }
                    bad.append(commandString);
                } else {
                    targets.add(commandString);
                }
            }

            if (bad.length() > 0) {
                this.server.getLogger().warning(this.server.getLanguage().translateString("nukkit.command.alias.notFound", new String[]{alias, bad.toString()}));
                continue;
            }

            if (!targets.isEmpty()) {
                this.knownCommands.put(alias.toLowerCase(), new FormattedCommandAlias(alias.toLowerCase(), targets));
            } else {
                this.knownCommands.remove(alias.toLowerCase());
            }
        }
    }
}
