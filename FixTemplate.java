import java.nio.file.*;
public class FixTemplate {
    public static void main(String[] args) throws Exception {
        Path p = Paths.get("src/main/resources/templates/battle-arena.html");
        String content = new String(Files.readAllBytes(p), "UTF-8");
        content = content.replace("<div th:if=\"${!submissions.isEmpty() and (battle.status == 'VOTING' or battle.status == 'COMPLETED' or battle.status == 'TIE')}\" class=\"leaderboard-section\">",
            "<div th:if=\"${submissions != null and !submissions.isEmpty() and battle != null and (battle.status == 'VOTING' or battle.status == 'COMPLETED' or battle.status == 'TIE')}\" class=\"leaderboard-section\">");
        Files.write(p, content.getBytes("UTF-8"));
        System.out.println("Fixed!");
    }
}
