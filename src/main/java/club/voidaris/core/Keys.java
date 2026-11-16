package club.voidaris.core;

public final class Keys {

    private Keys() {
    }

    public static final class Permission {

        private Permission() {
        }

        // core
        public static final String CORE_RELOAD = "voidaris.core.reload";

        // cosmetics
        /** Followed by the cosmetic id under join-messages.cosmetics in cosmetics.yml. */
        public static final String COSMETICS_JOIN_MESSAGE = "voidaris.cosmetics.joinmessage.";
        /** Followed by the cosmetic id under teleport-particles.cosmetics in cosmetics.yml. */
        public static final String COSMETICS_TELEPORT_PARTICLE = "voidaris.cosmetics.teleportparticle.";

        // teleport
        public static final String TELEPORT_BYPASS = "voidaris.teleport.bypass";

        // spawn
        public static final String SPAWN_USE = "voidaris.spawn.use";
        public static final String SPAWN_SET = "voidaris.spawn.set";
        public static final String SPAWN_DELETE = "voidaris.spawn.delete";

        // warps
        public static final String WARPS_USE = "voidaris.warps.use";
        /** Followed by the lowercase warp name; only checked when per-warp-permission is true in warps.yml. */
        public static final String WARPS_USE_WARP = "voidaris.warps.use.";
        public static final String WARPS_SET = "voidaris.warps.set";
        public static final String WARPS_DELETE = "voidaris.warps.delete";

        // motd
        public static final String MOTD_USE = "voidaris.motd.use";

        // utilities
        public static final String UTILITIES_CRAFT = "voidaris.utilities.craft";
        public static final String UTILITIES_ANVIL = "voidaris.utilities.anvil";
        public static final String UTILITIES_GRINDSTONE = "voidaris.utilities.grindstone";
        public static final String UTILITIES_STONECUTTER = "voidaris.utilities.stonecutter";
        public static final String UTILITIES_LOOM = "voidaris.utilities.loom";
        public static final String UTILITIES_CARTOGRAPHY = "voidaris.utilities.cartography";
        public static final String UTILITIES_SMITHING = "voidaris.utilities.smithing";
        public static final String UTILITIES_ENDERCHEST = "voidaris.utilities.enderchest";
        public static final String UTILITIES_ENDERCHEST_OTHERS = "voidaris.utilities.enderchest.others";
        public static final String UTILITIES_GAMEMODE_ALL = "voidaris.utilities.gamemode.*";
        public static final String UTILITIES_GAMEMODE_SURVIVAL = "voidaris.utilities.gamemode.survival";
        public static final String UTILITIES_GAMEMODE_CREATIVE = "voidaris.utilities.gamemode.creative";
        public static final String UTILITIES_GAMEMODE_ADVENTURE = "voidaris.utilities.gamemode.adventure";
        public static final String UTILITIES_GAMEMODE_SPECTATOR = "voidaris.utilities.gamemode.spectator";
        public static final String UTILITIES_GAMEMODE_OTHERS = "voidaris.utilities.gamemode.others";
        public static final String UTILITIES_HEAL = "voidaris.utilities.heal";
        public static final String UTILITIES_HEAL_OTHERS = "voidaris.utilities.heal.others";
        public static final String UTILITIES_FEED = "voidaris.utilities.feed";
        public static final String UTILITIES_FEED_OTHERS = "voidaris.utilities.feed.others";
        public static final String UTILITIES_FLY = "voidaris.utilities.fly";
        public static final String UTILITIES_FLY_OTHERS = "voidaris.utilities.fly.others";
        public static final String UTILITIES_GOD = "voidaris.utilities.god";
        public static final String UTILITIES_GOD_OTHERS = "voidaris.utilities.god.others";
        public static final String UTILITIES_SPEED = "voidaris.utilities.speed";
        public static final String UTILITIES_SPEED_OTHERS = "voidaris.utilities.speed.others";
        public static final String UTILITIES_HAT = "voidaris.utilities.hat";
        public static final String UTILITIES_TIME = "voidaris.utilities.time";
        public static final String UTILITIES_WEATHER = "voidaris.utilities.weather";
    }

    public static final class Message {

        private Message() {
        }

        public static final String PREFIX = "prefix";

        // general
        public static final String GENERAL_NO_PERMISSION = "general.no-permission";
        public static final String GENERAL_PLAYER_ONLY = "general.player-only";
        public static final String GENERAL_PLAYER_NOT_FOUND = "general.player-not-found";
        public static final String GENERAL_RELOADED = "general.reloaded";
        public static final String GENERAL_MODULE_DISABLED = "general.module-disabled";

