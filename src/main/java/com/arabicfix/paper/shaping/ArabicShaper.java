package com.arabicfix.paper.shaping;

import java.util.HashMap;
import java.util.Map;

/**
 * Pre-shapes Arabic-script text for Minecraft Bedrock clients.
 *
 * <p>Bedrock draws every glyph on its own and does neither contextual joining nor
 * right-to-left reordering for chat, titles, signs and so on. So the text is joined
 * into Arabic Presentation Forms (U+FB50..U+FEFF) and reordered visually before it is sent.</p>
 *
 * <p>The joining rules and the "reverse the line, keep Latin and number runs readable,
 * mirror brackets" approach follow the Arabic Text Fixer behaviour pack. Two improvements:
 * (1) lam-alef ligatures take their final form when joined to the previous letter,
 * and (2) with {@code autoDirection} a line that starts with Latin text (for example
 * {@code [VIP] Mavi: مرحبا}) only has its Arabic parts reversed, so prefixes stay in place.</p>
 *
 * <p>Output contains no joinable logical letters any more, so shaping twice is a no-op.</p>
 */
public final class ArabicShaper {

    /** Shaped text and one tag per UTF-16 unit; tags follow their characters through the shaping. */
    public record Shaped(String text, int[] tags) {
    }

    private static final int ISO = 0, FIN = 1, INI = 2, MED = 3;
    private static final int TATWEEL = 0x0640;
    private static final int SECTION = 0x00A7;

    /** letter -> {isolated, final, initial, medial}; 0 = form does not exist. */
    private static final Map<Integer, int[]> FORMS = new HashMap<>(160);
    /** lam + alef variant -> {isolated ligature, final ligature}. */
    private static final Map<Integer, int[]> LAM_ALEF = new HashMap<>(8);

    private static void dual(int letter, int iso, int fin, int ini, int med) {
        FORMS.put(letter, new int[]{iso, fin, ini, med});
    }

    private static void right(int letter, int iso, int fin) {
        FORMS.put(letter, new int[]{iso, fin, 0, 0});
    }

