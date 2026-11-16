package club.voidaris.core.gui;

import club.voidaris.core.message.PlayerFormatter;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.IntConsumer;
import java.util.logging.Logger;

/**
 * Builds menus from files in guis/, which follow the DeluxeMenus layout with MiniMessage text.
 */
public final class MenuLoader {

    private static final String BASEHEAD = "basehead-";
    private static final int DEFAULT_SIZE = 54;

    private final PlayerFormatter formatter;
    private final Logger logger;

    public MenuLoader(PlayerFormatter formatter, Logger logger) {
        this.formatter = formatter;
        this.logger = logger;
    }

    /**
     * Creates the menu with its title, size and the static entries under items.
     *
     * @param resolvers placeholders available in the title and the static items
     */
    public Menu load(ConfigurationSection file, Player player, TagResolver... resolvers) {
        int size = file.getInt("size", DEFAULT_SIZE);
        // Inventories only accept full rows of 9, up to 6 rows.
        if (size <= 0 || size > 54 || size % 9 != 0) {
            logger.warning("Invalid menu size " + size + " in " + file.getName() + "; using " + DEFAULT_SIZE + ".");
            size = DEFAULT_SIZE;
        }
        var menu = new Menu(formatter.format(player, file.getString("menu_title", ""), resolvers), size);
        var items = file.getConfigurationSection("items");
        if (items != null) {
            for (String name : items.getKeys(false)) {
                var section = items.getConfigurationSection(name);
                if (section == null) {
                    continue;
                }
                item(section, section.getString("material"), player, resolvers).ifPresent(item -> {
                    for (int slot : slots(section)) {
                        menu.set(slot, item, event -> run(player, section.getStringList(
                                event.isRightClick() ? "right_click_commands" : "left_click_commands")));
                    }
                });
            }
        }
        return menu;
    }

    /**
     * Builds the item with the display_name, lore and glow of the section, using material instead of the section's one.
     *
     * @return empty when the material is invalid (a warning is logged)
     */
    public Optional<ItemStack> item(ConfigurationSection section, String material, Player player, TagResolver... resolvers) {
        var item = createItem(section, material);
        if (item == null) {
            return Optional.empty();
        }
        var meta = item.getItemMeta();
        if (section.isSet("display_name")) {
            meta.customName(text(player, section.getString("display_name"), resolvers));
        }
        var lore = lore(player, section.getStringList("lore"), resolvers);
        if (!lore.isEmpty()) {
            meta.lore(lore);
        }
        if (section.getBoolean("glow")) {
            meta.setEnchantmentGlintOverride(true);
        }
        item.setItemMeta(meta);
        return Optional.of(item);
    }

    /**
     * Fills content_slots with the given page of entries and draws the previous/next buttons of pagination,
     * or its disabled item where there is no page to go to.
     */
    public void page(Menu menu, ConfigurationSection file, Player player, List<MenuEntry> entries, int page,
                     IntConsumer openPage) {
        var slots = file.getIntegerList("content_slots");
        if (slots.isEmpty()) {
            logger.warning("Menu " + file.getName() + " has no content_slots.");
            return;
        }
        int pages = Math.max(1, Math.ceilDiv(entries.size(), slots.size()));
        int current = Math.clamp(page, 0, pages - 1);
        for (int i = 0; i < slots.size(); i++) {
            int index = current * slots.size() + i;
            var entry = index < entries.size() ? entries.get(index) : null;
            menu.set(slots.get(i), entry == null ? null : entry.item(), entry == null ? null : entry.handler());
        }
        var pagination = file.getConfigurationSection("pagination");
        if (pagination == null) {
            return;
        }
        TagResolver resolvers = TagResolver.resolver(
                Placeholder.unparsed("page", String.valueOf(current + 1)),
                Placeholder.unparsed("pages", String.valueOf(pages)));
        pageButton(menu, pagination, "previous", player, resolvers, current > 0, () -> openPage.accept(current - 1));
        pageButton(menu, pagination, "next", player, resolvers, current < pages - 1, () -> openPage.accept(current + 1));
    }

