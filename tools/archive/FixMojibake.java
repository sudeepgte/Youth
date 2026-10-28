import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class FixMojibake {
    public static void main(String[] args) throws IOException {
        Map<String, String> replacements = new HashMap<>();
        replacements.put("ðŸ—³ï¸ ", "&#128499;");
        replacements.put("â€œ", "&ldquo;");
        replacements.put("â€ ", "&rdquo;");
        replacements.put("â€º", "&rsaquo;");
        replacements.put("â€”", "&mdash;");
        replacements.put("â€¢", "&bull;");
        replacements.put("Ã¢â‚¬Â¢", "&bull;");
        replacements.put("Ãƒâ€”", "&times;");
        replacements.put("â†’", "&rarr;");
        replacements.put("âœ•", "&times;");
        replacements.put("â ¤ï¸ ", "&#10084;&#65039;");
        replacements.put("ðŸ˜‚", "&#128514;");
        replacements.put("ðŸ”¥", "&#128293;");
        replacements.put("ðŸ˜ ", "&#128525;");
        replacements.put("ðŸ‘ ", "&#128079;");
        replacements.put("ðŸ™Œ", "&#128588;");
        replacements.put("ðŸ˜¢", "&#128546;");
        replacements.put("ðŸ˜®", "&#128558;");
        replacements.put("ðŸŽ‰", "&#127881;");
        replacements.put("ðŸ’¯", "&#128175;");
        replacements.put("ðŸ˜€", "&#128512;");
        replacements.put("ðŸ ±", "&#128049;");
        replacements.put("ðŸ •", "&#127829;");
        replacements.put("âš½", "&#9917;");
        replacements.put("âœ‰ï¸ ", "&#9993;&#65039;");
        replacements.put("â”€", "-");

        Path dir = Paths.get("src/main/resources/templates");
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".html"))
                 .forEach(path -> {
                     try {
                         String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                         boolean modified = false;
                         for (Map.Entry<String, String> entry : replacements.entrySet()) {
                             if (content.contains(entry.getKey())) {
                                 content = content.replace(entry.getKey(), entry.getValue());
                                 modified = true;
                             }
                         }
                         if (modified) {
                             Files.write(path, content.getBytes(StandardCharsets.UTF_8));
                             System.out.println("Fixed " + path.getFileName());
                         }
                     } catch (IOException e) {
                         e.printStackTrace();
                     }
                 });
        }
    }
}
