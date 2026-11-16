package club.voidaris.core.utilities;

import club.voidaris.core.Keys;
import club.voidaris.core.command.HelpEntry;
import club.voidaris.core.message.Messages;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

/**
 * {@code /gm <mode> [player]} and its shortcuts {@code /gms}, {@code /gmc}, {@code /gma} and {@code /gmsp}.
 */
public final class GameModeCommands {

    private static final String GAME_MODE = "gm";
    private static final String MODE = "mode";

    private static final Map<GameMode, String> SHORTCUTS = Map.of(
            GameMode.SURVIVAL, "gms",
            GameMode.CREATIVE, "gmc",
            GameMode.ADVENTURE, "gma",
            GameMode.SPECTATOR, "gmsp");

    private static final Map<String, GameMode> NUMERIC_IDS = Map.of(
            "0", GameMode.SURVIVAL,
            "1", GameMode.CREATIVE,
            "2", GameMode.ADVENTURE,
            "3", GameMode.SPECTATOR);

    private static final Map<GameMode, String> PERMISSIONS = Map.of(
            GameMode.SURVIVAL, Keys.Permission.UTILITIES_GAMEMODE_SURVIVAL,
            GameMode.CREATIVE, Keys.Permission.UTILITIES_GAMEMODE_CREATIVE,
            GameMode.ADVENTURE, Keys.Permission.UTILITIES_GAMEMODE_ADVENTURE,
            GameMode.SPECTATOR, Keys.Permission.UTILITIES_GAMEMODE_SPECTATOR);

    private static final Map<GameMode, String> NAMES = Map.of(
            GameMode.SURVIVAL, Keys.Message.UTILITIES_GAMEMODE_NAME_SURVIVAL,
            GameMode.CREATIVE, Keys.Message.UTILITIES_GAMEMODE_NAME_CREATIVE,
            GameMode.ADVENTURE, Keys.Message.UTILITIES_GAMEMODE_NAME_ADVENTURE,
            GameMode.SPECTATOR, Keys.Message.UTILITIES_GAMEMODE_NAME_SPECTATOR);

    private final CommandSupport support;

    public GameModeCommands(UtilitiesModule module, Messages messages) {
        this.support = new CommandSupport(module, messages);
    }

    public void register(Commands registrar) {
        var messages = support.messages();
        var gameMode = Commands.literal(GAME_MODE)
                .then(Commands.argument(MODE, ArgumentTypes.gameMode())
                        .executes(context -> change(context, context.getArgument(MODE, GameMode.class), false))
                        .then(CommandSupport.targetArgument().executes(context ->
                                change(context, context.getArgument(MODE, GameMode.class), true))));
        NUMERIC_IDS.forEach((id, mode) -> gameMode.then(Commands.literal(id)
                .executes(context -> change(context, mode, false))
                .then(CommandSupport.targetArgument().executes(context -> change(context, mode, true)))));
        registrar.register(gameMode.build(), messages.raw(Keys.Message.COMMAND_GM_DESCRIPTION));
        SHORTCUTS.forEach((mode, command) -> registrar.register(Commands.literal(command)
                        .executes(context -> change(context, mode, false))
                        .then(CommandSupport.targetArgument().executes(context -> change(context, mode, true)))
                        .build(),
                messages.raw(Keys.Message.COMMAND_GM_DESCRIPTION)));
    }

    public List<HelpEntry> helpEntries() {
        return List.of(new HelpEntry(HelpEntry.Category.ADMIN, "/" + GAME_MODE, Keys.Message.HELP_GM_ARGUMENTS,
                Keys.Message.HELP_GM_DESCRIPTION, Keys.Permission.UTILITIES_GAMEMODE_ALL, support.module()));
    }

    private int change(CommandContext<CommandSourceStack> context, GameMode mode, boolean hasTarget) {
        Player target = support.resolveTarget(context, PERMISSIONS.get(mode),
                Keys.Permission.UTILITIES_GAMEMODE_OTHERS, hasTarget);
        if (target == null) {
            return 0;
        }
        target.setGameMode(mode);
        support.notify(context.getSource().getSender(), target, Keys.Message.UTILITIES_GAMEMODE_CHANGED,
                Keys.Message.UTILITIES_GAMEMODE_CHANGED_OTHER,
                Placeholder.component("gamemode", support.messages().get(NAMES.get(mode))));
        return Command.SINGLE_SUCCESS;
    }
}
