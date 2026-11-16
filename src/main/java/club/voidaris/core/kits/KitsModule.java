package club.voidaris.core.kits;

import club.voidaris.core.Keys;
import club.voidaris.core.config.ConfigManager;
import club.voidaris.core.message.Messages;
import club.voidaris.core.module.Module;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public final class KitsModule extends Module {

    /** Size of the storage part (hotbar and main inventory) of a player inventory. */
    private static final int STORAGE_SIZE = 36;

    enum Status {
        AVAILABLE,
        COOLDOWN,
        /** A one-time kit the player already claimed. */
        CLAIMED,
        /** The kit requires a permission the player does not have. */
        LOCKED
    }

    enum GiveResult {
        GIVEN,
        /** Given, but some items did not fit and were dropped at the player's feet. */
        DROPPED,
        /** Not given because some items would not fit. */
        NO_ROOM
    }

    private final JavaPlugin plugin;
    private final ConfigManager configs;
    private final Messages messages;
    private final KitStorage storage;
    private Pattern namePattern;

    public KitsModule(JavaPlugin plugin, ConfigManager configs, Messages messages) {
        super("kits");
        this.plugin = plugin;
        this.configs = configs;
        this.messages = messages;
        this.storage = new KitStorage(plugin);
    }

    @Override
    protected List<Listener> listeners() {
        return List.of(new KitsListener(this, messages));
    }

    @Override
    protected void onEnable() {
        storage.open();
        onReload();
    }

    @Override
    protected void onReload() {
        namePattern = Pattern.compile(configs.kits().get().getString("name-pattern"));
        storage.kits().forEach(KitsModule::registerPermission);
        String firstJoinKit = firstJoinKitName();
        if (!firstJoinKit.isEmpty() && storage.find(firstJoinKit).isEmpty()) {
            plugin.getLogger().warning("first-join-kit '" + firstJoinKit + "' in kits.yml does not exist yet; create it with /createkit "
                    + firstJoinKit + ".");
        }
    }

    @Override
    protected void onDisable() {
        storage.close();
    }

    KitStorage storage() {
        return storage;
    }

    boolean isValidName(String name) {
        return namePattern.matcher(name).matches();
    }

    long defaultCooldown() {
        String value = configs.kits().get().getString("defaults.cooldown");
        var cooldown = value == null ? null : KitTime.parse(value);
        if (cooldown == null || cooldown.isEmpty()) {
            plugin.getLogger().warning("Invalid defaults.cooldown '" + value + "' in kits.yml; new kits have no cooldown.");
            return Kit.NO_COOLDOWN;
        }
        return cooldown.getAsLong();
    }

    boolean defaultPermission() {
        return configs.kits().get().getBoolean("defaults.permission");
    }

    boolean showLocked() {
        return configs.kits().get().getBoolean("show-locked");
    }

    boolean dropWhenFull() {
        return configs.kits().get().getBoolean("drop-when-full");
    }

    Optional<Kit> firstJoinKit() {
        String name = firstJoinKitName();
        return name.isEmpty() ? Optional.empty() : storage.find(name);
    }

    private String firstJoinKitName() {
        String name = configs.kits().get().getString("first-join-kit");
        return name == null ? "" : name.strip();
    }

    boolean canAccess(Permissible permissible, Kit kit) {
        return !kit.permission() || permissible.hasPermission(Keys.Permission.KITS_KIT + kit.name());
    }

    /**
     * @return milliseconds until the player can claim the kit again; {@link Long#MAX_VALUE} for a claimed one-time kit
     */
    long remaining(Player player, Kit kit) {
        if (!kit.oneTime() && kit.cooldownSeconds() <= Kit.NO_COOLDOWN) {
            return 0;
        }
        var last = storage.lastClaim(player.getUniqueId(), kit);
        if (last.isEmpty()) {
            return 0;
        }
        if (kit.oneTime()) {
            return Long.MAX_VALUE;
        }
        long end = last.getAsLong() + TimeUnit.SECONDS.toMillis(kit.cooldownSeconds());
        return Math.max(0, end - System.currentTimeMillis());
    }

    Status status(Player player, Kit kit) {
        if (!canAccess(player, kit)) {
            return Status.LOCKED;
        }
        long remaining = remaining(player, kit);
        if (remaining == 0) {
            return Status.AVAILABLE;
        }
        if (kit.oneTime()) {
            return Status.CLAIMED;
        }
        return player.hasPermission(Keys.Permission.KITS_COOLDOWN_BYPASS) ? Status.AVAILABLE : Status.COOLDOWN;
    }

    /**
     * Puts each item back in the slot it had when the kit was created when that slot is free, and anywhere in the
     * inventory otherwise, so armor is equipped and the hotbar layout is kept on an empty inventory.
     *
     * @param dropOverflow whether items that do not fit are dropped, instead of not giving the kit at all
     */
    GiveResult give(Player player, Kit kit, boolean dropOverflow) {
        var inventory = player.getInventory();
        ItemStack[] contents = Arrays.stream(inventory.getContents())
                .map(item -> item == null ? null : item.clone())
                .toArray(ItemStack[]::new);
        List<ItemStack> unplaced = new ArrayList<>();
        for (Map.Entry<Integer, ItemStack> entry : kit.items().entrySet()) {
            int slot = entry.getKey();
            var item = entry.getValue().clone();
            if (slot >= 0 && slot < contents.length && (contents[slot] == null || contents[slot].isEmpty())) {
                contents[slot] = item;
            } else {
                unplaced.add(item);
            }
        }
        // Stacking is simulated on a detached copy of the storage slots: addItem on the player inventory itself could
        // fill the armor and offhand slots with anything.
        var simulated = Bukkit.createInventory(null, STORAGE_SIZE);
        simulated.setContents(Arrays.copyOf(contents, STORAGE_SIZE));
        var leftovers = simulated.addItem(unplaced.toArray(ItemStack[]::new)).values();
        if (!leftovers.isEmpty() && !dropOverflow) {
            return GiveResult.NO_ROOM;
        }
        System.arraycopy(simulated.getContents(), 0, contents, 0, STORAGE_SIZE);
        inventory.setContents(contents);
        for (ItemStack leftover : leftovers) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
        return leftovers.isEmpty() ? GiveResult.GIVEN : GiveResult.DROPPED;
    }

    // Registered so permission plugins can suggest the node; ops keep access like with any unknown node.
    static void registerPermission(Kit kit) {
        String node = Keys.Permission.KITS_KIT + kit.name();
        var pluginManager = Bukkit.getPluginManager();
        if (pluginManager.getPermission(node) == null) {
            pluginManager.addPermission(new Permission(node, PermissionDefault.OP));
        }
    }
}
