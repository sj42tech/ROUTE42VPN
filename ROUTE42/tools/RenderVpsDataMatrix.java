import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.datamatrix.DataMatrixWriter;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.imageio.ImageIO;

public final class RenderVpsDataMatrix {
    private static final int SIZE_PX = 1024;

    private static final Map<String, String> LINKS = new LinkedHashMap<>();

    static {
        LINKS.put(
            "hostkey-vless-datamatrix.png",
            "vless://775ed879-a162-45e3-b8af-c49f96eaede5@5.39.219.74:443?encryption=none&security=reality&sni=www.debian.org&fp=chrome&pbk=xAjc3oJaoU9psF_G2zQB5N-HV1ClgKQ1K8atsPPL6CY&sid=a1&type=tcp#Hostkey"
        );
        LINKS.put(
            "exoscale-vless-datamatrix.png",
            "vless://775ed879-a162-45e3-b8af-c49f96eaede5@194.182.174.240:443?encryption=none&security=reality&sni=www.debian.org&fp=chrome&pbk=xAjc3oJaoU9psF_G2zQB5N-HV1ClgKQ1K8atsPPL6CY&sid=a1&type=tcp#Exoscale"
        );
        LINKS.put(
            "aws-vless-datamatrix.png",
            "vless://775ed879-a162-45e3-b8af-c49f96eaede5@3.34.30.191:443?encryption=none&flow=xtls-rprx-vision&security=reality&sni=vk.com&fp=chrome&pbk=J3MXsoLLi7QnTkStJB45USiju6bVucQgrDqnCmmTSlw&sid=f3aa&type=tcp#AWS"
        );
    }

    private RenderVpsDataMatrix() {
    }

    public static void main(String[] args) throws IOException {
        Path outputDir = args.length > 0
            ? Path.of(args[0])
            : Path.of("docs/share-codes");
        Files.createDirectories(outputDir);

        for (Map.Entry<String, String> entry : LINKS.entrySet()) {
            BufferedImage image = render(entry.getValue());
            Path outputPath = outputDir.resolve(entry.getKey());
            ImageIO.write(image, "png", outputPath.toFile());
            System.out.println(outputPath.toAbsolutePath());
        }
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
