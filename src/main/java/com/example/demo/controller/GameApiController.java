package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.RewardService;
import com.example.demo.repository.CoinTransactionRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/games")
public class GameApiController {

    @Autowired
    private RewardService rewardService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private CoinTransactionRepository coinTransactionRepository;

    @RequestMapping(value = "/reward", method = RequestMethod.POST)
    public ResponseEntity<?> awardReward(@RequestBody Map<String, String> payload, HttpSession session) {
        // Authenticate user
        User user = getUserFromSession(session);
        if (user == null || user.getId() == null) {
            return ResponseEntity.status(401).body("User not authenticated");
        }

        // Refresh user from DB to ensure state is current
        user = userRepository.findById(user.getId()).orElse(user);

        String result = payload.get("result"); // "WIN", "PLAY", or "SCORE"
        String gameName = payload.getOrDefault("gameName", "Unknown Game");
        String scoreStr = payload.get("score");

        Map<String, Object> response = new HashMap<>();

        if ("SCORE".equalsIgnoreCase(result) && scoreStr != null) {
            try {
                int score = Integer.parseInt(scoreStr);
                int coinsToAward = score / 1000;
                if (coinsToAward > 0) {
                    rewardService.awardGameScore(user, gameName, coinsToAward);
                    response.put("message", "Amazing! You earned " + coinsToAward + " coins for your score of " + score + " in " + gameName + "!");
                } else {
                    response.put("message", "Keep going! Reach 1000 points to earn your next coin.");
                }
                response.put("coins", user.getCoins());
                return ResponseEntity.ok(response);
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body("Invalid score format");
            }
        }

        if ("WIN".equalsIgnoreCase(result)) {
            rewardService.awardGameWin(user, gameName);
            response.put("message", "Congratulations! You earned " + rewardService.getConfig().getGameWin() + " coins for winning " + gameName);
        } else {
            rewardService.awardGamePlay(user, gameName);
            response.put("message", "You earned " + rewardService.getConfig().getGamePlay() + " coins for playing " + gameName);
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
