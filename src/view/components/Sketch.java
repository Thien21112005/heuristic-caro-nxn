package view.components;

import java.awt.Shape;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;

/**
 * Hand-drawn shapes: lines and loops with a slight ballpoint wobble. The wobble
 * comes from a seed, so a shape looks the same on every repaint and never shimmers.
 */
public final class Sketch {
    private Sketch() {}

    /** A slowly wandering offset for position {@code t}; a few px at most, unique per seed. */
    private static double wobble(int seed, double t, double amount) {
        double a = 1.3 + (seed * 0.37 % 1.0 + 1.0) % 1.0;
        double b = 2.9 + (seed * 0.73 % 1.0 + 1.0) % 1.0;
        return amount * (0.6 * Math.sin(t * a * Math.PI * 2 + seed) + 0.4 * Math.sin(t * b * Math.PI * 2 + seed * 1.7));
    }

    /** A straight line as a pen draws it. */
    public static Path2D line(double x1, double y1, double x2, double y2, int seed, double amount) {
        Path2D.Float p = new Path2D.Float();
        addLine(p, x1, y1, x2, y2, seed, amount);
        return p;
    }

    /** Appends a wobbly line to {@code p}, so a whole grid can be one shape. */
    public static void addLine(Path2D p, double x1, double y1, double x2, double y2, int seed, double amount) {
        double len = Math.hypot(x2 - x1, y2 - y1);
        if (len == 0) return;
        double nx = -(y2 - y1) / len, ny = (x2 - x1) / len;
        int steps = Math.max(2, (int) (len / 24));
        p.moveTo(x1, y1);
        for (int i = 1; i <= steps; i++) {
            double t = i / (double) steps;
            // Both ends stay put, so lines still meet where they should
            double w = wobble(seed, t, amount) * Math.sin(Math.PI * t);
            p.lineTo(x1 + (x2 - x1) * t + nx * w, y1 + (y2 - y1) * t + ny * w);
        }
    }

    /** A closed box with slightly bowed sides; fill it for a button, stroke it for an outline. */
    public static Path2D box(double x, double y, double w, double h, double r, int seed) {
        double wob = Math.min(1.2, Math.min(w, h) / 30);
        double k = r * 0.45;
        double bowTop = wobble(seed, 0.3, wob), bowRight = wobble(seed + 1, 0.5, wob);
        double bowBottom = wobble(seed + 2, 0.7, wob), bowLeft = wobble(seed + 3, 0.9, wob);
        Path2D.Float p = new Path2D.Float();
        p.moveTo(x + r, y);
        p.quadTo(x + w / 2, y + bowTop, x + w - r, y);
        p.curveTo(x + w - k, y, x + w, y + k, x + w, y + r);
        p.quadTo(x + w + bowRight, y + h / 2, x + w, y + h - r);
        p.curveTo(x + w, y + h - k, x + w - k, y + h, x + w - r, y + h);
        p.quadTo(x + w / 2, y + h + bowBottom, x + r, y + h);
        p.curveTo(x + k, y + h, x, y + h - k, x, y + h - r);
        p.quadTo(x + bowLeft, y + h / 2, x, y + r);
        p.curveTo(x, y + k, x + k, y, x + r, y);
        p.closePath();
        return p;
    }

