package view.components;

import org.junit.jupiter.api.Test;

import java.awt.Rectangle;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SketchTest {
    @Test
    void sameSeedDrawsTheSameShapeSoNothingShimmers() {
        assertEquals(points(Sketch.loop(0, 0, 100, 40, 7)), points(Sketch.loop(0, 0, 100, 40, 7)));
        assertEquals(points(Sketch.stadium(0, 0, 100, 100, 20, 3)), points(Sketch.stadium(0, 0, 100, 100, 20, 3)));
    }

    @Test
    void differentSeedsDrawDifferentShapes() {
        assertTrue(!points(Sketch.loop(0, 0, 100, 40, 1)).equals(points(Sketch.loop(0, 0, 100, 40, 2))));
    }

    @Test
    void lineKeepsItsEndsFixedSoGridLinesMeet() {
        List<double[]> p = coords(Sketch.line(10, 20, 210, 20, 5, 2));
        assertEquals(10, p.get(0)[0], 0.01);
        assertEquals(20, p.get(0)[1], 0.01);
        assertEquals(210, p.get(p.size() - 1)[0], 0.01);
        assertEquals(20, p.get(p.size() - 1)[1], 0.01);
    }

    @Test
    void winLoopStaysClearOfTheLine() {
        // The loop must not cross the segment it surrounds, or it would strike through the faces
        double radius = 30;
        for (double[] p : coords(Sketch.stadium(100, 100, 300, 100, radius, 11))) {
            double dx = p[0] < 100 ? 100 - p[0] : p[0] > 300 ? p[0] - 300 : 0;
            assertTrue(Math.hypot(dx, p[1] - 100) > radius * 0.85, "too close at " + p[0] + "," + p[1]);
        }
    }

    @Test
    void partialStopsPartWayAndFullCoversTheWholeShape() {
        Path2D line = Sketch.line(0, 0, 200, 0, 1, 0);
        Rectangle half = Sketch.partial(line, 0.5f).getBounds();
        assertEquals(100, half.width, 3);
        assertEquals(200, Sketch.partial(line, 1f).getBounds().width, 3);
        assertEquals(0, Sketch.partial(line, 0f).getBounds().width);
    }

    private static List<String> points(Path2D shape) {
        List<String> out = new ArrayList<>();
        for (double[] p : coords(shape)) out.add(p[0] + "," + p[1]);
        return out;
    }

    private static List<double[]> coords(Path2D shape) {
        List<double[]> out = new ArrayList<>();
        double[] c = new double[6];
        for (PathIterator it = shape.getPathIterator(null); !it.isDone(); it.next()) {
            if (it.currentSegment(c) != PathIterator.SEG_CLOSE) out.add(new double[]{c[0], c[1]});
        }
        return out;
    }
}
