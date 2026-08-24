import java.nio.file.*;
public class FixLeaderboard {
    public static void main(String[] args) throws Exception {
        Path p = Paths.get("src/main/resources/templates/battle-arena.html");
        String content = new String(Files.readAllBytes(p), "UTF-8");
        
        String widgetCardHtml = "        <aside class=\"widgets\">\n            <div class=\"widget-card\">";
        String leaderboardHtml = "        <aside class=\"widgets\">\n" +
            "            <!-- Global Leaderboard (visible on /battles) -->\n" +
            "            <div th:if=\"${battle == null}\" class=\"widget-card\" id=\"global-leaderboard-widget\">\n" +
            "                <div class=\"widget-header\" style=\"justify-content: space-between;\">\n" +
            "                    <h3><i class=\"fas fa-trophy\" style=\"color: #FFD700; margin-right: 6px;\"></i> Leaderboard</h3>\n" +
            "                    <select id=\"leaderboardCategoryFilter\" onchange=\"loadGlobalLeaderboard()\" style=\"padding: 4px; font-size: 11px; border-radius: 6px; border: 1px solid var(--arena-border); background: var(--bg-primary);\">\n" +
            "                        <option value=\"global\">All Categories</option>\n" +
            "                        <option value=\"Coding\">Coding</option>\n" +
            "                        <option value=\"Music\">Music</option>\n" +
            "                        <option value=\"Dance\">Dance</option>\n" +
            "                        <option value=\"Photography\">Photography</option>\n" +
            "                        <option value=\"Singing\">Singing</option>\n" +
            "                        <option value=\"Video\">Video</option>\n" +
            "                    </select>\n" +
            "                </div>\n" +
            "                <div id=\"global-leaderboard-list\" style=\"display:flex; flex-direction:column; gap:8px;\">\n" +
            "                    <div style=\"text-align:center; padding:20px; color:var(--text-muted);\"><i class=\"fas fa-spinner fa-spin\"></i> Loading...</div>\n" +
            "                </div>\n" +
            "            </div>\n\n            <div class=\"widget-card\">";
        
        content = content.replace(widgetCardHtml, leaderboardHtml);
        
        String jsHtml = "        function loadGlobalLeaderboard() {\n" +
            "            const cat = document.getElementById('leaderboardCategoryFilter').value;\n" +
            "            fetch('/api/leaderboards?time=alltime&category=' + cat)\n" +
            "                .then(res => res.json())\n" +
            "                .then(data => {\n" +
            "                    const list = document.getElementById('global-leaderboard-list');\n" +
            "                    if(!list) return;\n" +
            "                    list.innerHTML = '';\n" +
            "                    if (data.length === 0) {\n" +
            "                        list.innerHTML = '<div style=\"text-align:center; font-size:12px; color:var(--text-muted); padding:10px;\">No rankings yet</div>';\n" +
            "                        return;\n" +
            "                    }\n" +
            "                    data.slice(0, 10).forEach(u => {\n" +
            "                        let rankClass = u.rank === 1 ? 'rank-1' : (u.rank === 2 ? 'rank-2' : (u.rank === 3 ? 'rank-3' : 'rank-default'));\n" +
            "                        list.innerHTML += `\n" +
            "                        <div style=\"display:flex; justify-content:space-between; align-items:center; padding:8px 12px; background:rgba(255,255,255,0.6); border:1px solid rgba(15,23,42,0.05); border-radius:10px;\">\n" +
            "                            <div style=\"display:flex; align-items:center; gap:10px;\">\n" +
            "                                <div class=\"leaderboard-rank ${rankClass}\">${u.rank}</div>\n" +
            "                                <div style=\"display:flex; align-items:center; gap:6px;\">\n" +
            "                                    <img src=\"${u.avatar}\" style=\"width:28px; height:28px; border-radius:50%; border:1px solid var(--arena-border); object-fit:cover;\">\n" +
            "                                    <span style=\"font-weight:800; font-size:13px; color:var(--text-primary);\">${u.username}</span>\n" +
            "                                </div>\n" +
            "                            </div>\n" +
            "                            <div style=\"text-align:right;\">\n" +
            "                                <div style=\"font-weight:900; font-size:13px; color:var(--arena-primary);\"><i class=\"fas fa-bolt\" style=\"color:#F59E0B; font-size:10px;\"></i> ${u.rating}</div>\n" +
            "                            </div>\n" +
            "                        </div>`;\n" +
            "                    });\n" +
            "                }).catch(err => console.error(err));\n" +
            "        }\n" +
            "        document.addEventListener('DOMContentLoaded', () => {\n" +
            "            if (document.getElementById('global-leaderboard-widget')) {\n" +
            "                loadGlobalLeaderboard();\n" +
            "            }\n" +
            "        });\n\n";

        if (!content.contains("loadGlobalLeaderboard()")) {
            content = content.replace("// ─── Live Countdown Timer", jsHtml + "        // ─── Live Countdown Timer");
        }
        
        Files.write(p, content.getBytes("UTF-8"));
        System.out.println("Leaderboard Widget and JS inserted successfully.");
    }
}
