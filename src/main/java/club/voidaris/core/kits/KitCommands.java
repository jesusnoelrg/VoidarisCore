package club.voidaris.core.kits;

import club.voidaris.core.Keys;
import club.voidaris.core.command.CommandUsage;
import club.voidaris.core.command.HelpEntry;
import club.voidaris.core.config.ConfigManager;
import club.voidaris.core.gui.MenuLoader;
import club.voidaris.core.message.Messages;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

public final class KitCommands {

    private static final String KIT = "kit";
    private static final String KITS = "kits";
    private static final String CREATE_KIT = "createkit";
    private static final String EDIT_KIT = "editkit";
    private static final String DEL_KIT = "delkit";
    private static final String ITEMS = "items";
    private static final String ICON = "icon";
    private static final String COOLDOWN = "cooldown";
    private static final String PERMISSION = "permission";
    private static final String RESET = "reset";
    private static final String KIT_ARGUMENT = "kit";
    private static final String PLAYER_ARGUMENT = "player";
    private static final String VALUE_ARGUMENT = "value";

    private final KitsModule module;
    private final Messages messages;
    private final KitsMenu menu;

    public KitCommands(JavaPlugin plugin, KitsModule module, ConfigManager configs, Messages messages,
                       MenuLoader menuLoader) {
        this.module = module;
        this.messages = messages;
        this.menu = new KitsMenu(plugin, configs, messages, menuLoader, module);
    }

    public void register(Commands registrar) {
        registrar.register(Commands.literal(KIT)
                        .executes(this::openMenu)
                        .then(Commands.argument(KIT_ARGUMENT, StringArgumentType.word())
                                .suggests(kits(Keys.Permission.KITS_USE, true))
                                .executes(this::claim)
                                .then(Commands.argument(PLAYER_ARGUMENT, StringArgumentType.word())
                                        .suggests(onlinePlayers(Keys.Permission.KITS_GIVE))
                                        .executes(this::give)))
                        .build(),
                messages.raw(Keys.Message.COMMAND_KIT_DESCRIPTION), List.of(KITS));
        registrar.register(Commands.literal(CREATE_KIT)
                        .executes(context -> usage(context, Keys.Permission.KITS_CREATE, CREATE_KIT,
                                Keys.Message.HELP_CREATEKIT_ARGUMENTS))
                        .then(Commands.argument(KIT_ARGUMENT, StringArgumentType.word())
                                .executes(this::create))
                        .build(),
                messages.raw(Keys.Message.COMMAND_CREATEKIT_DESCRIPTION));
        registrar.register(Commands.literal(DEL_KIT)
                        .executes(context -> usage(context, Keys.Permission.KITS_DELETE, DEL_KIT,
                                Keys.Message.HELP_DELKIT_ARGUMENTS))
                        .then(Commands.argument(KIT_ARGUMENT, StringArgumentType.word())
                                .suggests(kits(Keys.Permission.KITS_DELETE, false))
                                .executes(this::delete))
                        .build(),
                messages.raw(Keys.Message.COMMAND_DELKIT_DESCRIPTION));
        registrar.register(Commands.literal(EDIT_KIT)
                        .executes(this::editUsage)
                        .then(Commands.argument(KIT_ARGUMENT, StringArgumentType.word())
                                .suggests(kits(Keys.Permission.KITS_EDIT, false))
                                .executes(this::editUsage)
                                .then(Commands.literal(ITEMS).executes(this::setItems))
                                .then(Commands.literal(ICON).executes(this::setIcon))
                                .then(Commands.literal(COOLDOWN)
                                        .executes(context -> usage(context, Keys.Permission.KITS_EDIT, EDIT_KIT,
                                                Keys.Message.HELP_EDITKIT_COOLDOWN_ARGUMENTS))
                                        .then(Commands.argument(VALUE_ARGUMENT, StringArgumentType.word())
                                                .suggests((context, builder) -> {
                                                    for (String example : List.of(KitTime.ONCE, "0", "30m", "1h", "1d", "7d")) {
                                                        if (example.startsWith(builder.getRemainingLowerCase())) {
                                                            builder.suggest(example);
                                                        }
                                                    }
                                                    return builder.buildFuture();
                                                })
                                                .executes(this::setCooldown)))
                                .then(Commands.literal(PERMISSION)
                                        .executes(context -> usage(context, Keys.Permission.KITS_EDIT, EDIT_KIT,
                                                Keys.Message.HELP_EDITKIT_PERMISSION_ARGUMENTS))
                                        .then(Commands.argument(VALUE_ARGUMENT, StringArgumentType.word())
                                                .suggests((context, builder) -> {
                                                    for (String value : List.of("true", "false")) {
                                                        if (value.startsWith(builder.getRemainingLowerCase())) {
                                                            builder.suggest(value);
                                                        }
                                                    }
                                                    return builder.buildFuture();
                                                })
                                                .executes(this::setPermission)))
                                .then(Commands.literal(RESET)
                                        .executes(context -> usage(context, Keys.Permission.KITS_EDIT, EDIT_KIT,
                                                Keys.Message.HELP_EDITKIT_RESET_ARGUMENTS))
                                        .then(Commands.argument(PLAYER_ARGUMENT, StringArgumentType.word())
                                                .suggests(onlinePlayers(Keys.Permission.KITS_EDIT))
                                                .executes(this::resetClaim))))
                        .build(),
                messages.raw(Keys.Message.COMMAND_EDITKIT_DESCRIPTION));
    }

