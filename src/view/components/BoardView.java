package view.components;

import controller.GameSession;
import model.GameModel;
import utils.ResourceUtils;
import view.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * The game board, painted as a single component: pen lines on a sheet of paper,
 * hover preview, last-move marker, pop-in animation, wheel zoom and drag to pan.
 * A win is highlighted and looped in red pen, never struck through the faces.
 * With focus, the arrow keys move a cursor and Enter or Space plays there.
 */
public class BoardView extends JComponent {
    public static final int MIN_CELL = 22;
    public static final int MAX_CELL = 120;
    private static final int PAD = 14;
    private static final int ANIM_MS = 220;
    private static final int WIN_MS = 450;

    private final GameSession controller;
    private final GameModel liveModel;
    private GameModel display; // a replayed position shown instead of the live game, or null
    private boolean showMoveNumbers;
    private final int n;

    private static final Image X_IMAGE = ResourceUtils.getImageIcon("/assets/x_image_funny.png").getImage();
    private static final Image O_IMAGE = ResourceUtils.getImageIcon("/assets/o_image_funny.png").getImage();
    private static final Map<Integer, Image> X_CACHE = new HashMap<>();
    private static final Map<Integer, Image> O_CACHE = new HashMap<>();

    private int cell = 48;
    private int hoverRow = -1;
    private int hoverCol = -1;

    private int[] animatedMove;
    private long animStart;
    private final Timer animTimer;

    private Point pressPoint;
    private boolean dragged;
    private Runnable onZoom = () -> {};

    private static final int HINT_MS = 2500;
    private int[] hint;          // {row, col} being suggested, or null
    private long hintStart;
    private final Timer hintTimer;

    private String winKey;       // the win line already announced, so a replay never redraws it
    private long winStart;
    private final Timer winTimer;

    private int cursorRow = -1;
    private int cursorCol = -1;

    private Path2D gridShape;    // cached per cell size, so the pen lines hold still
    private int gridCell;

