package club.voidaris.core.cosmetics;

import club.voidaris.core.Keys;
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

/**
 * guis/badges.yml: one icon per badge section (each opening guis/badge-list.yml with its own icons) followed by the icon
 * of the player's custom badge.
 */
final class BadgesMenu {

    private final JavaPlugin plugin;
    private final ConfigManager configs;
    private final CosmeticsModule module;
    private final Messages messages;
    private final MenuLoader loader;
    private final MessageCosmeticsMenu listMenu;

    BadgesMenu(JavaPlugin plugin, ConfigManager configs, CosmeticsModule module, Messages messages, MenuLoader loader,
             MessageCosmeticsMenu listMenu) {
        this.plugin = plugin;
        this.configs = configs;
        this.module = module;
        this.messages = messages;
        this.loader = loader;
        this.listMenu = listMenu;
    }

    void open(Player player) {
        var menu = loader.load(configs.badgesGui().get(), player);
        render(menu, player, 0);
        player.openInventory(menu.getInventory());
    }

    private void render(Menu menu, Player player, int page) {
        var file = configs.badgesGui().get();
        List<MenuEntry> entries = new ArrayList<>();
        var sections = file.getConfigurationSection("sections");
        if (sections != null) {
            for (String id : sections.getKeys(false)) {
                var section = sections.getConfigurationSection(id);
                if (section != null) {
                    sectionEntry(player, section).ifPresent(entries::add);
                }
            }
        }
        var custom = file.getConfigurationSection("custom");
        if (custom != null) {
            customEntry(menu, player, custom).ifPresent(entries::add);
        }
        loader.page(menu, file, player, entries, page, newPage -> render(menu, player, newPage));
    }

    private Optional<MenuEntry> sectionEntry(Player player, ConfigurationSection section) {
        var title = Placeholder.parsed("section", section.getString("title", section.getName()));
        return loader.item(section, section.getString("material"), player).map(item -> new MenuEntry(item, event -> {
            if (!module.isEnabled()) {
                player.closeInventory();
                return;
            }
            loader.run(player, section.getStringList("click_commands"));
            listMenu.open(player, section.getConfigurationSection("icons"), title);
        }));
    }

    private Optional<MenuEntry> customEntry(Menu menu, Player player, ConfigurationSection custom) {
        var text = module.customBadge(player);
        Status status = !player.hasPermission(Keys.Permission.COSMETICS_CUSTOM_BADGE) ? Status.LOCKED
                : text.isEmpty() ? Status.EMPTY
                : module.customBadgeEquipped(player) ? Status.SELECTED
                : Status.AVAILABLE;
        TagResolver resolvers = TagResolver.resolver(
                Placeholder.component("custom_badge", text.map(t -> module.withSeparator(CosmeticsModule.customBadgeContent(t)))
                        .orElse(Component.empty())),
                Placeholder.unparsed("max", String.valueOf(module.customBadgeMaxLength())));
        return loader.item(custom, custom.getString("material"), player, resolvers).map(item -> {
            var lore = new ArrayList<Component>(loader.lore(player, custom.getStringList("lore"), resolvers));
            lore.addAll(loader.lore(player, custom.getStringList(status.lore), resolvers));
            item.editMeta(meta -> {
                meta.lore(lore);
                if (status == Status.SELECTED) {
                    meta.setEnchantmentGlintOverride(true);
                }
            });
            return new MenuEntry(item, event -> click(menu, player, custom, status, resolvers));
        });
    }

    private void click(Menu menu, Player player, ConfigurationSection custom, Status status, TagResolver resolvers) {
        if (!module.isEnabled()) {
            player.closeInventory();
            return;
        }
        switch (status) {
            case LOCKED -> {
                messages.send(player, Keys.Message.COSMETICS_BADGE_CUSTOM_LOCKED);
                loader.run(player, custom.getStringList("locked_commands"));
            }
            // Writing the badge needs the chat box, so the menu closes and the hint suggests the command.
            case EMPTY, SELECTED -> {
                player.closeInventory();
                messages.send(player, Keys.Message.COSMETICS_BADGE_CUSTOM_HINT, resolvers);
                loader.run(player, custom.getStringList("click_commands"));
            }
            case AVAILABLE -> module.selections()
                    .select(player.getUniqueId(), MessageCategory.BADGE.slot, CosmeticsModule.CUSTOM_BADGE_ID)
                    .whenComplete((ignored, e) -> player.getScheduler().run(plugin, task -> {
                        if (e != null) {
                            messages.send(player, Keys.Message.COSMETICS_UNAVAILABLE);
                            return;
                        }
                        messages.send(player, Keys.Message.COSMETICS_BADGE_CUSTOM_SELECTED, resolvers);
                        loader.run(player, custom.getStringList("select_commands"));
                        if (player.getOpenInventory().getTopInventory().getHolder(false) == menu) {
                            render(menu, player, 0);
                        }
                    }, null));
        }
    }

    private enum Status {
        /** Has the permission but never wrote a badge. */
        EMPTY("empty_lore"),
        AVAILABLE("available_lore"),
        SELECTED("selected_lore"),
        LOCKED("locked_lore");

        private final String lore;

        Status(String lore) {
            this.lore = lore;
        }
    }
}