    public List<HelpEntry> helpEntries() {
        return List.of(
                new HelpEntry(HelpEntry.Category.PLAYER, "/" + KIT, Keys.Message.HELP_KIT_ARGUMENTS,
                        Keys.Message.HELP_KIT_DESCRIPTION, Keys.Permission.KITS_USE, module),
                new HelpEntry(HelpEntry.Category.ADMIN, "/" + KIT, Keys.Message.HELP_KIT_GIVE_ARGUMENTS,
                        Keys.Message.HELP_KIT_GIVE_DESCRIPTION, Keys.Permission.KITS_GIVE, module),
                new HelpEntry(HelpEntry.Category.ADMIN, "/" + CREATE_KIT, Keys.Message.HELP_CREATEKIT_ARGUMENTS,
                        Keys.Message.HELP_CREATEKIT_DESCRIPTION, Keys.Permission.KITS_CREATE, module),
                new HelpEntry(HelpEntry.Category.ADMIN, "/" + EDIT_KIT, Keys.Message.HELP_EDITKIT_ARGUMENTS,
                        Keys.Message.HELP_EDITKIT_DESCRIPTION, Keys.Permission.KITS_EDIT, module),
                new HelpEntry(HelpEntry.Category.ADMIN, "/" + DEL_KIT, Keys.Message.HELP_DELKIT_ARGUMENTS,
                        Keys.Message.HELP_DELKIT_DESCRIPTION, Keys.Permission.KITS_DELETE, module));
    }

    private int openMenu(CommandContext<CommandSourceStack> context) {
        var sender = context.getSource().getSender();
        if (!(sender instanceof Player)) {
            return usage(context, Keys.Permission.KITS_GIVE, KIT, Keys.Message.HELP_KIT_GIVE_ARGUMENTS);
        }
        var player = requirePlayer(context, Keys.Permission.KITS_USE);
        if (player == null) {
            return 0;
        }
        if (module.storage().kits().stream().noneMatch(kit -> module.showLocked() || module.canAccess(player, kit))) {
            messages.send(player, Keys.Message.KITS_LIST_EMPTY);
            return 0;
        }
        menu.open(player, this::claim);
        return Command.SINGLE_SUCCESS;
    }