    private void pageButton(Menu menu, ConfigurationSection pagination, String name, Player player, TagResolver resolvers,
                            boolean enabled, Runnable openPage) {
        var button = pagination.getConfigurationSection(name);
        var look = enabled ? button : pagination.getConfigurationSection("disabled");
        if (button == null || look == null) {
            return;
        }
        item(look, look.getString("material"), player, resolvers).ifPresent(item -> menu.set(button.getInt("slot"), item,
                enabled ? event -> {
                    run(player, button.getStringList("click_commands"));
                    openPage.run();
                } : null));
    }

    public List<Component> lore(Player player, List<String> lines, TagResolver... resolvers) {
        var lore = new ArrayList<Component>(lines.size());
        for (String line : lines) {
            lore.add(text(player, line, resolvers));
        }
        return lore;
    }

    public List<Integer> slots(ConfigurationSection section) {
        return section.isList("slots") ? section.getIntegerList("slots") : List.of(section.getInt("slot"));
    }

    /**
     * Runs DeluxeMenus actions: [player], [console], [message], [sound] and [close].
     */
    public void run(Player player, List<String> actions) {
        for (String action : actions) {
            int end = action.indexOf(']');
            if (!action.startsWith("[") || end < 0) {
                logger.warning("Invalid menu action (expected [type]value): " + action);
                continue;
            }
            String type = action.substring(1, end).toLowerCase(Locale.ROOT);
            String value = action.substring(end + 1).strip();
            switch (type) {
                // Sent as chat so PlayerCommandPreprocessEvent fires: DeluxeMenus opens menus without register_command
                // from that event, and performCommand skips it.
                case "player" -> player.chat("/" + value);
                case "console" -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), value.replace("<player>", player.getName()));
                case "message" -> player.sendMessage(formatter.format(player, value));
                case "sound" -> sound(value).ifPresentOrElse(sound -> player.playSound(player, sound, 1, 1),
                        () -> logger.warning("Unknown sound in menu action: " + value));
                case "close" -> player.closeInventory();
                default -> logger.warning("Unknown menu action type: " + action);
            }
        }
    }

    private Component text(Player player, String raw, TagResolver... resolvers) {
        // Item names and lore are italic by default; an explicit <italic> in the text still applies.
        return formatter.format(player, raw, resolvers).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    private ItemStack createItem(ConfigurationSection section, String material) {
        if (material != null && material.startsWith(BASEHEAD)) {
            var item = new ItemStack(Material.PLAYER_HEAD);
            var meta = (SkullMeta) item.getItemMeta();
            // A random id gives a profile with no owner, so the client renders the texture value as is.
            var profile = Bukkit.createProfile(UUID.randomUUID());
            profile.setProperty(new ProfileProperty("textures", material.substring(BASEHEAD.length())));
            meta.setPlayerProfile(profile);
            item.setItemMeta(meta);
            return item;
        }
        Material type = material == null ? null : Material.matchMaterial(material);
        if (type == null || !type.isItem() || type.isAir()) {
            logger.warning("Invalid material '" + material + "' in menu item " + section.getCurrentPath());
            return null;
        }
        return new ItemStack(type);
    }

    // Accepts DeluxeMenus enum names (ENTITY_EXPERIENCE_ORB_PICKUP) as well as namespaced keys (entity.experience_orb.pickup).
    private Optional<Sound> sound(String name) {
        var key = NamespacedKey.fromString(name.toLowerCase(Locale.ROOT));
        var sound = key == null ? null : Registry.SOUNDS.get(key);
        if (sound != null) {
            return Optional.of(sound);
        }
        // Enum names only differ from vanilla keys by using _ for the dots.
        String enumName = name.toLowerCase(Locale.ROOT);
        return Registry.SOUNDS.keyStream()
                .filter(soundKey -> soundKey.getKey().replace('.', '_').equals(enumName))
                .findFirst()
                .map(Registry.SOUNDS::get);
    }
}
