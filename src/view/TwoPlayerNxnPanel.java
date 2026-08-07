package view;

import controller.ButtonHoverEffect;
import model.BoardState; // kept for legacy
import model.GameModel;
import utils.ResourceUtils;
import utils.SoundPlayer;

import javax.swing.*;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Stack;

public class TwoPlayerNxnPanel extends JPanel {
    private GameMenu gameMenu;
    private BackgroundPanel backgroundPanel;
    private int n;
    private GameModel gameModel;
    
    private String selectPath = "/assets/Select.wav";
    private String scrollPath = "/assets/Scroll.wav";
    private String alertPath = "/assets/Alert.wav";
    private String congratulationPath = "/assets/Congratulation.wav";
    
    private ImageIcon xImage;
    private ImageIcon oImage;

    private JButton[][] btn;
    private JButton undoBtn, resetBtn, mainMenuBtn, backBtn;
    private JButton zoomInBtn, zoomOutBtn;
    
    private JPanel grid;
    private JScrollPane scrollPane;
    private int cellSize = 60; // Default cell size

    public TwoPlayerNxnPanel(GameMenu gameMenu, BackgroundPanel backgroundPanel, int n) {
        this.n = n;
        this.gameMenu = gameMenu;
        this.backgroundPanel = backgroundPanel;
        this.gameModel = new GameModel(n);
        this.btn = new JButton[n][n];

        this.xImage = ResourceUtils.getImageIcon("/assets/x_image.png");
        this.oImage = ResourceUtils.getImageIcon("/assets/o_image.png");

        SoundPlayer alert = new SoundPlayer(alertPath);
        SoundPlayer congratulation = new SoundPlayer(congratulationPath);

        grid = new JPanel(new GridLayout(n, n));
        grid.setBackground(Color.DARK_GRAY);
        
        scrollPane = new JScrollPane(grid);
        scrollPane.setBounds(100, 75, 600, 450);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16); // Faster scrolling
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        
        undoBtn = new CustomButton("Undo");
        undoBtn.setBounds(220, 20, 100, 40);
        resetBtn = new CustomButton("Reset");
        resetBtn.setBounds(580, 20, 100, 40);
        mainMenuBtn = new CustomButton("Main Menu");
        mainMenuBtn.setBounds(320, 540, 160, 40);
        backBtn = new CustomButton("Back");
        backBtn.setBounds(100, 20, 100, 40);
        
        zoomInBtn = new CustomButton("+");
        zoomInBtn.setBounds(340, 20, 100, 40);
        zoomOutBtn = new CustomButton("-");
        zoomOutBtn.setBounds(460, 20, 100, 40);

        zoomInBtn.addMouseListener(new ButtonHoverEffect(zoomInBtn, scrollPath));
        zoomInBtn.addActionListener(e -> {
            new SoundPlayer(selectPath).playOnce();
            if (cellSize < 150) {
                cellSize += 10;
                updateGridSize();
            }
        });

        zoomOutBtn.addMouseListener(new ButtonHoverEffect(zoomOutBtn, scrollPath));
        zoomOutBtn.addActionListener(e -> {
            new SoundPlayer(selectPath).playOnce();
            if (cellSize > 30) {
                cellSize -= 10;
                updateGridSize();
            }
        });

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
                if (option == JOptionPane.YES_OPTION) gameMenu.getCardLayout().show(gameMenu.getCardPanel(), "TwoPlayerPanel");
            }
        });

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                btn[i][j] = new JButton("");
                btn[i][j].setPreferredSize(new Dimension(cellSize, cellSize));
                btn[i][j].setBackground(new Color(245, 245, 245));
                btn[i][j].setFocusPainted(false);
                btn[i][j].setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
                
                int finalI = i;
                int finalJ = j;
                btn[i][j].addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (((JButton) e.getSource()).getIcon() == null) {
                            gameModel.saveState();

                            if (gameModel.isXTurn()) {
                                ((JButton) e.getSource()).setIcon(scaleImage(xImage, cellSize, cellSize));
                                gameModel.setArrayValue(finalI, finalJ, 1);
                            } else {
                                ((JButton) e.getSource()).setIcon(scaleImage(oImage, cellSize, cellSize));
                                gameModel.setArrayValue(finalI, finalJ, 2);
                            }

                            gameModel.toggleTurn();

                            if (gameModel.checkWin(finalI, finalJ, 1)) {
                                disableBoard();
                                congratulation.playOnce();
                                int option = JOptionPane.showOptionDialog(null, "Winner is X. Do you want to return to the main menu?", "Notification", JOptionPane.YES_OPTION, JOptionPane.INFORMATION_MESSAGE, null, new Object[]{"Yes"}, "Yes");
                                if (option == JOptionPane.YES_OPTION) gameMenu.getCardLayout().show(gameMenu.getCardPanel(), "MainMenu");
                            } else if (gameModel.checkWin(finalI, finalJ, 2)) {
                                disableBoard();
                                congratulation.playOnce();
                                int option = JOptionPane.showOptionDialog(null, "Winner is O. Do you want to return to the main menu?", "Notification", JOptionPane.YES_OPTION, JOptionPane.INFORMATION_MESSAGE, null, new Object[]{"Yes"}, "Yes");
                                if (option == JOptionPane.YES_OPTION) gameMenu.getCardLayout().show(gameMenu.getCardPanel(), "MainMenu");
                            } else if (gameModel.isBoardFull()) {
                                disableBoard();
                                alert.playOnce();
                                int option = JOptionPane.showOptionDialog(null, "It's a draw!. Do you want to return to the main menu?", "Notification", JOptionPane.YES_OPTION, JOptionPane.INFORMATION_MESSAGE, null, new Object[]{"Yes"}, "Yes");
                                if (option == JOptionPane.YES_OPTION) gameMenu.getCardLayout().show(gameMenu.getCardPanel(), "MainMenu");
                            }
                        }
                    }
                });
                grid.add(btn[i][j]);
            }
        }

        backgroundPanel.add(scrollPane);
        backgroundPanel.add(undoBtn);
        backgroundPanel.add(resetBtn);
        backgroundPanel.add(mainMenuBtn);
        backgroundPanel.add(backBtn);
        backgroundPanel.add(zoomInBtn);
        backgroundPanel.add(zoomOutBtn);
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
    }
}