    static {
        // Letters that join only to the letter before them
        right(0x0627, 0xFE8D, 0xFE8E); // ا
        right(0x0622, 0xFE81, 0xFE82); // آ
        right(0x0623, 0xFE83, 0xFE84); // أ
        right(0x0625, 0xFE87, 0xFE88); // إ
        right(0x0624, 0xFE85, 0xFE86); // ؤ
        right(0x0621, 0xFE80, 0);      // ء (never joins)
        right(0x0629, 0xFE93, 0xFE94); // ة
        right(0x0649, 0xFEEF, 0xFEF0); // ى
        right(0x062F, 0xFEA9, 0xFEAA); // د
        right(0x0630, 0xFEAB, 0xFEAC); // ذ
        right(0x0631, 0xFEAD, 0xFEAE); // ر
        right(0x0632, 0xFEAF, 0xFEB0); // ز
        right(0x0648, 0xFEED, 0xFEEE); // و

        // Letters that join on both sides
        dual(0x0628, 0xFE8F, 0xFE90, 0xFE91, 0xFE92); // ب
        dual(0x062A, 0xFE95, 0xFE96, 0xFE97, 0xFE98); // ت
        dual(0x062B, 0xFE99, 0xFE9A, 0xFE9B, 0xFE9C); // ث
        dual(0x062C, 0xFE9D, 0xFE9E, 0xFE9F, 0xFEA0); // ج
        dual(0x062D, 0xFEA1, 0xFEA2, 0xFEA3, 0xFEA4); // ح
        dual(0x062E, 0xFEA5, 0xFEA6, 0xFEA7, 0xFEA8); // خ
        dual(0x0633, 0xFEB1, 0xFEB2, 0xFEB3, 0xFEB4); // س
        dual(0x0634, 0xFEB5, 0xFEB6, 0xFEB7, 0xFEB8); // ش
        dual(0x0635, 0xFEB9, 0xFEBA, 0xFEBB, 0xFEBC); // ص
        dual(0x0636, 0xFEBD, 0xFEBE, 0xFEBF, 0xFEC0); // ض
        dual(0x0637, 0xFEC1, 0xFEC2, 0xFEC3, 0xFEC4); // ط
        dual(0x0638, 0xFEC5, 0xFEC6, 0xFEC7, 0xFEC8); // ظ
        dual(0x0639, 0xFEC9, 0xFECA, 0xFECB, 0xFECC); // ع
        dual(0x063A, 0xFECD, 0xFECE, 0xFECF, 0xFED0); // غ
        dual(0x0641, 0xFED1, 0xFED2, 0xFED3, 0xFED4); // ف
        dual(0x0642, 0xFED5, 0xFED6, 0xFED7, 0xFED8); // ق
        dual(0x0643, 0xFED9, 0xFEDA, 0xFEDB, 0xFEDC); // ك
        dual(0x0644, 0xFEDD, 0xFEDE, 0xFEDF, 0xFEE0); // ل
        dual(0x0645, 0xFEE1, 0xFEE2, 0xFEE3, 0xFEE4); // م
        dual(0x0646, 0xFEE5, 0xFEE6, 0xFEE7, 0xFEE8); // ن
        dual(0x0647, 0xFEE9, 0xFEEA, 0xFEEB, 0xFEEC); // ه
        dual(0x064A, 0xFEF1, 0xFEF2, 0xFEF3, 0xFEF4); // ي
        dual(0x0626, 0xFE89, 0xFE8A, 0xFE8B, 0xFE8C); // ئ

        // Persian / Urdu letters
        dual(0x067E, 0xFB56, 0xFB57, 0xFB58, 0xFB59); // پ
        dual(0x0679, 0xFB66, 0xFB67, 0xFB68, 0xFB69); // ٹ
        dual(0x06A4, 0xFB6A, 0xFB6B, 0xFB6C, 0xFB6D); // ڤ
        dual(0x0686, 0xFB7A, 0xFB7B, 0xFB7C, 0xFB7D); // چ
        dual(0x06A9, 0xFB8E, 0xFB8F, 0xFB90, 0xFB91); // ک
        dual(0x06AF, 0xFB92, 0xFB93, 0xFB94, 0xFB95); // گ
        dual(0x06BE, 0xFBAA, 0xFBAB, 0xFBAC, 0xFBAD); // ھ
        dual(0x06C1, 0xFBA6, 0xFBA7, 0xFBA8, 0xFBA9); // ہ
        dual(0x06CC, 0xFBFC, 0xFBFD, 0xFBFE, 0xFBFF); // ی
        right(0x0688, 0xFB88, 0xFB89); // ڈ
        right(0x0691, 0xFB8C, 0xFB8D); // ڑ
        right(0x0698, 0xFB8A, 0xFB8B); // ژ
        right(0x06D2, 0xFBAE, 0xFBAF); // ے
        right(0x06C0, 0xFBA4, 0xFBA5); // ۀ

        LAM_ALEF.put(0x0627, new int[]{0xFEFB, 0xFEFC}); // لا
        LAM_ALEF.put(0x0623, new int[]{0xFEF7, 0xFEF8}); // لأ
        LAM_ALEF.put(0x0625, new int[]{0xFEF9, 0xFEFA}); // لإ
        LAM_ALEF.put(0x0622, new int[]{0xFEF5, 0xFEF6}); // لآ
    }

    private ArabicShaper() {
    }

    // ------------------------------------------------------------------ public API

