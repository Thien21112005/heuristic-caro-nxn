package main;

import view.MainWindow;
import view.theme.Theme;

import javax.swing.SwingUtilities;

public class RunGame {
    public static void main(String[] args) {
        Theme.install();
        SwingUtilities.invokeLater(() -> new MainWindow("Caro"));
    }
}