        // commands
        public static final String COMMAND_VOIDARIS_DESCRIPTION = "commands.voidaris.description";
        public static final String COMMAND_COSMETICS_DESCRIPTION = "commands.cosmetics.description";
        public static final String COMMAND_SPAWN_DESCRIPTION = "commands.spawn.description";
        public static final String COMMAND_SETSPAWN_DESCRIPTION = "commands.setspawn.description";
        public static final String COMMAND_DELSPAWN_DESCRIPTION = "commands.delspawn.description";
        public static final String COMMAND_WARP_DESCRIPTION = "commands.warp.description";
        public static final String COMMAND_SETWARP_DESCRIPTION = "commands.setwarp.description";
        public static final String COMMAND_DELWARP_DESCRIPTION = "commands.delwarp.description";
        public static final String COMMAND_CRAFT_DESCRIPTION = "commands.craft.description";
        public static final String COMMAND_ANVIL_DESCRIPTION = "commands.anvil.description";
        public static final String COMMAND_GRINDSTONE_DESCRIPTION = "commands.grindstone.description";
        public static final String COMMAND_STONECUTTER_DESCRIPTION = "commands.stonecutter.description";
        public static final String COMMAND_LOOM_DESCRIPTION = "commands.loom.description";
        public static final String COMMAND_CARTOGRAPHY_DESCRIPTION = "commands.cartography.description";
        public static final String COMMAND_SMITHING_DESCRIPTION = "commands.smithing.description";
        public static final String COMMAND_ENDERCHEST_DESCRIPTION = "commands.enderchest.description";
        public static final String COMMAND_GM_DESCRIPTION = "commands.gm.description";
        public static final String COMMAND_HEAL_DESCRIPTION = "commands.heal.description";
        public static final String COMMAND_FEED_DESCRIPTION = "commands.feed.description";
        public static final String COMMAND_FLY_DESCRIPTION = "commands.fly.description";
        public static final String COMMAND_GOD_DESCRIPTION = "commands.god.description";
        public static final String COMMAND_SPEED_DESCRIPTION = "commands.speed.description";
        public static final String COMMAND_HAT_DESCRIPTION = "commands.hat.description";
        public static final String COMMAND_DAY_DESCRIPTION = "commands.day.description";
        public static final String COMMAND_NIGHT_DESCRIPTION = "commands.night.description";
        public static final String COMMAND_SUN_DESCRIPTION = "commands.sun.description";
        public static final String COMMAND_RAIN_DESCRIPTION = "commands.rain.description";
        public static final String COMMAND_MOTD_DESCRIPTION = "commands.motd.description";