    /** True if the text has at least one logical (unshaped) Arabic-script letter we can shape. */
    public static boolean needsShaping(CharSequence text) {
        if (text == null) return false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 0x0621 && c <= 0x06D2 && FORMS.containsKey((int) c)) return true;
        }
        return false;
    }

    public static String shape(String text, boolean autoDirection) {
        if (!needsShaping(text)) return text;
        return shape(text, null, autoDirection).text();
    }

    /**
     * @param tags one value per UTF-16 unit of {@code text} (or {@code null}); the tag of every character is
     *             kept on it through joining and reordering, so callers can map styling back afterwards
     */
    public static Shaped shape(String text, int[] tags, boolean autoDirection) {
        if (tags == null) tags = new int[text.length()];
        if (!needsShaping(text)) return new Shaped(text, tags);

        StringBuilder out = new StringBuilder(text.length());
        int[] outTags = new int[text.length()];
        int outLen = 0;

        int lineStart = 0;
        while (lineStart <= text.length()) {
            int nl = text.indexOf('\n', lineStart);
            int lineEnd = nl < 0 ? text.length() : nl;

            String line = text.substring(lineStart, lineEnd);
            int[] lineTags = java.util.Arrays.copyOfRange(tags, lineStart, lineEnd);
            Shaped shaped = needsShaping(line)
                    ? shapeLine(line, lineTags, autoDirection)
                    : new Shaped(line, lineTags);

            out.append(shaped.text());
            System.arraycopy(shaped.tags(), 0, outTags, outLen, shaped.tags().length);
            outLen += shaped.tags().length;

            if (nl < 0) break;
            out.append('\n');
            outTags[outLen++] = tags[nl];
            lineStart = nl + 1;
        }
        return new Shaped(out.toString(), java.util.Arrays.copyOf(outTags, outLen));
    }

    // ------------------------------------------------------------------ one line

    private static Shaped shapeLine(String line, int[] charTags, boolean autoDirection) {
        // code points + a tag for each
        int[] cp = line.codePoints().toArray();
        int[] tag = new int[cp.length];
        for (int i = 0, ci = 0; i < cp.length; i++) {
            tag[i] = charTags[ci];
            ci += Character.charCount(cp[i]);
        }

        // 1. drop harakat (diacritics); they cannot be placed correctly after reordering
        int n = 0;
        for (int i = 0; i < cp.length; i++) {
            int c = cp[i];
            if ((c >= 0x064B && c <= 0x065F) || c == 0x0670) continue;
            cp[n] = c;
            tag[n] = tag[i];
            n++;
        }

        boolean rtlBase = !autoDirection || firstStrongIsArabic(cp, n);

        // 2. join letters into presentation forms
        Joined joined = join(cp, tag, n);
        cp = joined.cp;
        tag = joined.tag;
        n = joined.len;

        // 3. visual reordering
        if (rtlBase) {
            reorder(cp, tag, 0, n);
        } else {
            int i = 0;
            while (i < n) {
                if (isStrongArabic(cp[i])) {
                    int last = i;
                    int j = i + 1;
                    while (j < n) {
                        if (isFormatCodeAt(cp, j, n)) {
                            j += 2; // colour code: neutral
                        } else if (isRunMember(cp[j])) {
                            if (isStrongArabic(cp[j])) last = j;
                            j++;
                        } else {
                            break;
                        }
                    }
                    reorder(cp, tag, i, last + 1);
                    i = last + 1;
                } else {
                    i++;
                }
            }
        }

        // back to UTF-16 + one tag per unit
        StringBuilder sb = new StringBuilder(n);
        int[] outTags = new int[line.length() + n]; // upper bound
        int len = 0;
        for (int i = 0; i < n; i++) {
            int units = Character.charCount(cp[i]);
            sb.appendCodePoint(cp[i]);
            for (int u = 0; u < units; u++) outTags[len++] = tag[i];
        }
        return new Shaped(sb.toString(), java.util.Arrays.copyOf(outTags, len));
    }

    // ------------------------------------------------------------------ joining

    private static final class Joined {
        int[] cp;
        int[] tag;
        int len;
    }

    private static Joined join(int[] cp, int[] tag, int n) {
        // Build joining units first (lam+alef collapses into one unit)
        int[] unitCp = new int[n];
        int[] unitTag = new int[n];
        int[][] unitForms = new int[n][];
        boolean[] dual = new boolean[n];        // may connect to the NEXT unit
        boolean[] acceptsPrev = new boolean[n]; // may connect to the PREVIOUS unit
        int units = 0;

        for (int i = 0; i < n; i++) {
            int c = cp[i];
            int u = units++;
            unitTag[u] = tag[i];
            unitCp[u] = c;

            int[] lig = (c == 0x0644 && i + 1 < n) ? LAM_ALEF.get(cp[i + 1]) : null;
            if (lig != null) {
                unitForms[u] = new int[]{lig[0], lig[1], 0, 0};
                acceptsPrev[u] = true;
                i++; // alef consumed
                continue;
            }
            int[] f = FORMS.get(c);
            if (f != null) {
                unitForms[u] = f;
                dual[u] = f[INI] != 0;
                acceptsPrev[u] = f[FIN] != 0;
            } else if (c == TATWEEL) {
                dual[u] = true;
                acceptsPrev[u] = true;
            }
        }

        Joined result = new Joined();
        result.cp = new int[units];
        result.tag = new int[units];
        result.len = units;

        for (int u = 0; u < units; u++) {
            result.tag[u] = unitTag[u];
            int[] f = unitForms[u];
            if (f == null) {
                result.cp[u] = unitCp[u];
                continue;
            }
            boolean prev = u > 0 && dual[u - 1] && acceptsPrev[u];
            boolean next = dual[u] && u + 1 < units && acceptsPrev[u + 1];
            int form;
            if (prev && next) form = f[MED];
            else if (prev) form = f[FIN];
            else if (next) form = f[INI];
            else form = f[ISO];
            if (form == 0) form = f[ISO];
            result.cp[u] = form;
        }
        return result;
    }

    // ------------------------------------------------------------------ reordering

    /** Reverse [from, to), then keep numbers/Latin readable, keep colour codes in front, mirror brackets. */
    private static void reorder(int[] cp, int[] tag, int from, int to) {
        reverse(cp, tag, from, to);
        fixFormatCodes(cp, tag, from, to);
        restoreLtrRuns(cp, tag, from, to);
        mirrorBrackets(cp, from, to);
    }

    private static void reverse(int[] cp, int[] tag, int from, int to) {
        for (int i = from, j = to - 1; i < j; i++, j--) {
            int c = cp[i];
            cp[i] = cp[j];
            cp[j] = c;
            int t = tag[i];
            tag[i] = tag[j];
            tag[j] = t;
        }
    }

    /** After reversing, "x§" must become "§x" again. */
    private static void fixFormatCodes(int[] cp, int[] tag, int from, int to) {
        for (int i = from; i + 1 < to; i++) {
            if (cp[i + 1] == SECTION && isFormatCodeChar(cp[i])) {
                int c = cp[i];
                cp[i] = SECTION;
                cp[i + 1] = c;
                int t = tag[i];
                tag[i] = tag[i + 1];
                tag[i + 1] = t;
                i++;
            }
        }
    }

    private static boolean isFormatCodeChar(int c) {
        return "0123456789abcdefklmnorxABCDEFKLMNORX".indexOf(c) >= 0 && c < 128;
    }

    private static void restoreLtrRuns(int[] cp, int[] tag, int from, int to) {
        int i = from;
        while (i < to) {
            if (!isLtrWordChar(cp[i])) {
                i++;
                continue;
            }
            int j = i + 1;
            while (j < to) {
                if (isLtrWordChar(cp[j])) {
                    j++;
                } else if (isNumberSeparator(cp[j]) && j + 1 < to && isLtrWordChar(cp[j + 1])) {
                    j += 2; // keeps "3.5", "10:30", "a-b" together
                } else {
                    break;
                }
            }
            reverse(cp, tag, i, j);
            i = j;
        }
    }

    private static void mirrorBrackets(int[] cp, int from, int to) {
        for (int i = from; i < to; i++) {
            cp[i] = switch (cp[i]) {
                case '(' -> ')';
                case ')' -> '(';
                case '[' -> ']';
                case ']' -> '[';
                case '{' -> '}';
                case '}' -> '{';
                case '<' -> '>';
                case '>' -> '<';
                case 0x00AB -> 0x00BB; // «
                case 0x00BB -> 0x00AB; // »
                default -> cp[i];
            };
        }
    }

    // ------------------------------------------------------------------ character classes

    private static boolean isArabicScript(int c) {
        return (c >= 0x0600 && c <= 0x06FF) || (c >= 0x0750 && c <= 0x077F) || (c >= 0x08A0 && c <= 0x08FF)
                || (c >= 0xFB50 && c <= 0xFDFF) || (c >= 0xFE70 && c <= 0xFEFF);
    }

    private static boolean isArabicDigit(int c) {
        return (c >= 0x0660 && c <= 0x0669) || (c >= 0x06F0 && c <= 0x06F9);
    }

    /** Arabic-script letter (logical or presentation form). */
    private static boolean isStrongArabic(int c) {
        return isArabicScript(c) && Character.isLetter(c);
    }

    private static boolean isStrongLatin(int c) {
        return Character.isLetter(c) && !isArabicScript(c);
    }

    /** Characters that may sit inside an Arabic run of a left-to-right line. */
    private static boolean isRunMember(int c) {
        return !isStrongLatin(c) && c != '\n';
    }

    private static boolean isLtrWordChar(int c) {
        if (isArabicDigit(c)) return true;
        return (Character.isLetterOrDigit(c) && !isArabicScript(c)) || c == '_';
    }

    private static boolean isNumberSeparator(int c) {
        return c == '.' || c == ',' || c == ':' || c == '/' || c == '-' || c == '@';
    }

    private static boolean isFormatCodeAt(int[] cp, int i, int n) {
        return cp[i] == SECTION && i + 1 < n && isFormatCodeChar(cp[i + 1]);
    }

    private static boolean firstStrongIsArabic(int[] cp, int n) {
        for (int i = 0; i < n; i++) {
            if (isFormatCodeAt(cp, i, n)) {
                i++; // skip the code letter, it is not text
                continue;
            }
            if (isStrongArabic(cp[i])) return true;
            if (isStrongLatin(cp[i])) return false;
        }
        return true;
    }
}
