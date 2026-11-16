package club.voidaris.core.warps;

import club.voidaris.core.Keys;
import club.voidaris.core.command.HelpEntry;
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
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;

import java.util.List;

public final class WarpCommands {

    private static final String WARP = "warp";
    private static final String WARPS = "warps";
    private static final String SET_WARP = "setwarp";
    private static final String DEL_WARP = "delwarp";
    private static final String ARGUMENT = "warp";

    private final WarpsModule module;
    private final Messages messages;
    private final Teleporter teleporter;

    public WarpCommands(WarpsModule module, Messages messages, Teleporter teleporter) {
        this.module = module;
        this.messages = messages;
        this.teleporter = teleporter;
    }

    public void register(Commands registrar) {
        SuggestionProvider<CommandSourceStack> usable = (context, builder) -> {
            var sender = context.getSource().getSender();
            if (module.isEnabled()) {
                module.names().stream().filter(name -> module.canUse(sender, name)).forEach(builder::suggest);
            }
            return builder.buildFuture();
        };
        SuggestionProvider<CommandSourceStack> all = (context, builder) -> {
            if (module.isEnabled() && context.getSource().getSender().hasPermission(Keys.Permission.WARPS_DELETE)) {
                module.names().forEach(builder::suggest);
            }
            return builder.buildFuture();
        };

        registrar.register(Commands.literal(WARP)
                        .executes(this::list)
                        .then(Commands.argument(ARGUMENT, StringArgumentType.word())
                                .suggests(usable)
                                .executes(this::warp))
                        .build(),
                messages.raw(Keys.Message.COMMAND_WARP_DESCRIPTION), List.of(WARPS));
        registrar.register(Commands.literal(SET_WARP)
                        .then(Commands.argument(ARGUMENT, StringArgumentType.word()).executes(this::setWarp))
                        .build(),
                messages.raw(Keys.Message.COMMAND_SETWARP_DESCRIPTION));
        registrar.register(Commands.literal(DEL_WARP)
                        .then(Commands.argument(ARGUMENT, StringArgumentType.word())
                                .suggests(all)
                                .executes(this::deleteWarp))
                        .build(),
                messages.raw(Keys.Message.COMMAND_DELWARP_DESCRIPTION));
    }

    public List<HelpEntry> helpEntries() {
        return List.of(
                new HelpEntry(HelpEntry.Category.PLAYER, "/" + WARP, Keys.Message.HELP_WARP_ARGUMENTS,
                        Keys.Message.HELP_WARP_DESCRIPTION, null, module),
                new HelpEntry(HelpEntry.Category.ADMIN, "/" + SET_WARP, Keys.Message.HELP_SETWARP_ARGUMENTS,
                        Keys.Message.HELP_SETWARP_DESCRIPTION, Keys.Permission.WARPS_SET, module),
                new HelpEntry(HelpEntry.Category.ADMIN, "/" + DEL_WARP, Keys.Message.HELP_DELWARP_ARGUMENTS,
                        Keys.Message.HELP_DELWARP_DESCRIPTION, Keys.Permission.WARPS_DELETE, module));
    }

    private int list(CommandContext<CommandSourceStack> context) {
        var sender = context.getSource().getSender();
        if (!checkEnabled(context)) {
            return 0;
        }
        var usable = module.names().stream().filter(name -> module.canUse(sender, name)).toList();
        if (usable.isEmpty()) {
            messages.send(sender, Keys.Message.WARPS_LIST_EMPTY);
            return 0;
        }
        var hover = HoverEvent.showText(messages.get(Keys.Message.WARPS_LIST_HOVER));
        var entries = usable.stream()
                .map(name -> messages.get(Keys.Message.WARPS_LIST_ENTRY, Placeholder.unparsed("warp", name))
                        .clickEvent(ClickEvent.runCommand("/" + WARP + " " + name))
                        .hoverEvent(hover))
                .toList();
        var joined = Component.join(
                JoinConfiguration.separator(messages.get(Keys.Message.WARPS_LIST_SEPARATOR)),
                entries);
        messages.send(sender, Keys.Message.WARPS_LIST, Placeholder.component("warps", joined));
        return Command.SINGLE_SUCCESS;
    }

    private int warp(CommandContext<CommandSourceStack> context) {
        if (!checkEnabled(context)) {
            return 0;
        }
        if (!(context.getSource().getSender() instanceof Player player)) {
            messages.send(context.getSource().getSender(), Keys.Message.GENERAL_PLAYER_ONLY);
            return 0;
        }
        String name = StringArgumentType.getString(context, ARGUMENT);
        var warpName = Placeholder.unparsed("warp", name);
        var stored = module.find(name);
        if (stored.isEmpty()) {
            messages.send(player, Keys.Message.WARPS_NOT_FOUND, warpName);
            return 0;
        }
        if (!module.canUse(player, name)) {
            messages.send(player, Keys.Message.GENERAL_NO_PERMISSION);
            return 0;
        }
        var target = stored.get().toLocation();
        if (target.isEmpty()) {
            messages.send(player, Keys.Message.TELEPORT_WORLD_UNAVAILABLE);
            return 0;
        }
        return teleporter.teleport(player, WARP, target.get(), module.teleportSettings(),
                Keys.Message.WARPS_TELEPORTED, warpName) ? Command.SINGLE_SUCCESS : 0;
    }

    private int setWarp(CommandContext<CommandSourceStack> context) {
        if (!checkAccess(context, Keys.Permission.WARPS_SET)) {
            return 0;
        }
        if (!(context.getSource().getSender() instanceof Player player)) {
            messages.send(context.getSource().getSender(), Keys.Message.GENERAL_PLAYER_ONLY);
            return 0;
        }
        String name = StringArgumentType.getString(context, ARGUMENT);
        if (!module.isValidName(name)) {
            messages.send(player, Keys.Message.WARPS_INVALID_NAME);
            return 0;
        }
        module.set(name, StoredLocation.of(player.getLocation()));
        messages.send(player, Keys.Message.WARPS_SET, Placeholder.unparsed("warp", name));
        return Command.SINGLE_SUCCESS;
    }

    private int deleteWarp(CommandContext<CommandSourceStack> context) {
        var sender = context.getSource().getSender();
        if (!checkAccess(context, Keys.Permission.WARPS_DELETE)) {
            return 0;
        }
        String name = StringArgumentType.getString(context, ARGUMENT);
        var warpName = Placeholder.unparsed("warp", name);
        if (!module.delete(name)) {
            messages.send(sender, Keys.Message.WARPS_NOT_FOUND, warpName);
            return 0;
        }
        messages.send(sender, Keys.Message.WARPS_DELETED, warpName);
        return Command.SINGLE_SUCCESS;
    }

    private boolean checkEnabled(CommandContext<CommandSourceStack> context) {
        if (!module.isEnabled()) {
            messages.send(context.getSource().getSender(), Keys.Message.GENERAL_MODULE_DISABLED);
            return false;
        }
        return true;
    }

    // Checked here instead of requires() so the sender gets the Spanish message rather than "unknown command".
    private boolean checkAccess(CommandContext<CommandSourceStack> context, String permission) {
        if (!checkEnabled(context)) {
            return false;
        }
        if (!context.getSource().getSender().hasPermission(permission)) {
            messages.send(context.getSource().getSender(), Keys.Message.GENERAL_NO_PERMISSION);
            return false;
        }
        return true;
    }
}
