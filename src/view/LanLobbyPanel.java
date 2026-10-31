package view;

import controller.LanMatchmaker;
import controller.NetworkGameController;
import model.Settings;
import net.LanDiscovery;
import net.LanHost;
import utils.I18n;
import view.components.AppButton;
import view.components.CenteringScroll;
import view.components.RoundedPanel;
import view.components.SegmentedControl;
import view.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.List;

/**
 * LAN lobby: pick a name, then host a room (and wait) or join one.
 */
public class LanLobbyPanel extends PaperPanel {
    static final int COLUMN_WIDTH = 400;
    private static final int[] SIZES = {3, 10, 15, 20};
    private static final int[] TIMES = {0, 20, 30, 60};

    private final MainWindow window;
    private final JTextField nameField = new JTextField();
    private final CardLayout hostCards = new CardLayout();
    private final JPanel hostArea = new JPanel(hostCards);
    private final SegmentedControl<Integer> size = new SegmentedControl<>();
    private final SegmentedControl<Integer> time = new SegmentedControl<>();
    private final JLabel waitingTitle = new JLabel();
    private final JLabel addressLabel = new JLabel();
    private final JLabel hostError = new JLabel();
    private final Timer dots;
    private int dotCount;
    private LanMatchmaker matchmaker;

    // Join column
    private final JPanel roomList = column();
    private final JTextField addressField = new JTextField();
    private final JLabel joinStatus = new JLabel(" ");
    private final AppButton connect = new AppButton(I18n.t("lan.join.connect"), AppButton.Variant.SECONDARY);
    private LanDiscovery.Browser browser;
    private boolean joining;

