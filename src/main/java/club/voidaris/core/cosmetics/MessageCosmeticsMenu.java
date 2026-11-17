package club.voidaris.core.cosmetics;

import club.voidaris.core.Keys;
import club.voidaris.core.config.ConfigManager;
import club.voidaris.core.gui.LoreWrapper;
import club.voidaris.core.gui.Menu;
import club.voidaris.core.gui.MenuEntry;
import club.voidaris.core.gui.MenuLoader;
import club.voidaris.core.message.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * The menu of a {@link MessageCategory}: the static items plus one paginated icon per cosmetic under
 * {@code <guiSection>.icons}.
 */
final class MessageCosmeticsMenu {

    private static final String PREVIEW = "preview";
    private static final int DEFAULT_PREVIEW_WIDTH = 32;

    private final JavaPlugin plugin;
    private final ConfigManager configs;
    private final CosmeticsModule module;
    private final Messages messages;
    private final MenuLoader loader;
    private final MessageCategory category;
    private final BiFunction<Player, String, Component> preview;

    /**
     * @param preview renders a non-empty format as the player would show it
     */
    MessageCosmeticsMenu(JavaPlugin plugin, ConfigManager configs, CosmeticsModule module, Messages messages,
                         MenuLoader loader, MessageCategory category, BiFunction<Player, String, Component> preview) {
        this.plugin = plugin;
        this.configs = configs;
        this.module = module;
        this.messages = messages;
        this.loader = loader;
        this.category = category;
        this.preview = preview;
    }

    void open(Player player) {
        var menu = loader.load(category.gui.apply(configs).get(), player);
        renderPage(menu, player, 0);
        player.openInventory(menu.getInventory());
    }

    private void renderPage(Menu menu, Player player, int page) {
        var file = category.gui.apply(configs).get();
        var template = file.getConfigurationSection(category.guiSection);
        var icons = template == null ? null : template.getConfigurationSection("icons");
        List<MenuEntry> entries = new ArrayList<>();
        if (icons != null) {
            String equipped = module.equipped(player, category).orElse(null);
            for (String id : icons.getKeys(false)) {
                // Unknown or out-of-season cosmetics are skipped so they leave no gap in the page.
                module.messageCosmetics(category).find(id).ifPresent(message -> {
                    Status status = id.equals(equipped) ? Status.SELECTED
                            : player.hasPermission(category.permission + id) ? Status.AVAILABLE
                            : Status.LOCKED;
                    entry(menu, player, page, template, icons.getString(id), message, status).ifPresent(entries::add);
                });
            }
        }
        loader.page(menu, file, player, entries, page, newPage -> renderPage(menu, player, newPage));
    }

    private Optional<MenuEntry> entry(Menu menu, Player player, int page, ConfigurationSection template,
                                      String material, MessageCosmetic message, Status status) {
        TagResolver resolvers = Placeholder.parsed("cosmetic", message.name());
        var preview = LoreWrapper.wrap(preview(player, message), template.getInt("preview_width", DEFAULT_PREVIEW_WIDTH));
        return loader.item(template, material, player, resolvers).map(item -> {
            var lore = new ArrayList<Component>();
            for (String line : template.getStringList("lore")) {
                // A line with <preview> is repeated once per wrapped preview line, keeping its indentation and colors.
                if (line.contains("<" + PREVIEW + ">")) {
                    preview.forEach(part -> lore.addAll(loader.lore(player, List.of(line),
                            resolvers, Placeholder.component(PREVIEW, part))));
                } else {
                    lore.addAll(loader.lore(player, List.of(line), resolvers));
                }
            }
            lore.addAll(loader.lore(player, template.getStringList(status.lore), resolvers));
            item.editMeta(meta -> {
                meta.lore(lore);
                if (status == Status.SELECTED) {
                    meta.setEnchantmentGlintOverride(true);
                }
            });
            return new MenuEntry(item, event -> click(menu, player, page, template, message, status));
        });
    }

    private Component preview(Player player, MessageCosmetic message) {
        return message.format().isBlank()
                ? messages.get(category.emptyPreviewMessage)
                : preview.apply(player, message.format());
    }

    private void click(Menu menu, Player player, int page, ConfigurationSection template, MessageCosmetic message, Status status) {
        if (!module.isEnabled()) {
            player.closeInventory();
            return;
        }
        var name = Placeholder.parsed("cosmetic", message.name());
        switch (status) {
            case SELECTED -> loader.run(player, template.getStringList("selected_commands"));
            case LOCKED -> {
                messages.send(player, category.lockedMessage, name);
                loader.run(player, template.getStringList("locked_commands"));
            }
            case AVAILABLE -> module.selections()
                    .select(player.getUniqueId(), category.slot, message.id())
                    .whenComplete((ignored, e) -> player.getScheduler().run(plugin, task -> {
                        if (e != null) {
                            messages.send(player, Keys.Message.COSMETICS_UNAVAILABLE);
                            return;
                        }
                        messages.send(player, category.selectedMessage, name);
                        loader.run(player, template.getStringList("select_commands"));
                        // Redraws the page so the glow and the selected lore move to the new choice.
                        if (player.getOpenInventory().getTopInventory().getHolder(false) == menu) {
                            renderPage(menu, player, page);
                        }
                    }, null));
        }
    }

    private enum Status {
        AVAILABLE("available_lore"),
        SELECTED("selected_lore"),
        LOCKED("locked_lore");

        private final String lore;

        Status(String lore) {
            this.lore = lore;
        }
    }
}
