package view.theme;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.fonts.inter.FlatInterFont;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Central design tokens (colors, fonts, spacing) and FlatLaf setup.
 * Every screen pulls its look from here so the app stays visually consistent.
 */
public final class Theme {
    // Surfaces
    public static final Color BG = new Color(0x0B0D1A);
    public static final Color SURFACE = new Color(0x151831);
    public static final Color SURFACE_ALT = new Color(0x1E2245);
    public static final Color BORDER = new Color(0x2C3160);

    // Text
    public static final Color TEXT = new Color(0xECEEFA);
    public static final Color TEXT_MUTED = new Color(0x969CC4);

    // Accents
    public static final Color ACCENT = new Color(0x7C5CFF);
    public static final Color ACCENT_HOVER = new Color(0x947BFF);
    public static final Color ACCENT_PRESSED = new Color(0x6646F0);
    public static final Color X_COLOR = new Color(0x22D3EE);
    public static final Color O_COLOR = new Color(0xFF5A36);
    public static final Color WIN = new Color(0xFACC15);
    public static final Color DANGER = new Color(0xF43F5E);

    // Shape
    public static final int RADIUS = 14;
    public static final int RADIUS_LG = 22;

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

        FlatDarkLaf.setup();
        view.components.FocusRing.install();

        UIManager.put("defaultFont", font(Font.PLAIN, 14));
        UIManager.put("Component.arc", RADIUS);
        UIManager.put("Button.arc", RADIUS);
        UIManager.put("TextComponent.arc", RADIUS);
        UIManager.put("Component.focusWidth", 1);
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
        UIManager.put("ToolTip.background", SURFACE_ALT);
        UIManager.put("ToolTip.foreground", TEXT);
        UIManager.put("ToolTip.border", BorderFactory.createEmptyBorder(6, 10, 6, 10));
    }

    public static Font font(int style, float size) {
        return new Font(FlatInterFont.FAMILY, style, 1).deriveFont(size);
    }

    public static Font semibold(float size) {
        return new Font(FlatInterFont.FAMILY_SEMIBOLD, Font.PLAIN, 1).deriveFont(size);
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

    private static String hex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }
}
