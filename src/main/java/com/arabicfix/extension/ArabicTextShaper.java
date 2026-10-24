package com.arabicfix.extension;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class ArabicTextShaper {

    private static boolean isArabicChar(char c) {
        return (c >= '\u0600' && c <= '\u06FF')
            || (c >= '\u0750' && c <= '\u077F')
            || (c >= '\u08A0' && c <= '\u08FF')
            || (c >= '\uFB50' && c <= '\uFDFF')
            || (c >= '\uFE70' && c <= '\uFEFF');
    }

    public static boolean containsArabic(String text) {
        if (text == null) return false;
        for (int i = 0; i < text.length(); i++) {
            if (isArabicChar(text.charAt(i))) return true;
        }
        return false;
    }

    private static final Map<Character, char[]> FORMS = new HashMap<>();

    static {
        f('\u0627', '\uFE8D', '\uFE8E', '\0', '\0');
        f('\u0622', '\uFE81', '\uFE82', '\0', '\0');
        f('\u0623', '\uFE83', '\uFE84', '\0', '\0');
        f('\u0625', '\uFE87', '\uFE88', '\0', '\0');
        f('\u0624', '\uFE85', '\uFE86', '\0', '\0');
        f('\u0621', '\uFE80', '\0',     '\0', '\0');
        f('\u0629', '\uFE93', '\uFE94', '\0', '\0');
        f('\u0649', '\uFEEF', '\uFEF0', '\0', '\0');
        f('\u062F', '\uFEA9', '\uFEAA', '\0', '\0');
        f('\u0630', '\uFEAB', '\uFEAC', '\0', '\0');
        f('\u0631', '\uFEAD', '\uFEAE', '\0', '\0');
        f('\u0632', '\uFEAF', '\uFEB0', '\0', '\0');
        f('\u0648', '\uFEED', '\uFEEE', '\0', '\0');
        f('\u0628', '\uFE8F', '\uFE90', '\uFE91', '\uFE92');
        f('\u062A', '\uFE95', '\uFE96', '\uFE97', '\uFE98');
        f('\u062B', '\uFE99', '\uFE9A', '\uFE9B', '\uFE9C');
        f('\u062C', '\uFE9D', '\uFE9E', '\uFE9F', '\uFEA0');
        f('\u062D', '\uFEA1', '\uFEA2', '\uFEA3', '\uFEA4');
        f('\u062E', '\uFEA5', '\uFEA6', '\uFEA7', '\uFEA8');
        f('\u0633', '\uFEB1', '\uFEB2', '\uFEB3', '\uFEB4');
        f('\u0634', '\uFEB5', '\uFEB6', '\uFEB7', '\uFEB8');
        f('\u0635', '\uFEB9', '\uFEBA', '\uFEBB', '\uFEBC');
        f('\u0636', '\uFEBD', '\uFEBE', '\uFEBF', '\uFEC0');
        f('\u0637', '\uFEC1', '\uFEC2', '\uFEC3', '\uFEC4');
        f('\u0638', '\uFEC5', '\uFEC6', '\uFEC7', '\uFEC8');
        f('\u0639', '\uFEC9', '\uFECA', '\uFECB', '\uFECC');
        f('\u063A', '\uFECD', '\uFECE', '\uFECF', '\uFED0');
        f('\u0641', '\uFED1', '\uFED2', '\uFED3', '\uFED4');
        f('\u0642', '\uFED5', '\uFED6', '\uFED7', '\uFED8');
        f('\u0643', '\uFED9', '\uFEDA', '\uFEDB', '\uFEDC');
        f('\u0644', '\uFEDD', '\uFEDE', '\uFEDF', '\uFEE0');
        f('\u0645', '\uFEE1', '\uFEE2', '\uFEE3', '\uFEE4');
        f('\u0646', '\uFEE5', '\uFEE6', '\uFEE7', '\uFEE8');
        f('\u0647', '\uFEE9', '\uFEEA', '\uFEEB', '\uFEEC');
        f('\u064A', '\uFEF1', '\uFEF2', '\uFEF3', '\uFEF4');
        f('\u0626', '\uFE89', '\uFE8A', '\uFE8B', '\uFE8C');
    }

    private static void f(char letter, char iso, char fin, char ini, char med) {
        FORMS.put(letter, new char[]{ iso, fin, ini, med });
    }

    private static final Set<Character> NO_CONNECT_NEXT = new HashSet<>();
    static {
        for (char c : new char[]{
            '\u0627','\u0622','\u0623','\u0625','\u0624','\u0621',
            '\u0629','\u0649','\u062F','\u0630','\u0631','\u0632','\u0648',
            '\uFEFB','\uFEF7','\uFEF9','\uFEF5'
        }) NO_CONNECT_NEXT.add(c);
    }

    public static String shape(String text) {
        if (!containsArabic(text)) return text;
        if (text.contains("\n")) {
            String[] lines = text.split("\n", -1);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < lines.length; i++) {
                if (i > 0) sb.append('\n');
                sb.append(shapeLine(lines[i]));
            }
            return sb.toString();
        }
        return shapeLine(text);
    }

    private static String shapeLine(String text) {
        if (text == null || text.isEmpty()) return text;
        if (!containsArabic(text)) return text;
        try {
            text = stripDiacritics(text);
            text = processLamAlef(text);
            text = applyLetterForms(text);
            text = reverseForRTL(text);
            return text;
        } catch (Exception e) {
            return text;
        }
    }

    private static String stripDiacritics(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < '\u064B' || c > '\u0670') sb.append(c);
        }
        return sb.toString();
    }

    private static String processLamAlef(String text) {
        return text
            .replace("\u0644\u0627", "\uFEFB")
            .replace("\u0644\u0623", "\uFEF7")
            .replace("\u0644\u0625", "\uFEF9")
            .replace("\u0644\u0622", "\uFEF5");
    }

    private static String applyLetterForms(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            char[] forms = FORMS.get(c);
            if (forms == null) { sb.append(c); continue; }
            char prev = i > 0 ? text.charAt(i - 1) : '\0';
            char next = i < text.length() - 1 ? text.charAt(i + 1) : '\0';
            boolean connectsPrev = isArabicChar(prev) && !isPunct(prev)
                && !NO_CONNECT_NEXT.contains(prev);
            boolean connectsNext = isArabicChar(next) && !isPunct(c) && !isPunct(next)
                && !NO_CONNECT_NEXT.contains(c);
            char form;
            if (!connectsPrev && !connectsNext)      form = forms[0];
            else if (!connectsPrev)                  form = forms[2] != '\0' ? forms[2] : forms[0];
            else if (!connectsNext)                  form = forms[1] != '\0' ? forms[1] : forms[0];
            else                                     form = forms[3] != '\0' ? forms[3] : forms[0];
            sb.append(form);
        }
        return sb.toString();
    }

    private static String reverseForRTL(String text) {
        String rev = new StringBuilder(text).reverse().toString();
        StringBuilder sb = new StringBuilder(rev.length());
        for (int i = 0; i < rev.length(); i++) {
            char c = rev.charAt(i);
            if (c == '\u00A7' && i > 0) {
                char codeChar = sb.charAt(sb.length() - 1);
                sb.setCharAt(sb.length() - 1, '\u00A7');
                sb.append(codeChar);
            } else {
                sb.append(c);
            }
        }
        rev = sb.toString();
        rev = restoreLatinRuns(rev);
        rev = mirrorBrackets(rev);
        return rev;
    }

    private static String restoreLatinRuns(String text) {
        StringBuilder result = new StringBuilder(text.length());
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (isLatinOrDigit(c)) {
                int start = i;
                while (i < text.length() && isLatinOrDigit(text.charAt(i))) i++;
                for (int j = i - 1; j >= start; j--) result.append(text.charAt(j));
            } else {
                result.append(c);
                i++;
            }
        }
        return result.toString();
    }

    private static boolean isLatinOrDigit(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
            || (c >= '0' && c <= '9') || c == '_'
            || (c >= '\u0660' && c <= '\u0669');
    }

    private static String mirrorBrackets(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '(' -> sb.append(')');
                case ')' -> sb.append('(');
                case '[' -> sb.append(']');
                case ']' -> sb.append('[');
                case '{' -> sb.append('}');
                case '}' -> sb.append('{');
                case '<' -> sb.append('>');
                case '>' -> sb.append('<');
                default  -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private static boolean isPunct(char c) {
        return "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~".indexOf(c) >= 0;
    }
}
