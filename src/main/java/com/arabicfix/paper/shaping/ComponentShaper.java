package com.arabicfix.paper.shaping;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Applies {@link ArabicShaper} to Adventure components without losing their styling.
 *
 * <p>A component tree is flattened into styled text segments (non-text parts such as
 * translatable components are kept as opaque pieces), the text is shaped as one line, and the
 * components are rebuilt from the shaped result. Every character carries the index of the segment
 * it came from, so colours, decorations and click/hover events follow their characters even
 * though the order is reversed. Arguments of translatable components (for example the message in
 * {@code chat.type.text}) are shaped as lines of their own.</p>
 */
public final class ComponentShaper {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('§')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private static final char PLACEHOLDER = ' ';

    private final boolean autoDirection;

    public ComponentShaper(boolean autoDirection) {
        this.autoDirection = autoDirection;
    }

    /** Returns the very same instance when there is nothing to shape. */
    public Component shape(Component component) {
        if (component == null || !needsShaping(component)) return component;
        return process(component);
    }

    /** Plain strings (no styling), for example the plain content of a signed chat message. */
    public String shape(String text) {
        return ArabicShaper.shape(text, autoDirection);
    }

    // ------------------------------------------------------------------ detection

    private boolean needsShaping(Component c) {
        if (c instanceof TextComponent text && ArabicShaper.needsShaping(text.content())) return true;
        if (c instanceof TranslatableComponent translatable) {
            for (TranslationArgument arg : translatable.arguments()) {
                if (arg.value() instanceof Component argComponent && needsShaping(argComponent)) return true;
            }
        }
        for (Component child : c.children()) {
            if (needsShaping(child)) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ processing

    private record Segment(String text, Style style, Component atom) {
        static Segment text(String text, Style style) {
            return new Segment(text, style, null);
        }

        static Segment atom(Component atom) {
            return new Segment(null, null, atom);
        }
    }

    private Component process(Component root) {
        List<Segment> segments = new ArrayList<>();
        flatten(root, Style.empty(), true, segments);

        StringBuilder text = new StringBuilder();
        List<Integer> tagList = new ArrayList<>();
        for (int i = 0; i < segments.size(); i++) {
            Segment segment = segments.get(i);
            if (segment.atom() != null) {
                text.append(PLACEHOLDER);
                tagList.add(i);
            } else {
                text.append(segment.text());
                for (int c = 0; c < segment.text().length(); c++) tagList.add(i);
            }
        }
        int[] tags = new int[tagList.size()];
        for (int i = 0; i < tags.length; i++) tags[i] = tagList.get(i);

        ArabicShaper.Shaped shaped = ArabicShaper.shape(text.toString(), tags, autoDirection);

        // rebuild: one piece per run of equal tags, then merge neighbours with identical style
        List<Component> parts = new ArrayList<>();
        StringBuilder pending = null;
        Style pendingStyle = null;
        String out = shaped.text();
        int[] outTags = shaped.tags();
        int i = 0;
        while (i < out.length()) {
            int tag = outTags[i];
            int j = i + 1;
            while (j < out.length() && outTags[j] == tag) j++;
            Segment segment = segments.get(tag);
            if (segment.atom() != null) {
                if (pending != null) {
                    parts.add(Component.text(pending.toString(), pendingStyle));
                    pending = null;
                }
                parts.add(segment.atom());
            } else {
                if (pending != null && pendingStyle.equals(segment.style())) {
                    pending.append(out, i, j);
                } else {
                    if (pending != null) parts.add(Component.text(pending.toString(), pendingStyle));
                    pending = new StringBuilder(out.substring(i, j));
                    pendingStyle = segment.style();
                }
            }
            i = j;
        }
        if (pending != null) parts.add(Component.text(pending.toString(), pendingStyle));

        if (parts.size() == 1) return parts.get(0);
        return Component.empty().children(parts);
    }

    /**
     * @param legacy whether literal section-sign colour codes inside text should be turned into styles
     *               (false for text that already came out of that conversion)
     */
    private void flatten(Component component, Style parentStyle, boolean legacy, List<Segment> out) {
        Style style = component.style().merge(parentStyle, Style.Merge.Strategy.IF_ABSENT_ON_TARGET);

        if (component instanceof TextComponent text) {
            String content = text.content();
            if (legacy && content.indexOf('§') >= 0) {
                flatten(LEGACY.deserialize(content), style, false, out);
            } else if (!content.isEmpty()) {
                out.add(Segment.text(content, style));
            }
        } else {
            Component atom = component.children(Collections.emptyList()).style(style);
            if (atom instanceof TranslatableComponent translatable) {
                atom = shapeArguments(translatable);
            }
            out.add(Segment.atom(atom));
        }

        for (Component child : component.children()) {
            flatten(child, style, legacy, out);
        }
    }

    private Component shapeArguments(TranslatableComponent translatable) {
        List<TranslationArgument> arguments = translatable.arguments();
        if (arguments.isEmpty()) return translatable;

        List<TranslationArgument> mapped = new ArrayList<>(arguments.size());
        boolean changed = false;
        for (TranslationArgument argument : arguments) {
            if (argument.value() instanceof Component argComponent) {
                Component shaped = shape(argComponent);
                if (shaped != argComponent) {
                    mapped.add(TranslationArgument.component(shaped));
                    changed = true;
                    continue;
                }
            }
            mapped.add(argument);
        }
        return changed ? translatable.arguments(mapped) : translatable;
    }
}
