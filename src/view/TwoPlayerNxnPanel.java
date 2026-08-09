package view;

import controller.ButtonHoverEffect;
import model.GameAI;
import model.GameModel;
import utils.ResourceUtils;
import utils.SoundPlayer;

import javax.swing.*;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class TwoPlayerNxnPanel extends JPanel {
    private GameMenu gameMenu;
    private BackgroundPanel backgroundPanel;
    private int n;
    private GameModel gameModel;
    private boolean isVsAI = false;
    private GameAI ai;
    
    private String selectPath = "/assets/Select.wav";
    private String scrollPath = "/assets/Scroll.wav";
    private String alertPath = "/assets/Alert.wav";
    private String congratulationPath = "/assets/Congratulation.wav";
    
    private ImageIcon xImage;
    private ImageIcon oImage;
    private boolean isDragging = false;

    private JButton[][] btn;
    private JButton undoBtn, resetBtn, mainMenuBtn, backBtn;
    
    private JPanel grid;
    private JScrollPane scrollPane;
    private int cellSize = 60; // Default cell size
    
    private AvatarPanel avatarX;
    private AvatarPanel avatarO;
    private JLabel winnerLabelX;
    private JLabel winnerLabelO;
    private int[] aiDelays = {400, 600, 800, 1000, 1200, 1500};

    public TwoPlayerNxnPanel(GameMenu gameMenu, BackgroundPanel backgroundPanel, int n, boolean isVsAI) {
        this.gameMenu = gameMenu;
        this.backgroundPanel = backgroundPanel;
        this.n = n;
        this.gameModel = new GameModel(n);
        this.isVsAI = isVsAI;
        this.cellSize = Math.max(30, Math.min(150, (int)(450.0 / n)));
        if (isVsAI) {
            this.ai = new GameAI(n, 2, 1);
        }
        
        this.btn = new JButton[n][n];

        this.xImage = ResourceUtils.getImageIcon("/assets/x_image_funny.jpg");
        this.oImage = ResourceUtils.getImageIcon("/assets/o_image_funny.jpg");

        grid = new JPanel(new GridLayout(n, n)) {
            @Override
            public void paint(Graphics g) {
                super.paint(g);
                int[] winLine = gameModel.getWinningLine();
                if (winLine != null && winLine.length == 4) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(255, 50, 50, 200));
                    g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    
                    int r1 = winLine[0];
                    int c1 = winLine[1];
                    int r2 = winLine[2];
                    int c2 = winLine[3];
                    
                    if (btn[r1][c1] != null && btn[r2][c2] != null) {
                        int x1 = btn[r1][c1].getX() + btn[r1][c1].getWidth() / 2;
                        int y1 = btn[r1][c1].getY() + btn[r1][c1].getHeight() / 2;
                        int x2 = btn[r2][c2].getX() + btn[r2][c2].getWidth() / 2;
                        int y2 = btn[r2][c2].getY() + btn[r2][c2].getHeight() / 2;
                        
                        // Extend line slightly past the center of the start/end buttons
                        double angle = Math.atan2(y2 - y1, x2 - x1);
                        int extension = 20;
                        int startX = x1 - (int)(Math.cos(angle) * extension);
                        int startY = y1 - (int)(Math.sin(angle) * extension);
                        int endX = x2 + (int)(Math.cos(angle) * extension);
                        int endY = y2 + (int)(Math.sin(angle) * extension);
                        
                        g2.drawLine(startX, startY, endX, endY);
                    }
                }
            }
        };
        grid.setBackground(Color.DARK_GRAY);
        
        scrollPane = new JScrollPane(grid);
        scrollPane.setBounds(100, 75, 600, 450);
        
        undoBtn = new CustomButton("Undo");
        undoBtn.setBounds(340, 20, 100, 40);
        undoBtn.setVisible(gameMenu.isCheatModeEnabled());
        resetBtn = new CustomButton("Reset");
        resetBtn.setBounds(460, 20, 100, 40);
        mainMenuBtn = new CustomButton("Main Menu");
        mainMenuBtn.setBounds(320, 540, 160, 40);
        backBtn = new CustomButton("Back");
        backBtn.setBounds(220, 20, 100, 40);

        scrollPane.setWheelScrollingEnabled(false);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

        resetBtn.addMouseListener(new ButtonHoverEffect(resetBtn, scrollPath));
        resetBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new SoundPlayer(selectPath).playOnce();
                int option = CustomDialog.showDialog(gameMenu, "Start fresh?\nThis will clear the entire board.", "Confirm Reset", CustomDialog.YES_NO_OPTION);
                if (option == CustomDialog.YES_OPTION) resetGame();
            }
        });

        undoBtn.addMouseListener(new ButtonHoverEffect(undoBtn, scrollPath));
        undoBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new SoundPlayer(selectPath).playOnce();
                gameModel.toggleTurn();
                undo();
                if (isVsAI && !gameModel.isXTurn()) {
                    gameModel.toggleTurn();
                    undo();
                }
                updateTurnIndicator();
            }
        });

        mainMenuBtn.addMouseListener(new ButtonHoverEffect(mainMenuBtn, scrollPath));
        mainMenuBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new SoundPlayer(selectPath).playOnce();
                int option = CustomDialog.showDialog(gameMenu, "Leaving so soon?\nYour current game progress will be lost.", "Confirm", CustomDialog.YES_NO_OPTION);
                if (option == CustomDialog.YES_OPTION) gameMenu.getCardLayout().show(gameMenu.getCardPanel(), "MainMenu");
            }
        });

        backBtn.addMouseListener(new ButtonHoverEffect(backBtn, scrollPath));
        backBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new SoundPlayer(selectPath).playOnce();
                int option = CustomDialog.showDialog(gameMenu, "Leaving so soon?\nYour current game progress will be lost.", "Confirm", CustomDialog.YES_NO_OPTION);
                if (option == CustomDialog.YES_OPTION) gameMenu.getCardLayout().show(gameMenu.getCardPanel(), isVsAI ? "NewGame" : "TwoPlayerPanel");
            }
        });

        java.awt.event.MouseAdapter wheelAdapter = new java.awt.event.MouseAdapter() {
            @Override
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent e) {
                if (e.getWheelRotation() < 0) {
                    if (cellSize < 150) {
                        cellSize += 10;
                        updateGridSize();
                    }
                } else {
                    int minSize = Math.max(30, (int) (450.0 / n));
                    if (cellSize - 10 >= minSize) {
                        cellSize -= 10;
                        updateGridSize();
                    } else if (cellSize > minSize) {
                        cellSize = minSize;
                        updateGridSize();
                    }
                }
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
                if (origin != null) {
                    Point current = e.getLocationOnScreen();
                    int dx = origin.x - current.x;
                    int dy = origin.y - current.y;
                    
                    if (Math.abs(dx) > 3 || Math.abs(dy) > 3) {
                        isDragging = true;
                    }

                    JScrollBar hBar = scrollPane.getHorizontalScrollBar();
                    JScrollBar vBar = scrollPane.getVerticalScrollBar();

                    hBar.setValue(hBar.getValue() + dx);
                    vBar.setValue(vBar.getValue() + dy);

                    origin = current;
                }
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
                        int val = gameModel.getArray()[finalI][finalJ];
                        if (val > 0) {
                            Graphics2D g2 = (Graphics2D) g;
                            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                            int padding = 10;
                            int w = getWidth() - padding * 2;
                            int h = getHeight() - padding * 2;
                            if (w > 0 && h > 0) {
                                Image img = (val == 1) ? xImage.getImage() : oImage.getImage();
                                g2.drawImage(img, padding, padding, w, h, this);
                            }
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
                
                btn[i][j].addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (isDragging) return;
                        
                        // Ignore player clicks if it's AI's turn
                        if (isVsAI && !gameModel.isXTurn()) return;
                        
                        if (gameModel.getArray()[finalI][finalJ] == 0) {
                            gameModel.saveState();

                            if (gameModel.isXTurn()) {
                                gameModel.setArrayValue(finalI, finalJ, 1);
                                btn[finalI][finalJ].repaint();
                                gameModel.toggleTurn();
                                updateTurnIndicator();
                                
                                if (checkGameOver(finalI, finalJ, 1)) return;
                                
                                if (isVsAI) {
                                    // Simulate AI thinking time with random delay
                                    int randomDelay = aiDelays[new java.util.Random().nextInt(aiDelays.length)];
                                    Timer timer = new Timer(randomDelay, evt -> makeAIMove());
                                    timer.setRepeats(false);
                                    timer.start();
                                }
                            } else if (!isVsAI) {
                                gameModel.setArrayValue(finalI, finalJ, 2);
                                btn[finalI][finalJ].repaint();
                                gameModel.toggleTurn();
                                updateTurnIndicator();
                                checkGameOver(finalI, finalJ, 2);
                            }
                        }
                    }
                });
                grid.add(btn[i][j]);
            }
        }

        avatarX = new AvatarPanel("P1", Color.CYAN);
        avatarX.setBounds(10, 260, 80, 80);
        
        avatarO = new AvatarPanel(isVsAI ? "AI" : "P2", Color.RED);
        avatarO.setBounds(710, 260, 80, 80);
        
        winnerLabelX = new JLabel("WINNER", SwingConstants.CENTER);
        winnerLabelX.setFont(new Font("Segoe UI", Font.BOLD, 16));
        winnerLabelX.setForeground(Color.YELLOW);
        winnerLabelX.setBounds(10, 235, 80, 20);
        winnerLabelX.setVisible(false);
        
        winnerLabelO = new JLabel("WINNER", SwingConstants.CENTER);
        winnerLabelO.setFont(new Font("Segoe UI", Font.BOLD, 16));
        winnerLabelO.setForeground(Color.YELLOW);
        winnerLabelO.setBounds(710, 235, 80, 20);
        winnerLabelO.setVisible(false);

        backgroundPanel.add(avatarX);
        backgroundPanel.add(avatarO);
        backgroundPanel.add(winnerLabelX);
        backgroundPanel.add(winnerLabelO);

        backgroundPanel.add(scrollPane);
        backgroundPanel.add(undoBtn);
        backgroundPanel.add(resetBtn);
        backgroundPanel.add(mainMenuBtn);
        backgroundPanel.add(backBtn);
        
        updateTurnIndicator();
    }
    
    private void updateTurnIndicator() {
        if (gameModel.isXTurn()) {
            avatarX.setActive(true);
            avatarO.setActive(false);
        } else {
            avatarX.setActive(false);
            avatarO.setActive(true);
        }
    }
    
    private void makeAIMove() {
        int[] bestMove = ai.getBestMove(gameModel.getArray());
        int r = bestMove[0];
        int c = bestMove[1];
        if (r != -1 && c != -1) {
            gameModel.saveState();
            gameModel.setArrayValue(r, c, 2);
            btn[r][c].repaint();
            gameModel.toggleTurn();
            updateTurnIndicator();
            checkGameOver(r, c, 2);
        }
    }
    
    private boolean checkGameOver(int r, int c, int player) {
        if (gameModel.checkWin(r, c, player)) {
            grid.repaint();
            if (player == 1) {
                avatarX.setActive(true);
                avatarO.setActive(false);
                winnerLabelX.setVisible(true);
            } else {
                avatarX.setActive(false);
                avatarO.setActive(true);
                winnerLabelO.setVisible(true);
            }
            disableBoard();
            new SoundPlayer(congratulationPath).playOnce();
            
            String winnerName = (player == 1) ? "Player 1" : (isVsAI ? "AI" : "Player 2");
            String msg = winnerName + " has claimed victory!\nDo you want to play another game?";
            
            int option = CustomDialog.showDialog(gameMenu, msg, "Notification", CustomDialog.YES_NO_OPTION);
            if (option == CustomDialog.YES_OPTION) gameMenu.getCardLayout().show(gameMenu.getCardPanel(), "MainMenu");
            return true;
        } else if (gameModel.isBoardFull()) {
            disableBoard();
            new SoundPlayer(alertPath).playOnce();
            int option = CustomDialog.showDialog(gameMenu, "It's a draw!\nDo you want to play another game?", "Notification", CustomDialog.YES_NO_OPTION);
            if (option == CustomDialog.YES_OPTION) gameMenu.getCardLayout().show(gameMenu.getCardPanel(), "MainMenu");
            return true;
        }
        return false;
    }
    
    // Legacy constructor for TwoPlayerPanel compatibility
    public TwoPlayerNxnPanel(GameMenu gameMenu, BackgroundPanel backgroundPanel, int n) {
        this(gameMenu, backgroundPanel, n, false);
    }
    
    private void updateGridSize() {
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                btn[i][j].setPreferredSize(new Dimension(cellSize, cellSize));
            }
        }
        grid.revalidate();
        grid.repaint();
    }

    public void undo() {
        if (gameModel.canUndo()) {
            gameModel.undo();
            updateGridSize(); 
        } else {
            try {
                UIManager.setLookAndFeel(new NimbusLookAndFeel());
                gameModel.setXTurn(true);
                CustomDialog.showDialog(gameMenu, "Hold on, you haven't made any moves yet!", "Hold On", CustomDialog.MESSAGE_OPTION);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private ImageIcon scaleImage(ImageIcon originalImage, int w, int h) {
        Image img = originalImage.getImage();
        if (w == 0 || h == 0) return originalImage;
        int padding = 10;
        int imgW = Math.max(1, w - padding);
        int imgH = Math.max(1, h - padding);
        Image scaledImage = img.getScaledInstance(imgW, imgH, Image.SCALE_SMOOTH);
        return new ImageIcon(scaledImage);
    }

    private void disableBoard() {
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                btn[i][j].setEnabled(false);
            }
        }
        undoBtn.setEnabled(false);
    }

    private void enableBoard() {
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                btn[i][j].setEnabled(true);
            }
        }
        undoBtn.setEnabled(true);
    }

    private void resetGame() {
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                btn[i][j].setIcon(null);
            }
        }
        gameModel.reset();
        winnerLabelX.setVisible(false);
        winnerLabelO.setVisible(false);
        enableBoard();
        updateTurnIndicator();
    }
}
