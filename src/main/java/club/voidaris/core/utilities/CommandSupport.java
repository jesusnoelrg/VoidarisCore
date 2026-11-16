package club.voidaris.core.utilities;

import club.voidaris.core.Keys;
import club.voidaris.core.message.Messages;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Access checks and the optional {@code [player]} argument shared by every utilities command.
 */
final class CommandSupport {

    static final String TARGET = "player";

    private final UtilitiesModule module;
    private final Messages messages;

    CommandSupport(UtilitiesModule module, Messages messages) {
        this.module = module;
        this.messages = messages;
    }

    UtilitiesModule module() {
        return module;
    }

    Messages messages() {
        return messages;
    }

    static RequiredArgumentBuilder<CommandSourceStack, String> targetArgument() {
        return Commands.argument(TARGET, StringArgumentType.word()).suggests((context, builder) -> {
            var sender = context.getSource().getSender();
            String remaining = builder.getRemainingLowerCase();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if ((!(sender instanceof Player viewer) || viewer.canSee(online))
                        && online.getName().toLowerCase().startsWith(remaining)) {
                    builder.suggest(online.getName());
                }
            }
            return builder.buildFuture();
        });
    }

    // Checked here instead of requires() so the sender gets the Spanish message rather than "unknown command".
    boolean checkAccess(CommandSender sender, String permission) {
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

    Player requirePlayer(CommandContext<CommandSourceStack> context, String permission) {
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

    /**
     * Resolves who the command acts on: the sender itself, or the {@code player} argument when present, which also
     * requires {@code othersPermission}. Sends the error and returns {@code null} when the command cannot proceed.
     */
    Player resolveTarget(CommandContext<CommandSourceStack> context, String permission, String othersPermission,
                         boolean hasTarget) {
        if (!hasTarget) {
            return requirePlayer(context, permission);
        }
        var sender = context.getSource().getSender();
        if (!checkAccess(sender, permission) || !checkAccess(sender, othersPermission)) {
            return null;
        }
        String name = StringArgumentType.getString(context, TARGET);
        Player target = Bukkit.getPlayerExact(name);
        if (target == null || (sender instanceof Player viewer && !viewer.canSee(target))) {
            messages.send(sender, Keys.Message.GENERAL_PLAYER_NOT_FOUND, Placeholder.unparsed("player", name));
            return null;
        }
        return target;
    }

    /**
     * Notifies the target with {@code selfKey}, and the sender with {@code otherKey} when they are different people.
     */
    void notify(CommandSender sender, Player target, String selfKey, String otherKey, TagResolver... resolvers) {
        messages.send(target, selfKey, resolvers);
        if (sender != target) {
            messages.send(sender, otherKey, TagResolver.resolver(resolvers),
                    Placeholder.unparsed("player", target.getName()));
        }
    }
}
