package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/battles")
public class BattleController {

    @Autowired private BattleRepository battleRepository;
    @Autowired private BattleParticipantRepository participantRepository;
    @Autowired private BattleSubmissionRepository submissionRepository;
    @Autowired private BattleVoteRepository voteRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private HttpServletRequest httpServletRequest;

    private User getUserFromSession(HttpSession session) {
        Object authUser = httpServletRequest.getAttribute("authenticatedUser");
        if (authUser instanceof User) return (User) authUser;
        Object sessionUser = session.getAttribute("user");
        if (sessionUser instanceof User) return (User) sessionUser;
        Object userIdObj = session.getAttribute("userId");
        if (userIdObj != null) {
            try {
                Long userId = userIdObj instanceof Number ? ((Number) userIdObj).longValue() : Long.parseLong(userIdObj.toString());
                return userRepository.findById(userId).orElse(null);
            } catch (Exception e) { /* ignore */ }
        }
        return null;
    }

    // ─── Main Page ──────────────────────────────────────────
    @GetMapping
    public String battleArena(Model model, HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/login";
        user = userRepository.findById(user.getId()).orElse(user);

        // Auto-complete expired battles
        autoCompleteBattles();

        List<Battle> allBattles = battleRepository.findAllByOrderByCreatedAtDesc();
        List<Battle> activeBattles = allBattles.stream()
                .filter(b -> "WAITING".equals(b.getStatus()) || "ACTIVE".equals(b.getStatus()) || "VOTING".equals(b.getStatus()))
                .collect(Collectors.toList());
        List<Battle> completedBattles = allBattles.stream()
                .filter(b -> "COMPLETED".equals(b.getStatus()))
                .limit(10)
                .collect(Collectors.toList());

        model.addAttribute("user", user);
        model.addAttribute("activeBattles", activeBattles);
        model.addAttribute("completedBattles", completedBattles);
        return "battle-arena";
    }

    // ─── View Single Battle ─────────────────────────────────
    @GetMapping("/{id}")
    @Transactional
    public String viewBattle(@PathVariable Long id, Model model, HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/login";
        user = userRepository.findById(user.getId()).orElse(user);

        autoCompleteBattles();

        Battle battle = battleRepository.findById(id).orElse(null);
        if (battle == null) return "redirect:/battles";

        boolean isParticipant = participantRepository.existsByBattleAndUser(battle, user);
        boolean isCreator = battle.getCreator() != null && battle.getCreator().getId().equals(user.getId());
        boolean hasSubmitted = submissionRepository.existsByBattleAndUser(battle, user);
        boolean hasVoted = voteRepository.existsByBattleAndVoter(battle, user);

        List<BattleSubmission> submissions = submissionRepository.findByBattleOrderByVoteCountDesc(battle);
        List<BattleParticipant> participants = participantRepository.findByBattle(battle);

        model.addAttribute("user", user);
        model.addAttribute("battle", battle);
        model.addAttribute("isParticipant", isParticipant);
        model.addAttribute("isCreator", isCreator);
        model.addAttribute("hasSubmitted", hasSubmitted);
        model.addAttribute("hasVoted", hasVoted);
        model.addAttribute("submissions", submissions);
        model.addAttribute("participants", participants);
        model.addAttribute("participantCount", participants.size());
        return "battle-arena";
    }

    // ─── Create Battle ──────────────────────────────────────
    @PostMapping("/create")
    public String createBattle(
            @RequestParam String title,
            @RequestParam String category,
            @RequestParam(defaultValue = "24") Integer durationHours,
            @RequestParam(defaultValue = "2") Integer maxParticipants,
            HttpSession session) {

        User user = getUserFromSession(session);
        if (user == null) return "redirect:/login";
        user = userRepository.findById(user.getId()).orElse(user);

        Battle battle = new Battle();
        battle.setTitle(title);
        battle.setCategory(category);
        battle.setDurationHours(durationHours);
        battle.setMaxParticipants(Math.min(Math.max(maxParticipants, 2), 100));
        battle.setCreator(user);
        battle.setStatus("WAITING");
        battleRepository.save(battle);

        // Creator auto-joins
        BattleParticipant bp = new BattleParticipant();
        bp.setBattle(battle);
        bp.setUser(user);
        participantRepository.save(bp);

        return "redirect:/battles/" + battle.getId();
    }

    // ─── Join Battle ────────────────────────────────────────
    @PostMapping("/join")
    public String joinBattle(@RequestParam String roomCode, HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/login";
        user = userRepository.findById(user.getId()).orElse(user);

        Battle battle = battleRepository.findByRoomCode(roomCode.trim().toUpperCase()).orElse(null);
        if (battle == null) return "redirect:/battles?error=not_found";
        if ("COMPLETED".equals(battle.getStatus())) return "redirect:/battles?error=ended";

        if (participantRepository.existsByBattleAndUser(battle, user)) {
            return "redirect:/battles/" + battle.getId();
        }

        long count = participantRepository.countByBattle(battle);
        if (count >= battle.getMaxParticipants()) return "redirect:/battles?error=full";

        BattleParticipant bp = new BattleParticipant();
        bp.setBattle(battle);
        bp.setUser(user);
        participantRepository.save(bp);

        return "redirect:/battles/" + battle.getId();
    }

    // ─── Start Battle (Creator Only) ────────────────────────
    @PostMapping("/{id}/start")
    public String startBattle(@PathVariable Long id, HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/login";

        Battle battle = battleRepository.findById(id).orElse(null);
        if (battle == null) return "redirect:/battles";
        if (!battle.getCreator().getId().equals(user.getId())) return "redirect:/battles/" + id;
        if (!"WAITING".equals(battle.getStatus())) return "redirect:/battles/" + id;

        battle.setStatus("ACTIVE");
        battle.setStartedAt(LocalDateTime.now());
        battle.setEndsAt(LocalDateTime.now().plusHours(battle.getDurationHours()));
        battleRepository.save(battle);

        return "redirect:/battles/" + id;
    }