        // help
        public static final String HELP_HEADER = "help.header";
        public static final String HELP_SECTION_PLAYER = "help.section.player";
        public static final String HELP_SECTION_ADMIN = "help.section.admin";
        public static final String HELP_ENTRY = "help.entry";
        public static final String HELP_HOVER_RUN = "help.hover.run";
        public static final String HELP_HOVER_SUGGEST = "help.hover.suggest";
        public static final String HELP_HOVER_PAGE = "help.hover.page";
        public static final String HELP_FOOTER = "help.footer";
        public static final String HELP_PREVIOUS = "help.previous";
        public static final String HELP_PREVIOUS_UNAVAILABLE = "help.previous-unavailable";
        public static final String HELP_NEXT = "help.next";
        public static final String HELP_NEXT_UNAVAILABLE = "help.next-unavailable";
        public static final String HELP_INVALID_PAGE = "help.invalid-page";
        public static final String HELP_VOIDARIS_HELP_ARGUMENTS = "help.commands.voidaris-help.arguments";
        public static final String HELP_VOIDARIS_HELP_DESCRIPTION = "help.commands.voidaris-help.description";
        public static final String HELP_VOIDARIS_RELOAD_DESCRIPTION = "help.commands.voidaris-reload.description";
        public static final String HELP_COSMETICS_DESCRIPTION = "help.commands.cosmetics.description";
        public static final String HELP_COSMETICS_JOIN_MESSAGE_ARGUMENTS = "help.commands.cosmetics-joinmessage.arguments";
        public static final String HELP_COSMETICS_JOIN_MESSAGE_DESCRIPTION = "help.commands.cosmetics-joinmessage.description";
        public static final String HELP_COSMETICS_TELEPORT_PARTICLE_ARGUMENTS = "help.commands.cosmetics-teleportparticle.arguments";
        public static final String HELP_COSMETICS_TELEPORT_PARTICLE_DESCRIPTION = "help.commands.cosmetics-teleportparticle.description";
        public static final String HELP_SPAWN_DESCRIPTION = "help.commands.spawn.description";
        public static final String HELP_SETSPAWN_DESCRIPTION = "help.commands.setspawn.description";
        public static final String HELP_DELSPAWN_DESCRIPTION = "help.commands.delspawn.description";
        public static final String HELP_WARP_ARGUMENTS = "help.commands.warp.arguments";
        public static final String HELP_WARP_DESCRIPTION = "help.commands.warp.description";
        public static final String HELP_SETWARP_ARGUMENTS = "help.commands.setwarp.arguments";
        public static final String HELP_SETWARP_DESCRIPTION = "help.commands.setwarp.description";
        public static final String HELP_DELWARP_ARGUMENTS = "help.commands.delwarp.arguments";
        public static final String HELP_DELWARP_DESCRIPTION = "help.commands.delwarp.description";
        public static final String HELP_UTILITIES_TARGET_ARGUMENTS = "help.commands.utilities-target.arguments";
        public static final String HELP_GM_ARGUMENTS = "help.commands.gm.arguments";
        public static final String HELP_SPEED_ARGUMENTS = "help.commands.speed.arguments";
        public static final String HELP_CRAFT_DESCRIPTION = "help.commands.craft.description";
        public static final String HELP_ANVIL_DESCRIPTION = "help.commands.anvil.description";
        public static final String HELP_GRINDSTONE_DESCRIPTION = "help.commands.grindstone.description";
        public static final String HELP_STONECUTTER_DESCRIPTION = "help.commands.stonecutter.description";
        public static final String HELP_LOOM_DESCRIPTION = "help.commands.loom.description";
        public static final String HELP_CARTOGRAPHY_DESCRIPTION = "help.commands.cartography.description";
        public static final String HELP_SMITHING_DESCRIPTION = "help.commands.smithing.description";
        public static final String HELP_ENDERCHEST_DESCRIPTION = "help.commands.enderchest.description";
        public static final String HELP_GM_DESCRIPTION = "help.commands.gm.description";
        public static final String HELP_HEAL_DESCRIPTION = "help.commands.heal.description";
        public static final String HELP_FEED_DESCRIPTION = "help.commands.feed.description";
        public static final String HELP_FLY_DESCRIPTION = "help.commands.fly.description";
        public static final String HELP_GOD_DESCRIPTION = "help.commands.god.description";
        public static final String HELP_SPEED_DESCRIPTION = "help.commands.speed.description";
        public static final String HELP_HAT_DESCRIPTION = "help.commands.hat.description";
        public static final String HELP_DAY_DESCRIPTION = "help.commands.day.description";
        public static final String HELP_NIGHT_DESCRIPTION = "help.commands.night.description";
        public static final String HELP_SUN_DESCRIPTION = "help.commands.sun.description";
        public static final String HELP_RAIN_DESCRIPTION = "help.commands.rain.description";
        public static final String HELP_MOTD_DESCRIPTION = "help.commands.motd.description";

        // teleport
        public static final String TELEPORT_WARMUP = "teleport.warmup";
        public static final String TELEPORT_CANCELLED_MOVE = "teleport.cancelled-move";
        public static final String TELEPORT_CANCELLED_DAMAGE = "teleport.cancelled-damage";
        public static final String TELEPORT_COOLDOWN = "teleport.cooldown";
        public static final String TELEPORT_WORLD_UNAVAILABLE = "teleport.world-unavailable";
        public static final String TELEPORT_FAILED = "teleport.failed";

        // spawn
        public static final String SPAWN_TELEPORTED = "spawn.teleported";
        public static final String SPAWN_SET = "spawn.set";
        public static final String SPAWN_NOT_SET = "spawn.not-set";
        public static final String SPAWN_DELETED = "spawn.deleted";
        public static final String SPAWN_VOID_RETURN = "spawn.void-return";

        // warps
        public static final String WARPS_TELEPORTED = "warps.teleported";
        public static final String WARPS_SET = "warps.set";
        public static final String WARPS_DELETED = "warps.deleted";
        public static final String WARPS_NOT_FOUND = "warps.not-found";
        public static final String WARPS_INVALID_NAME = "warps.invalid-name";
        public static final String WARPS_LIST = "warps.list";
        public static final String WARPS_LIST_EMPTY = "warps.list-empty";
        public static final String WARPS_LIST_ENTRY = "warps.list-entry";
        public static final String WARPS_LIST_SEPARATOR = "warps.list-separator";
        public static final String WARPS_LIST_HOVER = "warps.list-hover";

