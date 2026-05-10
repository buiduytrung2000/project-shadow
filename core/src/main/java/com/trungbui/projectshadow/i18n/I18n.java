package com.trungbui.projectshadow.i18n;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Sprint 9 — runtime i18n via Java {@link ResourceBundle}.
 *
 * <p>Default locale is Vietnamese ({@code vi}); switch to English with
 * {@link #setLocale(Locale)}. Bundle keys live in
 * {@code core/src/main/resources/messages/messages[_en].properties}.</p>
 *
 * <p>Missing keys return a sentinel like {@code "??key.name??"} so the developer
 * can spot them on screen rather than crashing the UI.</p>
 */
public final class I18n {

    private static final String BUNDLE_PATH = "messages.messages";

    /**
     * Disable JVM default-locale fallback. Without this, {@code getBundle(path, vi)} on a
     * machine with system locale=en would load messages_en.properties before falling
     * back to the root messages.properties.
     */
    private static final ResourceBundle.Control NO_FALLBACK =
            ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_DEFAULT);

    public static final Locale VI = new Locale("vi");
    public static final Locale EN = new Locale("en");

    private static Locale current = VI;
    private static ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_PATH, current, NO_FALLBACK);

    private I18n() {
    }

    public static String t(String key, Object... args) {
        try {
            String pattern = bundle.getString(key);
            return args == null || args.length == 0 ? pattern : MessageFormat.format(pattern, args);
        } catch (MissingResourceException e) {
            return "??" + key + "??";
        }
    }

    public static void setLocale(Locale locale) {
        if (locale == null) throw new IllegalArgumentException("locale must not be null");
        current = locale;
        bundle = ResourceBundle.getBundle(BUNDLE_PATH, locale, NO_FALLBACK);
    }

    public static Locale currentLocale() {
        return current;
    }

    public static boolean isEnglish() {
        return "en".equals(current.getLanguage());
    }

    /** Toggle between VN and EN. Returns the new locale. */
    public static Locale toggleLocale() {
        setLocale(isEnglish() ? VI : EN);
        return current;
    }
}
