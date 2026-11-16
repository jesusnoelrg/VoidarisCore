package club.voidaris.core.homes;

import club.voidaris.core.config.ConfigManager;
import club.voidaris.core.gui.Menu;
import club.voidaris.core.gui.MenuEntry;
import club.voidaris.core.gui.MenuLoader;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * guis/homes.yml: the static items plus one paginated icon per home, which teleports there when clicked.
 */
final class HomesMenu {

    private static final String SECTION = "homes";

    private final ConfigManager configs;
    private final MenuLoader loader;

    HomesMenu(ConfigManager configs, MenuLoader loader) {
        this.configs = configs;
        this.loader = loader;
    }

    /**
     * @param ownerName shown as {@code <player>} in the icons
     */
    void open(Player viewer, String ownerName, Collection<Home> homes, Consumer<Home> onSelect) {
        var file = configs.homesGui().get();
        var menu = loader.load(file, viewer);
        var template = file.getConfigurationSection(SECTION);
        List<MenuEntry> entries = new ArrayList<>();
        if (template != null) {
            for (Home home : homes) {
                entry(viewer, template, ownerName, home, onSelect).ifPresent(entries::add);
            }
        }
        renderPage(menu, viewer, entries, 0);
        viewer.openInventory(menu.getInventory());
    }

    private void renderPage(Menu menu, Player viewer, List<MenuEntry> entries, int page) {
        loader.page(menu, configs.homesGui().get(), viewer, entries, page,
                newPage -> renderPage(menu, viewer, entries, newPage));
    }

    private Optional<MenuEntry> entry(Player viewer, ConfigurationSection template, String ownerName, Home home,
                                      Consumer<Home> onSelect) {
        var location = home.location();
        TagResolver resolvers = TagResolver.resolver(
                Placeholder.unparsed("home", home.name()),
                Placeholder.unparsed("player", ownerName),
                Placeholder.unparsed("world", location.world()),
                Placeholder.unparsed("x", String.valueOf((long) Math.floor(location.x()))),
                Placeholder.unparsed("y", String.valueOf((long) Math.floor(location.y()))),
                Placeholder.unparsed("z", String.valueOf((long) Math.floor(location.z()))));
        String defaultIcon = template.getString("default_icon");
        // An icon item removed in a later version falls back to the default one instead of hiding the home.
        var item = home.icon() == null ? Optional.<ItemStack>empty()
                : loader.item(template, home.icon(), viewer, resolvers);
        return item.or(() -> loader.item(template, defaultIcon, viewer, resolvers))
                .map(icon -> new MenuEntry(icon, event -> {
                    loader.run(viewer, template.getStringList("click_commands"));
                    onSelect.accept(home);
                }));
    }
}
