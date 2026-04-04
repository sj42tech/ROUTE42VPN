import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.datamatrix.DataMatrixWriter;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

public final class RenderVpsDataMatrix {
    private static final int SIZE_PX = 1024;

    private RenderVpsDataMatrix() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1 || args.length > 2) {
            printUsage();
            System.exit(64);
            return;
        }

        Path manifestPath = Path.of(args[0]);
        Path outputDir = args.length > 1
            ? Path.of(args[1])
            : Path.of("build/share-codes");

        if (!Files.isRegularFile(manifestPath)) {
            throw new IllegalArgumentException("Manifest file not found: " + manifestPath);
        }

        Map<String, String> links = loadLinks(manifestPath);
        Files.createDirectories(outputDir);

        for (Map.Entry<String, String> entry : links.entrySet()) {
            BufferedImage image = render(entry.getValue());
            Path outputPath = outputDir.resolve(entry.getKey());
            ImageIO.write(image, "png", outputPath.toFile());
            System.out.println(outputPath.toAbsolutePath());
        }
    }

    private static Map<String, String> loadLinks(Path manifestPath) throws IOException {
        Map<String, String> links = new LinkedHashMap<>();
        List<String> lines = Files.readAllLines(manifestPath, StandardCharsets.UTF_8);

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] parts = line.split("\\t", 2);
            if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
                throw new IllegalArgumentException(
                    "Invalid manifest row. Expected <filename><TAB><vless-link>: " + rawLine
                );
            }

            links.put(parts[0].trim(), parts[1].trim());
        }

        if (links.isEmpty()) {
            throw new IllegalArgumentException("No links found in manifest: " + manifestPath);
        }

        return links;
    }

    private static void printUsage() {
        System.err.println("Usage: RenderVpsDataMatrix <manifest.tsv> [output-dir]");
        System.err.println("Manifest format: <filename><TAB><vless-link>");
        System.err.println("Default output dir: build/share-codes");
    }

    private static BufferedImage render(String contents) {
        Map<EncodeHintType, Object> hints = Map.of(EncodeHintType.MARGIN, 4);
        BitMatrix matrix;
        try {
            matrix = new DataMatrixWriter().encode(
                contents,
                BarcodeFormat.DATA_MATRIX,
                SIZE_PX,
                SIZE_PX,
                hints
            );
        } catch (Exception error) {
            throw new IllegalStateException("Unable to render Data Matrix", error);
        }

        BufferedImage image = new BufferedImage(matrix.getWidth(), matrix.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < matrix.getHeight(); y++) {
            for (int x = 0; x < matrix.getWidth(); x++) {
                image.setRGB(x, y, matrix.get(x, y) ? 0x000000 : 0xFFFFFF);
            }
        }
        return image;
    }
}
