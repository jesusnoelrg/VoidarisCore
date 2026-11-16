package club.voidaris.core.kits;

import club.voidaris.core.Keys;
import club.voidaris.core.config.ConfigManager;
import club.voidaris.core.gui.Menu;
import club.voidaris.core.gui.MenuEntry;
import club.voidaris.core.gui.MenuLoader;
import club.voidaris.core.message.Messages;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.BiConsumer;

/**
 * guis/kits.yml: the static items plus one paginated icon per kit, drawn with the template of its status and redrawn
 * every {@code refresh_ticks} while the menu stays open, so cooldowns count down live. A right click on an icon opens
 * guis/kit-preview.yml with the kit's items.
 */
final class KitsMenu {

    private static final String SECTION = "kits";

    private final JavaPlugin plugin;
    private final ConfigManager configs;
    private final Messages messages;
    private final MenuLoader loader;
    private final KitsModule module;

    KitsMenu(JavaPlugin plugin, ConfigManager configs, Messages messages, MenuLoader loader, KitsModule module) {
        this.plugin = plugin;
        this.configs = configs;
        this.messages = messages;
        this.loader = loader;
        this.module = module;
    }

    /**
     * @param onClaim called when an icon is clicked; the menu is redrawn right after it
     */
    void open(Player viewer, BiConsumer<Player, Kit> onClaim) {
        var file = configs.kitsGui().get();
        var view = new View(viewer, file, loader.load(file, viewer), onClaim);
        view.render();
        viewer.openInventory(view.menu.getInventory());
        long ticks = Math.max(1, file.getLong("refresh_ticks", 20));
        view.task = plugin.getServer().getScheduler().runTaskTimer(plugin, view::refresh, ticks, ticks);
    }

    /**
     * Shows copies of the kit's items in slot order (hotbar, inventory, armor, offhand) with all their data, so players
     * can read enchantments and lore; menu clicks are cancelled, so nothing can be taken out.
     */
    void openPreview(Player viewer, Kit kit) {
        var file = configs.kitPreviewGui().get();
        var menu = loader.load(file, viewer, kit.placeholders());
        List<MenuEntry> entries = new TreeMap<>(kit.items()).values().stream()
                .map(item -> new MenuEntry(item.clone(), null))
                .toList();
        renderPreview(menu, file, viewer, entries, 0);
        viewer.openInventory(menu.getInventory());
    }

    private void renderPreview(Menu menu, FileConfiguration file, Player viewer, List<MenuEntry> entries, int page) {
        loader.page(menu, file, viewer, entries, page, newPage -> renderPreview(menu, file, viewer, entries, newPage));
    }

    private final class View {

        private final Player viewer;
        private final FileConfiguration file;
        private final Menu menu;
        private final BiConsumer<Player, Kit> onClaim;
        private int page;
        private BukkitTask task;

        private View(Player viewer, FileConfiguration file, Menu menu, BiConsumer<Player, Kit> onClaim) {
            this.viewer = viewer;
            this.file = file;
            this.menu = menu;
            this.onClaim = onClaim;
        }

        private void refresh() {
            boolean open = viewer.isOnline() && viewer.getOpenInventory().getTopInventory().getHolder(false) == menu;
            if (!open || !module.isEnabled()) {
                task.cancel();
                return;
            }
            render();
        }

        private void render() {
            loader.page(menu, file, viewer, entries(), page, newPage -> {
                page = newPage;
                render();
            });
        }

        private List<MenuEntry> entries() {
            var template = file.getConfigurationSection(SECTION);
            List<MenuEntry> entries = new ArrayList<>();
            if (template == null || !module.isEnabled()) {
                return entries;
            }
            boolean showLocked = module.showLocked();
            for (Kit kit : module.storage().kits()) {
                var status = module.status(viewer, kit);
                if (status == KitsModule.Status.LOCKED && !showLocked) {
                    continue;
                }
                entry(template, kit, status).ifPresent(entries::add);
            }
            return entries;
        }

        private Optional<MenuEntry> entry(ConfigurationSection template, Kit kit, KitsModule.Status status) {
            var look = template.getConfigurationSection(status.name().toLowerCase(Locale.ROOT));
            if (look == null) {
                return Optional.empty();
            }
            long remaining = module.remaining(viewer, kit);
            TagResolver resolvers = TagResolver.resolver(
                    kit.placeholders(),
                    Placeholder.component("cooldown", KitTime.describe(messages, kit)),
                    Placeholder.component("time", KitTime.format(messages, remaining == Long.MAX_VALUE ? 0 : remaining)),
                    Placeholder.unparsed("items", String.valueOf(kit.items().size())));
            String defaultIcon = template.getString("default_icon");
            // An icon item removed in a later version falls back to the default one instead of hiding the kit.
            var item = kit.icon() == null ? Optional.<ItemStack>empty() : loader.item(look, kit.icon(), viewer, resolvers);
            return item.or(() -> loader.item(look, defaultIcon, viewer, resolvers))
                    .map(icon -> new MenuEntry(icon, event -> {
                        if (event.isRightClick()) {
                            loader.run(viewer, template.getStringList("preview_click_commands"));
                            module.storage().find(kit.name()).ifPresentOrElse(
                                    current -> openPreview(viewer, current),
                                    () -> messages.send(viewer, Keys.Message.KITS_NOT_FOUND,
                                            Placeholder.unparsed("kit", kit.name())));
                            return;
                        }
                        loader.run(viewer, look.getStringList("click_commands"));
                        // Looked up again because the kit may have been edited or deleted while the menu was open.
                        module.storage().find(kit.name()).ifPresentOrElse(
                                current -> onClaim.accept(viewer, current),
                                () -> messages.send(viewer, Keys.Message.KITS_NOT_FOUND,
                                        Placeholder.unparsed("kit", kit.name())));
                        if (viewer.getOpenInventory().getTopInventory().getHolder(false) == menu) {
                            render();
                        }
                    }));
        }
    }
}
