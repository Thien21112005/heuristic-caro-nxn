package main;

import view.GameMenu;

public class RunGame {
    public static void main(String[] args) {
        String bgMusic = "/assets/bgmusic.wav";
        new GameMenu("Game Tic Tac Toe", bgMusic);
    }
}
