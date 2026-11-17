package club.voidaris.core.utilities;

import club.voidaris.core.Keys;
import club.voidaris.core.command.HelpEntry;
import club.voidaris.core.message.Messages;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.inventory.InventoryView;
import org.bukkit.entity.Player;
import org.bukkit.inventory.MenuType;
import org.bukkit.inventory.view.builder.LocationInventoryViewBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Commands that open a virtual workstation, a player's ender chest or a player's inventory anywhere.
 */
public final class VirtualInventoryCommands {

    private static final String ENDER_CHEST = "enderchest";
    private static final String INVSEE = "invsee";
    private static final LegacyComponentSerializer LEGACY_TITLE = LegacyComponentSerializer.builder()
            .hexColors().useUnusualXRepeatedCharacterHexFormat().build();

    private record Station(
            String command,
            List<String> aliases,
            MenuType.Typed<?, ? extends LocationInventoryViewBuilder<?>> menu,
            String permission,
            String titleKey,
            String commandDescriptionKey,
            String helpDescriptionKey
    ) {
    }

    private static final List<Station> STATIONS = List.of(
            new Station("craft", List.of("workbench", "wb"), MenuType.CRAFTING,
                    Keys.Permission.UTILITIES_CRAFT, Keys.Message.UTILITIES_TITLE_CRAFT,
                    Keys.Message.COMMAND_CRAFT_DESCRIPTION, Keys.Message.HELP_CRAFT_DESCRIPTION),
            new Station("anvil", List.of(), MenuType.ANVIL,
                    Keys.Permission.UTILITIES_ANVIL, Keys.Message.UTILITIES_TITLE_ANVIL,
                    Keys.Message.COMMAND_ANVIL_DESCRIPTION, Keys.Message.HELP_ANVIL_DESCRIPTION),
            new Station("grindstone", List.of(), MenuType.GRINDSTONE,
                    Keys.Permission.UTILITIES_GRINDSTONE, Keys.Message.UTILITIES_TITLE_GRINDSTONE,
                    Keys.Message.COMMAND_GRINDSTONE_DESCRIPTION, Keys.Message.HELP_GRINDSTONE_DESCRIPTION),
            new Station("stonecutter", List.of(), MenuType.STONECUTTER,
                    Keys.Permission.UTILITIES_STONECUTTER, Keys.Message.UTILITIES_TITLE_STONECUTTER,
                    Keys.Message.COMMAND_STONECUTTER_DESCRIPTION, Keys.Message.HELP_STONECUTTER_DESCRIPTION),
            new Station("loom", List.of(), MenuType.LOOM,
                    Keys.Permission.UTILITIES_LOOM, Keys.Message.UTILITIES_TITLE_LOOM,
                    Keys.Message.COMMAND_LOOM_DESCRIPTION, Keys.Message.HELP_LOOM_DESCRIPTION),
            new Station("cartography", List.of("cartographytable"), MenuType.CARTOGRAPHY_TABLE,
                    Keys.Permission.UTILITIES_CARTOGRAPHY, Keys.Message.UTILITIES_TITLE_CARTOGRAPHY,
                    Keys.Message.COMMAND_CARTOGRAPHY_DESCRIPTION, Keys.Message.HELP_CARTOGRAPHY_DESCRIPTION),
            new Station("smithing", List.of("smithingtable"), MenuType.SMITHING,
                    Keys.Permission.UTILITIES_SMITHING, Keys.Message.UTILITIES_TITLE_SMITHING,
                    Keys.Message.COMMAND_SMITHING_DESCRIPTION, Keys.Message.HELP_SMITHING_DESCRIPTION));

    private final CommandSupport support;

    public VirtualInventoryCommands(UtilitiesModule module, Messages messages) {
        this.support = new CommandSupport(module, messages);
    }

    public void register(Commands registrar) {
        var messages = support.messages();
        for (Station station : STATIONS) {
            registrar.register(Commands.literal(station.command()).executes(context -> open(context, station)).build(),
                    messages.raw(station.commandDescriptionKey()), station.aliases());
        }
        registrar.register(Commands.literal(ENDER_CHEST)
                        .executes(context -> enderChest(context, false))
                        .then(CommandSupport.targetArgument().executes(context -> enderChest(context, true)))
                        .build(),
                messages.raw(Keys.Message.COMMAND_ENDERCHEST_DESCRIPTION), List.of("ec", "echest"));
        registrar.register(Commands.literal(INVSEE)
                        .executes(context -> support.usage(context.getSource().getSender(),
                                Keys.Permission.UTILITIES_INVSEE, "/" + INVSEE, Keys.Message.HELP_INVSEE_ARGUMENTS))
                        .then(CommandSupport.targetArgument().executes(this::invsee))
                        .build(),
                messages.raw(Keys.Message.COMMAND_INVSEE_DESCRIPTION));
    }

