import java.nio.file.*;
import java.util.regex.*;

public class InjectJS {
    public static void main(String[] args) throws Exception {
        Path p = Paths.get("src/main/resources/templates/battle-arena.html");
        String content = new String(Files.readAllBytes(p), "UTF-8");
        
        String jsHtml = "        function loadGlobalLeaderboard() {\n" +
            "            const cat = document.getElementById('leaderboardCategoryFilter').value;\n" +
            "            fetch('/api/leaderboards?time=alltime&category=' + cat)\n" +
            "                .then(res => {\n" +
            "                    if (!res.ok) throw new Error('Network response was not ok');\n" +
            "                    return res.json();\n" +
            "                })\n" +
            "                .then(data => {\n" +
            "                    const list = document.getElementById('global-leaderboard-list');\n" +
            "                    if(!list) return;\n" +
            "                    list.innerHTML = '';\n" +
            "                    if (!data || data.length === 0) {\n" +
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
            "                }).catch(err => {\n" +
            "                    console.error(err);\n" +
            "                    const list = document.getElementById('global-leaderboard-list');\n" +
            "                    if(list) list.innerHTML = '<div style=\"text-align:center; font-size:12px; color:#EF4444; padding:10px;\">Failed to load</div>';\n" +
            "                });\n" +
            "        }\n" +
            "        document.addEventListener('DOMContentLoaded', () => {\n" +
            "            if (document.getElementById('global-leaderboard-widget')) {\n" +
            "                loadGlobalLeaderboard();\n" +
            "            }\n" +
            "        });\n\n";

        if (!content.contains("function loadGlobalLeaderboard")) {
            content = content.replace("function initCountdown() {", jsHtml + "        function initCountdown() {");
            Files.write(p, content.getBytes("UTF-8"));
            System.out.println("JS Injected!");
        } else {
            System.out.println("JS ALREADY INJECTED!");
        }
    }
}
