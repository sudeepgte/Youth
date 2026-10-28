import java.nio.file.*;
public class CheckTags {
    public static void main(String[] args) throws Exception {
        String content = new String(Files.readAllBytes(Paths.get("src/main/resources/templates/battle-arena.html")), "UTF-8");
        System.out.println("dashboard-container index: " + content.indexOf("<div class=\"dashboard-container\">"));
        System.out.println("sidebar index: " + content.indexOf("<aside class=\"sidebar\">"));
        System.out.println("/sidebar index: " + content.indexOf("</aside>", content.indexOf("<aside class=\"sidebar\">")));
        System.out.println("main index: " + content.indexOf("<main class=\"social-feed\""));
        System.out.println("/main index: " + content.indexOf("</main>"));
        System.out.println("widgets index: " + content.indexOf("<aside class=\"widgets\">"));
        System.out.println("/widgets index: " + content.indexOf("</aside>", content.indexOf("<aside class=\"widgets\">")));
    }
}
