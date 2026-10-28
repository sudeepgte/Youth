package com.example.demo.controller;

import com.example.demo.config.ActiveLoginRegistry;
import com.example.demo.model.Battle;
import com.example.demo.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminRealtimeStatsController {

    @Autowired private UserRepository userRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private BattleRepository battleRepository;
    @Autowired private BattleParticipantRepository battleParticipantRepository;
    @Autowired private WalletTransactionRepository walletTransactionRepository;
    @Autowired private ContactMessageRepository contactMessageRepository;
    @Autowired private PostRepository postRepository;
    @Autowired private ActiveLoginRegistry activeLoginRegistry;

    private boolean checkAdmin(HttpSession session, HttpServletRequest request) {
        if (session != null) {
            Object u = session.getAttribute("user");
            if ("admin".equals(u)) return true;
            if (u instanceof com.example.demo.model.User && "admin".equalsIgnoreCase(((com.example.demo.model.User) u).getUsername())) return true;
            if ("admin".equals(session.getAttribute("admin"))) return true;
        }
        Object authUser = request.getAttribute("authenticatedUser");
        return "admin".equals(authUser);
    }

    @GetMapping("/realtime-stats")
    public ResponseEntity<Map<String, Object>> getRealtimeStats(HttpSession session, HttpServletRequest request) {
        if (!checkAdmin(session, request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        Map<String, Object> stats = new HashMap<>();

        long totalUsers = userRepository.count();
        int activeUsers = activeLoginRegistry.getActiveSessionCount();

        long totalEvents = eventRepository.count();
        long upcomingEvents = eventRepository.countByStatus("UPCOMING");
        long ongoingEvents = eventRepository.countByStatus("ONGOING");
        long votingEvents = eventRepository.countByStatus("VOTING");
        long completedEvents = eventRepository.countByStatus("COMPLETED");

        List<Battle> battles = battleRepository.findAll();
        long totalBattles = battles.size();
        long runningBattles = battles.stream().filter(b -> "ACTIVE".equals(b.getStatus())).count();
        long totalJoinedUsers = battleParticipantRepository.count();

        double totalAdminCommission = 0.0;
        for (Battle b : battles) {
            double entryFee = b.getEntryFee() != null ? b.getEntryFee() : 0.0;
            int joinsCount = b.getParticipants() != null ? Math.max(0, b.getParticipants().size() - 1) : 0;
            double pct = b.getAdminCommissionPct() != null ? b.getAdminCommissionPct() : 7.0;
            totalAdminCommission += entryFee * (pct / 100.0) * joinsCount;
        }

        long pendingWithdrawals = walletTransactionRepository.findAll().stream()
                .filter(tx -> "WITHDRAWAL".equals(tx.getType()) && "PENDING".equals(tx.getStatus()))
                .count();

        long supportInquiries = contactMessageRepository.count();
        long totalPosts = postRepository.count();

        String timeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss a"));

        stats.put("status", "ONLINE");
        stats.put("timestamp", timeStr);
        stats.put("totalUsers", totalUsers);
        stats.put("activeOnlineUsers", activeUsers);
        stats.put("totalEvents", totalEvents);
        stats.put("upcomingEvents", upcomingEvents);
        stats.put("ongoingEvents", ongoingEvents);
        stats.put("votingEvents", votingEvents);
        stats.put("completedEvents", completedEvents);
        stats.put("totalBattles", totalBattles);
        stats.put("runningBattles", runningBattles);
        stats.put("totalJoinedUsers", totalJoinedUsers);
        stats.put("totalAdminCommission", totalAdminCommission);
        stats.put("formattedCommission", String.format("%.2f", totalAdminCommission));
        stats.put("pendingWithdrawals", pendingWithdrawals);
        stats.put("supportInquiries", supportInquiries);
        stats.put("totalPosts", totalPosts);

        return ResponseEntity.ok(stats);
    }
}
