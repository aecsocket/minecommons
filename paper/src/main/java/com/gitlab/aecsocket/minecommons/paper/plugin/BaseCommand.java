package com.gitlab.aecsocket.minecommons.paper.plugin;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.bukkit.arguments.selector.MultiplePlayerSelector;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.minecraft.extras.MinecraftHelp;
import cloud.commandframework.paper.PaperCommandManager;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Utility class for using a Cloud {@link PaperCommandManager}.
 */
public class BaseCommand<P extends BasePlugin<P>> {
    /**
     * An exception that represents a user-facing error.
     */
    protected static class CommandException extends RuntimeException {
        /** The localization key. */
        private final String key;
        /** The localization arguments. */
        private final Object[] args;

        /**
         * Creates an instance.
         * @param key The localization key. This is automatically prefixed with {@code chat.error.}.
         * @param args The localization arguments.
         */
        public CommandException(String key, Object[] args) {
            this.key = key;
            this.args = args;
        }

        /**
         * Gets the localization key. This is automatically prefixed with {@code chat.error.}.
         * @return The key.
         */
        public String key() { return key; }

        /**
         * Gets the localization arguments.
         * @return The arguments.
         */
        public Object[] args() { return args; }
    }

    /** The plugin that this command is registered under. */
    protected final P plugin;
    /** The underlying command manager. */
    protected final PaperCommandManager<CommandSender> manager;
    /** The help command builder. */
    protected final MinecraftHelp<CommandSender> help;
    /** The name of the root command. */
    protected final String rootName;
    /** The name of the root command. */
    protected final Command.Builder<CommandSender> root;

    /**
     * Creates an instance.
     * @param plugin The plugin this command is registered under.
     * @param rootName The name of the root command.
     * @param rootFactory A factory for the root command.
     * @throws Exception If an error occurred when making the command manager.
     */
    public BaseCommand(P plugin, String rootName, BiFunction<PaperCommandManager<CommandSender>, String, Command.Builder<CommandSender>> rootFactory) throws Exception {
        this.plugin = plugin;
        manager = new PaperCommandManager<>(plugin,
                CommandExecutionCoordinator.simpleCoordinator(),
                Function.identity(), Function.identity());
        manager.registerBrigadier();
        manager.registerAsynchronousCompletions();

        this.rootName = rootName;
        help = new MinecraftHelp<>("/%s help".formatted(rootName), s -> s, manager);
        root = rootFactory.apply(manager, rootName);

        manager.command(root
                .literal("help", ArgumentDescription.of("Lists help information."))
                .argument(StringArgument.optional("query", StringArgument.StringMode.GREEDY))
                .handler(ctx -> help.queryCommands(ctx.getOrDefault("query", ""), ctx.getSender())));
        manager.command(root
                .literal("version", ArgumentDescription.of("Gets version information."))
                .handler(c -> handle(c, this::version)));
        manager.command(root
                .literal("reload", ArgumentDescription.of("Reloads all plugin data."))
                .permission("%s.command.reload".formatted(rootName))
                .handler(c -> handle(c, this::reload)));
    }

    /**
     * Gets the plugin this is registered under.
     * @return The plugin.
     */
    public P plugin() { return plugin; }

    /**
     * Gets the underlying command manager.
     * @return The manager.
     */
    public PaperCommandManager<CommandSender> manager() { return manager; }

    /**
     * Gets the help command builder.
     * @return The help command builder.
     */
    public MinecraftHelp<CommandSender> help() { return help; }

    /**
     * Gets the name of the root command.
     * @return The root name.
     */
    public String rootName() { return rootName; }

    /**
     * Gets the root command builder.
     * @return The root command builder.
     */
    public Command.Builder<CommandSender> root() { return root; }

    /**
     * Gets the locale of a command sender.
     * @param sender The sender.
     * @return The locale.
     * @see BasePlugin#locale(CommandSender)
     */
    protected Locale locale(CommandSender sender) { return plugin().locale(sender); }

