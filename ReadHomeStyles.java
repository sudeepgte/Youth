import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ReadHomeStyles {
    public static void main(String[] args) throws Exception {
        String filename = "src/main/resources/templates/home.html";
        File f = new File(filename);
        if (!f.exists()) return;
        byte[] bytes = Files.readAllBytes(Paths.get(filename));
        String content = new String(bytes, StandardCharsets.UTF_8);
        
        System.out.println("=== " + filename + " (first 150 lines) ===");
        String[] lines = content.split("\\r?\\n");
        for (int i = 0; i < Math.min(150, lines.length); i++) {
            System.out.println((i + 1) + ": " + lines[i]);
        }
    }
}
