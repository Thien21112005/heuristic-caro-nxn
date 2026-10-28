package utils;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Looks up UI text from i18n/messages*.properties (UTF-8).
 * Strings with {0}-style arguments go through MessageFormat, so a literal
 * apostrophe in those strings must be written as two ('').
 */
public final class I18n {
    public static final String ENGLISH = "en";
    public static final String VIETNAMESE = "vi";

    private static final String BUNDLE = "i18n.messages";
    // Without this control a missing Vietnamese key would fall back to the OS locale first
    private static final ResourceBundle.Control NO_FALLBACK =
            ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES);

    private static String language = ENGLISH;
    private static ResourceBundle bundle = load(ENGLISH);

    private I18n() {}

    public static void setLanguage(String lang) {
        language = VIETNAMESE.equals(lang) ? VIETNAMESE : ENGLISH;
        bundle = load(language);
    }

    public static String getLanguage() { return language; }

    public static String t(String key, Object... args) {
        String value;
        try {
            value = bundle.getString(key);
        } catch (MissingResourceException e) {
            System.err.println("Missing i18n key: " + key);
            return "!" + key + "!";
        }
        return args.length == 0 ? value : MessageFormat.format(value, args);
    }

    private static ResourceBundle load(String lang) {
        Locale locale = ENGLISH.equals(lang) ? Locale.ROOT : Locale.forLanguageTag(lang);
        return ResourceBundle.getBundle(BUNDLE, locale, NO_FALLBACK);
    }
}
