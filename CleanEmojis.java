import java.nio.file.*;
import java.nio.charset.StandardCharsets;

public class CleanEmojis {
    public static void main(String[] args) throws Exception {
        Path path = Paths.get("src/main/resources/templates/battle-arena.html");
        String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        
        // Remove corrupted HTML comment lines (those containing a lot of A??, or similar)
        // We will just replace any line that has "<!-- A" and "-->"
        content = content.replaceAll("(?m)^\\s*<!--\\s*A.*-->\\s*$", "");
        
        // Replace corrupted "Battle Arena" emoji
        content = content.replaceAll("A\\?\\?A_A,A\\?\\s*Battle Arena", "⚔️ Battle Arena");
        
        // Replace corrupted "Create Battle" emoji
        content = content.replaceAll("A\\?\\?A_A,A\\?\\s*Create Battle", "⚔️ Create Battle");
        
        // Replace corrupted "Online" and "Offline" emojis
        content = content.replaceAll("A,'A\\?\\s*Online", "🌐 Online");
        content = content.replaceAll("A,A\\?A\\s*Offline", "🏢 Offline");

        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }
}