    public LanLobbyPanel(MainWindow window) {
                this.window = window;
        setLayout(new BorderLayout());
        add(TopBar.withBack(this::leave), BorderLayout.NORTH);

        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 0));
        card.setBorder(BorderFactory.createEmptyBorder(32, 40, 36, 40));

        JPanel header = column();
        JLabel title = new JLabel(I18n.t("lan.title"));
        title.setFont(Theme.handBold(32f));
        title.setForeground(Theme.TEXT);
        JLabel subtitle = new JLabel("<html>" + I18n.t("lan.subtitle") + "</html>");
        subtitle.setFont(Theme.font(Font.PLAIN, 16f));
        subtitle.setForeground(Theme.TEXT_MUTED);
        addRow(header, title, 0);
        addRow(header, subtitle, 6);
        addRow(header, SetupPanel.sectionLabel(I18n.t("lan.name")), 22);
        styleField(nameField, I18n.t("lan.name.placeholder"));
        nameField.setText(Settings.get().getPlayerName());
        nameField.setMaximumSize(new Dimension(COLUMN_WIDTH, 44));
        addRow(header, nameField, 10);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 24, 0));

        hostArea.setOpaque(false);
        hostArea.add(buildHostForm(), "form");
        hostArea.add(buildWaiting(), "waiting");

        JPanel body = new JPanel(new GridLayout(1, 2, 40, 0));
        body.setOpaque(false);
        body.add(hostArea);
        body.add(buildJoinColumn());

        card.add(header, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        card.setPreferredSize(new Dimension(2 * COLUMN_WIDTH + 40 + 80, card.getPreferredSize().height));
        add(CenteringScroll.wrap(card), BorderLayout.CENTER);

        dots = new Timer(400, e -> {
            dotCount = (dotCount + 1) % 4;
            waitingTitle.setText(I18n.t("lan.waiting") + ".".repeat(dotCount));
        });

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                startBrowsing();
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                cancel();
                stopBrowsing();
            }
        });
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "back");
        getActionMap().put("back", new AbstractAction() {
            @Override
            public boolean isEnabled() { return isShowing(); }

            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) { leave(); }
        });
    }

    private JComponent buildHostForm() {
        JPanel col = column();
        for (int s : SIZES) size.add(s, s + "×" + s);
        size.setSelected(15);
        for (int t : TIMES) time.add(t, t == 0 ? I18n.t("setup.turnTime.off") : I18n.t("setup.turnTime.seconds", t));
        time.setSelected(0);

        AppButton create = new AppButton(I18n.t("lan.host.create"), AppButton.Variant.PRIMARY);
        create.setFont(Theme.semibold(18f));
        create.addActionListener(e -> startHosting());
        create.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));

        hostError.setFont(Theme.font(Font.PLAIN, 14f));
        hostError.setForeground(Theme.DANGER);

        addRow(col, SetupPanel.sectionLabel(I18n.t("lan.host")), 0);
        addRow(col, SetupPanel.sectionLabel(I18n.t("setup.boardSize")), 16);
        addRow(col, size, 10);
        addRow(col, SetupPanel.sectionLabel(I18n.t("setup.turnTime")), 18);
        addRow(col, time, 10);
        addRow(col, create, 24);
        addRow(col, hostError, 10);
        return col;
    }

    private JComponent buildJoinColumn() {
        JPanel col = column();
        RoundedPanel rooms = new RoundedPanel(Theme.RADIUS);
        rooms.setFill(Theme.alpha(Theme.SURFACE_ALT, 200));
        rooms.setLayout(new BorderLayout());
        rooms.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        rooms.add(roomList, BorderLayout.NORTH);
        rooms.setPreferredSize(new Dimension(COLUMN_WIDTH, 168));
        rooms.setMaximumSize(new Dimension(Integer.MAX_VALUE, 168));
        showRooms(List.of());

        styleField(addressField, I18n.t("lan.join.placeholder"));
        addressField.addActionListener(e -> connect.doClick());
        connect.compact().setFont(Theme.semibold(15f));
        connect.addActionListener(e -> joinManual());
        JPanel manual = new JPanel(new BorderLayout(10, 0));
        manual.setOpaque(false);
        manual.add(addressField, BorderLayout.CENTER);
        manual.add(connect, BorderLayout.EAST);
        manual.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        joinStatus.setFont(Theme.font(Font.PLAIN, 14f));
        joinStatus.setForeground(Theme.TEXT_MUTED);

        addRow(col, SetupPanel.sectionLabel(I18n.t("lan.join")), 0);
        addRow(col, rooms, 16);
        addRow(col, SetupPanel.sectionLabel(I18n.t("lan.join.manual")), 18);
        addRow(col, manual, 10);
        addRow(col, joinStatus, 10);
        return col;
    }

    private void showRooms(List<LanDiscovery.Room> rooms) {
        roomList.removeAll();
        if (rooms.isEmpty()) {
            JLabel empty = new JLabel("<html><div style='width:260px'>" + I18n.t("lan.join.searching") + "</div></html>");
            empty.setFont(Theme.font(Font.PLAIN, 14f));
            empty.setForeground(Theme.TEXT_MUTED);
            empty.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            addRow(roomList, empty, 0);
        }
        for (LanDiscovery.Room room : rooms.subList(0, Math.min(3, rooms.size()))) {
            AppButton row = new AppButton("<html><b>" + escape(room.name()) + "</b> &nbsp;\u00b7&nbsp; "
                    + room.size() + "\u00d7" + room.size() + " &nbsp;\u00b7&nbsp; " + room.address() + "</html>",
                    AppButton.Variant.SECONDARY);
            row.compact().setFont(Theme.font(Font.PLAIN, 14f));
            row.setHorizontalAlignment(SwingConstants.LEFT);
            row.setToolTipText(I18n.t("lan.join.room.tooltip"));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
            row.addActionListener(e -> join(room.address(), room.port()));
            row.setEnabled(!joining);
            addRow(roomList, row, roomList.getComponentCount() == 0 ? 0 : 6);
        }
        roomList.revalidate();
        roomList.repaint();
    }

    private void joinManual() {
        String text = addressField.getText().trim();
        if (text.isEmpty()) {
            joinStatus.setForeground(Theme.DANGER);
            joinStatus.setText(I18n.t("lan.join.enterAddress"));
            return;
        }
        String host = text;
        int port = LanHost.DEFAULT_PORT;
        int colon = text.lastIndexOf(':');
        if (colon > 0) {
            host = text.substring(0, colon).trim();
            try {
                port = Integer.parseInt(text.substring(colon + 1).trim());
            } catch (NumberFormatException e) {
                port = -1;
            }
            if (port < 1 || port > 65535) {
                joinStatus.setForeground(Theme.DANGER);
                joinStatus.setText(I18n.t("lan.join.badPort"));
                return;
            }
        }
        join(host, port);
    }

    private void join(String address, int port) {
        if (joining || matchmaker != null) return;
        joining = true;
        connect.setEnabled(false);
        joinStatus.setForeground(Theme.TEXT_MUTED);
        joinStatus.setText(I18n.t("lan.join.connecting", address));
        matchmaker = new LanMatchmaker(callbacks(joinStatus));
        matchmaker.join(playerName(), address, port);
    }

    private void startBrowsing() {
        if (browser != null) return;
        try {
            browser = new LanDiscovery.Browser(this::showRooms);
        } catch (IOException e) {
            // Another program holds the discovery port; typing an address still works
            joinStatus.setText(I18n.t("lan.join.noSearch"));
        }
    }

    private void stopBrowsing() {
        if (browser != null) browser.close();
        browser = null;
        showRooms(List.of());
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private JComponent buildWaiting() {
        JPanel col = column();
        waitingTitle.setFont(Theme.semibold(20f));
        waitingTitle.setForeground(Theme.TEXT);
        addressLabel.setFont(Theme.font(Font.PLAIN, 15f));
        addressLabel.setForeground(Theme.TEXT);
        JLabel firewall = new JLabel("<html><div style='width:280px'>" + I18n.t("lan.firewall") + "</div></html>");
        firewall.setFont(Theme.font(Font.PLAIN, 13f));
        firewall.setForeground(Theme.TEXT_MUTED);

        AppButton cancel = new AppButton(I18n.t("common.cancel"), AppButton.Variant.SECONDARY);
        cancel.addActionListener(e -> cancel());
        cancel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));

        addRow(col, waitingTitle, 6);
        addRow(col, addressLabel, 14);
        addRow(col, firewall, 14);
        addRow(col, cancel, 24);
        return col;
    }

    private void startHosting() {
        hostError.setText(" ");
        String name = playerName();
        matchmaker = new LanMatchmaker(callbacks(hostError));
        try {
            stopBrowsing(); // our own announcement would otherwise show up as a room
            int port = matchmaker.host(name, size.getSelected(), time.getSelected());
            List<String> addresses = LanHost.localAddresses();
            String where = addresses.isEmpty() ? I18n.t("lan.noNetwork") : String.join(", ", addresses);
            addressLabel.setText("<html>" + I18n.t("lan.joinAt") + " <b>" + where + "</b> &nbsp;·&nbsp; "
                    + I18n.t("lan.port", port) + "</html>");
            waitingTitle.setText(I18n.t("lan.waiting"));
            dots.start();
            hostCards.show(hostArea, "waiting");
        } catch (IOException e) {
            matchmaker = null;
            hostError.setText(I18n.t("lan.error.busy"));
        }
    }

    LanMatchmaker.Callbacks callbacks(JLabel errorTarget) {
        return new LanMatchmaker.Callbacks() {
            @Override
            public void onReady(NetworkGameController game) {
                matchmaker = null;
                resetHostArea();
                resetJoin(" ");
                window.startLanGame(game);
            }

            @Override
            public void onError(String code) {
                matchmaker = null;
                resetHostArea();
                resetJoin(" ");
                errorTarget.setForeground(Theme.DANGER);
                errorTarget.setText("<html><div style='width:280px'>" + I18n.t("lan.error." + code) + "</div></html>");
                if (isShowing()) startBrowsing();
            }
        };
    }

    private void resetJoin(String status) {
        joining = false;
        connect.setEnabled(true);
        joinStatus.setForeground(Theme.TEXT_MUTED);
        joinStatus.setText(status);
    }

    String playerName() {
        String name = net.Protocol.cleanName(nameField.getText());
        nameField.setText(name);
        Settings.get().setPlayerName(name);
        return name;
    }

    private void resetHostArea() {
        dots.stop();
        hostCards.show(hostArea, "form");
    }

    private void cancel() {
        if (matchmaker != null) matchmaker.close();
        matchmaker = null;
        resetHostArea();
        resetJoin(" ");
        if (isShowing()) startBrowsing();
    }

    private void leave() {
        cancel();
        window.showScreen(MainWindow.MAIN_MENU);
    }

    static void styleField(JTextField field, String placeholder) {
        field.putClientProperty("JTextField.placeholderText", placeholder);
        field.setFont(Theme.font(Font.PLAIN, 16f));
        field.setBackground(Theme.SURFACE_ALT);
        field.setForeground(Theme.TEXT);
        field.setCaretColor(Theme.TEXT);
        field.setPreferredSize(new Dimension(COLUMN_WIDTH, 44));
    }

    static JPanel column() {
        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        return col;
    }

    static void addRow(JPanel col, JComponent c, int gapBefore) {
        if (gapBefore > 0) col.add(Box.createVerticalStrut(gapBefore));
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        col.add(c);
    }
}
