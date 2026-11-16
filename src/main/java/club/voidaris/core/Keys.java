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

        // cosmetics
        public static final String COSMETICS_UNAVAILABLE = "cosmetics.unavailable";
        public static final String COSMETICS_JOIN_MESSAGE_SELECTED = "cosmetics.join-message.selected";
        public static final String COSMETICS_JOIN_MESSAGE_NOT_FOUND = "cosmetics.join-message.not-found";
        public static final String COSMETICS_JOIN_MESSAGE_LOCKED = "cosmetics.join-message.locked";
        public static final String COSMETICS_JOIN_MESSAGE_PREVIEW_HIDDEN = "cosmetics.join-message.preview-hidden";
    }
}
