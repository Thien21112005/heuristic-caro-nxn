package utils;

import java.net.URL;
import javax.swing.ImageIcon;

public class ResourceUtils {
    public static URL getResource(String path) {
        URL url = ResourceUtils.class.getResource(path);
        if (url == null) {
            System.err.println("Resource not found: " + path);
        }
        return url;
    }

    public static ImageIcon getImageIcon(String path) {
        URL url = getResource(path);
        if (url != null) {
            return new ImageIcon(url);
        }
        return new ImageIcon();
    }
}
