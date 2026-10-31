package view.theme;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.fonts.inter.FlatInterFont;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Central design tokens (colors, fonts, spacing) and FlatLaf setup.
 * The look is a page from a school notebook: cool paper, ink, a violet ballpoint,
 * the teacher's red pen and a yellow highlighter.
 */
public final class Theme {
    // Paper
    public static final Color BG = new Color(0xF7F8FC);
    public static final Color SURFACE = new Color(0xFFFFFF);
    public static final Color SURFACE_ALT = new Color(0xEBEDF8);
    public static final Color RULE = new Color(0xE2E5F4);      // the faint printed squares
    public static final Color RULE_FINE = new Color(0xF0F1F9); // the small squares inside them
    public static final Color MARGIN = new Color(0xE88C9A);    // the printed margin line
    public static final Color BORDER = new Color(0x7C85B3);    // outlines of controls and board lines, at least 3:1

    // Ink
    public static final Color TEXT = new Color(0x1E2A5A);
    public static final Color TEXT_MUTED = new Color(0x4E5A86);
    public static final Color TEXT_DISABLED = new Color(0x8A91B3);

    // Pens
    public static final Color ACCENT = new Color(0x4B2FB5);
    public static final Color ACCENT_HOVER = new Color(0x5C3FCB);
    public static final Color ACCENT_PRESSED = new Color(0x3D2596);
    public static final Color X_COLOR = new Color(0x0B7285);
    public static final Color O_COLOR = new Color(0xC2410C);
    public static final Color WIN = new Color(0xFFE45C);       // highlighter
    public static final Color DANGER = new Color(0xC62828);    // red pen
    public static final Color DANGER_PRESSED = new Color(0xA31F1F);
    public static final Color SCRIM = new Color(0x1E2A5A);     // tint behind dialogs

    // Shape
    public static final int RADIUS = 10;
    public static final int RADIUS_LG = 14;

    // Type scale
    public static final float SMALL = 13f;
    public static final float BODY = 15f;
    public static final float LEAD = 17f;
    public static final float H3 = 20f;
    public static final float H2 = 28f;
    public static final float NUMBER = 48f;
    public static final float DISPLAY = 88f;

    // Mali, a Vietnamese school handwriting face; null if a file could not be read
    private static final Font HAND = loadFont("Mali-SemiBold.ttf");
    private static final Font HAND_BOLD = loadFont("Mali-Bold.ttf");

    private Theme() {}

    public static void install() {
        FlatInterFont.install();
        FlatLaf.setPreferredFontFamily(FlatInterFont.FAMILY);
        FlatLaf.setPreferredLightFontFamily(FlatInterFont.FAMILY_LIGHT);
        FlatLaf.setPreferredSemiboldFontFamily(FlatInterFont.FAMILY_SEMIBOLD);

        Map<String, String> vars = new HashMap<>();
        vars.put("@background", hex(BG));
        vars.put("@foreground", hex(TEXT));
        vars.put("@accentColor", hex(ACCENT));
        FlatLaf.setGlobalExtraDefaults(vars);

        FlatLightLaf.setup();
        UIManager.put("LabelUI", RoomyLabelUI.class.getName());
        view.components.FocusRing.install();

        UIManager.put("defaultFont", font(Font.PLAIN, BODY));
        UIManager.put("Component.arc", RADIUS);
        UIManager.put("Button.arc", RADIUS);
        UIManager.put("TextComponent.arc", RADIUS);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.borderColor", BORDER);
        UIManager.put("Component.focusedBorderColor", ACCENT);
        UIManager.put("Component.focusColor", ACCENT);
        UIManager.put("Button.disabledText", TEXT_DISABLED);
        UIManager.put("Label.disabledForeground", TEXT_DISABLED);
        UIManager.put("TextField.background", SURFACE);
        UIManager.put("TextField.placeholderForeground", TEXT_MUTED);
        UIManager.put("Slider.trackColor", BORDER);
        UIManager.put("Slider.trackValueColor", TEXT);
        UIManager.put("Slider.thumbColor", TEXT);
        UIManager.put("Slider.disabledTrackColor", RULE);
        UIManager.put("Slider.disabledThumbColor", TEXT_DISABLED);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("ScrollBar.track", new Color(0, 0, 0, 0));
        UIManager.put("ScrollBar.thumb", BORDER);
        UIManager.put("ScrollPane.border", BorderFactory.createEmptyBorder());
        UIManager.put("TitlePane.background", BG);
        UIManager.put("TitlePane.inactiveBackground", BG);
        UIManager.put("TitlePane.foreground", TEXT_MUTED);
        UIManager.put("TitlePane.unifiedBackground", true);
        UIManager.put("ToolTip.background", TEXT);
        UIManager.put("ToolTip.foreground", SURFACE);
        UIManager.put("ToolTip.border", BorderFactory.createEmptyBorder(6, 10, 6, 10));
    }

    /** Small body text: Inter at the exact size. */
    public static Font font(int style, float size) {
        return new Font(FlatInterFont.FAMILY, style, Math.round(size));
    }

    public static Font semibold(float size) {
        return new Font(FlatInterFont.FAMILY_SEMIBOLD, Font.PLAIN, Math.round(size));
    }

    /** Handwriting for buttons, labels and short lines. */
    public static Font hand(float size) {
        return HAND != null ? HAND.deriveFont(size) : semibold(size);
    }

    /** Heavier handwriting for titles and big numbers. */
    public static Font handBold(float size) {
        return HAND_BOLD != null ? HAND_BOLD.deriveFont(size) : font(Font.BOLD, size);
    }

    public static Color alpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    public static void antialias(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    /** CSS form of a colour, for labels that render HTML. */
    public static String hex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    private static Font loadFont(String file) {
        try (InputStream in = Theme.class.getResourceAsStream("/assets/fonts/" + file)) {
            return in == null ? null : Font.createFont(Font.TRUETYPE_FONT, in);
        } catch (IOException | FontFormatException e) {
            return null;
        }
    }
}