    /**
     * Localizes a key and arguments into a component, using a specific locale.
     * @param locale The locale to localize for.
     * @param key The key of the localization value.
     * @param args The arguments.
     * @return The localized component.
     * @see BasePlugin#localize(Locale, String, Object...)
     */
    protected Component localize(Locale locale, String key, Object... args) { return plugin().localize(locale, key, args); }

    /**
     * Returns a player if the sender if a player, otherwise null.
     * @param sender The sender.
     * @return The player, or null.
     */
    protected Player player(CommandSender sender) { return sender instanceof Player ? (Player) sender : null; }

    /** A command handler, with pre-determined slots. */
    protected interface CommandHandler {
        /**
         * Handles a command.
         * @param ctx The command context.
         * @param sender The command sender.
         * @param locale The locale of the sender.
         * @param pSender The sender as a player, if they are a player, otherwise null.
         */
        void handle(CommandContext<CommandSender> ctx, CommandSender sender, Locale locale, Player pSender);
    }

    /**
     * Handles a command by using a {@link CommandHandler}.
     * @param ctx The command context.
     * @param handler The handler.
     */
    protected void handle(CommandContext<CommandSender> ctx, CommandHandler handler) {
        CommandSender sender = ctx.getSender();
        Locale locale = locale(sender);
        try {
            handler.handle(ctx, sender, locale, player(sender));
        } catch (CommandException e) {
            sender.sendMessage(localize(locale, e.key, e.args));
        }
    }

    /**
     * Creates an error, which is caught by {@link #handle(CommandContext, CommandHandler)}.
     * @param key The error chat key, localized by the key {@code chat.error.(key argument)}.
     * @param args The localization arguments.
     * @return The exception.
     */
    protected static CommandException error(String key, Object... args) {
        return new CommandException("chat.error." + key, args);
    }

    /**
     * Gets an argument from a command context that has a default value based on if the sender is a player.
     * @param ctx The context.
     * @param key The argument key.
     * @param pSender The player sender, or null if the sender is not a player.
     * @param ifPlayer The function to create a {@link T} if the sender is a player.
     * @param <T> The type of argument.
     * @return The argument.
     * @throws CommandException If there was no value, and the default value was null or there was no player sender.
     */
    protected @NonNull <T> T defaultedArg(CommandContext<CommandSender> ctx, String key, Player pSender, Supplier<T> ifPlayer) throws CommandException {
        return ctx.<T>getOptional(key).orElseGet(() -> {
            T result = pSender == null ? null : ifPlayer.get();
            if (result == null)
                throw error("no_arg", "arg", key);
            return result;
        });
    }

    /**
     * Gets a list of targets based on a command argument. If no targets are specified, throws an error.
     * @param ctx The context.
     * @param key The argument key.
     * @param pSender The player sender, or null if the sender is not a player.
     * @return The players.
     * @throws CommandException If there were no targets selected.
     */
    protected List<Player> targets(CommandContext<CommandSender> ctx, String key, Player pSender) throws CommandException {
        List<Player> targets = this.defaultedArg(ctx, key, pSender, () -> new MultiplePlayerSelector("", Collections.singletonList(pSender))).getPlayers();
        if (targets.size() == 0)
            throw error("no_targets");
        return targets;
    }


    /**
     * Command for {@code version}.
     * @param ctx The command context.
     * @param sender The command sender.
     * @param locale The locale of the sender.
     * @param pSender The sender as a player, if they are a player, otherwise null.
     */
    protected void version(CommandContext<CommandSender> ctx, CommandSender sender, Locale locale, Player pSender) {
        PluginDescriptionFile desc = plugin.getDescription();
        sender.sendMessage(localize(locale, "chat.version",
                "name", desc.getName(),
                "version", desc.getVersion(),
                "authors", String.join(", ", desc.getAuthors())));
    }

    /**
     * Command for {@code reload}.
     * @param ctx The command context.
     * @param sender The command sender.
     * @param locale The locale of the sender.
     * @param pSender The sender as a player, if they are a player, otherwise null.
     */
    protected void reload(CommandContext<CommandSender> ctx, CommandSender sender, Locale locale, Player pSender) {
        sender.sendMessage(localize(locale, "chat.reload.start"));
        plugin.reload();
        sender.sendMessage(localize(locale, "chat.reload.end"));
    }
}
