package club.voidaris.core.cosmetics;

import club.voidaris.core.Keys;
import club.voidaris.core.config.ConfigManager;
import club.voidaris.core.gui.Menu;
import club.voidaris.core.gui.MenuEntry;
import club.voidaris.core.gui.MenuLoader;
import club.voidaris.core.message.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * guis/chat-colors.yml: the static items plus one paginated icon per chat color under chat_colors.icons, followed by
 * the custom hex color icon.
 */
final class ChatColorsMenu {

    private static final String SECTION = "chat_colors";
    private static final String CUSTOM = "custom";
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final JavaPlugin plugin;
    private final ConfigManager configs;
    private final CosmeticsModule module;
    private final Messages messages;
    private final MenuLoader loader;

    ChatColorsMenu(JavaPlugin plugin, ConfigManager configs, CosmeticsModule module, Messages messages, MenuLoader loader) {
        this.plugin = plugin;
        this.configs = configs;
        this.module = module;
        this.messages = messages;
        this.loader = loader;
    }

    void open(Player player) {
        var menu = loader.load(configs.chatColorsGui().get(), player);
        renderPage(menu, player, 0);
        player.openInventory(menu.getInventory());
    }

    private void renderPage(Menu menu, Player player, int page) {
        var file = configs.chatColorsGui().get();
        var template = file.getConfigurationSection(SECTION);
        List<MenuEntry> entries = new ArrayList<>();
        if (template != null) {
            String equipped = module.equippedChatColor(player).orElse(null);
            var icons = template.getConfigurationSection("icons");
            if (icons != null) {
                for (String id : icons.getKeys(false)) {
                    // Unknown colors are skipped so they leave no gap in the page.
                    module.chatColors().find(id).ifPresent(color -> {
                        Status status = id.equals(equipped) ? Status.SELECTED
                                : player.hasPermission(Keys.Permission.COSMETICS_CHAT_COLOR + id) ? Status.AVAILABLE
                                : Status.LOCKED;
                        entry(menu, player, page, template, icons.getString(id), color, status).ifPresent(entries::add);
                    });
                }
            }
            customEntry(player, template, equipped).ifPresent(entries::add);
        }
        loader.page(menu, file, player, entries, page, newPage -> renderPage(menu, player, newPage));
    }

    private Optional<MenuEntry> entry(Menu menu, Player player, int page, ConfigurationSection template,
                                      String material, ChatColor color, Status status) {
        TagResolver resolvers = TagResolver.resolver(Placeholder.parsed("cosmetic", color.name()),
                Placeholder.component("preview", preview(color.color())));
        return loader.item(template, material, player, resolvers).map(item -> {
            var lore = new ArrayList<Component>(loader.lore(player, template.getStringList("lore"), resolvers));
            if (color.premium()) {
                lore.addAll(loader.lore(player, template.getStringList("premium_lore"), resolvers));
            }
            lore.addAll(loader.lore(player, template.getStringList(status.lore), resolvers));
            item.editMeta(meta -> {
                meta.lore(lore);
                if (status == Status.SELECTED) {
                    meta.setEnchantmentGlintOverride(true);
                }
            });
            return new MenuEntry(item, event -> click(menu, player, page, template, color, status));
        });
    }

    private Optional<MenuEntry> customEntry(Player player, ConfigurationSection template, String equipped) {
        var custom = template.getConfigurationSection(CUSTOM);
        if (custom == null) {
            return Optional.empty();
        }
        boolean selected = equipped != null && ChatColors.isCustom(equipped);
        Status status = selected ? Status.SELECTED
                : player.hasPermission(Keys.Permission.COSMETICS_CUSTOM_CHAT_COLOR) ? Status.AVAILABLE
                : Status.LOCKED;
        TagResolver resolvers = Placeholder.component("color",
                selected ? MINI_MESSAGE.deserialize("<" + equipped + ">" + equipped) : Component.empty());
        return loader.item(custom, custom.getString("material"), player, resolvers).map(item -> {
            var lore = new ArrayList<Component>(loader.lore(player, custom.getStringList("lore"), resolvers));
            lore.addAll(loader.lore(player, custom.getStringList(status.lore), resolvers));
            item.editMeta(meta -> {
                meta.lore(lore);
                if (selected) {
                    meta.setEnchantmentGlintOverride(true);
                }
            });
            return new MenuEntry(item, event -> {
                if (!module.isEnabled()) {
                    player.closeInventory();
                    return;
                }
                if (status == Status.LOCKED) {
                    messages.send(player, Keys.Message.COSMETICS_CHAT_COLOR_CUSTOM_LOCKED);
                    loader.run(player, template.getStringList("locked_commands"));
                    return;
                }
                // Typing the color needs the chat box, so the menu closes and the hint suggests the command.
                player.closeInventory();
                messages.send(player, Keys.Message.COSMETICS_CHAT_COLOR_CUSTOM_HINT);
                loader.run(player, custom.getStringList("click_commands"));
            });
        });
    }

    private Component preview(String color) {
        return MINI_MESSAGE.deserialize(color + MINI_MESSAGE.escapeTags(messages.raw(Keys.Message.COSMETICS_CHAT_COLOR_PREVIEW)));
    }

    private void click(Menu menu, Player player, int page, ConfigurationSection template, ChatColor color, Status status) {
        if (!module.isEnabled()) {
            player.closeInventory();
            return;
        }
        var name = Placeholder.parsed("cosmetic", color.name());
        switch (status) {
            case SELECTED -> loader.run(player, template.getStringList("selected_commands"));
            case LOCKED -> {
                messages.send(player, Keys.Message.COSMETICS_CHAT_COLOR_LOCKED, name);
                loader.run(player, template.getStringList("locked_commands"));
            }
            case AVAILABLE -> module.selections()
                    .select(player.getUniqueId(), CosmeticsModule.CHAT_COLOR_SLOT, color.id())
                    .whenComplete((ignored, e) -> player.getScheduler().run(plugin, task -> {
                        if (e != null) {
                            messages.send(player, Keys.Message.COSMETICS_UNAVAILABLE);
                            return;
                        }
                        messages.send(player, Keys.Message.COSMETICS_CHAT_COLOR_SELECTED, name);
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
