package utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class I18nTest {

    private static Properties load(String name) throws IOException {
        Properties p = new Properties();
        try (InputStream in = I18nTest.class.getResourceAsStream("/i18n/" + name)) {
            assertNotNull(in, name + " not on the classpath");
            p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        return p;
    }

    @AfterEach
    void resetLanguage() {
        I18n.setLanguage(I18n.ENGLISH);
    }

    @Test
    void bothBundlesDefineTheSameKeys() throws IOException {
        Set<String> en = load("messages.properties").stringPropertyNames();
        Set<String> vi = load("messages_vi.properties").stringPropertyNames();

        Set<String> missingInVi = new TreeSet<>(en);
        missingInVi.removeAll(vi);
        Set<String> missingInEn = new TreeSet<>(vi);
        missingInEn.removeAll(en);

        assertTrue(missingInVi.isEmpty(), "Missing Vietnamese translations: " + missingInVi);
        assertTrue(missingInEn.isEmpty(), "Keys only in Vietnamese: " + missingInEn);
    }

    @Test
    void everyKeyUsedInSourceExists() throws IOException {
        Set<String> keys = load("messages.properties").stringPropertyNames();
        Pattern call = Pattern.compile("I18n\\.t\\(\"([a-zA-Z0-9_.]+)\"");
        Set<String> missing = new TreeSet<>();
        try (Stream<Path> files = Files.walk(Paths.get("src"))) {
            for (Path file : (Iterable<Path>) files.filter(f -> f.toString().endsWith(".java"))::iterator) {
                Matcher m = call.matcher(Files.readString(file));
                while (m.find()) {
                    if (!keys.contains(m.group(1))) missing.add(m.group(1) + " (" + file.getFileName() + ")");
                }
            }
        }
        assertTrue(missing.isEmpty(), "Keys used in code but not defined: " + missing);
    }

    @Test
    void parameterizedStringsFormatCleanly() throws IOException {
        for (String bundle : List.of("messages.properties", "messages_vi.properties")) {
            Properties p = load(bundle);
            for (String key : p.stringPropertyNames()) {
                String value = p.getProperty(key);
                if (!value.contains("{0")) continue;
                String formatted = MessageFormat.format(value, 3, "B", "C");
                assertFalse(formatted.contains("{"), bundle + " " + key + " left a placeholder: " + formatted);
                // A lone apostrophe is swallowed by MessageFormat; it must be written as ''
                assertFalse(value.replace("''", "").contains("'"), bundle + " " + key + " has an unescaped apostrophe");
            }
        }
    }

    @Test
    void switchesLanguageAndFormatsArguments() {
        I18n.setLanguage(I18n.VIETNAMESE);
        assertEquals("Cài đặt", I18n.t("settings.title"));
        I18n.setLanguage(I18n.ENGLISH);
        assertEquals("Player 1's turn — place an X", I18n.t("game.status.turn", "Player 1", "X"));
        assertEquals("2 draws", I18n.t("game.result.draws", 2));
        assertEquals("1 draw", I18n.t("game.result.draws", 1));
    }

    @Test
    void unknownKeysAreVisibleInsteadOfCrashing() {
        assertEquals("!no.such.key!", I18n.t("no.such.key"));
    }
}