    public List<HelpEntry> helpEntries() {
        var module = support.module();
        var entries = new ArrayList<HelpEntry>();
        for (Station station : STATIONS) {
            entries.add(new HelpEntry(HelpEntry.Category.PLAYER, "/" + station.command(), null,
                    station.helpDescriptionKey(), station.permission(), module));
        }
        entries.add(new HelpEntry(HelpEntry.Category.PLAYER, "/" + ENDER_CHEST, Keys.Message.HELP_UTILITIES_TARGET_ARGUMENTS,
                Keys.Message.HELP_ENDERCHEST_DESCRIPTION, Keys.Permission.UTILITIES_ENDERCHEST, module));
        entries.add(new HelpEntry(HelpEntry.Category.ADMIN, "/" + INVSEE, Keys.Message.HELP_INVSEE_ARGUMENTS,
                Keys.Message.HELP_INVSEE_DESCRIPTION, Keys.Permission.UTILITIES_INVSEE, module));
        return entries;
    }

    private int open(CommandContext<CommandSourceStack> context, Station station) {
        Player player = support.requirePlayer(context, station.permission());
        if (player == null) {
            return 0;
        }
        // Without a real block behind the menu, the server's reach check would close it on the next tick.
        var view = station.menu().builder()
                .title(support.messages().get(station.titleKey()))
                .checkReachable(false)
                .build(player);
        player.openInventory(view);
        return Command.SINGLE_SUCCESS;
    }

    // Opens the live inventory, so changes apply at once; the title is replaced once the view exists.
    private int invsee(CommandContext<CommandSourceStack> context) {
        Player viewer = support.requirePlayer(context, Keys.Permission.UTILITIES_INVSEE);
        if (viewer == null) {
            return 0;
        }
        Player target = support.resolveTarget(context, Keys.Permission.UTILITIES_INVSEE,
                Keys.Permission.UTILITIES_INVSEE, true);
        if (target == null) {
            return 0;
        }
        // The same inventory as both halves of the view would let items be moved onto themselves and duplicated.
        if (target == viewer) {
            support.messages().send(viewer, Keys.Message.UTILITIES_INVSEE_SELF);
            return 0;
        }
        var inventory = target.getInventory();
        var view = viewer.openInventory(inventory);
        if (view == null) {
            return 0;
        }
        retitle(view, support.messages().get(Keys.Message.UTILITIES_TITLE_INVSEE,
                Placeholder.unparsed("player", target.getName())));
        if (!viewer.hasPermission(Keys.Permission.UTILITIES_INVSEE_MODIFY)) {
            support.module().addReadOnlyView(viewer, inventory);
        }
        return Command.SINGLE_SUCCESS;
    }

    private int enderChest(CommandContext<CommandSourceStack> context, boolean hasTarget) {
        Player target = support.resolveTarget(context, Keys.Permission.UTILITIES_ENDERCHEST,
                Keys.Permission.UTILITIES_ENDERCHEST_OTHERS, hasTarget);
        if (target == null) {
            return 0;
        }
        if (!(context.getSource().getSender() instanceof Player viewer)) {
            support.messages().send(context.getSource().getSender(), Keys.Message.GENERAL_PLAYER_ONLY);
            return 0;
        }
        var view = viewer.openInventory(target.getEnderChest());
        if (view != null) {
            var title = target == viewer
                    ? support.messages().get(Keys.Message.UTILITIES_TITLE_ENDERCHEST)
                    : support.messages().get(Keys.Message.UTILITIES_TITLE_ENDERCHEST_OTHER,
                    Placeholder.unparsed("player", target.getName()));
            retitle(view, title);
        }
        return Command.SINGLE_SUCCESS;
    }

    // Opening an existing inventory offers no title parameter, and InventoryView only accepts a legacy string.
    private static void retitle(InventoryView view, Component title) {
        view.setTitle(LEGACY_TITLE.serialize(title));
    }
}
