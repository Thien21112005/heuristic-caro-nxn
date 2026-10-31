package view;

import utils.I18n;
import utils.SoundManager;
import view.components.AppButton;
import view.components.RoundedPanel;
import view.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Modal dialog drawn as a card over a dimmed copy of the window.
 * Enter confirms, Escape cancels.
 */
public class CustomDialog extends JDialog {
    public static final int YES_NO_OPTION = 1;
    public static final int MESSAGE_OPTION = 2;

    public static final int YES_OPTION = 1;
    public static final int NO_OPTION = 0;

    private int result = NO_OPTION;

    private CustomDialog(JFrame parent, String title, String message, String yesLabel, String noLabel,
                         AppButton.Variant yesVariant, Color accent) {
        super(parent, title, true);
        setUndecorated(true);

        boolean translucent = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
                .isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT);
        if (translucent) {
            setBackground(new Color(0, 0, 0, 0));
        }

        JPanel backdrop = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(translucent ? Theme.alpha(Theme.BG, 170) : Theme.BG);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        backdrop.setOpaque(false);

        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 18));
        card.setFill(Theme.SURFACE);
        card.setBorderColor(Theme.alpha(accent, 140));
        card.setBorder(BorderFactory.createEmptyBorder(28, 32, 24, 32));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(Theme.handBold(22f));
        titleLabel.setForeground(Theme.TEXT);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel messageLabel = new JLabel("<html><div style='text-align:center;width:300px'>"
                + message.replace("\n", "<br>") + "</div></html>");
        messageLabel.setFont(Theme.font(Font.PLAIN, 15f));
        messageLabel.setForeground(Theme.TEXT_MUTED);
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        messageLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        text.add(titleLabel);
        text.add(messageLabel);

        JPanel buttons = new JPanel(new GridLayout(1, 0, 12, 0));
        buttons.setOpaque(false);
        AppButton yesBtn = new AppButton(yesLabel, yesVariant);
        yesBtn.addActionListener(e -> close(YES_OPTION));
        if (noLabel != null) {
            AppButton noBtn = new AppButton(noLabel, AppButton.Variant.SECONDARY);
            noBtn.addActionListener(e -> close(NO_OPTION));
            buttons.add(noBtn);
        }
        buttons.add(yesBtn);

        card.add(text, BorderLayout.CENTER);
        card.add(buttons, BorderLayout.SOUTH);
        backdrop.add(card);
        setContentPane(backdrop);

        getRootPane().setDefaultButton(yesBtn);
        getRootPane().registerKeyboardAction(e -> close(noLabel != null ? NO_OPTION : YES_OPTION),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        if (parent != null && parent.isShowing()) {
            setBounds(parent.getBounds());
        } else {
            pack();
            setLocationRelativeTo(parent);
        }
    }

    private void close(int value) {
        result = value;
        dispose();
    }

    /** Yes/No question with custom button labels. Returns YES_OPTION or NO_OPTION. */
    public static int confirm(JFrame parent, String title, String message, String yesLabel, String noLabel,
                              boolean destructive) {
        CustomDialog dialog = new CustomDialog(parent, title, message, yesLabel, noLabel,
                destructive ? AppButton.Variant.DANGER : AppButton.Variant.PRIMARY,
                destructive ? Theme.DANGER : Theme.ACCENT);
        dialog.setVisible(true);
        return dialog.result;
    }

    /** Informational message with a single button. */
    public static void message(JFrame parent, String title, String message) {
        SoundManager.play(SoundManager.ALERT);
        new CustomDialog(parent, title, message, I18n.t("common.gotIt"), null, AppButton.Variant.PRIMARY, Theme.ACCENT)
                .setVisible(true);
    }

    /** Legacy API kept for existing callers. */
    public static int showDialog(JFrame parent, String message, String title, int optionType) {
        if (optionType == YES_NO_OPTION) {
            return confirm(parent, title, message, I18n.t("common.yes"), I18n.t("common.no"), false);
        }
        new CustomDialog(parent, title, message, I18n.t("common.ok"), null, AppButton.Variant.PRIMARY, Theme.ACCENT)
                .setVisible(true);
        return YES_OPTION;
    }
}
