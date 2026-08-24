import java.nio.file.*;
public class CheckTags2 {
    public static void main(String[] args) throws Exception {
        String content = new String(Files.readAllBytes(Paths.get("src/main/resources/templates/battle-arena.html")), "UTF-8");
        int startIdx = content.indexOf("<div class=\"dashboard-container\">");
        int sidebar = content.indexOf("<aside class=\"sidebar\">");
        int sidebarEnd = content.indexOf("</aside>", sidebar);
        int main = content.indexOf("<main class=\"social-feed\"");
        int mainEnd = content.indexOf("</main>");
        int widgets = content.indexOf("<aside class=\"widgets\">");
        int widgetsEnd = content.indexOf("</aside>", widgets);
        int containerEnd = content.indexOf("</div>", widgetsEnd);
        
        System.out.println("container start: " + startIdx);
        System.out.println("sidebar: " + sidebar + " to " + sidebarEnd);
        System.out.println("main: " + main + " to " + mainEnd);
        System.out.println("widgets: " + widgets + " to " + widgetsEnd);
        System.out.println("container close: " + containerEnd);
        
        System.out.println("Next 100 chars after container close: ");
        System.out.println(content.substring(containerEnd, Math.min(containerEnd + 100, content.length())));
    }
}
