package club.voidaris.core.utilities;

import club.voidaris.core.Keys;
import club.voidaris.core.command.HelpEntry;
import club.voidaris.core.message.Messages;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
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
                .executes(this::usage)
                .then(Commands.argument(MODE, StringArgumentType.word())
                        .suggests((context, builder) -> {
                            String remaining = builder.getRemainingLowerCase();
                            for (GameMode mode : GameMode.values()) {
                                String name = mode.name().toLowerCase(Locale.ROOT);
                                if (name.startsWith(remaining)) {
                                    builder.suggest(name);
                                }
                            }
                            NUMERIC_IDS.keySet().stream().sorted().filter(id -> id.startsWith(remaining)).forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(context -> changeTyped(context, false))
                        .then(CommandSupport.targetArgument().executes(context -> changeTyped(context, true))));
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

    private int usage(CommandContext<CommandSourceStack> context) {
        var sender = context.getSource().getSender();
        // Any mode node is enough to be told how /gm works; the chosen mode is checked when it runs.
        String permission = PERMISSIONS.values().stream().filter(sender::hasPermission).findFirst()
                .orElse(Keys.Permission.UTILITIES_GAMEMODE_ALL);
        return support.usage(sender, permission, "/" + GAME_MODE, Keys.Message.HELP_GM_ARGUMENTS);
    }

    private int changeTyped(CommandContext<CommandSourceStack> context, boolean hasTarget) {
        String typed = StringArgumentType.getString(context, MODE).toLowerCase(Locale.ROOT);
        GameMode mode = NUMERIC_IDS.get(typed);
        if (mode == null) {
            mode = Arrays.stream(GameMode.values())
                    .filter(candidate -> candidate.name().toLowerCase(Locale.ROOT).equals(typed))
                    .findFirst().orElse(null);
        }
        return mode != null ? change(context, mode, hasTarget) : usage(context);
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
