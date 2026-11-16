package club.voidaris.core.utilities;

import club.voidaris.core.Keys;
import club.voidaris.core.command.HelpEntry;
import club.voidaris.core.message.Messages;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

/**
 * Commands that act on a player: health, hunger, flight, invulnerability, speed, helmet, item repair, inventory
 * clearing, ping and teleporting to the surface.
 */
public final class PlayerCommands {

    private static final String HEAL = "heal";
    private static final String FEED = "feed";
    private static final String FLY = "fly";
    private static final String GOD = "god";
    private static final String SPEED = "speed";
    private static final String HAT = "hat";
    private static final String SPEED_VALUE = "speed";
    private static final String REPAIR = "repair";
    private static final String REPAIR_ALL = "all";
    private static final String CLEAR = "clear";
    private static final String PING = "ping";
    private static final String TOP = "top";

    private static final float DEFAULT_WALK_SPEED = 0.2f;
    private static final float DEFAULT_FLY_SPEED = 0.1f;
    private static final float MAX_SPEED = 1.0f;
    private static final float MAX_SPEED_INPUT = 10.0f;
    private static final int MAX_FOOD = 20;

    @FunctionalInterface
    private interface TargetAction {
        int run(CommandContext<CommandSourceStack> context, boolean hasTarget);
    }

    private final CommandSupport support;

    public PlayerCommands(UtilitiesModule module, Messages messages) {
        this.support = new CommandSupport(module, messages);
    }

