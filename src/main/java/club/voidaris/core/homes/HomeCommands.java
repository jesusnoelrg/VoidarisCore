package club.voidaris.core.homes;

import club.voidaris.core.Keys;
import club.voidaris.core.command.CommandUsage;
import club.voidaris.core.command.HelpEntry;
import club.voidaris.core.config.ConfigManager;
import club.voidaris.core.gui.MenuLoader;
import club.voidaris.core.message.Messages;
import club.voidaris.core.storage.StoredLocation;
import club.voidaris.core.teleport.Teleporter;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public final class HomeCommands {

    private static final String HOME = "home";
    private static final String SET_HOME = "sethome";
    private static final String DEL_HOME = "delhome";
    private static final String LIST_HOME = "listhome";
    private static final String HOMES = "homes";
    private static final String SET_ICON = "seticon";
    private static final String HOME_ARGUMENT = "home";
    private static final String PLAYER_ARGUMENT = "player";
    /** Cooldown bucket of the teleporter, so a home cooldown does not block spawn or warps. */
    private static final String TELEPORT_TYPE = "home";

    private final HomesModule module;
    private final Messages messages;
    private final Teleporter teleporter;
    private final HomesMenu menu;

    public HomeCommands(HomesModule module, ConfigManager configs, Messages messages, Teleporter teleporter,
                        MenuLoader menuLoader) {
        this.module = module;
        this.messages = messages;
        this.teleporter = teleporter;
        this.menu = new HomesMenu(configs, menuLoader);
    }

    public void register(Commands registrar) {
        SuggestionProvider<CommandSourceStack> ownHomes = (context, builder) -> {
            if (module.isEnabled() && context.getSource().getSender() instanceof Player player
                    && player.hasPermission(Keys.Permission.HOMES_USE)) {
                String remaining = builder.getRemainingLowerCase();
                module.store().homes(player.getUniqueId()).keySet().stream()
                        .filter(name -> name.startsWith(remaining))
                        .forEach(builder::suggest);
            }
            return builder.buildFuture();
        };

        registrar.register(Commands.literal(HOME)
                        .executes(this::homeWithoutName)
                        .then(Commands.argument(HOME_ARGUMENT, StringArgumentType.word())
                                .suggests(ownHomes)
                                .executes(this::teleportOwn)
                                .then(Commands.literal(SET_ICON).executes(this::setIcon))
                                .then(Commands.argument(PLAYER_ARGUMENT, StringArgumentType.word())
                                        .suggests(players(Keys.Permission.HOMES_TELEPORT_OTHERS))
                                        .executes(this::teleportOther)))
                        .build(),
                messages.raw(Keys.Message.COMMAND_HOME_DESCRIPTION));
        registrar.register(Commands.literal(SET_HOME)
                        .executes(context -> setOwn(context, module.defaultHomeName()))
                        .then(Commands.argument(HOME_ARGUMENT, StringArgumentType.word())
                                .executes(context -> setOwn(context, StringArgumentType.getString(context, HOME_ARGUMENT)))
                                .then(Commands.argument(PLAYER_ARGUMENT, StringArgumentType.word())
                                        .suggests(players(Keys.Permission.HOMES_SET_OTHERS))
                                        .executes(this::setOther)))
                        .build(),
                messages.raw(Keys.Message.COMMAND_SETHOME_DESCRIPTION));
        registrar.register(Commands.literal(DEL_HOME)
                        .executes(this::deleteUsage)
                        .then(Commands.argument(HOME_ARGUMENT, StringArgumentType.word())
                                .suggests(ownHomes)
                                .executes(this::deleteOwn)
                                .then(Commands.argument(PLAYER_ARGUMENT, StringArgumentType.word())
                                        .suggests(players(Keys.Permission.HOMES_DELETE_OTHERS))
                                        .executes(this::deleteOther)))
                        .build(),
                messages.raw(Keys.Message.COMMAND_DELHOME_DESCRIPTION));
        registrar.register(Commands.literal(LIST_HOME)
                        .executes(this::listOwn)
                        .then(Commands.argument(PLAYER_ARGUMENT, StringArgumentType.word())
                                .suggests(players(Keys.Permission.HOMES_LIST_OTHERS))
                                .executes(this::listOther))
                        .build(),
                messages.raw(Keys.Message.COMMAND_LISTHOME_DESCRIPTION), List.of(HOMES));
    }

    public List<HelpEntry> helpEntries() {
        return List.of(
                new HelpEntry(HelpEntry.Category.PLAYER, "/" + HOME, Keys.Message.HELP_HOME_ARGUMENTS,
                        Keys.Message.HELP_HOME_DESCRIPTION, Keys.Permission.HOMES_USE, module),
                new HelpEntry(HelpEntry.Category.PLAYER, "/" + SET_HOME, Keys.Message.HELP_SETHOME_ARGUMENTS,
                        Keys.Message.HELP_SETHOME_DESCRIPTION, Keys.Permission.HOMES_USE, module),
                new HelpEntry(HelpEntry.Category.PLAYER, "/" + DEL_HOME, Keys.Message.HELP_DELHOME_ARGUMENTS,
                        Keys.Message.HELP_DELHOME_DESCRIPTION, Keys.Permission.HOMES_USE, module),
                new HelpEntry(HelpEntry.Category.PLAYER, "/" + LIST_HOME, null,
                        Keys.Message.HELP_LISTHOME_DESCRIPTION, Keys.Permission.HOMES_USE, module),
                new HelpEntry(HelpEntry.Category.PLAYER, "/" + HOME, Keys.Message.HELP_HOME_SETICON_ARGUMENTS,
                        Keys.Message.HELP_HOME_SETICON_DESCRIPTION, Keys.Permission.HOMES_SETICON, module),
                new HelpEntry(HelpEntry.Category.ADMIN, "/" + HOME, Keys.Message.HELP_HOMES_OTHERS_ARGUMENTS,
                        Keys.Message.HELP_HOME_OTHERS_DESCRIPTION, Keys.Permission.HOMES_TELEPORT_OTHERS, module),
                new HelpEntry(HelpEntry.Category.ADMIN, "/" + SET_HOME, Keys.Message.HELP_HOMES_OTHERS_ARGUMENTS,
                        Keys.Message.HELP_SETHOME_OTHERS_DESCRIPTION, Keys.Permission.HOMES_SET_OTHERS, module),
                new HelpEntry(HelpEntry.Category.ADMIN, "/" + DEL_HOME, Keys.Message.HELP_HOMES_OTHERS_ARGUMENTS,
                        Keys.Message.HELP_DELHOME_OTHERS_DESCRIPTION, Keys.Permission.HOMES_DELETE_OTHERS, module),
                new HelpEntry(HelpEntry.Category.ADMIN, "/" + LIST_HOME, Keys.Message.HELP_LISTHOME_OTHERS_ARGUMENTS,
                        Keys.Message.HELP_LISTHOME_OTHERS_DESCRIPTION, Keys.Permission.HOMES_LIST_OTHERS, module));
    }

    private int homeWithoutName(CommandContext<CommandSourceStack> context) {
        var player = requirePlayer(context, Keys.Permission.HOMES_USE);
        if (player == null) {
            return 0;
        }
        var homes = module.store().homes(player.getUniqueId());
        if (homes.isEmpty()) {
            messages.send(player, Keys.Message.HOMES_LIST_EMPTY);
            return 0;
        }
        if (homes.size() > 1) {
            messages.send(player, Keys.Message.HOMES_CHOOSE);
            return 0;
        }
        return teleport(player, player, homes.firstEntry().getValue()) ? Command.SINGLE_SUCCESS : 0;
    }

    private int teleportOwn(CommandContext<CommandSourceStack> context) {
        var player = requirePlayer(context, Keys.Permission.HOMES_USE);
        if (player == null) {
            return 0;
        }
        String name = StringArgumentType.getString(context, HOME_ARGUMENT);
        var home = module.store().find(player.getUniqueId(), name);
        if (home.isEmpty()) {
            messages.send(player, Keys.Message.HOMES_NOT_FOUND, Placeholder.unparsed("home", name));
            return 0;
        }
        return teleport(player, player, home.get()) ? Command.SINGLE_SUCCESS : 0;
    }

    private int teleportOther(CommandContext<CommandSourceStack> context) {
        var player = requirePlayer(context, Keys.Permission.HOMES_TELEPORT_OTHERS);
        var owner = player == null ? null : resolveOwner(player, context);
        if (owner == null) {
            return 0;
        }
        String name = StringArgumentType.getString(context, HOME_ARGUMENT);
        var home = module.store().find(owner.getUniqueId(), name);
        if (home.isEmpty()) {
            messages.send(player, Keys.Message.HOMES_NOT_FOUND_OTHER, homeAndOwner(name, owner));
            return 0;
        }
        return teleport(player, owner, home.get()) ? Command.SINGLE_SUCCESS : 0;
    }

    private boolean teleport(Player player, OfflinePlayer owner, Home home) {
        var target = home.location().toLocation();
        if (target.isEmpty()) {
            messages.send(player, Keys.Message.TELEPORT_WORLD_UNAVAILABLE);
            return false;
        }
        boolean own = owner.getUniqueId().equals(player.getUniqueId());
        return teleporter.teleport(player, TELEPORT_TYPE, target.get(), module.teleportSettings(),
                own ? Keys.Message.HOMES_TELEPORTED : Keys.Message.HOMES_TELEPORTED_OTHER,
                homeAndOwner(home.name(), owner));
    }

    private int setIcon(CommandContext<CommandSourceStack> context) {
        var player = requirePlayer(context, Keys.Permission.HOMES_SETICON);
        if (player == null) {
            return 0;
        }
        String name = StringArgumentType.getString(context, HOME_ARGUMENT);
        var home = module.store().find(player.getUniqueId(), name);
        if (home.isEmpty()) {
            messages.send(player, Keys.Message.HOMES_NOT_FOUND, Placeholder.unparsed("home", name));
            return 0;
        }
        var item = player.getInventory().getItemInMainHand();
        if (item.isEmpty()) {
            messages.send(player, Keys.Message.HOMES_ICON_EMPTY_HAND);
            return 0;
        }
        // Only the item type is kept, so names, enchantments and other data of the held item never reach the menu.
        module.store().put(player.getUniqueId(), home.get().withIcon(item.getType().key().asString()));
        messages.send(player, Keys.Message.HOMES_ICON_SET, Placeholder.unparsed("home", home.get().name()),
                Placeholder.component("item", Component.translatable(item.getType().translationKey())));
        return Command.SINGLE_SUCCESS;
    }

    private int setOwn(CommandContext<CommandSourceStack> context, String name) {
        var player = requirePlayer(context, Keys.Permission.HOMES_USE);
        if (player == null) {
            return 0;
        }
        if (!module.isValidName(name)) {
            messages.send(player, Keys.Message.HOMES_INVALID_NAME);
            return 0;
        }
        if (module.isBlocked(player.getWorld())) {
            messages.send(player, Keys.Message.HOMES_BLOCKED_WORLD);
            return 0;
        }
        var existing = module.store().find(player.getUniqueId(), name);
        int limit = module.limit(player);
        if (existing.isEmpty() && module.store().homes(player.getUniqueId()).size() >= limit) {
            messages.send(player, Keys.Message.HOMES_LIMIT_REACHED, Placeholder.unparsed("limit", String.valueOf(limit)));
            return 0;
        }
        save(player, player, name, existing.map(Home::icon).orElse(null));
        messages.send(player, Keys.Message.HOMES_SET, Placeholder.unparsed("home", HomeStore.normalize(name)));
        return Command.SINGLE_SUCCESS;
    }

    // Staff placing a home for someone else skips the limit and blocked worlds, which are rules for the owner.
    private int setOther(CommandContext<CommandSourceStack> context) {
        var player = requirePlayer(context, Keys.Permission.HOMES_SET_OTHERS);
        var owner = player == null ? null : resolveOwner(player, context);
        if (owner == null) {
            return 0;
        }
        String name = StringArgumentType.getString(context, HOME_ARGUMENT);
        if (!module.isValidName(name)) {
            messages.send(player, Keys.Message.HOMES_INVALID_NAME);
            return 0;
        }
        var existing = module.store().find(owner.getUniqueId(), name);
        save(player, owner, name, existing.map(Home::icon).orElse(null));
        messages.send(player, Keys.Message.HOMES_SET_OTHER, homeAndOwner(HomeStore.normalize(name), owner));
        return Command.SINGLE_SUCCESS;
    }

    private void save(Player player, OfflinePlayer owner, String name, String icon) {
        module.store().put(owner.getUniqueId(),
                new Home(HomeStore.normalize(name), StoredLocation.of(player.getLocation()), icon));
    }

    private int deleteUsage(CommandContext<CommandSourceStack> context) {
        var sender = context.getSource().getSender();
        return checkAccess(sender, Keys.Permission.HOMES_USE)
                ? CommandUsage.send(messages, sender, "/" + DEL_HOME, Keys.Message.HELP_DELHOME_ARGUMENTS)
                : 0;
    }

    private int deleteOwn(CommandContext<CommandSourceStack> context) {
        var player = requirePlayer(context, Keys.Permission.HOMES_USE);
        if (player == null) {
            return 0;
        }
        String name = StringArgumentType.getString(context, HOME_ARGUMENT);
        var home = Placeholder.unparsed("home", name);
        if (!module.store().remove(player.getUniqueId(), name)) {
            messages.send(player, Keys.Message.HOMES_NOT_FOUND, home);
            return 0;
        }
        messages.send(player, Keys.Message.HOMES_DELETED, home);
        return Command.SINGLE_SUCCESS;
    }

    private int deleteOther(CommandContext<CommandSourceStack> context) {
        var sender = context.getSource().getSender();
        if (!checkAccess(sender, Keys.Permission.HOMES_DELETE_OTHERS)) {
            return 0;
        }
        var owner = resolveOwner(sender, context);
        if (owner == null) {
            return 0;
        }
        String name = StringArgumentType.getString(context, HOME_ARGUMENT);
        if (!module.store().remove(owner.getUniqueId(), name)) {
            messages.send(sender, Keys.Message.HOMES_NOT_FOUND_OTHER, homeAndOwner(name, owner));
            return 0;
        }
        messages.send(sender, Keys.Message.HOMES_DELETED_OTHER, homeAndOwner(name, owner));
        return Command.SINGLE_SUCCESS;
    }

    private int listOwn(CommandContext<CommandSourceStack> context) {
        var player = requirePlayer(context, Keys.Permission.HOMES_USE);
        if (player == null) {
            return 0;
        }
        var homes = module.store().homes(player.getUniqueId());
        if (homes.isEmpty()) {
            messages.send(player, Keys.Message.HOMES_LIST_EMPTY);
            return 0;
        }
        openMenu(player, player);
        return Command.SINGLE_SUCCESS;
    }

    private int listOther(CommandContext<CommandSourceStack> context) {
        var player = requirePlayer(context, Keys.Permission.HOMES_LIST_OTHERS);
        var owner = player == null ? null : resolveOwner(player, context);
        if (owner == null) {
            return 0;
        }
        if (module.store().homes(owner.getUniqueId()).isEmpty()) {
            messages.send(player, Keys.Message.HOMES_LIST_EMPTY_OTHER, Placeholder.unparsed("player", nameOf(owner)));
            return 0;
        }
        openMenu(player, owner);
        return Command.SINGLE_SUCCESS;
    }

    private void openMenu(Player viewer, OfflinePlayer owner) {
        menu.open(viewer, nameOf(owner), module.store().homes(owner.getUniqueId()).values(), clicked -> {
            viewer.closeInventory();
            if (!module.isEnabled()) {
                return;
            }
            // Looked up again because the home may have been moved or deleted while the menu was open.
            module.store().find(owner.getUniqueId(), clicked.name()).ifPresentOrElse(
                    home -> teleport(viewer, owner, home),
                    () -> messages.send(viewer, Keys.Message.HOMES_NOT_FOUND,
                            Placeholder.unparsed("home", clicked.name())));
        });
    }

    private SuggestionProvider<CommandSourceStack> players(String permission) {
        return (context, builder) -> {
            var sender = context.getSource().getSender();
            if (module.isEnabled() && sender.hasPermission(permission)) {
                String remaining = builder.getRemainingLowerCase();
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if ((!(sender instanceof Player viewer) || viewer.canSee(online))
                            && online.getName().toLowerCase().startsWith(remaining)) {
                        builder.suggest(online.getName());
                    }
                }
            }
            return builder.buildFuture();
        };
    }

    /**
     * Accepts offline players, as long as they joined the server before. Sends the error and returns {@code null}
     * otherwise.
     */
    private OfflinePlayer resolveOwner(CommandSender sender, CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, PLAYER_ARGUMENT);
        OfflinePlayer owner = Bukkit.getPlayerExact(name);
        if (owner == null) {
            owner = Bukkit.getOfflinePlayerIfCached(name);
        }
        if (owner == null || (!owner.isOnline() && !owner.hasPlayedBefore())) {
            messages.send(sender, Keys.Message.HOMES_PLAYER_UNKNOWN, Placeholder.unparsed("player", name));
            return null;
        }
        return owner;
    }

    private static TagResolver homeAndOwner(String home, OfflinePlayer owner) {
        return TagResolver.resolver(Placeholder.unparsed("home", home), Placeholder.unparsed("player", nameOf(owner)));
    }

    private static String nameOf(OfflinePlayer player) {
        String name = player.getName();
        return name == null ? player.getUniqueId().toString() : name;
    }

    private Player requirePlayer(CommandContext<CommandSourceStack> context, String permission) {
        var sender = context.getSource().getSender();
        if (!checkAccess(sender, permission)) {
            return null;
        }
        if (!(sender instanceof Player player)) {
            messages.send(sender, Keys.Message.GENERAL_PLAYER_ONLY);
            return null;
        }
        return player;
    }

    // Checked here instead of requires() so the sender gets the Spanish message rather than "unknown command".
    private boolean checkAccess(CommandSender sender, String permission) {
        if (!module.isEnabled()) {
            messages.send(sender, Keys.Message.GENERAL_MODULE_DISABLED);
            return false;
        }
        if (!sender.hasPermission(permission)) {
            messages.send(sender, Keys.Message.GENERAL_NO_PERMISSION);
            return false;
        }
        return true;
    }
}
