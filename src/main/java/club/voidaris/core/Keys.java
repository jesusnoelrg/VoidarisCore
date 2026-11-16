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
    }

    public static final class Message {

        private Message() {
        }

        public static final String PREFIX = "prefix";

        // general
        public static final String GENERAL_NO_PERMISSION = "general.no-permission";
        public static final String GENERAL_PLAYER_ONLY = "general.player-only";
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
        public static final String HELP_SPAWN_DESCRIPTION = "help.commands.spawn.description";
        public static final String HELP_SETSPAWN_DESCRIPTION = "help.commands.setspawn.description";
        public static final String HELP_DELSPAWN_DESCRIPTION = "help.commands.delspawn.description";
        public static final String HELP_WARP_ARGUMENTS = "help.commands.warp.arguments";
        public static final String HELP_WARP_DESCRIPTION = "help.commands.warp.description";
        public static final String HELP_SETWARP_ARGUMENTS = "help.commands.setwarp.arguments";
        public static final String HELP_SETWARP_DESCRIPTION = "help.commands.setwarp.description";
        public static final String HELP_DELWARP_ARGUMENTS = "help.commands.delwarp.arguments";
        public static final String HELP_DELWARP_DESCRIPTION = "help.commands.delwarp.description";

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

        // cosmetics
        public static final String COSMETICS_UNAVAILABLE = "cosmetics.unavailable";
        public static final String COSMETICS_JOIN_MESSAGE_SELECTED = "cosmetics.join-message.selected";
        public static final String COSMETICS_JOIN_MESSAGE_NOT_FOUND = "cosmetics.join-message.not-found";
        public static final String COSMETICS_JOIN_MESSAGE_LOCKED = "cosmetics.join-message.locked";
        public static final String COSMETICS_JOIN_MESSAGE_PREVIEW_HIDDEN = "cosmetics.join-message.preview-hidden";
    }
}
