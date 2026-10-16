package com.example.demo.controller;

import com.example.demo.model.Battle;
import com.example.demo.model.User;
import com.example.demo.repository.BattleRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/leaderboards")
public class LeaderboardApiController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BattleRepository battleRepository;

    @GetMapping
    public List<Map<String, Object>> getLeaderboard(
            @RequestParam(defaultValue = "alltime") String time,
            @RequestParam(defaultValue = "global") String category) {
        
        List<Map<String, Object>> result = new ArrayList<>();
        
        // If Global and All Time, just use global battleRating
        if ("alltime".equalsIgnoreCase(time) && "global".equalsIgnoreCase(category)) {
            List<User> users = userRepository.findAll();
            users.sort((u1, u2) -> {
                int r1 = u1.getBattleRating() != null ? u1.getBattleRating() : 1500;
                int r2 = u2.getBattleRating() != null ? u2.getBattleRating() : 1500;
                return Integer.compare(r2, r1);
            });
            List<User> topUsers = users.stream().limit(50).collect(Collectors.toList());
            int rank = 1;
            for (User u : topUsers) {
                result.add(buildUserMap(rank++, u, u.getBattleRating() != null ? u.getBattleRating() : 1500));
            }
            return result;
        }

        // For category-specific or timeframe-specific leaderboards, compute based on completed battles
        List<Battle> completedBattles = battleRepository.findByStatusOrderByCreatedAtDesc("COMPLETED");
        
        // Filter by time
        if ("weekly".equalsIgnoreCase(time)) {
            LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);
            completedBattles = completedBattles.stream().filter(b -> b.getEndsAt() != null && b.getEndsAt().isAfter(oneWeekAgo)).collect(Collectors.toList());
        } else if ("monthly".equalsIgnoreCase(time)) {
            LocalDateTime oneMonthAgo = LocalDateTime.now().minusDays(30);
            completedBattles = completedBattles.stream().filter(b -> b.getEndsAt() != null && b.getEndsAt().isAfter(oneMonthAgo)).collect(Collectors.toList());
        }
        
        // Filter by category
        if (!"global".equalsIgnoreCase(category)) {
            completedBattles = completedBattles.stream()
                .filter(b -> b.getCategory() != null && b.getCategory().equalsIgnoreCase(category))
                .collect(Collectors.toList());
        }

        // Aggregate points (XP or wins)
        Map<User, Integer> pointsMap = new HashMap<>();
        for (Battle b : completedBattles) {
            if (b.getWinner() != null) {
                pointsMap.put(b.getWinner(), pointsMap.getOrDefault(b.getWinner(), 0) + (b.getWinnerXp() != null ? b.getWinnerXp() : 500));
            }
        }
        
        // If it's alltime but specific category, and some users have 0 points, maybe we shouldn't show them or show them with 0.
        // Let's just show those who have points.
        List<Map.Entry<User, Integer>> sortedEntries = new ArrayList<>(pointsMap.entrySet());
        sortedEntries.sort((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()));
        
        int rank = 1;
        for (Map.Entry<User, Integer> entry : sortedEntries.stream().limit(50).collect(Collectors.toList())) {
            result.add(buildUserMap(rank++, entry.getKey(), entry.getValue()));
        }
        
        return result;
    }
    
    private Map<String, Object> buildUserMap(int rank, User u, int score) {
        Map<String, Object> map = new HashMap<>();
        map.put("rank", rank);
        map.put("username", u.getUsername());
        map.put("name", u.getUsername());
        map.put("rating", score);
        
        String avatar = u.getProfilePhotoUrl();
        if (avatar == null || avatar.isEmpty()) {
            avatar = "https://ui-avatars.com/api/?name=" + u.getUsername() + "&background=random&color=fff";
        }
        map.put("avatar", avatar);
        return map;
    }
}
