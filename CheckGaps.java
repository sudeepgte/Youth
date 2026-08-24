import java.nio.file.*;
public class CheckGaps {
    public static void main(String[] args) throws Exception {
        String text = new String(Files.readAllBytes(Paths.get("src/main/resources/templates/battle-arena.html")), "UTF-8");
        int startIdx = text.indexOf("<div class=\"dashboard-container\">");
        int endIdx = text.indexOf("<!-- CREATE BATTLE MODAL -->");
        String container = text.substring(startIdx + 33, endIdx);
        
        int aside1End = container.indexOf("</aside>", container.indexOf("<aside class=\"sidebar\">")) + 8;
        int mainStart = container.indexOf("<main");
        
        System.out.println("Gap 1:");
        System.out.println(container.substring(aside1End, mainStart));
        
        int mainEnd = container.indexOf("</main>") + 7;
        int aside2Start = container.indexOf("<aside class=\"widgets\">");
        
        System.out.println("Gap 2:");
        System.out.println(container.substring(mainEnd, aside2Start));
        
        int aside2End = container.indexOf("</aside>", aside2Start) + 8;
        int containerEnd = container.lastIndexOf("</div>");
        
        System.out.println("Gap 3:");
        System.out.println(container.substring(aside2End, containerEnd));
    }
}
