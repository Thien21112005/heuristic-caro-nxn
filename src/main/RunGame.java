package main;

import view.GameMenu;
import view.theme.Theme;

import javax.swing.SwingUtilities;

public class RunGame {
    public static void main(String[] args) {
        Theme.install();
        String bgMusic = "/assets/bgmusic.wav";
        SwingUtilities.invokeLater(() -> new GameMenu("Game Tic Tac Toe", bgMusic));
    }
}
