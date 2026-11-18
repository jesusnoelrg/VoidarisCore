package club.voidaris.core.cosmetics;

import club.voidaris.core.Keys;
import club.voidaris.core.config.ConfigFile;
import club.voidaris.core.config.ConfigManager;
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
import java.util.function.Function;

/**
 * A menu in guis/ with its static items plus one paginated icon per cosmetic under {@code <section>.icons}.
 */
final class CosmeticMenu<T extends Cosmetic> {

    /**
     * What differs between the cosmetic types that share this menu.
     *
     * @param file            the menu file in guis/
     * @param section         the section of the file with the icon template and icons
     * @param slot            the selection slot the choice is stored in
     * @param permission      permission prefix; the cosmetic id completes it
     * @param lockedMessage   message key sent when clicking a cosmetic the player does not own
     * @param selectedMessage message key sent after equipping a cosmetic
     * @param find            the cosmetic with the given id, empty when unknown or out of season
     * @param equipped        the id of the cosmetic the player has equipped
     */
    record Type<T extends Cosmetic>(Function<ConfigManager, ConfigFile> file, String section, String slot,
                                    String permission, String lockedMessage, String selectedMessage,
                                    Function<String, Optional<T>> find, Function<Player, Optional<String>> equipped) {
    }

    private final JavaPlugin plugin;
    private final ConfigManager configs;
    private final CosmeticsModule module;
    private final Messages messages;
    private final MenuLoader loader;
    private final Type<T> type;

    CosmeticMenu(JavaPlugin plugin, ConfigManager configs, CosmeticsModule module, Messages messages, MenuLoader loader,
                 Type<T> type) {
        this.plugin = plugin;
        this.configs = configs;
        this.module = module;
        this.messages = messages;
        this.loader = loader;
        this.type = type;
    }

    void open(Player player) {
        var menu = loader.load(type.file().apply(configs).get(), player);
        renderPage(menu, player, 0);
        player.openInventory(menu.getInventory());
    }

    private void renderPage(Menu menu, Player player, int page) {
        var file = type.file().apply(configs).get();
        var template = file.getConfigurationSection(type.section());
        var icons = template == null ? null : template.getConfigurationSection("icons");
        List<MenuEntry> entries = new ArrayList<>();
        if (icons != null) {
            String equipped = type.equipped().apply(player).orElse(null);
            for (String id : icons.getKeys(false)) {
                // Unknown or out-of-season cosmetics are skipped so they leave no gap in the page.
                type.find().apply(id).ifPresent(cosmetic -> {
                    Status status = id.equals(equipped) ? Status.SELECTED
                            : player.hasPermission(type.permission() + id) ? Status.AVAILABLE
                            : Status.LOCKED;
                    entry(menu, player, page, template, icons.getString(id), cosmetic, status).ifPresent(entries::add);
                });
            }
        }
        loader.page(menu, file, player, entries, page, newPage -> renderPage(menu, player, newPage));
    }

    private Optional<MenuEntry> entry(Menu menu, Player player, int page, ConfigurationSection template,
                                      String material, T cosmetic, Status status) {
        TagResolver resolvers = Placeholder.parsed("cosmetic", cosmetic.name());
        return loader.item(template, material, player, resolvers).map(item -> {
            var lore = new ArrayList<Component>(loader.lore(player, template.getStringList("lore"), resolvers));
            if (cosmetic.premium()) {
                lore.addAll(loader.lore(player, template.getStringList("premium_lore"), resolvers));
            }
            lore.addAll(loader.lore(player, template.getStringList(status.lore), resolvers));
            item.editMeta(meta -> {
                meta.lore(lore);
                if (status == Status.SELECTED) {
                    meta.setEnchantmentGlintOverride(true);
                }
            });
            return new MenuEntry(item, event -> click(menu, player, page, template, cosmetic, status));
        });
    }

    private void click(Menu menu, Player player, int page, ConfigurationSection template, T cosmetic, Status status) {
        if (!module.isEnabled()) {
            player.closeInventory();
            return;
        }
        var name = Placeholder.parsed("cosmetic", cosmetic.name());
        switch (status) {
            case SELECTED -> loader.run(player, template.getStringList("selected_commands"));
            case LOCKED -> {
                messages.send(player, type.lockedMessage(), name);
                loader.run(player, template.getStringList("locked_commands"));
            }
            case AVAILABLE -> module.selections()
                    .select(player.getUniqueId(), type.slot(), cosmetic.id())
                    .whenComplete((ignored, e) -> player.getScheduler().run(plugin, task -> {
                        if (e != null) {
                            messages.send(player, Keys.Message.COSMETICS_UNAVAILABLE);
                            return;
                        }
                        messages.send(player, type.selectedMessage(), name);
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
