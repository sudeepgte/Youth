import java.nio.file.*;
import java.nio.charset.StandardCharsets;

public class Injector {
    public static void main(String[] args) throws Exception {
        Path path = Paths.get("src/main/resources/templates/battle-arena.html");
        String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        
        String htmlToInject = "<!-- Result Screen (Phase 5) -->\n" +
                "<div th:if=\"${battle.status == 'COMPLETED' || battle.status == 'TIE'}\" style=\"background: linear-gradient(135deg, #1f2937, #111827); border-radius: 20px; padding: 40px; text-align: center; color: white; margin-bottom: 24px; border: 2px solid #374151; box-shadow: 0 10px 30px rgba(0,0,0,0.3);\">\n" +
                "    <div style=\"font-size: 64px; margin-bottom: 10px;\">🏆</div>\n" +
                "    <h2 style=\"font-size: 28px; font-weight: 900; margin: 0 0 10px 0; color: #F84464; text-transform: uppercase;\">BATTLE CONCLUDED</h2>\n" +
                "    <div th:if=\"${battle.status == 'TIE'}\">\n" +
                "        <h3 style=\"font-size: 20px; font-weight: 700; margin-bottom: 30px; color: #9CA3AF;\">It's a TIE!</h3>\n" +
                "        <div style=\"display: flex; justify-content: center; gap: 30px; align-items: center; margin-bottom: 30px;\">\n" +
                "            <div th:if=\"${battle.winner != null}\">\n" +
                "                <img th:src=\"${battle.winner.profilePhotoUrl != null ? battle.winner.profilePhotoUrl : 'https://ui-avatars.com/api/?name=' + battle.winner.username}\" style=\"width: 80px; height: 80px; border-radius: 50%; border: 3px solid #9CA3AF; object-fit: cover;\">\n" +
                "                <div style=\"font-weight: bold; margin-top: 10px;\" th:text=\"${battle.winner.username}\">Player 1</div>\n" +
                "            </div>\n" +
                "            <div style=\"font-weight: 900; color: #6B7280; font-size: 24px;\">VS</div>\n" +
                "            <div th:if=\"${battle.winner2 != null}\">\n" +
                "                <img th:src=\"${battle.winner2.profilePhotoUrl != null ? battle.winner2.profilePhotoUrl : 'https://ui-avatars.com/api/?name=' + battle.winner2.username}\" style=\"width: 80px; height: 80px; border-radius: 50%; border: 3px solid #9CA3AF; object-fit: cover;\">\n" +
                "                <div style=\"font-weight: bold; margin-top: 10px;\" th:text=\"${battle.winner2.username}\">Player 2</div>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "    <div th:if=\"${battle.status == 'COMPLETED'}\">\n" +
                "        <h3 style=\"font-size: 16px; font-weight: 800; margin-bottom: 20px; color: #10B981; letter-spacing: 2px;\">WINNER</h3>\n" +
                "        <div th:if=\"${battle.winner != null}\" style=\"margin-bottom: 30px;\">\n" +
                "            <img th:src=\"${battle.winner.profilePhotoUrl != null ? battle.winner.profilePhotoUrl : 'https://ui-avatars.com/api/?name=' + battle.winner.username}\" style=\"width: 100px; height: 100px; border-radius: 50%; border: 4px solid #10B981; box-shadow: 0 0 20px rgba(16, 185, 129, 0.4); object-fit: cover; margin: 0 auto;\">\n" +
                "            <div style=\"font-weight: 900; font-size: 24px; margin-top: 15px;\" th:text=\"${battle.winner.username}\">Winner Name</div>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "    <div style=\"background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.1); border-radius: 16px; padding: 20px; display: inline-flex; gap: 40px; margin-bottom: 30px;\">\n" +
                "        <div>\n" +
                "            <div style=\"font-size: 12px; color: #9CA3AF; font-weight: 700; margin-bottom: 5px;\">PRIZE POOL</div>\n" +
                "            <div style=\"font-size: 20px; font-weight: 900; color: #F59E0B;\"><i class=\"fas fa-coins\"></i> <span th:text=\"${battle.prizePool != null ? battle.prizePool : 0.0}\">0.0</span></div>\n" +
                "        </div>\n" +
                "        <div>\n" +
                "            <div style=\"font-size: 12px; color: #9CA3AF; font-weight: 700; margin-bottom: 5px;\">XP AWARDED</div>\n" +
                "            <div style=\"font-size: 20px; font-weight: 900; color: #8B5CF6;\"><i class=\"fas fa-star\"></i> <span th:text=\"${battle.winnerXp}\">100</span></div>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "    <div>\n" +
                "        <a th:href=\"@{/battles}\" style=\"background: #F84464; color: white; padding: 12px 30px; border-radius: 30px; text-decoration: none; font-weight: 800; font-size: 16px; transition: 0.2s; display: inline-block;\">Rematch / Find New</a>\n" +
                "    </div>\n" +
                "</div>";

        String target = "<!-- BATTLES LIST VIEW (when no single battle selected) -->";
        content = content.replace(target, htmlToInject + "\n\n            " + target);

        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }
}