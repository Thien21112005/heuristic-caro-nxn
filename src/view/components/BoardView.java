package view.components;

import controller.GameController;
import model.GameModel;
import utils.ResourceUtils;
import view.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * The game board, painted as a single component: hover preview, last-move
 * marker, pop-in animation, glowing win line, wheel zoom and drag to pan.
 */
public class BoardView extends JComponent {
    public static final int MIN_CELL = 26;
    public static final int MAX_CELL = 120;
    private static final int PAD = 14;
    private static final int ANIM_MS = 220;

    private final GameController controller;
    private final GameModel model;
    private final int n;

    private final Image xImage = ResourceUtils.getImageIcon("/assets/x_image_funny.png").getImage();
    private final Image oImage = ResourceUtils.getImageIcon("/assets/o_image_funny.png").getImage();
    private final Map<Integer, Image> xCache = new HashMap<>();
    private final Map<Integer, Image> oCache = new HashMap<>();

    private int cell = 48;
    private int hoverRow = -1;
    private int hoverCol = -1;

    private int[] animatedMove;
    private long animStart;
    private final Timer animTimer;

    private Point pressPoint;
    private boolean dragged;
    private Runnable onZoom = () -> {};

    public BoardView(GameController controller) {
        this.controller = controller;
        this.model = controller.getModel();
        this.n = model.getSize();
        setOpaque(false);

        animTimer = new Timer(15, e -> {
            if (System.currentTimeMillis() - animStart >= ANIM_MS) {
                animatedMove = null;
                ((Timer) e.getSource()).stop();
            }
            repaint();
        });

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                pressPoint = e.getLocationOnScreen();
                dragged = false;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (pressPoint == null) return;
                Point now = e.getLocationOnScreen();
                int dx = now.x - pressPoint.x;
                int dy = now.y - pressPoint.y;
                if (!dragged && Math.abs(dx) + Math.abs(dy) < 5) return;
                dragged = true;
                setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                pan(-dx, -dy);
                pressPoint = now;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!dragged && SwingUtilities.isLeftMouseButton(e)) {
                    int[] rc = cellAt(e.getPoint());
                    if (rc != null) controller.play(rc[0], rc[1]);
                }
                pressPoint = null;
                dragged = false;
                updateHover(e.getPoint());
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                updateHover(e.getPoint());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoverRow = hoverCol = -1;
                repaint();
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                int step = Math.max(2, cell / 8);
                zoomAt(cell - e.getWheelRotation() * step, e.getPoint());
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addMouseWheelListener(mouse);
    }

    public void setOnZoom(Runnable onZoom) { this.onZoom = onZoom; }

    public int getCellSize() { return cell; }

    /** Call after every model change; animates the newest piece. */
    public void stateChanged() {
        int[] last = model.getLastMove();
        if (last != null && (animatedMove == null || last[0] != animatedMove[0] || last[1] != animatedMove[1])) {
            animatedMove = last;
            animStart = System.currentTimeMillis();
            animTimer.restart();
        } else if (last == null) {
            animatedMove = null;
        }
        repaint();
    }

    /** Largest cell size that shows the whole board inside the given area. */
    public static int fitCellSize(int n, Dimension area) {
        int byW = (area.width - 2 * PAD) / n;
        int byH = (area.height - 2 * PAD) / n;
        return Math.max(MIN_CELL, Math.min(MAX_CELL, Math.min(byW, byH)));
    }

    public void setCellSize(int size) {
        size = Math.max(MIN_CELL, Math.min(MAX_CELL, size));
        if (size == cell) return;
        cell = size;
        revalidate();
        repaint();
    }

    /** Zoom keeping the board point under {@code anchor} fixed on screen. */
    public void zoomAt(int newSize, Point anchor) {
        JViewport vp = viewport();
        int old = cell;
        setCellSize(newSize);
        if (cell == old) return;
        onZoom.run();
        if (vp == null) return;
        double fx = (double) anchor.x / getPreferredWidth(old);
        double fy = (double) anchor.y / getPreferredWidth(old);
        Point onViewport = SwingUtilities.convertPoint(this, anchor, vp);
        SwingUtilities.invokeLater(() -> {
            Point origin = SwingUtilities.convertPoint(this, 0, 0, vp.getView());
            int x = (int) (origin.x + fx * getWidth()) - onViewport.x;
            int y = (int) (origin.y + fy * getHeight()) - onViewport.y;
            setViewPosition(vp, x, y);
        });
    }

    @Override
    public Dimension getPreferredSize() {
        int s = getPreferredWidth(cell);
        return new Dimension(s, s);
    }

    private int getPreferredWidth(int cellSize) {
        return n * cellSize + 2 * PAD;
    }

    private void pan(int dx, int dy) {
        JViewport vp = viewport();
        if (vp == null) return;
        Point p = vp.getViewPosition();
        setViewPosition(vp, p.x + dx, p.y + dy);
    }

    private static void setViewPosition(JViewport vp, int x, int y) {
        Dimension view = vp.getView().getSize();
        Dimension extent = vp.getExtentSize();
        x = Math.max(0, Math.min(x, view.width - extent.width));
        y = Math.max(0, Math.min(y, view.height - extent.height));
        vp.setViewPosition(new Point(x, y));
    }

    private JViewport viewport() {
        return (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, this);
    }

    private int[] cellAt(Point p) {
        int c = Math.floorDiv(p.x - PAD, cell);
        int r = Math.floorDiv(p.y - PAD, cell);
        if (r < 0 || r >= n || c < 0 || c >= n) return null;
        return new int[]{r, c};
    }

    private void updateHover(Point p) {
        int[] rc = cellAt(p);
        int r = rc == null ? -1 : rc[0];
        int c = rc == null ? -1 : rc[1];
        boolean playable = rc != null && controller.acceptsInput() && model.getCell(r, c) == GameModel.EMPTY;
        if (!dragged) {
            setCursor(Cursor.getPredefinedCursor(playable ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        }
        if (r != hoverRow || c != hoverCol) {
            hoverRow = r;
            hoverCol = c;
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.antialias(g2);
        int size = n * cell;
        int arc = Math.min(Theme.RADIUS_LG, cell);

        // Board surface
        g2.setColor(Theme.alpha(Color.BLACK, 70));
        g2.fillRoundRect(PAD - 2, PAD + 4, size + 4, size + 4, arc, arc);
        g2.setPaint(new GradientPaint(0, PAD, new Color(0x1B1F3F), 0, PAD + size, new Color(0x131631)));
        g2.fillRoundRect(PAD, PAD, size, size, arc, arc);

        // Winning cells highlight
        int[] win = model.getWinningLine();
        boolean[][] winning = winningCells(win);
        if (win != null) {
            g2.setColor(Theme.alpha(Theme.WIN, 38));
            for (int r = 0; r < n; r++)
                for (int c = 0; c < n; c++)
                    if (winning[r][c]) g2.fillRect(PAD + c * cell, PAD + r * cell, cell, cell);
        }

        // Hover preview
        boolean canPlay = controller.acceptsInput();
        int current = model.getCurrentPlayer();
        if (canPlay && hoverRow >= 0 && model.getCell(hoverRow, hoverCol) == GameModel.EMPTY) {
            int x = PAD + hoverCol * cell, y = PAD + hoverRow * cell;
            g2.setColor(Theme.alpha(colorOf(current), 34));
            g2.fillRoundRect(x + 2, y + 2, cell - 4, cell - 4, 10, 10);
            Composite oldComposite = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
            drawPiece(g2, current, x, y, 1f);
            g2.setComposite(oldComposite);
        }

        // Grid
        Shape clip = g2.getClip();
        g2.clip(new java.awt.geom.RoundRectangle2D.Float(PAD, PAD, size, size, arc, arc));
        g2.setColor(Theme.alpha(Theme.BORDER, n <= 5 ? 255 : 190));
        float line = n <= 5 ? 2f : 1f;
        g2.setStroke(new BasicStroke(line));
        for (int i = 1; i < n; i++) {
            int p = PAD + i * cell;
            g2.drawLine(p, PAD, p, PAD + size);
            g2.drawLine(PAD, p, PAD + size, p);
        }
        g2.setClip(clip);
        g2.setColor(Theme.alpha(Theme.ACCENT, 70));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(PAD, PAD, size, size, arc, arc);

        // Pieces
        int[] last = model.getLastMove();
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                int v = model.getCell(r, c);
                if (v == GameModel.EMPTY) continue;
                float scale = 1f;
                if (animatedMove != null && animatedMove[0] == r && animatedMove[1] == c) {
                    float t = Math.min(1f, (System.currentTimeMillis() - animStart) / (float) ANIM_MS);
                    scale = 0.55f + 0.45f * easeOutBack(t);
                }
                Composite oldComposite = g2.getComposite();
                if (win != null && !winning[r][c]) {
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
                }
                drawPiece(g2, v, PAD + c * cell, PAD + r * cell, scale);
                g2.setComposite(oldComposite);
            }
        }

        // Last move marker
        if (last != null && win == null) {
            int x = PAD + last[1] * cell, y = PAD + last[0] * cell;
            g2.setColor(Theme.alpha(colorOf(last[2]), 200));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(x + 3, y + 3, cell - 6, cell - 6, 10, 10);
        }

        // Win line with glow
        if (win != null) {
            float x1 = PAD + win[1] * cell + cell / 2f, y1 = PAD + win[0] * cell + cell / 2f;
            float x2 = PAD + win[3] * cell + cell / 2f, y2 = PAD + win[2] * cell + cell / 2f;
            double angle = Math.atan2(y2 - y1, x2 - x1);
            float ext = cell * 0.35f;
            java.awt.geom.Line2D.Float stroke = new java.awt.geom.Line2D.Float(
                    (float) (x1 - Math.cos(angle) * ext), (float) (y1 - Math.sin(angle) * ext),
                    (float) (x2 + Math.cos(angle) * ext), (float) (y2 + Math.sin(angle) * ext));
            float base = Math.max(4f, cell / 9f);
            int[][] layers = { {4, 30}, {2, 70}, {1, 255} };
            for (int[] layer : layers) {
                g2.setColor(Theme.alpha(Theme.WIN, layer[1]));
                g2.setStroke(new BasicStroke(base * layer[0], BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(stroke);
            }
        }
        g2.dispose();
    }

    private void drawPiece(Graphics2D g2, int player, int x, int y, float scale) {
        int inset = Math.max(3, Math.round(cell * 0.1f));
        int s = cell - inset * 2;
        if (s <= 0) return;
        if (scale >= 0.999f) {
            g2.drawImage(scaled(player, s), x + inset, y + inset, null);
        } else {
            int ss = Math.max(1, Math.round(s * scale));
            int off = (cell - ss) / 2;
            g2.drawImage(scaled(player, s), x + off, y + off, ss, ss, null);
        }
    }

    private Image scaled(int player, int size) {
        Map<Integer, Image> cache = player == GameModel.X ? xCache : oCache;
        return cache.computeIfAbsent(size, s -> player == GameModel.X
                ? highQualityScale(xImage, s, false)
                : highQualityScale(oImage, s, true));
    }

    /**
     * Halves the image step by step so large sources downscale without aliasing.
     * {@code fillHole} paints a light disc behind the O's transparent center so its face stays visible.
     */
    private static Image highQualityScale(Image src, int size, boolean fillHole) {
        int w = src.getWidth(null);
        int h = src.getHeight(null);
        if (w <= 0 || h <= 0) return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        BufferedImage current = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = current.createGraphics();
        if (fillHole) {
            Theme.antialias(g);
            int d = (int) (w * 0.56);
            g.setColor(new Color(0xFFF7EE));
            g.fillOval((w - d) / 2, (h - d) / 2, d, d);
        }
        g.drawImage(src, 0, 0, null);
        g.dispose();
        while (w / 2 >= size) {
            w /= 2;
            h /= 2;
            current = resize(current, w, h);
        }
        return resize(current, size, size);
    }

    private static BufferedImage resize(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    private boolean[][] winningCells(int[] win) {
        boolean[][] cells = new boolean[n][n];
        if (win == null) return cells;
        int dr = Integer.signum(win[2] - win[0]);
        int dc = Integer.signum(win[3] - win[1]);
        int r = win[0], c = win[1];
        while (true) {
            cells[r][c] = true;
            if (r == win[2] && c == win[3]) break;
            r += dr;
            c += dc;
        }
        return cells;
    }

    private static Color colorOf(int player) {
        return player == GameModel.X ? Theme.X_COLOR : Theme.O_COLOR;
    }

    private static float easeOutBack(float t) {
        float c1 = 1.70158f, c3 = c1 + 1;
        return (float) (1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2));
    }

    /**
     * Wraps the board for a JScrollPane: centers it while it is smaller than the
     * viewport and scrolls once it is bigger.
     */
    public static class Centered extends JPanel implements Scrollable {
        public Centered(BoardView board) {
            super(new GridBagLayout());
            setOpaque(false);
            add(board);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }

        @Override
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 24; }

        @Override
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 120; }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return getParent() instanceof JViewport && getParent().getWidth() > getPreferredSize().width;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return getParent() instanceof JViewport && getParent().getHeight() > getPreferredSize().height;
        }
    }
}
