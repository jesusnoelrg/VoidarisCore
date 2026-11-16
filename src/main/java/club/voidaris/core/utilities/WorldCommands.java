package club.voidaris.core.utilities;

import club.voidaris.core.Keys;
import club.voidaris.core.command.CommandUsage;
import club.voidaris.core.command.HelpEntry;
import club.voidaris.core.message.Messages;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Time and weather of the sender's current world, as shortcuts (/day, /sun...) and as /time and /weather with a value.
 */
public final class WorldCommands {

    private static final String DAY = "day";
    private static final String NIGHT = "night";
    private static final String SUN = "sun";
    private static final String RAIN = "rain";
    private static final String TIME = "time";
    private static final String WEATHER = "weather";
    private static final String THUNDER = "thunder";
    private static final String VALUE = "value";

    private static final long DAY_TIME = 1000;
    private static final long NIGHT_TIME = 13000;
    private static final long TICKS_PER_DAY = 24000;
    /** Values accepted by /time besides a number of ticks. */
    private static final Map<String, Long> TIME_NAMES = new LinkedHashMap<>();

    static {
        TIME_NAMES.put(DAY, DAY_TIME);
        TIME_NAMES.put("noon", 6000L);
        TIME_NAMES.put(NIGHT, NIGHT_TIME);
        TIME_NAMES.put("midnight", 18000L);
    }

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
                        WorldCommands::rain, Keys.Message.UTILITIES_WEATHER_RAIN)).build(),
                messages.raw(Keys.Message.COMMAND_RAIN_DESCRIPTION));
        registrar.register(Commands.literal(TIME)
                        .executes(context -> support.usage(context.getSource().getSender(), Keys.Permission.UTILITIES_TIME,
                                "/" + TIME, Keys.Message.HELP_TIME_ARGUMENTS))
                        .then(Commands.argument(VALUE, StringArgumentType.word())
                                .suggests((context, builder) -> suggest(builder, TIME_NAMES.keySet()))
                                .executes(this::time))
                        .build(),
                messages.raw(Keys.Message.COMMAND_TIME_DESCRIPTION));
        registrar.register(Commands.literal(WEATHER)
                        .executes(context -> support.usage(context.getSource().getSender(),
                                Keys.Permission.UTILITIES_WEATHER, "/" + WEATHER, Keys.Message.HELP_WEATHER_ARGUMENTS))
                        .then(Commands.argument(VALUE, StringArgumentType.word())
                                .suggests((context, builder) -> suggest(builder, List.of(SUN, RAIN, THUNDER)))
                                .executes(this::weather))
                        .build(),
                messages.raw(Keys.Message.COMMAND_WEATHER_DESCRIPTION));
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
                        Keys.Message.HELP_RAIN_DESCRIPTION, Keys.Permission.UTILITIES_WEATHER, module),
                new HelpEntry(HelpEntry.Category.ADMIN, "/" + TIME, Keys.Message.HELP_TIME_ARGUMENTS,
                        Keys.Message.HELP_TIME_DESCRIPTION, Keys.Permission.UTILITIES_TIME, module),
                new HelpEntry(HelpEntry.Category.ADMIN, "/" + WEATHER, Keys.Message.HELP_WEATHER_ARGUMENTS,
                        Keys.Message.HELP_WEATHER_DESCRIPTION, Keys.Permission.UTILITIES_WEATHER, module));
    }

    private int time(CommandContext<CommandSourceStack> context) {
        String value = StringArgumentType.getString(context, VALUE).toLowerCase(Locale.ROOT);
        Long ticks = TIME_NAMES.get(value);
        if (ticks == null) {
            Integer parsed = CommandUsage.parseInt(value, 0, (int) TICKS_PER_DAY - 1);
            ticks = parsed == null ? null : parsed.longValue();
        }
        if (ticks == null) {
            return support.usage(context.getSource().getSender(), Keys.Permission.UTILITIES_TIME, "/" + TIME,
                    Keys.Message.HELP_TIME_ARGUMENTS);
        }
        long target = ticks;
        return apply(context, Keys.Permission.UTILITIES_TIME, world -> world.setTime(target),
                Keys.Message.UTILITIES_TIME_SET, Placeholder.unparsed("time", value));
    }

    private int weather(CommandContext<CommandSourceStack> context) {
        String value = StringArgumentType.getString(context, VALUE).toLowerCase(Locale.ROOT);
        return switch (value) {
            case SUN -> apply(context, Keys.Permission.UTILITIES_WEATHER, WorldCommands::clearWeather,
                    Keys.Message.UTILITIES_WEATHER_SUN);
            case RAIN -> apply(context, Keys.Permission.UTILITIES_WEATHER, WorldCommands::rain,
                    Keys.Message.UTILITIES_WEATHER_RAIN);
            case THUNDER -> apply(context, Keys.Permission.UTILITIES_WEATHER, WorldCommands::thunder,
                    Keys.Message.UTILITIES_WEATHER_THUNDER);
            default -> support.usage(context.getSource().getSender(), Keys.Permission.UTILITIES_WEATHER,
                    "/" + WEATHER, Keys.Message.HELP_WEATHER_ARGUMENTS);
        };
    }

    private static CompletableFuture<Suggestions> suggest(SuggestionsBuilder builder, Collection<String> values) {
        for (String value : values) {
            if (value.startsWith(builder.getRemainingLowerCase())) {
                builder.suggest(value);
            }
        }
        return builder.buildFuture();
    }

    private static void rain(World world) {
        world.setStorm(true);
        world.setThundering(false);
    }

    private static void thunder(World world) {
        world.setStorm(true);
        world.setThundering(true);
    }

    private static void clearWeather(World world) {
        world.setStorm(false);
        world.setThundering(false);
    }

    private int apply(CommandContext<CommandSourceStack> context, String permission, Consumer<World> change,
                      String messageKey, TagResolver... resolvers) {
        Player player = support.requirePlayer(context, permission);
        if (player == null) {
            return 0;
        }
        World world = player.getWorld();
        change.accept(world);
        support.messages().send(player, messageKey, Placeholder.unparsed("world", world.getName()),
                TagResolver.resolver(resolvers));
        return Command.SINGLE_SUCCESS;
    }
}
