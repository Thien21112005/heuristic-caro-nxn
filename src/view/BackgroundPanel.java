package view;

import javax.swing.*;
import java.awt.*;
import utils.ResourceUtils;

public class BackgroundPanel extends JPanel {
    private ImageIcon backgroundImage;

    public BackgroundPanel(String imagePath) {
        this.backgroundImage = ResourceUtils.getImageIcon(imagePath);
        this.setLayout(null);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null && backgroundImage.getImage() != null) {
            g.drawImage(backgroundImage.getImage(), 0, 0, getWidth(), getHeight(), this);
        }
    }
}
