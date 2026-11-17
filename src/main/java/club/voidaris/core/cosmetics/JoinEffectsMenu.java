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
 * guis/join-effects.yml: the static items plus one paginated icon per join effect under join_effects.icons.
 */
final class JoinEffectsMenu {

    private static final String SECTION = "join_effects";

    private final JavaPlugin plugin;
    private final ConfigManager configs;
    private final CosmeticsModule module;
    private final Messages messages;
    private final MenuLoader loader;

    JoinEffectsMenu(JavaPlugin plugin, ConfigManager configs, CosmeticsModule module, Messages messages, MenuLoader loader) {
        this.plugin = plugin;
        this.configs = configs;
        this.module = module;
        this.messages = messages;
        this.loader = loader;
    }

    void open(Player player) {
        var menu = loader.load(configs.joinEffectsGui().get(), player);
        renderPage(menu, player, 0);
        player.openInventory(menu.getInventory());
    }

    private void renderPage(Menu menu, Player player, int page) {
        var file = configs.joinEffectsGui().get();
        var template = file.getConfigurationSection(SECTION);
        var icons = template == null ? null : template.getConfigurationSection("icons");
        List<MenuEntry> entries = new ArrayList<>();
        if (icons != null) {
            String equipped = module.equippedJoinEffect(player).orElse(null);
            for (String id : icons.getKeys(false)) {
                // Unknown or out-of-season effects are skipped so they leave no gap in the page.
                module.joinEffects().find(id).ifPresent(effect -> {
                    Status status = id.equals(equipped) ? Status.SELECTED
                            : player.hasPermission(Keys.Permission.COSMETICS_JOIN_EFFECT + id) ? Status.AVAILABLE
                            : Status.LOCKED;
                    entry(menu, player, page, template, icons.getString(id), effect, status).ifPresent(entries::add);
                });
            }
        }
        loader.page(menu, file, player, entries, page, newPage -> renderPage(menu, player, newPage));
    }

    private Optional<MenuEntry> entry(Menu menu, Player player, int page, ConfigurationSection template,
                                      String material, JoinEffect effect, Status status) {
        TagResolver resolvers = Placeholder.parsed("cosmetic", effect.name());
        return loader.item(template, material, player, resolvers).map(item -> {
            var lore = new ArrayList<Component>(loader.lore(player, template.getStringList("lore"), resolvers));
            if (effect.premium()) {
                lore.addAll(loader.lore(player, template.getStringList("premium_lore"), resolvers));
            }
            lore.addAll(loader.lore(player, template.getStringList(status.lore), resolvers));
            item.editMeta(meta -> {
                meta.lore(lore);
                if (status == Status.SELECTED) {
                    meta.setEnchantmentGlintOverride(true);
                }
            });
            return new MenuEntry(item, event -> click(menu, player, page, template, effect, status));
        });
    }

    private void click(Menu menu, Player player, int page, ConfigurationSection template, JoinEffect effect, Status status) {
        if (!module.isEnabled()) {
            player.closeInventory();
            return;
        }
        var name = Placeholder.parsed("cosmetic", effect.name());
        switch (status) {
            case SELECTED -> loader.run(player, template.getStringList("selected_commands"));
            case LOCKED -> {
                messages.send(player, Keys.Message.COSMETICS_JOIN_EFFECT_LOCKED, name);
                loader.run(player, template.getStringList("locked_commands"));
            }
            case AVAILABLE -> module.selections()
                    .select(player.getUniqueId(), CosmeticsModule.JOIN_EFFECT_SLOT, effect.id())
                    .whenComplete((ignored, e) -> player.getScheduler().run(plugin, task -> {
                        if (e != null) {
                            messages.send(player, Keys.Message.COSMETICS_UNAVAILABLE);
                            return;
                        }
                        messages.send(player, Keys.Message.COSMETICS_JOIN_EFFECT_SELECTED, name);
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
