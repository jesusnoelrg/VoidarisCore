package club.voidaris.core.utilities;

import club.voidaris.core.Keys;
import club.voidaris.core.command.HelpEntry;
import club.voidaris.core.message.Messages;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.Consumer;

/**
 * Time and weather shortcuts applied to the sender's current world.
 */
public final class WorldCommands {

    private static final String DAY = "day";
    private static final String NIGHT = "night";
    private static final String SUN = "sun";
    private static final String RAIN = "rain";

    private static final long DAY_TIME = 1000;
    private static final long NIGHT_TIME = 13000;

    private final CommandSupport support;

    public WorldCommands(UtilitiesModule module, Messages messages) {
        this.support = new CommandSupport(module, messages);
    }

    public void register(Commands registrar) {
        var messages = support.messages();
        registrar.register(Commands.literal(DAY).executes(context -> apply(context, Keys.Permission.UTILITIES_TIME,
                        world -> world.setTime(DAY_TIME), Keys.Message.UTILITIES_TIME_DAY)).build(),
                messages.raw(Keys.Message.COMMAND_DAY_DESCRIPTION));
        registrar.register(Commands.literal(NIGHT).executes(context -> apply(context, Keys.Permission.UTILITIES_TIME,
                        world -> world.setTime(NIGHT_TIME), Keys.Message.UTILITIES_TIME_NIGHT)).build(),
                messages.raw(Keys.Message.COMMAND_NIGHT_DESCRIPTION));
        registrar.register(Commands.literal(SUN).executes(context -> apply(context, Keys.Permission.UTILITIES_WEATHER,
                        WorldCommands::clearWeather, Keys.Message.UTILITIES_WEATHER_SUN)).build(),
                messages.raw(Keys.Message.COMMAND_SUN_DESCRIPTION));
        registrar.register(Commands.literal(RAIN).executes(context -> apply(context, Keys.Permission.UTILITIES_WEATHER,
                        world -> world.setStorm(true), Keys.Message.UTILITIES_WEATHER_RAIN)).build(),
                messages.raw(Keys.Message.COMMAND_RAIN_DESCRIPTION));
    }

    public List<HelpEntry> helpEntries() {
        var module = support.module();
        return List.of(
                new HelpEntry(HelpEntry.Category.ADMIN, "/" + DAY, null,
                        Keys.Message.HELP_DAY_DESCRIPTION, Keys.Permission.UTILITIES_TIME, module),
                new HelpEntry(HelpEntry.Category.ADMIN, "/" + NIGHT, null,
                        Keys.Message.HELP_NIGHT_DESCRIPTION, Keys.Permission.UTILITIES_TIME, module),
                new HelpEntry(HelpEntry.Category.ADMIN, "/" + SUN, null,
                        Keys.Message.HELP_SUN_DESCRIPTION, Keys.Permission.UTILITIES_WEATHER, module),
                new HelpEntry(HelpEntry.Category.ADMIN, "/" + RAIN, null,
                        Keys.Message.HELP_RAIN_DESCRIPTION, Keys.Permission.UTILITIES_WEATHER, module));
    }

    private static void clearWeather(World world) {
        world.setStorm(false);
        world.setThundering(false);
    }

    private int apply(CommandContext<CommandSourceStack> context, String permission, Consumer<World> change,
                      String messageKey) {
        Player player = support.requirePlayer(context, permission);
        if (player == null) {
            return 0;
        }
        World world = player.getWorld();
        change.accept(world);
        support.messages().send(player, messageKey, Placeholder.unparsed("world", world.getName()));
        return Command.SINGLE_SUCCESS;
    }
}