    public void register(Commands registrar) {
        var messages = support.messages();
        registrar.register(withTarget(HEAL, this::heal).build(), messages.raw(Keys.Message.COMMAND_HEAL_DESCRIPTION));
        registrar.register(withTarget(FEED, this::feed).build(), messages.raw(Keys.Message.COMMAND_FEED_DESCRIPTION));
        registrar.register(withTarget(FLY, this::fly).build(), messages.raw(Keys.Message.COMMAND_FLY_DESCRIPTION));
        registrar.register(withTarget(GOD, this::god).build(), messages.raw(Keys.Message.COMMAND_GOD_DESCRIPTION));
        registrar.register(Commands.literal(SPEED)
                        .executes(this::speedUsage)
                        .then(Commands.argument(SPEED_VALUE, StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    for (int value = 1; value <= MAX_SPEED_INPUT; value++) {
                                        builder.suggest(value);
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(context -> speed(context, false))
                                .then(CommandSupport.targetArgument().executes(context -> speed(context, true))))
                        .build(),
                messages.raw(Keys.Message.COMMAND_SPEED_DESCRIPTION));
        registrar.register(Commands.literal(HAT).executes(this::hat).build(),
                messages.raw(Keys.Message.COMMAND_HAT_DESCRIPTION));
        registrar.register(Commands.literal(REPAIR)
                        .executes(context -> repair(context, false))
                        .then(Commands.literal(REPAIR_ALL).executes(context -> repair(context, true)))
                        .build(),
                messages.raw(Keys.Message.COMMAND_REPAIR_DESCRIPTION), List.of("fix"));
        registrar.register(withTarget(CLEAR, this::clear).build(), messages.raw(Keys.Message.COMMAND_CLEAR_DESCRIPTION),
                List.of("clearinventory", "ci"));
        registrar.register(withTarget(PING, this::ping).build(), messages.raw(Keys.Message.COMMAND_PING_DESCRIPTION));
        registrar.register(Commands.literal(TOP).executes(this::top).build(),
                messages.raw(Keys.Message.COMMAND_TOP_DESCRIPTION));
    }

    public List<HelpEntry> helpEntries() {
        var module = support.module();
        var target = Keys.Message.HELP_UTILITIES_TARGET_ARGUMENTS;
        return List.of(
                new HelpEntry(HelpEntry.Category.PLAYER, "/" + HAT, null,
                        Keys.Message.HELP_HAT_DESCRIPTION, Keys.Permission.UTILITIES_HAT, module),
                new HelpEntry(HelpEntry.Category.PLAYER, "/" + PING, target,
                        Keys.Message.HELP_PING_DESCRIPTION, Keys.Permission.UTILITIES_PING, module),
                new HelpEntry(HelpEntry.Category.ADMIN, "/" + REPAIR, Keys.Message.HELP_REPAIR_ARGUMENTS,
                        Keys.Message.HELP_REPAIR_DESCRIPTION, Keys.Permission.UTILITIES_REPAIR, module),
                new HelpEntry(HelpEntry.Category.ADMIN, "/" + CLEAR, target,
                        Keys.Message.HELP_CLEAR_DESCRIPTION, Keys.Permission.UTILITIES_CLEAR, module),
                new HelpEntry(HelpEntry.Category.ADMIN, "/" + TOP, null,
                        Keys.Message.HELP_TOP_DESCRIPTION, Keys.Permission.UTILITIES_TOP, module),
                new HelpEntry(HelpEntry.Category.ADMIN, "/" + HEAL, target,
                        Keys.Message.HELP_HEAL_DESCRIPTION, Keys.Permission.UTILITIES_HEAL, module),
                new HelpEntry(HelpEntry.Category.ADMIN, "/" + FEED, target,
                        Keys.Message.HELP_FEED_DESCRIPTION, Keys.Permission.UTILITIES_FEED, module),
                new HelpEntry(HelpEntry.Category.ADMIN, "/" + FLY, target,
                        Keys.Message.HELP_FLY_DESCRIPTION, Keys.Permission.UTILITIES_FLY, module),
                new HelpEntry(HelpEntry.Category.ADMIN, "/" + GOD, target,
                        Keys.Message.HELP_GOD_DESCRIPTION, Keys.Permission.UTILITIES_GOD, module),
                new HelpEntry(HelpEntry.Category.ADMIN, "/" + SPEED, Keys.Message.HELP_SPEED_ARGUMENTS,
                        Keys.Message.HELP_SPEED_DESCRIPTION, Keys.Permission.UTILITIES_SPEED, module));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> withTarget(String command, TargetAction action) {
        return Commands.literal(command)
                .executes(context -> action.run(context, false))
                .then(CommandSupport.targetArgument().executes(context -> action.run(context, true)));
    }

    private int heal(CommandContext<CommandSourceStack> context, boolean hasTarget) {
        Player target = support.resolveTarget(context, Keys.Permission.UTILITIES_HEAL,
                Keys.Permission.UTILITIES_HEAL_OTHERS, hasTarget);
        if (target == null) {
            return 0;
        }
        var maxHealth = target.getAttribute(Attribute.MAX_HEALTH);
        target.setHealth(maxHealth != null ? maxHealth.getValue() : target.getHealth());
        target.setFoodLevel(MAX_FOOD);
        target.setSaturation(MAX_FOOD);
        target.setFireTicks(0);
        for (PotionEffect effect : target.getActivePotionEffects()) {
            if (effect.getType().getEffectCategory() == PotionEffectType.Category.HARMFUL) {
                target.removePotionEffect(effect.getType());
            }
        }
        support.notify(context.getSource().getSender(), target, Keys.Message.UTILITIES_HEAL_SELF,
                Keys.Message.UTILITIES_HEAL_OTHER);
        return Command.SINGLE_SUCCESS;
    }

    private int feed(CommandContext<CommandSourceStack> context, boolean hasTarget) {
        Player target = support.resolveTarget(context, Keys.Permission.UTILITIES_FEED,
                Keys.Permission.UTILITIES_FEED_OTHERS, hasTarget);
        if (target == null) {
            return 0;
        }
        target.setFoodLevel(MAX_FOOD);
        target.setSaturation(MAX_FOOD);
        support.notify(context.getSource().getSender(), target, Keys.Message.UTILITIES_FEED_SELF,
                Keys.Message.UTILITIES_FEED_OTHER);
        return Command.SINGLE_SUCCESS;
    }

    private int fly(CommandContext<CommandSourceStack> context, boolean hasTarget) {
        Player target = support.resolveTarget(context, Keys.Permission.UTILITIES_FLY,
                Keys.Permission.UTILITIES_FLY_OTHERS, hasTarget);
        if (target == null) {
            return 0;
        }
        boolean enabled = !target.getAllowFlight();
        target.setAllowFlight(enabled);
        if (!enabled) {
            target.setFlying(false);
        }
        var sender = context.getSource().getSender();
        if (enabled) {
            support.notify(sender, target, Keys.Message.UTILITIES_FLY_ENABLED, Keys.Message.UTILITIES_FLY_ENABLED_OTHER);
        } else {
            support.notify(sender, target, Keys.Message.UTILITIES_FLY_DISABLED, Keys.Message.UTILITIES_FLY_DISABLED_OTHER);
        }
        return Command.SINGLE_SUCCESS;
    }

    private int god(CommandContext<CommandSourceStack> context, boolean hasTarget) {
        Player target = support.resolveTarget(context, Keys.Permission.UTILITIES_GOD,
                Keys.Permission.UTILITIES_GOD_OTHERS, hasTarget);
        if (target == null) {
            return 0;
        }
        var sender = context.getSource().getSender();
        if (support.module().toggleGod(target)) {
            support.notify(sender, target, Keys.Message.UTILITIES_GOD_ENABLED, Keys.Message.UTILITIES_GOD_ENABLED_OTHER);
        } else {
            support.notify(sender, target, Keys.Message.UTILITIES_GOD_DISABLED, Keys.Message.UTILITIES_GOD_DISABLED_OTHER);
        }
        return Command.SINGLE_SUCCESS;
    }

    private int speed(CommandContext<CommandSourceStack> context, boolean hasTarget) {
        Float input = parseSpeed(StringArgumentType.getString(context, SPEED_VALUE));
        if (input == null) {
            return speedUsage(context);
        }
        Player target = support.resolveTarget(context, Keys.Permission.UTILITIES_SPEED,
                Keys.Permission.UTILITIES_SPEED_OTHERS, hasTarget);
        if (target == null) {
            return 0;
        }
        boolean flying = target.isFlying();
        if (flying) {
            target.setFlySpeed(toBukkitSpeed(input, DEFAULT_FLY_SPEED));
        } else {
            target.setWalkSpeed(toBukkitSpeed(input, DEFAULT_WALK_SPEED));
        }
        var value = Formatter.number("speed", input);
        var sender = context.getSource().getSender();
        if (flying) {
            support.notify(sender, target, Keys.Message.UTILITIES_SPEED_FLY, Keys.Message.UTILITIES_SPEED_FLY_OTHER, value);
        } else {
            support.notify(sender, target, Keys.Message.UTILITIES_SPEED_WALK, Keys.Message.UTILITIES_SPEED_WALK_OTHER, value);
        }
        return Command.SINGLE_SUCCESS;
    }

    private int speedUsage(CommandContext<CommandSourceStack> context) {
        return support.usage(context.getSource().getSender(), Keys.Permission.UTILITIES_SPEED, "/" + SPEED,
                Keys.Message.HELP_SPEED_ARGUMENTS);
    }

    private static Float parseSpeed(String text) {
        try {
            float value = Float.parseFloat(text);
            return value >= 0 && value <= MAX_SPEED_INPUT ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // Input 1 is the vanilla speed and 10 the maximum; below 1 it scales down linearly towards a standstill.
    private static float toBukkitSpeed(float input, float defaultSpeed) {
        if (input < 1) {
            return defaultSpeed * input;
        }
        return defaultSpeed + (input - 1) / (MAX_SPEED_INPUT - 1) * (MAX_SPEED - defaultSpeed);
    }

    /**
     * @param all whether to repair the whole inventory (armor and offhand included) instead of the held item
     */
    private int repair(CommandContext<CommandSourceStack> context, boolean all) {
        Player player = support.requirePlayer(context, Keys.Permission.UTILITIES_REPAIR);
        if (player == null || (all && !support.checkAccess(player, Keys.Permission.UTILITIES_REPAIR_ALL))) {
            return 0;
        }
        var inventory = player.getInventory();
        if (!all) {
            if (!repair(inventory.getItemInMainHand())) {
                support.messages().send(player, Keys.Message.UTILITIES_REPAIR_NOTHING);
                return 0;
            }
            support.messages().send(player, Keys.Message.UTILITIES_REPAIR_HAND);
            return Command.SINGLE_SUCCESS;
        }
        int repaired = 0;
        for (ItemStack item : inventory.getContents()) {
            if (repair(item)) {
                repaired++;
            }
        }
        if (repaired == 0) {
            support.messages().send(player, Keys.Message.UTILITIES_REPAIR_NOTHING_ALL);
            return 0;
        }
        support.messages().send(player, Keys.Message.UTILITIES_REPAIR_ALL, Formatter.number("count", repaired));
        return Command.SINGLE_SUCCESS;
    }

    // getContents and getItemInMainHand return live stacks, so setting the meta repairs the item in place.
    private static boolean repair(ItemStack item) {
        if (item == null || item.isEmpty() || !(item.getItemMeta() instanceof Damageable meta) || !meta.hasDamage()) {
            return false;
        }
        meta.setDamage(0);
        item.setItemMeta(meta);
        return true;
    }

    private int clear(CommandContext<CommandSourceStack> context, boolean hasTarget) {
        Player target = support.resolveTarget(context, Keys.Permission.UTILITIES_CLEAR,
                Keys.Permission.UTILITIES_CLEAR_OTHERS, hasTarget);
        if (target == null) {
            return 0;
        }
        target.getInventory().clear();
        support.notify(context.getSource().getSender(), target, Keys.Message.UTILITIES_CLEAR_SELF,
                Keys.Message.UTILITIES_CLEAR_OTHER);
        return Command.SINGLE_SUCCESS;
    }

    // Only the sender is told: checking someone's ping is not something they need to know about.
    private int ping(CommandContext<CommandSourceStack> context, boolean hasTarget) {
        Player target = support.resolveTarget(context, Keys.Permission.UTILITIES_PING,
                Keys.Permission.UTILITIES_PING_OTHERS, hasTarget);
        if (target == null) {
            return 0;
        }
        var sender = context.getSource().getSender();
        var ping = Formatter.number("ping", target.getPing());
        if (sender == target) {
            support.messages().send(sender, Keys.Message.UTILITIES_PING_SELF, ping);
        } else {
            support.messages().send(sender, Keys.Message.UTILITIES_PING_OTHER, ping,
                    Placeholder.unparsed("player", target.getName()));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int top(CommandContext<CommandSourceStack> context) {
        Player player = support.requirePlayer(context, Keys.Permission.UTILITIES_TOP);
        if (player == null) {
            return 0;
        }
        var location = player.getLocation();
        // The highest block of a world with a ceiling (the Nether) is its bedrock roof.
        if (location.getWorld().hasCeiling()) {
            support.messages().send(player, Keys.Message.UTILITIES_TOP_CEILING);
            return 0;
        }
        var surface = location.getWorld().getHighestBlockAt(location).getLocation().add(0.5, 1, 0.5);
        surface.setYaw(location.getYaw());
        surface.setPitch(location.getPitch());
        player.teleportAsync(surface).thenAccept(success -> {
            if (success) {
                support.messages().send(player, Keys.Message.UTILITIES_TOP_TELEPORTED);
            }
        });
        return Command.SINGLE_SUCCESS;
    }

    private int hat(CommandContext<CommandSourceStack> context) {
        Player player = support.requirePlayer(context, Keys.Permission.UTILITIES_HAT);
        if (player == null) {
            return 0;
        }
        var inventory = player.getInventory();
        ItemStack hand = inventory.getItemInMainHand();
        if (hand.isEmpty()) {
            support.messages().send(player, Keys.Message.UTILITIES_HAT_EMPTY_HAND);
            return 0;
        }
        ItemStack helmet = inventory.getHelmet();
        boolean hasHelmet = helmet != null && !helmet.isEmpty();
        if (hasHelmet && helmet.containsEnchantment(Enchantment.BINDING_CURSE) && player.getGameMode() != GameMode.CREATIVE) {
            support.messages().send(player, Keys.Message.UTILITIES_HAT_BOUND);
            return 0;
        }
        inventory.setHelmet(hand.asOne());
        inventory.setItemInMainHand(hand.subtract());
        if (hasHelmet) {
            inventory.addItem(helmet).values().forEach(leftover -> player.getWorld().dropItem(player.getLocation(), leftover));
        }
        support.messages().send(player, Keys.Message.UTILITIES_HAT_EQUIPPED);
        return Command.SINGLE_SUCCESS;
    }
}
