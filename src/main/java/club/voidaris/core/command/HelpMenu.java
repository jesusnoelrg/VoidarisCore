package club.voidaris.core.command;

import club.voidaris.core.Keys;
import club.voidaris.core.message.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public final class HelpMenu {

    public static final int PAGE_SIZE = 8;

    private final Messages messages;
    private final String pageCommand;
    private final List<HelpEntry> entries = new ArrayList<>();

    /**
     * @param pageCommand the command that opens a page when followed by its number, such as {@code /voidaris help}
     */
    public HelpMenu(Messages messages, String pageCommand) {
        this.messages = messages;
        this.pageCommand = pageCommand;
    }

    public void register(Collection<HelpEntry> entries) {
        this.entries.addAll(entries);
    }

    /**
     * @return {@code false} when the page does not exist for this sender
     */
    public boolean send(CommandSender sender, int page) {
        // Stable sort: player commands first, each category keeping its registration order.
        List<HelpEntry> visible = entries.stream()
                .filter(entry -> entry.visibleTo(sender))
                .sorted(Comparator.comparing(HelpEntry::category))
                .toList();
        int pages = Math.max(1, (visible.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        if (page < 1 || page > pages) {
            messages.send(sender, Keys.Message.HELP_INVALID_PAGE,
                    Placeholder.unparsed("page", Integer.toString(page)),
                    Placeholder.unparsed("pages", Integer.toString(pages)));
            return false;
        }

        var pageNumber = Placeholder.unparsed("page", Integer.toString(page));
        var pageCount = Placeholder.unparsed("pages", Integer.toString(pages));
        sender.sendMessage(messages.get(Keys.Message.HELP_HEADER, pageNumber, pageCount));

        List<HelpEntry> slice = visible.subList((page - 1) * PAGE_SIZE, Math.min(page * PAGE_SIZE, visible.size()));
        HelpEntry.Category section = null;
        for (HelpEntry entry : slice) {
            // Repeated at the top of each page so a page that starts mid-category still says which one it is.
            if (entry.category() != section) {
                section = entry.category();
                sender.sendMessage(messages.get(section == HelpEntry.Category.ADMIN
                        ? Keys.Message.HELP_SECTION_ADMIN
                        : Keys.Message.HELP_SECTION_PLAYER));
            }
            sender.sendMessage(line(entry));
        }

        if (pages > 1) {
            sender.sendMessage(messages.get(Keys.Message.HELP_FOOTER, pageNumber, pageCount,
                    Placeholder.component("previous", pageButton(page - 1, pages, Keys.Message.HELP_PREVIOUS, Keys.Message.HELP_PREVIOUS_UNAVAILABLE)),
                    Placeholder.component("next", pageButton(page + 1, pages, Keys.Message.HELP_NEXT, Keys.Message.HELP_NEXT_UNAVAILABLE))));
        }
        return true;
    }

    private Component line(HelpEntry entry) {
        Component arguments = entry.suggests()
                ? Component.space().append(messages.get(entry.argumentsKey()))
                : Component.empty();
        Component line = messages.get(Keys.Message.HELP_ENTRY,
                Placeholder.unparsed("command", entry.command()),
                Placeholder.component("arguments", arguments),
                Placeholder.component("description", messages.get(entry.descriptionKey())));
        if (entry.suggests()) {
            return line.clickEvent(ClickEvent.suggestCommand(entry.command() + " "))
                    .hoverEvent(HoverEvent.showText(messages.get(Keys.Message.HELP_HOVER_SUGGEST)));
        }
        return line.clickEvent(ClickEvent.runCommand(entry.command()))
                .hoverEvent(HoverEvent.showText(messages.get(Keys.Message.HELP_HOVER_RUN)));
    }

    private Component pageButton(int target, int pages, String key, String unavailableKey) {
        if (target < 1 || target > pages) {
            return messages.get(unavailableKey);
        }
        return messages.get(key)
                .clickEvent(ClickEvent.runCommand(pageCommand + " " + target))
                .hoverEvent(HoverEvent.showText(messages.get(Keys.Message.HELP_HOVER_PAGE,
                        Placeholder.unparsed("page", Integer.toString(target)))));
    }
}
