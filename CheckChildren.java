import java.nio.file.*;
import java.util.regex.*;

public class CheckChildren {
    public static void main(String[] args) throws Exception {
        String content = new String(Files.readAllBytes(Paths.get("src/main/resources/templates/battle-arena.html")), "UTF-8");
        int startIdx = content.indexOf("<div class=\"dashboard-container\">");
        int endIdx = content.lastIndexOf("</div>", content.indexOf("<!-- CREATE BATTLE MODAL -->"));
        String container = content.substring(startIdx + 33, endIdx);
        
        System.out.println("Container length: " + container.length());
        
        int aside1 = container.indexOf("<aside class=\"sidebar\">");
        int main = container.indexOf("<main");
        int aside2 = container.indexOf("<aside class=\"widgets\">");
        
        System.out.println("Aside1: " + aside1);
        System.out.println("Main: " + main);
        System.out.println("Aside2: " + aside2);
        
        // Let's find if there are any other direct div or tags outside of these 3
    }
}
