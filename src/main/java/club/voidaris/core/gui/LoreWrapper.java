package club.voidaris.core.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.flattener.FlattenerListener;
import net.kyori.adventure.text.format.Style;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Splits a component into lore lines, since the client never wraps item lore on its own.
 */
public final class LoreWrapper {

    private LoreWrapper() {
    }

    /**
     * Breaks at the last space before {@code width} characters (or mid-word when a word is longer), keeping each
     * character's style so gradients and colors survive the split.
     */
    public static List<Component> wrap(Component component, int width) {
        var glyphs = glyphs(component);
        var lines = new ArrayList<Component>();
        int start = 0;
        while (start < glyphs.size()) {
            int end = lineEnd(glyphs, start, Math.max(1, width));
            lines.add(line(glyphs, start, end));
            start = end;
            // The space or newline at the break is consumed instead of starting the next line.
            if (start < glyphs.size() && isBreak(glyphs.get(start).codePoint())) {
                start++;
            }
        }
        return lines.isEmpty() ? List.of(Component.empty()) : lines;
    }

    private static int lineEnd(List<Glyph> glyphs, int start, int width) {
        int limit = Math.min(start + width, glyphs.size());
        for (int i = start; i < limit; i++) {
            if (glyphs.get(i).codePoint() == '\n') {
                return i;
            }
        }
        if (limit == glyphs.size() || glyphs.get(limit).codePoint() == ' ') {
            return limit;
        }
        for (int i = limit - 1; i > start; i--) {
            if (glyphs.get(i).codePoint() == ' ') {
                return i;
            }
        }
        return limit;
    }

    private static boolean isBreak(int codePoint) {
        return codePoint == ' ' || codePoint == '\n';
    }

    private static Component line(List<Glyph> glyphs, int start, int end) {
        TextComponent.Builder line = Component.text();
        var run = new StringBuilder();
        Style runStyle = null;
        for (int i = start; i < end; i++) {
            var glyph = glyphs.get(i);
            if (runStyle != null && !glyph.style().equals(runStyle)) {
                line.append(Component.text(run.toString(), runStyle));
                run.setLength(0);
            }
            runStyle = glyph.style();
            run.appendCodePoint(glyph.codePoint());
        }
        if (runStyle != null) {
            line.append(Component.text(run.toString(), runStyle));
        }
        return line.build();
    }

    private static List<Glyph> glyphs(Component component) {
        var glyphs = new ArrayList<Glyph>();
        ComponentFlattener.basic().flatten(component, new FlattenerListener() {
            private final Deque<Style> styles = new ArrayDeque<>(List.of(Style.empty()));

            @Override
            public void pushStyle(Style style) {
                styles.push(styles.peek().merge(style));
            }

            @Override
            public void component(String text) {
                Style style = styles.peek();
                text.codePoints().forEach(codePoint -> glyphs.add(new Glyph(codePoint, style)));
            }

            @Override
            public void popStyle(Style style) {
                styles.pop();
            }
        });
        return glyphs;
    }

    private record Glyph(int codePoint, Style style) {
    }
}
