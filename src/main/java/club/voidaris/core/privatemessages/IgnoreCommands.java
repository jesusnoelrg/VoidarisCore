package club.voidaris.core.privatemessages;

import club.voidaris.core.Keys;
import club.voidaris.core.command.CommandUsage;
import club.voidaris.core.command.HelpEntry;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class IgnoreCommands {

    private static final String IGNORE = "ignore";
    private static final String SUPER_IGNORE = "superignore";
    private static final String LIST_IGNORE = "listignore";
    private static final String TARGET = "player";
    private static final String PAGE = "page";

    private record Target(UUID id, String name) {
    }

    private final PrivateMessagesModule module;

    public IgnoreCommands(PrivateMessagesModule module) {
        this.module = module;
    }

    public void register(Commands registrar) {
        var messages = module.messages();
        registrar.register(Commands.literal(IGNORE)
                        .executes(context -> module.usage(context.getSource().getSender(),
                                Keys.Permission.PRIVATE_MESSAGES_IGNORE, "/" + IGNORE, Keys.Message.HELP_IGNORE_ARGUMENTS))
                        .then(targetArgument().executes(context -> toggle(context, IgnoreLevel.PRIVATE)))
                        .build(),
                messages.raw(Keys.Message.COMMAND_IGNORE_DESCRIPTION));
        registrar.register(Commands.literal(SUPER_IGNORE)
                        .executes(context -> module.usage(context.getSource().getSender(),
                                Keys.Permission.PRIVATE_MESSAGES_SUPERIGNORE, "/" + SUPER_IGNORE, Keys.Message.HELP_IGNORE_ARGUMENTS))
                        .then(targetArgument().executes(context -> toggle(context, IgnoreLevel.ALL)))
                        .build(),
                messages.raw(Keys.Message.COMMAND_SUPERIGNORE_DESCRIPTION));
        registrar.register(Commands.literal(LIST_IGNORE)
                        .executes(context -> list(context, 1))
                        .then(Commands.argument(PAGE, StringArgumentType.word())
                                .executes(context -> {
                                    Integer page = CommandUsage.parseInt(StringArgumentType.getString(context, PAGE), 1, Integer.MAX_VALUE);
                                    return page != null ? list(context, page) : module.usage(context.getSource().getSender(),
                                            Keys.Permission.PRIVATE_MESSAGES_IGNORE, "/" + LIST_IGNORE, Keys.Message.HELP_LISTIGNORE_ARGUMENTS);
                                }))
                        .build(),
                messages.raw(Keys.Message.COMMAND_LISTIGNORE_DESCRIPTION));
    }

    public List<HelpEntry> helpEntries() {
        return List.of(
                new HelpEntry(HelpEntry.Category.PLAYER, "/" + IGNORE, Keys.Message.HELP_IGNORE_ARGUMENTS,
                        Keys.Message.HELP_IGNORE_DESCRIPTION, Keys.Permission.PRIVATE_MESSAGES_IGNORE, module),
                new HelpEntry(HelpEntry.Category.PLAYER, "/" + SUPER_IGNORE, Keys.Message.HELP_IGNORE_ARGUMENTS,
                        Keys.Message.HELP_SUPERIGNORE_DESCRIPTION, Keys.Permission.PRIVATE_MESSAGES_SUPERIGNORE, module),
                new HelpEntry(HelpEntry.Category.PLAYER, "/" + LIST_IGNORE, null,
                        Keys.Message.HELP_LISTIGNORE_DESCRIPTION, Keys.Permission.PRIVATE_MESSAGES_IGNORE, module));
    }

    // Online players plus the sender's ignored ones, so offline players can be unignored with tab completion.
    private RequiredArgumentBuilder<CommandSourceStack, String> targetArgument() {
        return Commands.argument(TARGET, StringArgumentType.word()).suggests((context, builder) -> {
            var sender = context.getSource().getSender();
            PrivateMessageCommands.suggestOnlinePlayers(sender, builder);
            String remaining = builder.getRemainingLowerCase();
            if (sender instanceof Player player) {
                for (var entry : module.store().ignored(player.getUniqueId()).values()) {
                    if (entry.name().toLowerCase().startsWith(remaining) && Bukkit.getPlayerExact(entry.name()) == null) {
                        builder.suggest(entry.name());
                    }
                }
            }
            return builder.buildFuture();
        });
    }

    private int toggle(CommandContext<CommandSourceStack> context, IgnoreLevel level) {
        String permission = level == IgnoreLevel.ALL
                ? Keys.Permission.PRIVATE_MESSAGES_SUPERIGNORE
                : Keys.Permission.PRIVATE_MESSAGES_IGNORE;
        Player owner = module.requirePlayer(context.getSource().getSender(), permission);
        if (owner == null) {
            return 0;
        }
        var messages = module.messages();
        String typed = StringArgumentType.getString(context, TARGET);
        Target target = resolve(owner, typed);
        if (target == null) {
            messages.send(owner, Keys.Message.PRIVATE_MESSAGES_PLAYER_UNKNOWN, Placeholder.unparsed("player", typed));
            return 0;
        }
        var player = Placeholder.unparsed("player", target.name());
        if (target.id().equals(owner.getUniqueId())) {
            messages.send(owner, Keys.Message.PRIVATE_MESSAGES_IGNORE_SELF);
            return 0;
        }

        var store = module.store();
        IgnoreLevel current = store.level(owner.getUniqueId(), target.id());
        if (current == level) {
            store.unignore(owner.getUniqueId(), target.id());
            messages.send(owner, level == IgnoreLevel.ALL
                    ? Keys.Message.PRIVATE_MESSAGES_SUPER_UNIGNORED
                    : Keys.Message.PRIVATE_MESSAGES_UNIGNORED, player);
            return Command.SINGLE_SUCCESS;
        }
        // /ignore must not silently downgrade a superignore; only /superignore can lift it.
        if (current == IgnoreLevel.ALL) {
            messages.send(owner, Keys.Message.PRIVATE_MESSAGES_ALREADY_SUPER_IGNORED, player);
            return 0;
        }
        store.ignore(owner.getUniqueId(), target.id(), target.name(), level);
        messages.send(owner, level == IgnoreLevel.ALL
                ? Keys.Message.PRIVATE_MESSAGES_SUPER_IGNORED
                : Keys.Message.PRIVATE_MESSAGES_IGNORED, player);
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Looks in the owner's own list first, so a player can be unignored even if the server no longer caches them.
     */
    private Target resolve(Player owner, String name) {
        for (Map.Entry<UUID, PrivateMessageStore.IgnoreEntry> entry : module.store().ignored(owner.getUniqueId()).entrySet()) {
            if (entry.getValue().name().equalsIgnoreCase(name)) {
                return new Target(entry.getKey(), entry.getValue().name());
            }
        }
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return new Target(online.getUniqueId(), online.getName());
        }
        var offline = Bukkit.getOfflinePlayerIfCached(name);
        if (offline == null) {
            return null;
        }
        return new Target(offline.getUniqueId(), offline.getName() != null ? offline.getName() : name);
    }

    private int list(CommandContext<CommandSourceStack> context, int page) {
        Player owner = module.requirePlayer(context.getSource().getSender(), Keys.Permission.PRIVATE_MESSAGES_IGNORE);
        if (owner == null) {
            return 0;
        }
        var messages = module.messages();
        List<PrivateMessageStore.IgnoreEntry> entries = module.store().ignored(owner.getUniqueId()).values().stream()
                .sorted(Comparator.comparing(PrivateMessageStore.IgnoreEntry::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
        if (entries.isEmpty()) {
            messages.send(owner, Keys.Message.PRIVATE_MESSAGES_IGNORE_LIST_EMPTY);
            return 0;
        }
        int size = module.ignoreListPageSize();
        int pages = (entries.size() + size - 1) / size;
        var pageCount = Placeholder.unparsed("pages", Integer.toString(pages));
        if (page > pages) {
            messages.send(owner, Keys.Message.HELP_INVALID_PAGE, Placeholder.unparsed("page", Integer.toString(page)), pageCount);
            return 0;
        }

        var pageNumber = Placeholder.unparsed("page", Integer.toString(page));
        owner.sendMessage(messages.get(Keys.Message.PRIVATE_MESSAGES_IGNORE_LIST_HEADER, pageNumber, pageCount));
        for (var entry : entries.subList((page - 1) * size, Math.min(page * size, entries.size()))) {
            owner.sendMessage(line(entry));
        }
        if (pages > 1) {
            owner.sendMessage(messages.get(Keys.Message.HELP_FOOTER, pageNumber, pageCount,
                    Placeholder.component("previous", pageButton(page - 1, pages, Keys.Message.HELP_PREVIOUS, Keys.Message.HELP_PREVIOUS_UNAVAILABLE)),
                    Placeholder.component("next", pageButton(page + 1, pages, Keys.Message.HELP_NEXT, Keys.Message.HELP_NEXT_UNAVAILABLE))));
        }
        return Command.SINGLE_SUCCESS;
    }

    private Component line(PrivateMessageStore.IgnoreEntry entry) {
        var messages = module.messages();
        boolean all = entry.level() == IgnoreLevel.ALL;
        var player = Placeholder.unparsed("player", entry.name());
        return messages.get(Keys.Message.PRIVATE_MESSAGES_IGNORE_LIST_ENTRY, player,
                        Placeholder.component("scope", messages.get(all
                                ? Keys.Message.PRIVATE_MESSAGES_IGNORE_LIST_SCOPE_ALL
                                : Keys.Message.PRIVATE_MESSAGES_IGNORE_LIST_SCOPE_PRIVATE)))
                .clickEvent(ClickEvent.runCommand("/" + (all ? SUPER_IGNORE : IGNORE) + " " + entry.name()))
                .hoverEvent(HoverEvent.showText(messages.get(Keys.Message.PRIVATE_MESSAGES_IGNORE_LIST_HOVER, player)));
    }

    private Component pageButton(int target, int pages, String key, String unavailableKey) {
        var messages = module.messages();
        if (target < 1 || target > pages) {
            return messages.get(unavailableKey);
        }
        return messages.get(key)
                .clickEvent(ClickEvent.runCommand("/" + LIST_IGNORE + " " + target))
                .hoverEvent(HoverEvent.showText(messages.get(Keys.Message.HELP_HOVER_PAGE,
                        Placeholder.unparsed("page", Integer.toString(target)))));
    }
}