    // ─── Submit Entry ───────────────────────────────────────
    @PostMapping("/{id}/submit")
    public String submitEntry(
            @PathVariable Long id,
            @RequestParam String submissionUrl,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String secondaryUrl,
            HttpSession session) {

        User user = getUserFromSession(session);
        if (user == null) return "redirect:/login";
        user = userRepository.findById(user.getId()).orElse(user);

        Battle battle = battleRepository.findById(id).orElse(null);
        if (battle == null) return "redirect:/battles";
        if (!"ACTIVE".equals(battle.getStatus())) return "redirect:/battles/" + id + "?error=not_active";
        if (!participantRepository.existsByBattleAndUser(battle, user)) return "redirect:/battles/" + id + "?error=not_participant";
        if (submissionRepository.existsByBattleAndUser(battle, user)) return "redirect:/battles/" + id + "?error=already_submitted";

        BattleSubmission sub = new BattleSubmission();
        sub.setBattle(battle);
        sub.setUser(user);
        sub.setSubmissionUrl(submissionUrl);
        sub.setDescription(description);
        sub.setSecondaryUrl(secondaryUrl);
        submissionRepository.save(sub);

        return "redirect:/battles/" + id + "?submitted=true";
    }

    // ─── Move to Voting (Creator Only) ──────────────────────
    @PostMapping("/{id}/start-voting")
    public String startVoting(@PathVariable Long id, HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/login";

        Battle battle = battleRepository.findById(id).orElse(null);
        if (battle == null) return "redirect:/battles";
        if (!battle.getCreator().getId().equals(user.getId())) return "redirect:/battles/" + id;

        battle.setStatus("VOTING");
        battle.setVotingEndsAt(LocalDateTime.now().plusHours(24));
        battleRepository.save(battle);

        return "redirect:/battles/" + id;
    }

    // ─── Cast Vote ──────────────────────────────────────────
    @PostMapping("/{id}/vote/{submissionId}")
    public String castVote(@PathVariable Long id, @PathVariable Long submissionId, HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/login";
        user = userRepository.findById(user.getId()).orElse(user);

        Battle battle = battleRepository.findById(id).orElse(null);
        if (battle == null) return "redirect:/battles";
        if (!"VOTING".equals(battle.getStatus())) return "redirect:/battles/" + id;
        if (voteRepository.existsByBattleAndVoter(battle, user)) return "redirect:/battles/" + id + "?error=already_voted";

        BattleSubmission sub = submissionRepository.findById(submissionId).orElse(null);
        if (sub == null || !sub.getBattle().getId().equals(id)) return "redirect:/battles/" + id;

        BattleVote vote = new BattleVote();
        vote.setBattle(battle);
        vote.setVoter(user);
        vote.setSubmission(sub);
        voteRepository.save(vote);

        sub.setVoteCount(sub.getVoteCount() + 1);
        submissionRepository.save(sub);

        return "redirect:/battles/" + id + "?voted=true";
    }

    // ─── End Battle & Declare Winner (Creator Only) ─────────
    @PostMapping("/{id}/end")
    @Transactional
    public String endBattle(@PathVariable Long id, HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/login";

        Battle battle = battleRepository.findById(id).orElse(null);
        if (battle == null) return "redirect:/battles";
        if (!battle.getCreator().getId().equals(user.getId())) return "redirect:/battles/" + id;

        completeBattle(battle);
        return "redirect:/battles/" + id;
    }

    // ─── Auto-Complete Logic ────────────────────────────────
    private void autoCompleteBattles() {
        LocalDateTime now = LocalDateTime.now();

        // Auto-move ACTIVE → VOTING when submission time ends
        List<Battle> activeBattles = battleRepository.findByStatusOrderByCreatedAtDesc("ACTIVE");
        for (Battle b : activeBattles) {
            if (b.getEndsAt() != null && b.getEndsAt().isBefore(now)) {
                b.setStatus("VOTING");
                b.setVotingEndsAt(now.plusHours(24));
                battleRepository.save(b);
            }
        }

        // Auto-complete VOTING battles when voting ends
        List<Battle> votingBattles = battleRepository.findByStatusOrderByCreatedAtDesc("VOTING");
        for (Battle b : votingBattles) {
            if (b.getVotingEndsAt() != null && b.getVotingEndsAt().isBefore(now)) {
                completeBattle(b);
            }
        }
    }

    private void completeBattle(Battle battle) {
        List<BattleSubmission> subs = submissionRepository.findByBattleOrderByVoteCountDesc(battle);
        if (!subs.isEmpty()) {
            BattleSubmission topSub = subs.get(0);
            User winner = topSub.getUser();
            battle.setWinner(winner);

            // Award XP
            if (winner != null) {
                User dbWinner = userRepository.findById(winner.getId()).orElse(winner);
                dbWinner.setXp((dbWinner.getXp() != null ? dbWinner.getXp() : 0) + battle.getWinnerXp());
                dbWinner.addCoins(100);
                // Update level
                updateLevel(dbWinner);
                userRepository.save(dbWinner);
            }
        }
        battle.setStatus("COMPLETED");
        battleRepository.save(battle);
    }

    private void updateLevel(User user) {
        int xp = user.getXp() != null ? user.getXp() : 0;
        if (xp >= 5000) user.setLevel("Platinum");
        else if (xp >= 2000) user.setLevel("Gold");
        else if (xp >= 1000) user.setLevel("Silver");
        else if (xp >= 500) user.setLevel("Bronze");
        else user.setLevel("Novice");
    }
}