        // motd
        public static final String MOTD_EMPTY = "motd.empty";

        // utilities
        public static final String UTILITIES_TITLE_CRAFT = "utilities.titles.craft";
        public static final String UTILITIES_TITLE_ANVIL = "utilities.titles.anvil";
        public static final String UTILITIES_TITLE_GRINDSTONE = "utilities.titles.grindstone";
        public static final String UTILITIES_TITLE_STONECUTTER = "utilities.titles.stonecutter";
        public static final String UTILITIES_TITLE_LOOM = "utilities.titles.loom";
        public static final String UTILITIES_TITLE_CARTOGRAPHY = "utilities.titles.cartography";
        public static final String UTILITIES_TITLE_SMITHING = "utilities.titles.smithing";
        public static final String UTILITIES_GAMEMODE_CHANGED = "utilities.gamemode.changed";
        public static final String UTILITIES_GAMEMODE_CHANGED_OTHER = "utilities.gamemode.changed-other";
        public static final String UTILITIES_GAMEMODE_NAME_SURVIVAL = "utilities.gamemode.names.survival";
        public static final String UTILITIES_GAMEMODE_NAME_CREATIVE = "utilities.gamemode.names.creative";
        public static final String UTILITIES_GAMEMODE_NAME_ADVENTURE = "utilities.gamemode.names.adventure";
        public static final String UTILITIES_GAMEMODE_NAME_SPECTATOR = "utilities.gamemode.names.spectator";
        public static final String UTILITIES_HEAL_SELF = "utilities.heal.self";
        public static final String UTILITIES_HEAL_OTHER = "utilities.heal.other";
        public static final String UTILITIES_FEED_SELF = "utilities.feed.self";
        public static final String UTILITIES_FEED_OTHER = "utilities.feed.other";
        public static final String UTILITIES_FLY_ENABLED = "utilities.fly.enabled";
        public static final String UTILITIES_FLY_ENABLED_OTHER = "utilities.fly.enabled-other";
        public static final String UTILITIES_FLY_DISABLED = "utilities.fly.disabled";
        public static final String UTILITIES_FLY_DISABLED_OTHER = "utilities.fly.disabled-other";
        public static final String UTILITIES_GOD_ENABLED = "utilities.god.enabled";
        public static final String UTILITIES_GOD_ENABLED_OTHER = "utilities.god.enabled-other";
        public static final String UTILITIES_GOD_DISABLED = "utilities.god.disabled";
        public static final String UTILITIES_GOD_DISABLED_OTHER = "utilities.god.disabled-other";
        public static final String UTILITIES_SPEED_WALK = "utilities.speed.walk";
        public static final String UTILITIES_SPEED_WALK_OTHER = "utilities.speed.walk-other";
        public static final String UTILITIES_SPEED_FLY = "utilities.speed.fly";
        public static final String UTILITIES_SPEED_FLY_OTHER = "utilities.speed.fly-other";
        public static final String UTILITIES_HAT_EQUIPPED = "utilities.hat.equipped";
        public static final String UTILITIES_HAT_EMPTY_HAND = "utilities.hat.empty-hand";
        public static final String UTILITIES_HAT_BOUND = "utilities.hat.bound";
        public static final String UTILITIES_TIME_DAY = "utilities.time.day";
        public static final String UTILITIES_TIME_NIGHT = "utilities.time.night";
        public static final String UTILITIES_WEATHER_SUN = "utilities.weather.sun";
        public static final String UTILITIES_WEATHER_RAIN = "utilities.weather.rain";

        // cosmetics
        public static final String COSMETICS_UNAVAILABLE = "cosmetics.unavailable";
        public static final String COSMETICS_JOIN_MESSAGE_SELECTED = "cosmetics.join-message.selected";
        public static final String COSMETICS_JOIN_MESSAGE_NOT_FOUND = "cosmetics.join-message.not-found";
        public static final String COSMETICS_JOIN_MESSAGE_LOCKED = "cosmetics.join-message.locked";
        public static final String COSMETICS_JOIN_MESSAGE_PREVIEW_HIDDEN = "cosmetics.join-message.preview-hidden";
        public static final String COSMETICS_TELEPORT_PARTICLE_SELECTED = "cosmetics.teleport-particle.selected";
        public static final String COSMETICS_TELEPORT_PARTICLE_NOT_FOUND = "cosmetics.teleport-particle.not-found";
        public static final String COSMETICS_TELEPORT_PARTICLE_LOCKED = "cosmetics.teleport-particle.locked";
    }
}
