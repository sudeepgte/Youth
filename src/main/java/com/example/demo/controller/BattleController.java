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
@RequestMapping(value = "/battles")
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
        model.addAttribute("isCreator", false);
        model.addAttribute("activeBattles", activeBattles);
        model.addAttribute("completedBattles", completedBattles);

        // Aggregate colleges participating in active battles
        Map<String, Long> collegeCountMap = new LinkedHashMap<>();
        for (Battle b : activeBattles) {
            if (b.getParticipants() != null) {
                for (BattleParticipant p : b.getParticipants()) {
                    String college = p.getUser() != null ? p.getUser().getCollegeName() : null;
                    if (college != null && !college.isBlank()) {
                        collegeCountMap.merge(college, 1L, Long::sum);
                    }
                }
            }
        }
        // Sort by count descending
        List<Map.Entry<String, Long>> sortedColleges = collegeCountMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toList());
        model.addAttribute("battleColleges", sortedColleges);

        return "battle-arena";
    }

    // ─── View Single Battle ─────────────────────────────────
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
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
        if ("OFFLINE".equals(battle.getMode())) {
            final double jw = battle.getJudgeWeight() != null ? battle.getJudgeWeight() : 70.0;
            final double aw = battle.getAudienceWeight() != null ? battle.getAudienceWeight() : 30.0;
            submissions.sort((s1, s2) -> {
                double score1 = (s1.getJudgeTotalScore() * jw / 100.0) + (s1.getVoteCount() * aw / 100.0);
                double score2 = (s2.getJudgeTotalScore() * jw / 100.0) + (s2.getVoteCount() * aw / 100.0);
                return Double.compare(score2, score1);
            });
        }
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

        // Aggregate colleges for this battle's participants
        Map<String, Long> battleCollegeMap = new LinkedHashMap<>();
        for (BattleParticipant p : participants) {
            String college = p.getUser() != null ? p.getUser().getCollegeName() : null;
            if (college != null && !college.isBlank()) {
                battleCollegeMap.merge(college, 1L, Long::sum);
            }
        }
        List<Map.Entry<String, Long>> sortedColleges = battleCollegeMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toList());
        model.addAttribute("battleColleges", sortedColleges);

        return "battle-arena";
    }

    // ─── Create Battle ──────────────────────────────────────
    @RequestMapping(value = "/create", method = RequestMethod.POST)
    public String createBattle(
            @RequestParam String title,
            @RequestParam String category,
            @RequestParam(defaultValue = "24") Integer durationHours,
            @RequestParam(defaultValue = "2") Integer maxParticipants,
            @RequestParam(required = false, defaultValue = "ONLINE") String mode,
            @RequestParam(required = false) String venue,
            @RequestParam(required = false) String eventDate,
            @RequestParam(required = false) String eventTime,
            @RequestParam(required = false, defaultValue = "0.0") Double entryFee,
            @RequestParam(required = false, defaultValue = "0.0") Double prize1,
            @RequestParam(required = false, defaultValue = "0.0") Double prize2,
            @RequestParam(required = false, defaultValue = "0.0") Double prize3,
            @RequestParam(required = false, defaultValue = "70.0") Double judgeWeight,
            @RequestParam(required = false, defaultValue = "30.0") Double audienceWeight,
            HttpSession session) {

        User user = getUserFromSession(session);
        if (user == null) return "redirect:/login";
        user = userRepository.findById(user.getId()).orElse(user);

        // Prizes are real money now, not deducting from virtual coins.

        Battle battle = new Battle();
        battle.setTitle(title);
        battle.setCategory(category);
        battle.setDurationHours(durationHours);
        battle.setMaxParticipants(Math.min(Math.max(maxParticipants, 2), 100));
        battle.setMode(mode);
        battle.setEntryFee(entryFee);
        battle.setPrize1(prize1);
        battle.setPrize2(prize2);
        battle.setPrize3(prize3);
        if ("OFFLINE".equals(mode)) {
            battle.setVenue(venue);
            battle.setEventDate(eventDate);
            battle.setEventTime(eventTime);
            battle.setJudgeWeight(judgeWeight);
            battle.setAudienceWeight(audienceWeight);
        }
        battle.setCreator(user);
        battle.setStatus("WAITING");
        battleRepository.save(battle);

        // Creator auto-joins
        BattleParticipant bp = new BattleParticipant();
        bp.setBattle(battle);
        bp.setUser(user);
        if ("OFFLINE".equals(mode)) {
            bp.setSeatNumber("Host");
            bp.setParticipantNumber("#00");
            bp.setCheckedIn(true);
        }
        participantRepository.save(bp);

        return "redirect:/battles/" + battle.getId();
    }

    // ─── Join Battle ────────────────────────────────────────
    @RequestMapping(value = "/join", method = RequestMethod.POST)
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

        if (battle.getEntryFee() != null && battle.getEntryFee() > 0) {
            return "redirect:/battles/" + battle.getId() + "/pay";
        }

        // If no entry fee, join directly
        return joinUserToBattle(battle, user);
    }

    @RequestMapping(value = "/{id}/pay", method = RequestMethod.GET)
    public String showPaymentPage(@PathVariable Long id, HttpSession session, Model model) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/login";

        Battle battle = battleRepository.findById(id).orElse(null);
        if (battle == null) return "redirect:/battles?error=not_found";

        if (participantRepository.existsByBattleAndUser(battle, user)) {
            return "redirect:/battles/" + battle.getId();
        }

        model.addAttribute("battle", battle);
        model.addAttribute("user", user);
        return "payment";
    }

    @RequestMapping(value = "/{id}/process-payment", method = RequestMethod.POST)
    public String processPayment(@PathVariable Long id, HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/login";
        user = userRepository.findById(user.getId()).orElse(user);

        Battle battle = battleRepository.findById(id).orElse(null);
        if (battle == null) return "redirect:/battles?error=not_found";

        if (participantRepository.existsByBattleAndUser(battle, user)) {
            return "redirect:/battles/" + battle.getId();
        }

        long count = participantRepository.countByBattle(battle);
        if (count >= battle.getMaxParticipants()) return "redirect:/battles?error=full";

        // In a real app, verify payment status from gateway here.
        // For MVP, we assume the mock payment was successful.

        return joinUserToBattle(battle, user);
    }

    private String joinUserToBattle(Battle battle, User user) {
        long count = participantRepository.countByBattle(battle);
        BattleParticipant bp = new BattleParticipant();
        bp.setBattle(battle);
        bp.setUser(user);
        if ("OFFLINE".equals(battle.getMode())) {
            char row = (char) ('A' + (count / 10));
            long num = (count % 10) + 1;
            bp.setSeatNumber(String.valueOf(row) + num);
            bp.setParticipantNumber(String.format("#%02d", count + 1));
            bp.setCheckedIn(false);
        }
        participantRepository.save(bp);
        return "redirect:/battles/" + battle.getId();
    }

    // ─── Start Battle (Creator Only) ────────────────────────
    @RequestMapping(value = "/{id}/start", method = RequestMethod.POST)
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

    // ─── Register for Offline Battle ────────────────────────
    @RequestMapping(value = "/{id}/register", method = RequestMethod.POST)
    public String registerForOffline(@PathVariable Long id, HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/login";
        user = userRepository.findById(user.getId()).orElse(user);

        Battle battle = battleRepository.findById(id).orElse(null);
        if (battle == null) return "redirect:/battles";
        if (!"OFFLINE".equals(battle.getMode())) return "redirect:/battles/" + id;

        if (participantRepository.existsByBattleAndUser(battle, user)) {
            return "redirect:/battles/" + id;
        }

        long count = participantRepository.countByBattle(battle);
        if (count >= battle.getMaxParticipants()) return "redirect:/battles?error=full";

        BattleParticipant bp = new BattleParticipant();
        bp.setBattle(battle);
        bp.setUser(user);

        char row = (char) ('A' + (count / 10));
        long num = (count % 10) + 1;
        bp.setSeatNumber(String.valueOf(row) + num);
        bp.setParticipantNumber(String.format("#%02d", count + 1));
        bp.setCheckedIn(false);

        participantRepository.save(bp);
        return "redirect:/battles/" + id;
    }

    // ─── Mark Attendance / Check-In ─────────────────────────
    @RequestMapping(value = "/{id}/checkin/{participantId}", method = RequestMethod.POST)
    public String checkInParticipant(@PathVariable Long id, @PathVariable Long participantId, HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/login";

        Battle battle = battleRepository.findById(id).orElse(null);
        if (battle == null) return "redirect:/battles";
        if (!battle.getCreator().getId().equals(user.getId())) return "redirect:/battles/" + id;

        BattleParticipant bp = participantRepository.findById(participantId).orElse(null);
        if (bp != null && bp.getBattle().getId().equals(id)) {
            bp.setCheckedIn(true);
            participantRepository.save(bp);
        }
        return "redirect:/battles/" + id;
    }

    // ─── Submit Judge Scores (Creator Only) ─────────────────
    @RequestMapping(value = "/{id}/submission/{submissionId}/score", method = RequestMethod.POST)
    public String scoreSubmission(
            @PathVariable Long id,
            @PathVariable Long submissionId,
            @RequestParam Integer creativity,
            @RequestParam Integer quality,
            @RequestParam Integer speed,
            @RequestParam Integer presentation,
            @RequestParam Integer accuracy,
            HttpSession session) {

        User user = getUserFromSession(session);
        if (user == null) return "redirect:/login";

        Battle battle = battleRepository.findById(id).orElse(null);
        if (battle == null) return "redirect:/battles";
        if (!battle.getCreator().getId().equals(user.getId())) return "redirect:/battles/" + id;

        BattleSubmission sub = submissionRepository.findById(submissionId).orElse(null);
        if (sub != null && sub.getBattle().getId().equals(id)) {
            sub.setJudgeCreativity(creativity);
            sub.setJudgeQuality(quality);
            sub.setJudgeSpeed(speed);
            sub.setJudgePresentation(presentation);
            sub.setJudgeAccuracy(accuracy);
            sub.setJudgeTotalScore(creativity + quality + speed + presentation + accuracy);
            submissionRepository.save(sub);
        }
        return "redirect:/battles/" + id;
    }

    // ─── Submit Entry ───────────────────────────────────────
    @RequestMapping(value = "/{id}/submit", method = RequestMethod.POST)
    public String submitEntry(
            @PathVariable Long id,
            @RequestParam String submissionUrl,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String secondaryUrl,
            HttpSession session) {

        User user = getUserFromSession(session);
        if (user == null) return "redirect:/login";
        user = userRepository.findById(user.getId()).orElse(user);

        if (submissionUrl == null || submissionUrl.trim().isEmpty()) {
            return "redirect:/battles/" + id + "?error=empty_submission";
        }

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
    @RequestMapping(value = "/{id}/start-voting", method = RequestMethod.POST)
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
    @RequestMapping(value = "/{id}/vote/{submissionId}", method = RequestMethod.POST)
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
    @RequestMapping(value = "/{id}/end", method = RequestMethod.POST)
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

    // ─── Delete Battle (Creator Only) ────────────────────────
    @RequestMapping(value = "/{id}/delete", method = RequestMethod.POST)
    @Transactional
    public String deleteBattle(@PathVariable Long id, HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/login";

        Battle battle = battleRepository.findById(id).orElse(null);
        if (battle == null) return "redirect:/battles";

        // Authorization check: only creator can delete
        if (battle.getCreator() == null || !battle.getCreator().getId().equals(user.getId())) {
            return "redirect:/battles/" + id + "?error=unauthorized";
        }

        battleRepository.delete(battle);
        return "redirect:/battles?deleted=true";
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
        if ("OFFLINE".equals(battle.getMode())) {
            final double jw = battle.getJudgeWeight() != null ? battle.getJudgeWeight() : 70.0;
            final double aw = battle.getAudienceWeight() != null ? battle.getAudienceWeight() : 30.0;
            subs.sort((s1, s2) -> {
                double score1 = (s1.getJudgeTotalScore() * jw / 100.0) + (s1.getVoteCount() * aw / 100.0);
                double score2 = (s2.getJudgeTotalScore() * jw / 100.0) + (s2.getVoteCount() * aw / 100.0);
                return Double.compare(score2, score1);
            });
        }
        
        boolean isTie = false;
        if (subs.size() >= 2) {
            if ("OFFLINE".equals(battle.getMode())) {
                final double jw = battle.getJudgeWeight() != null ? battle.getJudgeWeight() : 70.0;
                final double aw = battle.getAudienceWeight() != null ? battle.getAudienceWeight() : 30.0;
                double score0 = (subs.get(0).getJudgeTotalScore() * jw / 100.0) + (subs.get(0).getVoteCount() * aw / 100.0);
                double score1 = (subs.get(1).getJudgeTotalScore() * jw / 100.0) + (subs.get(1).getVoteCount() * aw / 100.0);
                isTie = (score0 == score1);
            } else {
                isTie = (subs.get(0).getVoteCount().equals(subs.get(1).getVoteCount()));
            }
        }

        if (isTie) {
            battle.setStatus("TIE");
            battle.setWinner(subs.get(0).getUser());
            battle.setWinner2(subs.get(1).getUser());
            battleRepository.save(battle);
            return;
        }

        if (!subs.isEmpty()) {
            BattleSubmission topSub = subs.get(0);
            User winner = topSub.getUser();
            battle.setWinner(winner);

            if (winner != null) {
                User dbWinner = userRepository.findById(winner.getId()).orElse(winner);
                dbWinner.setXp((dbWinner.getXp() != null ? dbWinner.getXp() : 0) + battle.getWinnerXp());
                dbWinner.addCoins(100); // Base gamification 100
                updateLevel(dbWinner);
                userRepository.save(dbWinner);
            }
        }
        if (subs.size() > 1) {
            User winner2 = subs.get(1).getUser();
            battle.setWinner2(winner2);
            if (winner2 != null) {
                User dbWinner2 = userRepository.findById(winner2.getId()).orElse(winner2);
                // Real money prize, not adding to virtual coins
            }
        }
        if (subs.size() > 2) {
            User winner3 = subs.get(2).getUser();
            battle.setWinner3(winner3);
            if (winner3 != null) {
                User dbWinner3 = userRepository.findById(winner3.getId()).orElse(winner3);
                // Real money prize, not adding to virtual coins
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