    /**
     * Circling an answer with a pen: a loop around the box that starts at the
     * upper left and overshoots its own start, as a quick hand does.
     */
    public static Path2D loop(double x, double y, double w, double h, int seed) {
        double cx = x + w / 2, cy = y + h / 2;
        double rx = w / 2, ry = h / 2;
        double amount = Math.min(1.6, Math.min(w, h) / 22);
        double start = -2.35;
        double sweep = Math.PI * 2 + 0.55;
        int steps = 72;
        // A superellipse hugs a wide word better than an ellipse, but a face gets a true circle
        double squareness = w > h * 1.4 ? 0.72 : 1.0;
        Path2D.Float p = new Path2D.Float();
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            double a = start + sweep * t;
            double c = Math.cos(a), s = Math.sin(a);
            double px = Math.signum(c) * Math.pow(Math.abs(c), squareness);
            double py = Math.signum(s) * Math.pow(Math.abs(s), squareness);
            double drift = 1 + 0.012 * (t - 0.5);
            double wx = wobble(seed, t, amount), wy = wobble(seed + 5, t, amount);
            double fx = cx + rx * px * drift + wx, fy = cy + ry * py * drift + wy;
            if (i == 0) p.moveTo(fx, fy);
            else p.lineTo(fx, fy);
        }
        return p;
    }

    /** A pen loop around the straight segment between two points, keeping {@code radius} clear of it. */
    public static Path2D stadium(double x1, double y1, double x2, double y2, double radius, int seed) {
        double angle = Math.atan2(y2 - y1, x2 - x1);
        double len = Math.hypot(x2 - x1, y2 - y1);
        double cos = Math.cos(angle), sin = Math.sin(angle);
        double perimeter = 2 * len + 2 * Math.PI * radius;
        double amount = Math.min(3.2, radius / 13);
        int steps = Math.max(48, (int) (perimeter / 6));
        double overshoot = 0.06;
        Path2D.Float p = new Path2D.Float();
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            double d = (t * (1 + overshoot) % 1.0) * perimeter;
            double lx, ly;
            if (d < len) {                                 // far side, start to end
                lx = d;
                ly = -radius;
            } else if (d < len + Math.PI * radius) {       // round the end
                double a = -Math.PI / 2 + (d - len) / radius;
                lx = len + radius * Math.cos(a);
                ly = radius * Math.sin(a);
            } else if (d < 2 * len + Math.PI * radius) {   // near side, end to start
                lx = len - (d - len - Math.PI * radius);
                ly = radius;
            } else {                                       // round the start
                double a = Math.PI / 2 + (d - 2 * len - Math.PI * radius) / radius;
                lx = radius * Math.cos(a);
                ly = radius * Math.sin(a);
            }
            lx += wobble(seed, t * 3, amount);
            ly += wobble(seed + 5, t * 3, amount);
            double fx = x1 + lx * cos - ly * sin, fy = y1 + lx * sin + ly * cos;
            if (i == 0) p.moveTo(fx, fy);
            else p.lineTo(fx, fy);
        }
        return p;
    }

    /** The first {@code fraction} of a shape's length, for drawing it stroke by stroke. */
    public static Path2D partial(Shape shape, float fraction) {
        double total = length(shape);
        double remaining = total * Math.max(0f, Math.min(1f, fraction));
        Path2D.Float out = new Path2D.Float();
        double[] c = new double[6];
        double lastX = 0, lastY = 0;
        PathIterator it = new FlatteningPathIterator(shape.getPathIterator(null), 0.5);
        while (!it.isDone() && remaining > 0) {
            int type = it.currentSegment(c);
            if (type == PathIterator.SEG_MOVETO) {
                out.moveTo(c[0], c[1]);
            } else if (type == PathIterator.SEG_LINETO) {
                double seg = Math.hypot(c[0] - lastX, c[1] - lastY);
                if (seg > remaining) {
                    double k = remaining / seg;
                    out.lineTo(lastX + (c[0] - lastX) * k, lastY + (c[1] - lastY) * k);
                    return out;
                }
                out.lineTo(c[0], c[1]);
                remaining -= seg;
            }
            if (type != PathIterator.SEG_CLOSE) {
                lastX = c[0];
                lastY = c[1];
            }
            it.next();
        }
        return out;
    }

    private static double length(Shape shape) {
        double[] c = new double[6];
        double lastX = 0, lastY = 0, total = 0;
        PathIterator it = new FlatteningPathIterator(shape.getPathIterator(null), 0.5);
        while (!it.isDone()) {
            int type = it.currentSegment(c);
            if (type == PathIterator.SEG_LINETO) total += Math.hypot(c[0] - lastX, c[1] - lastY);
            if (type != PathIterator.SEG_CLOSE) {
                lastX = c[0];
                lastY = c[1];
            }
            it.next();
        }
        return total;
    }
}
