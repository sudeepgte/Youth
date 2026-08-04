package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.RewardService;
import com.example.demo.repository.CoinTransactionRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/games")
public class GameApiController {

    private static final Logger logger = LoggerFactory.getLogger(GameApiController.class);

    @Autowired
    private RewardService rewardService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private CoinTransactionRepository coinTransactionRepository;

    // Deduplication cache for processed reward session nonces
    private static final Map<String, Long> processedSessions = new ConcurrentHashMap<>();

    @RequestMapping(value = "/reward", method = RequestMethod.POST)
    public ResponseEntity<?> awardReward(@RequestBody Map<String, Object> payload, HttpSession session) {
        // Authenticate user
        User user = getUserFromSession(session);
        if (user == null || user.getId() == null) {
            return ResponseEntity.status(401).body("User not authenticated");
        }

        // Refresh user from DB to ensure state is current
        user = userRepository.findById(user.getId()).orElse(user);

        String result = payload.get("result") != null ? payload.get("result").toString() : "PLAY";
        String gameName = payload.getOrDefault("gameName", "Unknown Game").toString();
        String scoreStr = payload.get("score") != null ? payload.get("score").toString() : null;
        String sessionId = payload.get("sessionId") != null ? payload.get("sessionId").toString() : null;

        // Parse coinsEarned
        int coinsEarned = -1;
        if (payload.containsKey("coinsEarned") && payload.get("coinsEarned") != null) {
            try {
                coinsEarned = Integer.parseInt(payload.get("coinsEarned").toString());
            } catch (Exception ignored) {}
        }

        // Deduplication Check
        if (sessionId != null && !sessionId.isBlank()) {
            String dedupKey = user.getId() + "_" + sessionId;
            if (processedSessions.containsKey(dedupKey)) {
                logger.info("Duplicate reward request suppressed for session: {}", dedupKey);
                Map<String, Object> resp = new HashMap<>();
                resp.put("coins", user.getCoins());
                resp.put("coinsAwarded", 0);
                resp.put("message", null);
                return ResponseEntity.ok(resp);
            }
            processedSessions.put(dedupKey, System.currentTimeMillis());
            // Evict old session nonces (older than 10 mins)
            long cutoff = System.currentTimeMillis() - 600000;
            processedSessions.entrySet().removeIf(e -> e.getValue() < cutoff);
        }

        Map<String, Object> response = new HashMap<>();

        // If coinsEarned is explicitly provided and <= 0 (e.g. Mario played but collected 0 coins):
        if (coinsEarned == 0) {
            logger.info("Player {} collected 0 coins in {}. No reward awarded.", user.getUsername(), gameName);
            response.put("coins", user.getCoins());
            response.put("coinsAwarded", 0);
            response.put("message", null);
            return ResponseEntity.ok(response);
        }

        if ("SCORE".equalsIgnoreCase(result) && scoreStr != null) {
            try {
                int score = Integer.parseInt(scoreStr);
                int coinsToAward = score / 1000;
                if (coinsToAward > 0 && (coinsEarned < 0 || coinsEarned > 0)) {
                    rewardService.awardGameScore(user, gameName, coinsToAward);
                    response.put("coinsAwarded", coinsToAward);
                    response.put("message", "Amazing! You earned " + coinsToAward + " coins for your score in " + gameName + "!");
                } else {
                    response.put("coinsAwarded", 0);
                    response.put("message", null);
                }
                response.put("coins", user.getCoins());
                return ResponseEntity.ok(response);
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body("Invalid score format");
            }
        }

        if (coinsEarned > 0) {
            // Valid coins collected in gameplay
            int winBonus = "WIN".equalsIgnoreCase(result) ? rewardService.getConfig().getGameWin() : 0;
            int totalAward = coinsEarned + winBonus;
            rewardService.awardGameScore(user, gameName, totalAward);
            response.put("coinsAwarded", totalAward);
            response.put("message", "Great job! You earned " + totalAward + " coins in " + gameName + "!");
            response.put("coins", user.getCoins());
            return ResponseEntity.ok(response);
        }

        // Generic fallback for other games where coinsEarned is not explicitly passed (coinsEarned == -1)
        if ("WIN".equalsIgnoreCase(result)) {
            rewardService.awardGameWin(user, gameName);
            int winAmount = rewardService.getConfig().getGameWin();
            response.put("coinsAwarded", winAmount);
            response.put("message", "Congratulations! You earned " + winAmount + " coins for winning " + gameName);
        } else {
            rewardService.awardGamePlay(user, gameName);
            int playAmount = rewardService.getConfig().getGamePlay();
            response.put("coinsAwarded", playAmount);
            response.put("message", "You earned " + playAmount + " coins for playing " + gameName);
        }
        
        response.put("coins", user.getCoins());
        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = "/history", method = RequestMethod.GET)
    public ResponseEntity<?> getHistory(HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null || user.getId() == null) {
            return ResponseEntity.status(401).body("User not authenticated");
        }

        List<Map<String, Object>> history = coinTransactionRepository.findByUserOrderByTimestampDesc(user).stream()
            .map(t -> {
                Map<String, Object> map = new HashMap<>();
                map.put("amount", t.getAmount());
                map.put("source", t.getSource() != null ? t.getSource() : "Zentrix Activity");
                map.put("reason", t.getReason() != null ? t.getReason() : "Reward");
                map.put("timestamp", t.getTimestamp() != null ? t.getTimestamp().toString() : "");
                return map;
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(history);
    }

    private User getUserFromSession(HttpSession session) {
        Object authUser = httpServletRequest.getAttribute("authenticatedUser");
        if (authUser instanceof User) {
            return (User) authUser;
        }
        if (session != null) {
            Object sessionUser = session.getAttribute("user");
            if (sessionUser instanceof User) {
                return (User) sessionUser;
            }
        }
        return null;
    }
}
