package club.voidaris.core.utilities;

import club.voidaris.core.Keys;
import club.voidaris.core.command.HelpEntry;
import club.voidaris.core.message.Messages;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;
import org.bukkit.inventory.MenuType;
import org.bukkit.inventory.view.builder.LocationInventoryViewBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Commands that open a virtual workstation or a player's ender chest anywhere.
 */
public final class VirtualInventoryCommands {

    private static final String ENDER_CHEST = "enderchest";

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
        viewer.openInventory(target.getEnderChest());
        return Command.SINGLE_SUCCESS;
    }
}
