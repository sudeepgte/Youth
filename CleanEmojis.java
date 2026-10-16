import java.nio.file.*;
import java.util.*;

public class CleanEmojis {
    public static void main(String[] args) throws Exception {
        Path p = Paths.get("src/main/resources/templates/battle-arena.html");
        byte[] bytes = Files.readAllBytes(p);
        String content = new String(bytes, "UTF-8");
        
        String[] lines = content.split("\n");
        List<String> newLines = new ArrayList<>();
        
        for (String line : lines) {
            if (line.contains("ðŸ †") || line.contains("ðŸ¥‡") || line.contains("ðŸ¥ˆ") || line.contains("ðŸ¥‰")) {
                continue; // Skip lines with broken trophy/medal emojis
            }
            if (line.contains("dY?+") || line.contains("dY")) {
                 if (line.contains("font-size: 64px") || line.contains("font-size: 24px")) {
                     continue; // Skip lines with alternative broken string
                 }
            }
            
            line = line.replace("ðŸ—‘ï¸", "");
            line = line.replace("ðŸŽ‰", "");
            line = line.replace("ðŸ—³ï¸", "");
            line = line.replace("ðŸ”´", "<i class=\"fas fa-circle\" style=\"color:red\"></i>");
            line = line.replace("ðŸ‘ ï¸", "<i class=\"fas fa-eye\"></i>");
            line = line.replace("ðŸ¤¼", "<i class=\"fas fa-fist-raised\"></i>");
            
            newLines.add(line);
        }
        
        Files.write(p, String.join("\n", newLines).getBytes("UTF-8"));
        System.out.println("Done cleaning!");
    }
}