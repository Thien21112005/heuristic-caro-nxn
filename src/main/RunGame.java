package main;

import model.Settings;
import utils.I18n;
import view.MainWindow;
import view.theme.Theme;

import javax.swing.SwingUtilities;

public class RunGame {
    public static void main(String[] args) {
        I18n.setLanguage(Settings.get().getLanguage());
        Theme.install();
        SwingUtilities.invokeLater(() -> new MainWindow(I18n.t("app.title")));
    }
}
