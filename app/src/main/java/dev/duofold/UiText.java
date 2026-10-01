package dev.duofold;

import java.util.Locale;

/**
 * Lightweight runtime localization: every call site passes the Chinese and English
 * text together and the current system language picks one. Keeps the codebase free
 * of a giant string-table refactor while answering the i18n request in Issue #2.
 */
public final class UiText {
    private static final boolean ENGLISH;
    static {
        String language=Locale.getDefault().getLanguage();
        ENGLISH=!"zh".equals(language);
    }
    private UiText() {}
    public static boolean isEnglish() { return ENGLISH; }
    /** Returns the English variant when the system language is not Chinese. */
    public static String t(String zh,String en) { return ENGLISH?en:zh; }
}