    public BoardView(GameSession controller) {
        this.controller = controller;
        this.liveModel = controller.getModel();
        this.n = liveModel.getSize();
        setOpaque(false);

        animTimer = new Timer(15, e -> {
            if (System.currentTimeMillis() - animStart >= ANIM_MS) {
                animatedMove = null;
                ((Timer) e.getSource()).stop();
            }
            repaint();
        });

        hintTimer = new Timer(30, e -> {
            if (System.currentTimeMillis() - hintStart >= HINT_MS) clearHint();
            repaint();
        });

        winTimer = new Timer(15, e -> {
            if (System.currentTimeMillis() - winStart >= WIN_MS) ((Timer) e.getSource()).stop();
            repaint();
        });

        setFocusable(true);
        bindBoardKey(KeyEvent.VK_UP, () -> moveCursor(-1, 0));
        bindBoardKey(KeyEvent.VK_DOWN, () -> moveCursor(1, 0));
        bindBoardKey(KeyEvent.VK_LEFT, () -> moveCursor(0, -1));
        bindBoardKey(KeyEvent.VK_RIGHT, () -> moveCursor(0, 1));
        bindBoardKey(KeyEvent.VK_ENTER, this::playAtCursor);
        bindBoardKey(KeyEvent.VK_SPACE, this::playAtCursor);
        addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) { repaint(); }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) { repaint(); }
        });

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
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
                    if (rc != null && display == null) controller.play(rc[0], rc[1]);
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

    /** A key that only works during live play; otherwise it falls through to the window's shortcuts. */
    private void bindBoardKey(int key, Runnable action) {
        String id = "board" + key;
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(key, 0), id);
        getActionMap().put(id, new AbstractAction() {
            @Override
            public boolean isEnabled() { return display == null && !liveModel.isOver(); }

            @Override
            public void actionPerformed(ActionEvent e) { action.run(); }
        });
    }

    /** Moves the keyboard cursor, starting from the last move or the middle of the board. */
    public void moveCursor(int dRow, int dCol) {
        if (cursorRow < 0) {
            int[] last = liveModel.getLastMove();
            cursorRow = last != null ? last[0] : n / 2;
            cursorCol = last != null ? last[1] : n / 2;
        } else {
            cursorRow = Math.max(0, Math.min(n - 1, cursorRow + dRow));
            cursorCol = Math.max(0, Math.min(n - 1, cursorCol + dCol));
        }
        scrollToCell(cursorRow, cursorCol);
        repaint();
    }

    private void playAtCursor() {
        if (cursorRow < 0) moveCursor(0, 0);
        else controller.play(cursorRow, cursorCol);
    }

    /**
     * Shows a different position (e.g. a step of a replay) without touching the
     * live game; pass null to go back to the live board. Input is ignored meanwhile.
     */
    public void setDisplayModel(GameModel model) {
        if (model != null && model.getSize() != n) throw new IllegalArgumentException("board size mismatch");
        display = model;
        clearHint();
        animatedMove = null;
        repaint();
    }

    public boolean isShowingReplay() { return display != null; }

    public void setShowMoveNumbers(boolean show) {
        showMoveNumbers = show;
        repaint();
    }

    public boolean isShowingMoveNumbers() { return showMoveNumbers; }

    private GameModel shown() {
        return display != null ? display : liveModel;
    }

    private boolean interactive() {
        return display == null && controller.acceptsInput();
    }

    public int getCellSize() { return cell; }

    /** Call after every model change; animates the newest piece. */
    /** Pulses a suggested cell for a few seconds (cleared by the next move). */
    public void flashHint(int row, int col) {
        hint = new int[]{row, col};
        hintStart = System.currentTimeMillis();
        hintTimer.restart();
        scrollToCell(row, col);
        repaint();
    }

    public void clearHint() {
        if (hint == null) return;
        hint = null;
        hintTimer.stop();
        repaint();
    }

    private void scrollToCell(int row, int col) {
        Rectangle cellRect = new Rectangle(PAD + col * cell - cell, PAD + row * cell - cell, cell * 3, cell * 3);
        scrollRectToVisible(cellRect);
    }

    public void stateChanged() {
        clearHint();
        announceWin();
        int[] last = shown().getLastMove();
        if (last != null && (animatedMove == null || last[0] != animatedMove[0] || last[1] != animatedMove[1])) {
            animatedMove = last;
            animStart = System.currentTimeMillis();
            animTimer.restart();
        } else if (last == null) {
            animatedMove = null;
        }
        repaint();
    }

    /** Starts drawing the win mark the first time a win line appears, and brings it into view. */
    private void announceWin() {
        int[] win = liveModel.getWinningLine();
        if (win == null) {
            winKey = null;
            return;
        }
        String key = java.util.Arrays.toString(win);
        if (key.equals(winKey)) return;
        winKey = key;
        winStart = System.currentTimeMillis();
        winTimer.restart();
        int top = Math.min(win[0], win[2]), left = Math.min(win[1], win[3]);
        int rows = Math.abs(win[2] - win[0]) + 1, cols = Math.abs(win[3] - win[1]) + 1;
        scrollRectToVisible(new Rectangle(PAD + left * cell - cell / 2, PAD + top * cell - cell / 2,
                (cols + 1) * cell, (rows + 1) * cell));
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
        boolean playable = rc != null && interactive() && shown().getCell(r, c) == GameModel.EMPTY;
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

        // The sheet covers the page's printed squares, so only the real grid shows
        Path2D sheet = Sketch.box(PAD, PAD, size, size, Math.min(Theme.RADIUS, cell / 3f), n);
        g2.setColor(Theme.SURFACE);
        g2.fill(sheet);

        // Highlighter under the grid, as it would be on paper
        int[] win = shown().getWinningLine();
        boolean[][] winning = winningCells(win);
        float winT = win == null ? 0f : Math.min(1f, (System.currentTimeMillis() - winStart) / (float) WIN_MS);
        if (win != null) {
            paintHighlight(g2, centerOf(win[1]), centerOf(win[0]), centerOf(win[3]), centerOf(win[2]), cell, n);
        }

        // Hover preview
        boolean canPlay = interactive();
        int current = shown().getCurrentPlayer();
        if (canPlay && hoverRow >= 0 && shown().getCell(hoverRow, hoverCol) == GameModel.EMPTY) {
            int x = PAD + hoverCol * cell, y = PAD + hoverRow * cell;
            g2.setColor(Theme.SURFACE_ALT);
            g2.fillRect(x, y, cell, cell);
            Composite oldComposite = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
            drawPiece(g2, current, x, y, 1f);
            g2.setComposite(oldComposite);
        }

        // Grid
        g2.setColor(Theme.BORDER);
        g2.setStroke(new BasicStroke(n <= 5 ? 2f : 1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(gridShape());
        g2.setColor(Theme.TEXT_MUTED);
        g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(sheet);

        // The red loop goes under the pieces and clear of them, so no face is ever crossed out
        if (win != null) {
            paintWinLoop(g2, centerOf(win[1]), centerOf(win[0]), centerOf(win[3]), centerOf(win[2]), cell, n, winT);
        }

        // Pieces
        int[] last = shown().getLastMove();
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                int v = shown().getCell(r, c);
                if (v == GameModel.EMPTY) continue;
                float scale = 1f;
                if (animatedMove != null && animatedMove[0] == r && animatedMove[1] == c) {
                    float t = Math.min(1f, (System.currentTimeMillis() - animStart) / (float) ANIM_MS);
                    scale = 0.55f + 0.45f * easeOutBack(t);
                }
                if (win != null && winning[r][c] && winT < 1f) {
                    // The winners hop as the pen closes its loop
                    scale = 1f + 0.14f * (float) Math.sin(Math.PI * Math.max(0f, (winT - 0.55f) / 0.45f));
                }
                Composite oldComposite = g2.getComposite();
                if (win != null && !winning[r][c]) {
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
                }
                drawPiece(g2, v, PAD + c * cell, PAD + r * cell, scale);
                g2.setComposite(oldComposite);
            }
        }

        // Hint: ghost of the suggested piece inside a pulsing pen circle
        if (hint != null && shown().getCell(hint[0], hint[1]) == GameModel.EMPTY) {
            int x = PAD + hint[1] * cell, y = PAD + hint[0] * cell;
            double phase = (System.currentTimeMillis() - hintStart) / 400.0;
            float pulse = (float) (0.5 + 0.5 * Math.sin(phase * Math.PI));
            g2.setColor(Theme.WIN);
            g2.fillRect(x + 1, y + 1, cell - 1, cell - 1);
            Composite oldComposite = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));
            drawPiece(g2, shown().getCurrentPlayer(), x, y, 1f);
            g2.setComposite(oldComposite);
            float grow = pulse * cell * 0.1f;
            g2.setColor(Theme.ACCENT);
            g2.setStroke(new BasicStroke(Math.max(2f, cell / 16f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(Sketch.loop(x + 1 - grow, y + 1 - grow, cell - 2 + 2 * grow, cell - 2 + 2 * grow, hint[0] * 31 + hint[1]));
        }

        // Last move marker: the most recent piece has a small pen circle round it
        if (last != null && !shown().isOver()) {
            int x = PAD + last[1] * cell, y = PAD + last[0] * cell;
            g2.setColor(colorOf(last[2]));
            g2.setStroke(new BasicStroke(Math.max(1.6f, cell / 22f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(Sketch.loop(x + 1, y + 1, cell - 2, cell - 2, last[0] * 31 + last[1]));
        }

        // Keyboard cursor, only while the player is actually using the keyboard
        if (cursorRow >= 0 && display == null && !liveModel.isOver() && FocusRing.shouldPaint(this)) {
            int x = PAD + cursorCol * cell, y = PAD + cursorRow * cell;
            if (canPlay && shown().getCell(cursorRow, cursorCol) == GameModel.EMPTY) {
                Composite oldComposite = g2.getComposite();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
                drawPiece(g2, current, x, y, 1f);
                g2.setComposite(oldComposite);
            }
            g2.setColor(Theme.ACCENT);
            g2.setStroke(new BasicStroke(Math.max(2.4f, cell / 14f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawRoundRect(x + 1, y + 1, cell - 2, cell - 2, 6, 6);
        }

        if (showMoveNumbers) drawMoveNumbers(g2);
        g2.dispose();
    }

    private float centerOf(int index) {
        return PAD + index * cell + cell / 2f;
    }

    private Path2D gridShape() {
        if (gridShape == null || gridCell != cell) {
            Path2D.Float grid = new Path2D.Float();
            int size = n * cell;
            double wob = Math.min(0.6, cell / 60.0);
            for (int i = 1; i < n; i++) {
                int p = PAD + i * cell;
                Sketch.addLine(grid, p, PAD + 1, p, PAD + size - 1, i * 13, wob);
                Sketch.addLine(grid, PAD + 1, p, PAD + size - 1, p, i * 29 + 3, wob);
            }
            gridShape = grid;
            gridCell = cell;
        }
        return gridShape;
    }

    /** Yellow highlighter swipe through the centers of a winning line. */
    public static void paintHighlight(Graphics2D g2, float x1, float y1, float x2, float y2, int cell, int seed) {
        Composite old = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.85f));
        g2.setColor(Theme.WIN);
        g2.setStroke(new BasicStroke(cell * 0.84f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(Sketch.line(x1, y1, x2, y2, seed, cell * 0.03));
        g2.setComposite(old);
    }

    /** Red pen loop around a winning line, drawn up to {@code progress}, with an eased hand. */
    public static void paintWinLoop(Graphics2D g2, float x1, float y1, float x2, float y2, int cell, int seed, float progress) {
        float eased = 1f - (1f - progress) * (1f - progress);
        g2.setColor(Theme.DANGER);
        g2.setStroke(new BasicStroke(Math.max(2f, cell / 13f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D loop = Sketch.stadium(x1, y1, x2, y2, cell * 0.57, seed);
        g2.draw(eased >= 1f ? loop : Sketch.partial(loop, eased));
    }

    /** Small numbered badge in the corner of every piece, in the order they were played. */
    private void drawMoveNumbers(Graphics2D g2) {
        java.util.List<int[]> moves = shown().getMoves();
        int d = Math.max(14, Math.round(cell * 0.36f));
        Font font = Theme.semibold(Math.max(9f, d * 0.55f));
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        for (int i = 0; i < moves.size(); i++) {
            int[] m = moves.get(i);
            int x = PAD + m[1] * cell + cell - d - 1;
            int y = PAD + m[0] * cell + cell - d - 1;
            String text = String.valueOf(i + 1);
            int w = Math.max(d, fm.stringWidth(text) + 6);
            x -= w - d;
            g2.setColor(Theme.alpha(Theme.SURFACE, 235));
            g2.fillRoundRect(x, y, w, d, d, d);
            g2.setColor(colorOf(m[2]));
            g2.drawString(text, x + (w - fm.stringWidth(text)) / 2, y + (d - fm.getHeight()) / 2 + fm.getAscent());
        }
    }

    private void drawPiece(Graphics2D g2, int player, int x, int y, float scale) {
        int inset = Math.max(1, Math.round(cell * 0.06f));
        int s = cell - inset * 2;
        if (s <= 0) return;
        if (scale >= 0.999f) {
            g2.drawImage(pieceImage(player, s), x + inset, y + inset, null);
        } else {
            int ss = Math.max(1, Math.round(s * scale));
            int off = (cell - ss) / 2;
            g2.drawImage(pieceImage(player, s), x + off, y + off, ss, ss, null);
        }
    }

    /** The X or O artwork at the given pixel size (cached). Must be called on the EDT. */
    public static Image pieceImage(int player, int size) {
        Map<Integer, Image> cache = player == GameModel.X ? X_CACHE : O_CACHE;
        return cache.computeIfAbsent(size, s -> player == GameModel.X
                ? highQualityScale(X_IMAGE, s, false)
                : highQualityScale(O_IMAGE, s, true));
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
            g.setColor(Theme.SURFACE);
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
