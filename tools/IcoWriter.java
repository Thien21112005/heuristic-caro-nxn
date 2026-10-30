import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Packs PNG files into a Windows .ico (PNG-compressed entries, supported since Vista).
 * Usage: java tools/IcoWriter.java <out.ico> <icon-16.png> <icon-32.png> ...
 * Each PNG must be square, at most 256 px.
 */
public class IcoWriter {
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: java tools/IcoWriter.java <out.ico> <png>...");
            System.exit(1);
        }
        List<byte[]> images = new ArrayList<>();
        List<Integer> sizes = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            byte[] png = Files.readAllBytes(new File(args[i]).toPath());
            ByteBuffer header = ByteBuffer.wrap(png, 16, 8); // IHDR width and height, big-endian
            int width = header.getInt();
            int height = header.getInt();
            if (width != height || width > 256) throw new IllegalArgumentException(args[i] + " must be square and <= 256 px");
            images.add(png);
            sizes.add(width);
        }

        int count = images.size();
        ByteBuffer dir = ByteBuffer.allocate(6 + 16 * count).order(ByteOrder.LITTLE_ENDIAN);
        dir.putShort((short) 0).putShort((short) 1).putShort((short) count); // reserved, type = icon, count
        int offset = 6 + 16 * count;
        for (int i = 0; i < count; i++) {
            int s = sizes.get(i);
            dir.put((byte) (s == 256 ? 0 : s)).put((byte) (s == 256 ? 0 : s)); // 0 means 256
            dir.put((byte) 0).put((byte) 0);                                  // palette, reserved
            dir.putShort((short) 1).putShort((short) 32);                      // planes, bits per pixel
            dir.putInt(images.get(i).length).putInt(offset);
            offset += images.get(i).length;
        }
        try (OutputStream out = new FileOutputStream(args[0])) {
            out.write(dir.array());
            for (byte[] png : images) out.write(png);
        }
        System.out.println("Wrote " + args[0] + " with " + count + " sizes");
    }
}
