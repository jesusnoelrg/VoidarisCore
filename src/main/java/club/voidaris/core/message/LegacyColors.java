package club.voidaris.core.message;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Turns legacy & codes (&a, &l, &#RRGGBB, &x&R&R&G&G&B&B) into MiniMessage tags, for values that come from other
 * plugins such as LuckPerms prefixes or PlaceholderAPI placeholders.
 */
public final class LegacyColors {

    private static final Map<Character, String> COLORS = Map.ofEntries(
            Map.entry('0', "black"), Map.entry('1', "dark_blue"), Map.entry('2', "dark_green"),
            Map.entry('3', "dark_aqua"), Map.entry('4', "dark_red"), Map.entry('5', "dark_purple"),
            Map.entry('6', "gold"), Map.entry('7', "gray"), Map.entry('8', "dark_gray"), Map.entry('9', "blue"),
            Map.entry('a', "green"), Map.entry('b', "aqua"), Map.entry('c', "red"), Map.entry('d', "light_purple"),
            Map.entry('e', "yellow"), Map.entry('f', "white"));
    private static final Map<Character, String> DECORATIONS = Map.of(
            'k', "obfuscated", 'l', "bold", 'm', "strikethrough", 'n', "underlined", 'o', "italic");

    private LegacyColors() {
    }

    public static String toMiniMessage(String text) {
        if (text.indexOf('&') < 0) {
            return text;
        }
        var out = new StringBuilder(text.length() + 16);
        // Legacy colors cancel the formatting before them, so decorations opened here are closed on the next color.
        List<String> open = new ArrayList<>();
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c != '&' || i + 1 >= text.length()) {
                out.append(c);
                i++;
                continue;
            }
            char code = Character.toLowerCase(text.charAt(i + 1));
            String hex = hex(text, i);
            if (hex != null) {
                close(out, open);
                out.append("<#").append(hex).append('>');
                i += code == 'x' ? 14 : 8;
            } else if (COLORS.containsKey(code)) {
                close(out, open);
                out.append('<').append(COLORS.get(code)).append('>');
                i += 2;
            } else if (DECORATIONS.containsKey(code)) {
                String decoration = DECORATIONS.get(code);
                out.append('<').append(decoration).append('>');
                open.add(decoration);
                i += 2;
            } else if (code == 'r') {
                open.clear();
                out.append("<reset>");
                i += 2;
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    private static void close(StringBuilder out, List<String> open) {
        for (int j = open.size() - 1; j >= 0; j--) {
            out.append("</").append(open.get(j)).append('>');
        }
        open.clear();
    }

    /**
     * @return the six hex digits of a &#RRGGBB or &x&R&R&G&G&B&B code starting at {@code start}, or null
     */
    private static String hex(String text, int start) {
        if (text.startsWith("&#", start) && start + 8 <= text.length()) {
            String digits = text.substring(start + 2, start + 8);
            return isHex(digits) ? digits.toLowerCase(Locale.ROOT) : null;
        }
        if (Character.toLowerCase(text.charAt(start + 1)) == 'x' && start + 14 <= text.length()) {
            var digits = new StringBuilder(6);
            for (int j = start + 2; j < start + 14; j += 2) {
                if (text.charAt(j) != '&') {
                    return null;
                }
                digits.append(text.charAt(j + 1));
            }
            return isHex(digits.toString()) ? digits.toString().toLowerCase(Locale.ROOT) : null;
        }
        return null;
    }

    private static boolean isHex(String digits) {
        return digits.chars().allMatch(ch -> Character.digit(ch, 16) >= 0);
    }
}
