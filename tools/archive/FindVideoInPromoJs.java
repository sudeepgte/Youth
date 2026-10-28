import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FindVideoInPromoJs {
    public static void main(String[] args) throws Exception {
        String filename = "src/main/resources/static/js/promo.js";
        File f = new File(filename);
        if (!f.exists()) return;
        byte[] bytes = Files.readAllBytes(Paths.get(filename));
        String content = new String(bytes, StandardCharsets.UTF_8);
        
        System.out.println("=== Search inside " + filename + " ===");
        String[] lines = content.split("\\r?\\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.contains("video") || line.contains("Video") || line.contains("mp4")) {
                System.out.println("  " + (i + 1) + ": " + line.trim());
            }
        }
    }
}
