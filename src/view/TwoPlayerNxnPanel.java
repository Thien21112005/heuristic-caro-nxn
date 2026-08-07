package view;

import controller.ButtonHoverEffect;
import model.BoardState;
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

    private Stack<BoardState[][]> stack;
    private JButton[][] btn;
    private JButton undoBtn, resetBtn, mainMenuBtn, backBtn;

    public TwoPlayerNxnPanel(GameMenu gameMenu, BackgroundPanel backgroundPanel, int n) {
        this.n = n;
        this.gameMenu = gameMenu;
        this.backgroundPanel = backgroundPanel;
        this.gameModel = new GameModel(n);
        this.stack = new Stack<>();
        this.btn = new JButton[n][n];

        this.xImage = ResourceUtils.getImageIcon("/assets/x_image.png");
        this.oImage = ResourceUtils.getImageIcon("/assets/o_image.png");

        SoundPlayer alert = new SoundPlayer(alertPath);
        SoundPlayer congratulation = new SoundPlayer(congratulationPath);

        JPanel grid = new JPanel(new GridLayout(n, n));
        grid.setBounds(100, 75, 600, 450);
        
        undoBtn = new CustomButton("Undo");
        undoBtn.setBounds(350, 30, 100, 30);
        resetBtn = new CustomButton("Reset");
        resetBtn.setBounds(600, 30, 100, 30);
        mainMenuBtn = new CustomButton("Main Menu");
        mainMenuBtn.setBounds(350, 540, 130, 30);
        backBtn = new CustomButton("Back");
        backBtn.setBounds(100, 30, 100, 30);

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
                btn[i][j].setPreferredSize(new Dimension(100, 100));
                int finalI = i;
                int finalJ = j;
                btn[i][j].addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (((JButton) e.getSource()).getIcon() == null) {
                            saveBoardState();
                            gameModel.saveState();

                            if (gameModel.isXTurn()) {
                                ((JButton) e.getSource()).setIcon(scaleImage(xImage, (JButton) e.getSource()));
                                gameModel.setArrayValue(finalI, finalJ, 1);
                            } else {
                                ((JButton) e.getSource()).setIcon(scaleImage(oImage, (JButton) e.getSource()));
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

        backgroundPanel.add(grid);
        backgroundPanel.add(undoBtn);
        backgroundPanel.add(resetBtn);
        backgroundPanel.add(mainMenuBtn);
        backgroundPanel.add(backBtn);
    }

    private void saveBoardState() {
        BoardState[][] currentState = new BoardState[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                currentState[i][j] = new BoardState(btn[i][j].getIcon());
            }
        }
        stack.push(currentState);
    }

    public void undo() {
        if (!stack.isEmpty() && gameModel.canUndo()) {
            BoardState[][] previousState = stack.pop();
            gameModel.undo();
            restoreBoardState(previousState);
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

    private void restoreBoardState(BoardState[][] state) {
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                btn[i][j].setIcon(state[i][j].getIcon());
            }
        }
    }

    private ImageIcon scaleImage(ImageIcon originalImage, JButton button) {
        Image img = originalImage.getImage();
        int buttonWidth = button.getWidth();
        int buttonHeight = button.getHeight();
        if (buttonWidth == 0 || buttonHeight == 0) return originalImage;
        Image scaledImage = img.getScaledInstance(buttonWidth, buttonHeight, Image.SCALE_SMOOTH);
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
        stack.clear();
        gameModel.reset();
        enableBoard();
    }
}
