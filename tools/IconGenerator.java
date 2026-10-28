import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Draws the app icon (cyan X + orange O on a violet tile) at every size Windows asks for.
 * Usage: java tools/IconGenerator.java <outDir>
 */
public class IconGenerator {
    static final int[] SIZES = {16, 24, 32, 48, 64, 128, 256};

    public static void main(String[] args) throws Exception {
        File dir = new File(args.length > 0 ? args[0] : "src/assets/icon");
        dir.mkdirs();
        for (int s : SIZES) {
            ImageIO.write(draw(s), "png", new File(dir, "icon-" + s + ".png"));
        }
        System.out.println("Wrote " + SIZES.length + " icons to " + dir);
    }

    static BufferedImage draw(int size) {
        // Render big then downscale once for crisp small sizes
        int big = 512;
        BufferedImage img = new BufferedImage(big, big, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        float pad = big * 0.04f;
        RoundRectangle2D tile = new RoundRectangle2D.Float(pad, pad, big - 2 * pad, big - 2 * pad, big * 0.42f, big * 0.42f);
        g.setPaint(new GradientPaint(0, 0, new Color(0x2A1F6B), big, big, new Color(0x0F1230)));
        g.fill(tile);
        g.setColor(new Color(0x7C5CFF));
        g.setStroke(new BasicStroke(big * 0.025f));
        g.draw(tile);

        float stroke = big * 0.11f;
        g.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // X in the upper-left
        float x0 = big * 0.18f, y0 = big * 0.18f, len = big * 0.30f;
        g.setColor(new Color(0x22D3EE));
        g.draw(new Line2D.Float(x0, y0, x0 + len, y0 + len));
        g.draw(new Line2D.Float(x0 + len, y0, x0, y0 + len));

        // O in the lower-right
        float d = big * 0.32f, ox = big * 0.82f - d, oy = big * 0.82f - d;
        g.setColor(new Color(0xFF5A36));
        g.draw(new Ellipse2D.Float(ox, oy, d, d));
        g.dispose();

        BufferedImage cur = img;
        int w = big;
        while (w / 2 >= size) {
            w /= 2;
            cur = scale(cur, w);
        }
        return w == size ? cur : scale(cur, size);
    }

    static BufferedImage scale(BufferedImage src, int s) {
        BufferedImage out = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, s, s, null);
        g.dispose();
        return out;
    }
}
