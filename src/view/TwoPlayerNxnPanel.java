package view;

import controller.GameController;
import model.GameMode;
import model.GameModel;
import utils.ResourceUtils;
import utils.SoundManager;

import javax.swing.*;
import java.awt.*;

public class TwoPlayerNxnPanel extends JPanel implements GameController.Listener {
    private final GameMenu gameMenu;
    private final GameController controller;
    private final GameModel gameModel;
    private final boolean isVsAI;
    private final int n;

    private final ImageIcon xImage;
    private final ImageIcon oImage;
    private boolean isDragging = false;

    private final JButton[][] btn;
    private final JButton undoBtn;
    private final JPanel grid;
    private final JScrollPane scrollPane;
    private int cellSize;

    private final AvatarPanel avatarX;
    private final AvatarPanel avatarO;
    private final JLabel winnerLabelX;
    private final JLabel winnerLabelO;

    public TwoPlayerNxnPanel(GameMenu gameMenu, BackgroundPanel backgroundPanel, int n, boolean isVsAI) {
        this.gameMenu = gameMenu;
        this.n = n;
        this.isVsAI = isVsAI;
        this.controller = new GameController(n, isVsAI ? GameMode.VS_AI : GameMode.TWO_PLAYERS);
        this.gameModel = controller.getModel();
        this.cellSize = Math.max(30, Math.min(150, (int) (450.0 / n)));
        this.btn = new JButton[n][n];

        this.xImage = ResourceUtils.getImageIcon("/assets/x_image_funny.png");
        this.oImage = ResourceUtils.getImageIcon("/assets/o_image_funny.png");

        grid = new JPanel(new GridLayout(n, n)) {
            @Override
            public void paint(Graphics g) {
                super.paint(g);
                int[] winLine = gameModel.getWinningLine();
                if (winLine == null) return;
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 50, 50, 200));
                g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                JButton a = btn[winLine[0]][winLine[1]];
                JButton b = btn[winLine[2]][winLine[3]];
                int x1 = a.getX() + a.getWidth() / 2, y1 = a.getY() + a.getHeight() / 2;
                int x2 = b.getX() + b.getWidth() / 2, y2 = b.getY() + b.getHeight() / 2;
                double angle = Math.atan2(y2 - y1, x2 - x1);
                int ext = 20;
                g2.drawLine(x1 - (int) (Math.cos(angle) * ext), y1 - (int) (Math.sin(angle) * ext),
                        x2 + (int) (Math.cos(angle) * ext), y2 + (int) (Math.sin(angle) * ext));
            }
        };
        grid.setBackground(Color.DARK_GRAY);

        scrollPane = new JScrollPane(grid);
        scrollPane.setBounds(100, 75, 600, 450);
        scrollPane.setWheelScrollingEnabled(false);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

        undoBtn = new CustomButton("Undo");
        undoBtn.setBounds(340, 20, 100, 40);
        undoBtn.setVisible(gameMenu.isCheatModeEnabled());
        JButton resetBtn = new CustomButton("Reset");
        resetBtn.setBounds(460, 20, 100, 40);
        JButton mainMenuBtn = new CustomButton("Main Menu");
        mainMenuBtn.setBounds(320, 540, 160, 40);
        JButton backBtn = new CustomButton("Back");
        backBtn.setBounds(220, 20, 100, 40);

        resetBtn.addActionListener(e -> {
            int option = CustomDialog.showDialog(gameMenu, "Start fresh?\nThis will clear the entire board.", "Confirm Reset", CustomDialog.YES_NO_OPTION);
            if (option == CustomDialog.YES_OPTION) controller.restart();
        });
        undoBtn.addActionListener(e -> {
            if (!gameModel.canUndo()) {
                CustomDialog.message(gameMenu, "Hold On", "You haven't made any moves yet!");
                return;
            }
            controller.undo();
        });
        mainMenuBtn.addActionListener(e -> leave("MainMenu"));
        backBtn.addActionListener(e -> leave(isVsAI ? "OnePlayerPanel" : "TwoPlayerPanel"));

        java.awt.event.MouseAdapter wheelAdapter = new java.awt.event.MouseAdapter() {
            @Override
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent e) {
                int minSize = Math.max(30, (int) (450.0 / n));
                cellSize = e.getWheelRotation() < 0 ? Math.min(150, cellSize + 10) : Math.max(minSize, cellSize - 10);
                updateGridSize();
            }
        };

        java.awt.event.MouseAdapter panAdapter = new java.awt.event.MouseAdapter() {
            private Point origin;

            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                origin = e.getLocationOnScreen();
                isDragging = false;
            }

            @Override
            public void mouseDragged(java.awt.event.MouseEvent e) {
                if (origin == null) return;
                Point current = e.getLocationOnScreen();
                int dx = origin.x - current.x;
                int dy = origin.y - current.y;
                if (Math.abs(dx) > 3 || Math.abs(dy) > 3) isDragging = true;
                JScrollBar hBar = scrollPane.getHorizontalScrollBar();
                JScrollBar vBar = scrollPane.getVerticalScrollBar();
                hBar.setValue(hBar.getValue() + dx);
                vBar.setValue(vBar.getValue() + dy);
                origin = current;
            }
        };

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                int finalI = i;
                int finalJ = j;
                btn[i][j] = new JButton("") {
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        int val = gameModel.getCell(finalI, finalJ);
                        if (val == GameModel.EMPTY) return;
                        Graphics2D g2 = (Graphics2D) g;
                        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        int padding = 10;
                        int w = getWidth() - padding * 2;
                        int h = getHeight() - padding * 2;
                        if (w > 0 && h > 0) {
                            Image img = (val == GameModel.X) ? xImage.getImage() : oImage.getImage();
                            g2.drawImage(img, padding, padding, w, h, this);
                        }
                    }
                };
                btn[i][j].setPreferredSize(new Dimension(cellSize, cellSize));
                btn[i][j].setBackground(new Color(245, 245, 245));
                btn[i][j].setFocusPainted(false);
                btn[i][j].setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
                btn[i][j].addMouseWheelListener(wheelAdapter);
                btn[i][j].addMouseListener(panAdapter);
                btn[i][j].addMouseMotionListener(panAdapter);
                btn[i][j].addActionListener(e -> {
                    if (!isDragging) controller.play(finalI, finalJ);
                });
                grid.add(btn[i][j]);
            }
        }

        avatarX = new AvatarPanel("P1", Color.CYAN);
        avatarX.setBounds(10, 260, 80, 80);
        avatarO = new AvatarPanel(isVsAI ? "AI" : "P2", Color.RED);
        avatarO.setBounds(710, 260, 80, 80);

        winnerLabelX = winnerLabel(10);
        winnerLabelO = winnerLabel(710);

        backgroundPanel.add(avatarX);
        backgroundPanel.add(avatarO);
        backgroundPanel.add(winnerLabelX);
        backgroundPanel.add(winnerLabelO);
        backgroundPanel.add(scrollPane);
        backgroundPanel.add(undoBtn);
        backgroundPanel.add(resetBtn);
        backgroundPanel.add(mainMenuBtn);
        backgroundPanel.add(backBtn);

        controller.setListener(this);
        onStateChanged();
    }

    // Legacy constructor for TwoPlayerPanel compatibility
    public TwoPlayerNxnPanel(GameMenu gameMenu, BackgroundPanel backgroundPanel, int n) {
        this(gameMenu, backgroundPanel, n, false);
    }

    private JLabel winnerLabel(int x) {
        JLabel label = new JLabel("WINNER", SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI", Font.BOLD, 16));
        label.setForeground(Color.YELLOW);
        label.setBounds(x, 235, 80, 20);
        label.setVisible(false);
        return label;
    }

    private void leave(String card) {
        int option = CustomDialog.confirm(gameMenu, "Leave this game?", "Your current progress will be lost.",
                "Leave", "Stay", true);
        if (option == CustomDialog.YES_OPTION) {
            controller.dispose();
            gameMenu.getCardLayout().show(gameMenu.getCardPanel(), card);
        }
    }

    @Override
    public void onStateChanged() {
        boolean xTurn = gameModel.getCurrentPlayer() == GameModel.X;
        avatarX.setActive(xTurn);
        avatarO.setActive(!xTurn);
        winnerLabelX.setVisible(gameModel.getWinner() == GameModel.X);
        winnerLabelO.setVisible(gameModel.getWinner() == GameModel.O);
        boolean enabled = !gameModel.isOver();
        for (JButton[] row : btn) {
            for (JButton b : row) b.setEnabled(enabled);
        }
        grid.repaint();
    }

    @Override
    public void onGameOver() {
        int winner = gameModel.getWinner();
        String msg;
        if (winner == GameModel.EMPTY) {
            SoundManager.play(SoundManager.ALERT);
            msg = "It's a draw!";
        } else {
            SoundManager.play(SoundManager.WIN);
            msg = ((winner == GameModel.X) ? "Player 1" : (isVsAI ? "AI" : "Player 2")) + " has claimed victory!";
        }
        int option = CustomDialog.confirm(gameMenu, "Game over", msg, "Play again", "Main menu", false);
        if (option == CustomDialog.YES_OPTION) {
            controller.restart();
        } else {
            controller.dispose();
            gameMenu.getCardLayout().show(gameMenu.getCardPanel(), "MainMenu");
        }
    }

    @Override
    public void onAiThinking(boolean thinking) {
        // No dedicated indicator on this screen yet
    }

    private void updateGridSize() {
        for (JButton[] row : btn) {
            for (JButton b : row) b.setPreferredSize(new Dimension(cellSize, cellSize));
        }
        grid.revalidate();
        grid.repaint();
    }
}
