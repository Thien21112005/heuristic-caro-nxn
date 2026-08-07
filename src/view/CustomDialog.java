package view;

import utils.SoundPlayer;
import javax.swing.*;
import java.awt.*;

public class CustomDialog extends JDialog {
    public static final int YES_NO_OPTION = 1;
    public static final int MESSAGE_OPTION = 2;
    
    public static final int YES_OPTION = 1;
    public static final int NO_OPTION = 0;
    
    private int result = NO_OPTION;

    public CustomDialog(JFrame parent, String message, String title, int optionType) {
        super(parent, title, true);
        setUndecorated(true);
        setSize(450, 220);
        setLocationRelativeTo(parent);
        
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Cyberpunk style gradient background
                GradientPaint gp = new GradientPaint(0, 0, new Color(15, 15, 25, 240), getWidth(), getHeight(), new Color(30, 10, 40, 240));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                
                // Neon Border
                g2d.setColor(new Color(0, 255, 255, 200));
                g2d.setStroke(new BasicStroke(3f));
                g2d.drawRect(1, 1, getWidth() - 2, getHeight() - 2);
            }
        };
        panel.setLayout(null);
        panel.setOpaque(false);
        
        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(new Color(0, 255, 255));
        titleLabel.setBounds(20, 15, 410, 30);
        panel.add(titleLabel);
        
        String htmlMessage = "<html><div style='text-align: center;'>" + message.replaceAll("\n", "<br>") + "</div></html>";
        JLabel messageLabel = new JLabel(htmlMessage, SwingConstants.CENTER);
        messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        messageLabel.setForeground(Color.WHITE);
        messageLabel.setBounds(20, 50, 410, 90);
        panel.add(messageLabel);
        
        if (optionType == YES_NO_OPTION) {
            JButton yesBtn = new CustomButton("Yes");
            yesBtn.setBounds(100, 150, 100, 40);
            yesBtn.addActionListener(e -> {
                new SoundPlayer("/assets/Select.wav").playOnce();
                result = YES_OPTION;
                dispose();
            });
            
            JButton noBtn = new CustomButton("No");
            noBtn.setBounds(250, 150, 100, 40);
            noBtn.addActionListener(e -> {
                new SoundPlayer("/assets/Select.wav").playOnce();
                result = NO_OPTION;
                dispose();
            });
            
            panel.add(yesBtn);
            panel.add(noBtn);
        } else {
            JButton okBtn = new CustomButton("OK");
            okBtn.setBounds(175, 150, 100, 40);
            okBtn.addActionListener(e -> {
                new SoundPlayer("/assets/Select.wav").playOnce();
                result = YES_OPTION;
                dispose();
            });
            panel.add(okBtn);
        }
        
        add(panel);
    }
    
    public static int showDialog(JFrame parent, String message, String title, int optionType) {
        CustomDialog dialog = new CustomDialog(parent, message, title, optionType);
        dialog.setVisible(true);
        return dialog.result;
    }
}