    private int claim(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getSender() instanceof Player)) {
            return usage(context, Keys.Permission.KITS_GIVE, KIT, Keys.Message.HELP_KIT_GIVE_ARGUMENTS);
        }
        var player = requirePlayer(context, Keys.Permission.KITS_USE);
        var kit = player == null ? null : findKit(player, context);
        return kit != null && claim(player, kit) ? Command.SINGLE_SUCCESS : 0;
    }

    private boolean claim(Player player, Kit kit) {
        var kitName = kitPlaceholder(kit);
        switch (module.status(player, kit)) {
            case LOCKED -> {
                messages.send(player, Keys.Message.KITS_LOCKED, kitName);
                return false;
            }
            case CLAIMED -> {
                messages.send(player, Keys.Message.KITS_ALREADY_CLAIMED, kitName);
                return false;
            }
            case COOLDOWN -> {
                messages.send(player, Keys.Message.KITS_COOLDOWN, kitName,
                        Placeholder.component("time", KitTime.format(messages, module.remaining(player, kit))));
                return false;
            }
            case AVAILABLE -> {
            }
        }
        var result = module.give(player, kit, module.dropWhenFull());
        if (result == KitsModule.GiveResult.NO_ROOM) {
            messages.send(player, Keys.Message.KITS_INVENTORY_FULL_DENIED, kitName);
            return false;
        }
        if (kit.oneTime() || kit.cooldownSeconds() > Kit.NO_COOLDOWN) {
            module.storage().claim(player.getUniqueId(), kit, System.currentTimeMillis());
        }
        messages.send(player, Keys.Message.KITS_RECEIVED, kitName);
        if (result == KitsModule.GiveResult.DROPPED) {
            messages.send(player, Keys.Message.KITS_INVENTORY_FULL);
        }
        return true;
    }

    // Staff deliveries skip the kit's permission, cooldown and one-time rules and do not count as a claim.
    private int give(CommandContext<CommandSourceStack> context) {
        var sender = context.getSource().getSender();
        if (!checkAccess(sender, Keys.Permission.KITS_GIVE)) {
            return 0;
        }
        var kit = findKit(sender, context);
        if (kit == null) {
            return 0;
        }
        String name = StringArgumentType.getString(context, PLAYER_ARGUMENT);
        var target = Bukkit.getPlayerExact(name);
        if (target == null) {
            messages.send(sender, Keys.Message.GENERAL_PLAYER_NOT_FOUND, Placeholder.unparsed("player", name));
            return 0;
        }
        var result = module.give(target, kit, true);
        messages.send(target, Keys.Message.KITS_RECEIVED, kitPlaceholder(kit));
        if (result == KitsModule.GiveResult.DROPPED) {
            messages.send(target, Keys.Message.KITS_INVENTORY_FULL);
        }
        if (sender != target) {
            messages.send(sender, Keys.Message.KITS_GIVEN, kitAndPlayer(kit, target.getName()));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int create(CommandContext<CommandSourceStack> context) {
        var player = requirePlayer(context, Keys.Permission.KITS_CREATE);
        if (player == null) {
            return 0;
        }
        String name = StringArgumentType.getString(context, KIT_ARGUMENT);
        if (!module.isValidName(name)) {
            messages.send(player, Keys.Message.KITS_INVALID_NAME);
            return 0;
        }
        String kitName = KitStorage.normalize(name);
        if (module.storage().find(kitName).isPresent()) {
            messages.send(player, Keys.Message.KITS_ALREADY_EXISTS, Placeholder.unparsed("kit", kitName));
            return 0;
        }
        var items = capture(player);
        if (items.isEmpty()) {
            messages.send(player, Keys.Message.KITS_EMPTY_INVENTORY);
            return 0;
        }
        var kit = new Kit(kitName, module.defaultCooldown(), module.defaultPermission(), null, items,
                System.currentTimeMillis());
        module.storage().put(kit);
        KitsModule.registerPermission(kit);
        messages.send(player, Keys.Message.KITS_CREATED, kitPlaceholder(kit),
                Placeholder.unparsed("items", String.valueOf(items.size())),
                Placeholder.component("cooldown", KitTime.describe(messages, kit)));
        return Command.SINGLE_SUCCESS;
    }

    private int delete(CommandContext<CommandSourceStack> context) {
        var sender = context.getSource().getSender();
        if (!checkAccess(sender, Keys.Permission.KITS_DELETE)) {
            return 0;
        }
        var kit = findKit(sender, context);
        if (kit == null) {
            return 0;
        }
        module.storage().remove(kit.name());
        messages.send(sender, Keys.Message.KITS_DELETED, kitPlaceholder(kit));
        return Command.SINGLE_SUCCESS;
    }

    private int editUsage(CommandContext<CommandSourceStack> context) {
        return usage(context, Keys.Permission.KITS_EDIT, EDIT_KIT, Keys.Message.HELP_EDITKIT_ARGUMENTS);
    }

    private int setItems(CommandContext<CommandSourceStack> context) {
        var player = requirePlayer(context, Keys.Permission.KITS_EDIT);
        var kit = player == null ? null : findKit(player, context);
        if (kit == null) {
            return 0;
        }
        var items = capture(player);
        if (items.isEmpty()) {
            messages.send(player, Keys.Message.KITS_EMPTY_INVENTORY);
            return 0;
        }
        module.storage().put(kit.withItems(items));
        messages.send(player, Keys.Message.KITS_ITEMS_UPDATED, kitPlaceholder(kit),
                Placeholder.unparsed("items", String.valueOf(items.size())));
        return Command.SINGLE_SUCCESS;
    }

    private int setIcon(CommandContext<CommandSourceStack> context) {
        var player = requirePlayer(context, Keys.Permission.KITS_EDIT);
        var kit = player == null ? null : findKit(player, context);
        if (kit == null) {
            return 0;
        }
        var item = player.getInventory().getItemInMainHand();
        if (item.isEmpty()) {
            messages.send(player, Keys.Message.KITS_ICON_EMPTY_HAND);
            return 0;
        }
        // Only the item type is kept, like home icons; the menu draws the name and lore from guis/kits.yml.
        module.storage().put(kit.withIcon(item.getType().key().asString()));
        messages.send(player, Keys.Message.KITS_ICON_SET, kitPlaceholder(kit),
                Placeholder.component("item", Component.translatable(item.getType().translationKey())));
        return Command.SINGLE_SUCCESS;
    }

    private int setCooldown(CommandContext<CommandSourceStack> context) {
        var sender = context.getSource().getSender();
        if (!checkAccess(sender, Keys.Permission.KITS_EDIT)) {
            return 0;
        }
        var kit = findKit(sender, context);
        if (kit == null) {
            return 0;
        }
        var cooldown = KitTime.parse(StringArgumentType.getString(context, VALUE_ARGUMENT));
        if (cooldown.isEmpty()) {
            return CommandUsage.send(messages, sender, "/" + EDIT_KIT, Keys.Message.HELP_EDITKIT_COOLDOWN_ARGUMENTS);
        }
        var edited = kit.withCooldown(cooldown.getAsLong());
        module.storage().put(edited);
        messages.send(sender, Keys.Message.KITS_COOLDOWN_SET, kitPlaceholder(kit),
                Placeholder.component("cooldown", KitTime.describe(messages, edited)));
        return Command.SINGLE_SUCCESS;
    }

    private int setPermission(CommandContext<CommandSourceStack> context) {
        var sender = context.getSource().getSender();
        if (!checkAccess(sender, Keys.Permission.KITS_EDIT)) {
            return 0;
        }
        var kit = findKit(sender, context);
        if (kit == null) {
            return 0;
        }
        String value = StringArgumentType.getString(context, VALUE_ARGUMENT).toLowerCase(Locale.ROOT);
        if (!value.equals("true") && !value.equals("false")) {
            return CommandUsage.send(messages, sender, "/" + EDIT_KIT, Keys.Message.HELP_EDITKIT_PERMISSION_ARGUMENTS);
        }
        boolean required = Boolean.parseBoolean(value);
        module.storage().put(kit.withPermission(required));
        messages.send(sender, required ? Keys.Message.KITS_PERMISSION_ENABLED : Keys.Message.KITS_PERMISSION_DISABLED,
                kitPlaceholder(kit), Placeholder.unparsed("permission", Keys.Permission.KITS_KIT + kit.name()));
        return Command.SINGLE_SUCCESS;
    }

    private int resetClaim(CommandContext<CommandSourceStack> context) {
        var sender = context.getSource().getSender();
        if (!checkAccess(sender, Keys.Permission.KITS_EDIT)) {
            return 0;
        }
        var kit = findKit(sender, context);
        if (kit == null) {
            return 0;
        }
        String name = StringArgumentType.getString(context, PLAYER_ARGUMENT);
        OfflinePlayer target = Bukkit.getPlayerExact(name);
        if (target == null) {
            target = Bukkit.getOfflinePlayerIfCached(name);
        }
        if (target == null || (!target.isOnline() && !target.hasPlayedBefore())) {
            messages.send(sender, Keys.Message.KITS_PLAYER_UNKNOWN, Placeholder.unparsed("player", name));
            return 0;
        }
        module.storage().resetClaim(target.getUniqueId(), kit);
        String targetName = target.getName() == null ? name : target.getName();
        messages.send(sender, Keys.Message.KITS_RESET, kitAndPlayer(kit, targetName));
        return Command.SINGLE_SUCCESS;
    }

    /**
     * @return every non-empty slot of the player's inventory, armor and offhand included
     */
    private static Map<Integer, ItemStack> capture(Player player) {
        var contents = player.getInventory().getContents();
        Map<Integer, ItemStack> items = new HashMap<>();
        for (int slot = 0; slot < contents.length; slot++) {
            var item = contents[slot];
            if (item != null && !item.isEmpty()) {
                items.put(slot, item.clone());
            }
        }
        return items;
    }

    private Kit findKit(CommandSender sender, CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, KIT_ARGUMENT);
        var kit = module.storage().find(name);
        if (kit.isEmpty()) {
            messages.send(sender, Keys.Message.KITS_NOT_FOUND, Placeholder.unparsed("kit", name));
            return null;
        }
        return kit.get();
    }

    /**
     * @param accessibleOnly whether players only get the kits they have the permission of
     */
    private SuggestionProvider<CommandSourceStack> kits(String permission, boolean accessibleOnly) {
        return (context, builder) -> {
            var sender = context.getSource().getSender();
            if (module.isEnabled() && sender.hasPermission(permission)) {
                Predicate<Kit> visible = kit -> !accessibleOnly || !(sender instanceof Player) || module.canAccess(sender, kit);
                String remaining = builder.getRemainingLowerCase();
                module.storage().kits().stream()
                        .filter(visible)
                        .map(Kit::name)
                        .filter(name -> name.startsWith(remaining))
                        .forEach(builder::suggest);
            }
            return builder.buildFuture();
        };
    }

    private SuggestionProvider<CommandSourceStack> onlinePlayers(String permission) {
        return (context, builder) -> {
            var sender = context.getSource().getSender();
            if (module.isEnabled() && sender.hasPermission(permission)) {
                String remaining = builder.getRemainingLowerCase();
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if ((!(sender instanceof Player viewer) || viewer.canSee(online))
                            && online.getName().toLowerCase(Locale.ROOT).startsWith(remaining)) {
                        builder.suggest(online.getName());
                    }
                }
            }
            return builder.buildFuture();
        };
    }

    private int usage(CommandContext<CommandSourceStack> context, String permission, String command, String argumentsKey) {
        var sender = context.getSource().getSender();
        return checkAccess(sender, permission) ? CommandUsage.send(messages, sender, "/" + command, argumentsKey) : 0;
    }

    private static TagResolver kitPlaceholder(Kit kit) {
        return Placeholder.unparsed("kit", kit.name());
    }

    private static TagResolver kitAndPlayer(Kit kit, String player) {
        return TagResolver.resolver(kitPlaceholder(kit), Placeholder.unparsed("player", player));
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
