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

        // cosmetics
        public static final String COSMETICS_UNAVAILABLE = "cosmetics.unavailable";
        public static final String COSMETICS_JOIN_MESSAGE_SELECTED = "cosmetics.join-message.selected";
        public static final String COSMETICS_JOIN_MESSAGE_NOT_FOUND = "cosmetics.join-message.not-found";
        public static final String COSMETICS_JOIN_MESSAGE_LOCKED = "cosmetics.join-message.locked";
        public static final String COSMETICS_JOIN_MESSAGE_PREVIEW_HIDDEN = "cosmetics.join-message.preview-hidden";
    }
}
