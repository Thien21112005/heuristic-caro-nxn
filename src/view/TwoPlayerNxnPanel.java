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
    private int[] aiDelays = {400, 600, 800, 1000, 1200, 1500};

    public TwoPlayerNxnPanel(GameMenu gameMenu, BackgroundPanel backgroundPanel, int n, boolean isVsAI) {
        this.n = n;
        this.gameMenu = gameMenu;
        this.backgroundPanel = backgroundPanel;
        this.gameModel = new GameModel(n);
        this.isVsAI = isVsAI;
        if (isVsAI) {
            this.ai = new GameAI(n, 2, 1);
        }
        
        this.btn = new JButton[n][n];

        this.xImage = ResourceUtils.getImageIcon("/assets/x_image.png");
        this.oImage = ResourceUtils.getImageIcon("/assets/o_image.png");

        grid = new JPanel(new GridLayout(n, n));
        grid.setBackground(Color.DARK_GRAY);
        
        scrollPane = new JScrollPane(grid);
        scrollPane.setBounds(100, 75, 600, 450);
        
        undoBtn = new CustomButton("Undo");
        undoBtn.setBounds(340, 20, 100, 40);
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
                int option = JOptionPane.showConfirmDialog(null, "Do you want to reset the game?", "Confirm Reset", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (option == JOptionPane.YES_OPTION) resetGame();
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
                int option = JOptionPane.showOptionDialog(null, "Are you sure you want to return to the main menu? All progress will be lost.", "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, new Object[]{"Yes", "No"}, "No");
                if (option == JOptionPane.YES_OPTION) gameMenu.getCardLayout().show(gameMenu.getCardPanel(), "MainMenu");
            }
        });

        backBtn.addMouseListener(new ButtonHoverEffect(backBtn, scrollPath));
        backBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new SoundPlayer(selectPath).playOnce();
                int option = JOptionPane.showOptionDialog(null, "Are you sure you want to return to the main menu? All progress will be lost.", "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, new Object[]{"Yes", "No"}, "No");
                if (option == JOptionPane.YES_OPTION) gameMenu.getCardLayout().show(gameMenu.getCardPanel(), isVsAI ? "OnePlayerPanel" : "TwoPlayerPanel");
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
                    if (cellSize > 30) {
                        cellSize -= 10;
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
                btn[i][j] = new JButton("");
                btn[i][j].setPreferredSize(new Dimension(cellSize, cellSize));
                btn[i][j].setBackground(new Color(245, 245, 245));
                btn[i][j].setFocusPainted(false);
                btn[i][j].setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
                
                btn[i][j].addMouseWheelListener(wheelAdapter);
                btn[i][j].addMouseListener(panAdapter);
                btn[i][j].addMouseMotionListener(panAdapter);
                
                int finalI = i;
                int finalJ = j;
                btn[i][j].addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (isDragging) return;
                        
                        // Ignore player clicks if it's AI's turn
                        if (isVsAI && !gameModel.isXTurn()) return;
                        
                        if (((JButton) e.getSource()).getIcon() == null) {
                            gameModel.saveState();

                            if (gameModel.isXTurn()) {
                                ((JButton) e.getSource()).setIcon(scaleImage(xImage, cellSize, cellSize));
                                gameModel.setArrayValue(finalI, finalJ, 1);
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
                                ((JButton) e.getSource()).setIcon(scaleImage(oImage, cellSize, cellSize));
                                gameModel.setArrayValue(finalI, finalJ, 2);
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

        avatarX = new AvatarPanel("/assets/avatar_x.jpg", Color.CYAN);
        avatarX.setBounds(10, 260, 80, 80);
        
        avatarO = new AvatarPanel("/assets/avatar_o.jpg", Color.RED);
        avatarO.setBounds(710, 260, 80, 80);

        backgroundPanel.add(avatarX);
        backgroundPanel.add(avatarO);

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
            btn[r][c].setIcon(scaleImage(oImage, cellSize, cellSize));
            gameModel.setArrayValue(r, c, 2);
            gameModel.toggleTurn();
            updateTurnIndicator();
            checkGameOver(r, c, 2);
        }
    }
    
    private boolean checkGameOver(int r, int c, int player) {
        if (gameModel.checkWin(r, c, player)) {
            disableBoard();
            new SoundPlayer(congratulationPath).playOnce();
            String msg = (player == 1) ? "Winner is X." : "Winner is O.";
            int option = JOptionPane.showOptionDialog(null, msg + " Do you want to return to the main menu?", "Notification", JOptionPane.YES_OPTION, JOptionPane.INFORMATION_MESSAGE, null, new Object[]{"Yes"}, "Yes");
            if (option == JOptionPane.YES_OPTION) gameMenu.getCardLayout().show(gameMenu.getCardPanel(), "MainMenu");
            return true;
        } else if (gameModel.isBoardFull()) {
            disableBoard();
            new SoundPlayer(alertPath).playOnce();
            int option = JOptionPane.showOptionDialog(null, "It's a draw!. Do you want to return to the main menu?", "Notification", JOptionPane.YES_OPTION, JOptionPane.INFORMATION_MESSAGE, null, new Object[]{"Yes"}, "Yes");
            if (option == JOptionPane.YES_OPTION) gameMenu.getCardLayout().show(gameMenu.getCardPanel(), "MainMenu");
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
                int val = gameModel.getArray()[i][j];
                if (val == 1) {
                    btn[i][j].setIcon(scaleImage(xImage, cellSize, cellSize));
                } else if (val == 2) {
                    btn[i][j].setIcon(scaleImage(oImage, cellSize, cellSize));
                } else {
                    btn[i][j].setIcon(null);
                }
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
                JOptionPane.showMessageDialog(null, "Nothing to undo", "Error", JOptionPane.ERROR_MESSAGE);
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
        enableBoard();
        updateTurnIndicator();
    }
}
