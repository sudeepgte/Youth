package com.example.demo.controller;

import com.example.demo.model.Tournament;
import com.example.demo.model.TournamentBattle;
import com.example.demo.model.TournamentJudgeScore;
import com.example.demo.model.TournamentParticipant;
import com.example.demo.model.User;
import com.example.demo.repository.TournamentBattleRepository;
import com.example.demo.repository.TournamentJudgeScoreRepository;
import com.example.demo.repository.TournamentParticipantRepository;
import com.example.demo.repository.TournamentRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;

import java.util.List;

@Controller
@RequestMapping("/tournaments/{id}/judge")
public class TournamentJudgeController {

    @Autowired private TournamentRepository tournamentRepository;
    @Autowired private TournamentBattleRepository tournamentBattleRepository;
    @Autowired private TournamentJudgeScoreRepository judgeScoreRepository;
    @Autowired private TournamentParticipantRepository participantRepository;
    @Autowired private UserRepository userRepository;

    private User getUserFromSession(HttpSession session) {
        Object userAttr = session.getAttribute("user");
        if (userAttr instanceof User) {
            return (User) userAttr;
        }
        return null;
    }

    @GetMapping
    public String judgePortal(@PathVariable Long id, Model model, HttpSession session) {
        User judge = getUserFromSession(session);
        if (judge == null) return "redirect:/login";

        Tournament t = tournamentRepository.findById(id).orElse(null);
        if (t == null) return "redirect:/tournaments";

        // Verify if user is a judge for this tournament
        boolean isJudge = t.getJudges().stream().anyMatch(u -> u.getId().equals(judge.getId()));
        if (!isJudge && !"admin".equals(session.getAttribute("user"))) {
            return "redirect:/tournaments/" + id + "?error=not_judge";
        }

        // Find matches in progress
        List<TournamentBattle> liveBattles = t.getTournamentBattles().stream()
                .filter(tb -> "READY".equals(tb.getStatus()) || "LIVE".equals(tb.getStatus()))
                .toList();

        model.addAttribute("tournament", t);
        model.addAttribute("liveBattles", liveBattles);
        return "tournament-judge";
    }

    @PostMapping("/{battleId}/score")
    public String submitScore(@PathVariable Long id, @PathVariable Long battleId,
                              @RequestParam Long participantId,
                              @RequestParam Integer techniqueScore,
                              @RequestParam Integer creativityScore,
                              @RequestParam Integer performanceScore,
                              @RequestParam Integer executionScore,
                              @RequestParam Integer audienceImpactScore,
                              HttpSession session) {
        User judge = getUserFromSession(session);
        if (judge == null) return "redirect:/login";

        Tournament t = tournamentRepository.findById(id).orElse(null);
        TournamentBattle tb = tournamentBattleRepository.findById(battleId).orElse(null);
        if (tb == null || t == null) return "redirect:/tournaments";

        TournamentParticipant participant = participantRepository.findById(participantId).orElse(null);
        if (participant == null) return "redirect:/tournaments/" + id + "/judge";

        TournamentJudgeScore score = judgeScoreRepository.findByTournamentBattleAndJudgeAndParticipant(tb, judge, participant).orElse(new TournamentJudgeScore());
        score.setTournamentBattle(tb);
        score.setJudge(judge);
        score.setParticipant(participant);
        score.setTechniqueScore(techniqueScore);
        score.setCreativityScore(creativityScore);
        score.setPerformanceScore(performanceScore);
        score.setExecutionScore(executionScore);
        score.setAudienceImpactScore(audienceImpactScore);

        judgeScoreRepository.save(score);

        // Check if we can determine the winner
        // Let's assume all judges have submitted. (For simplicity, we can do it manually or via a separate 'Finish Match' button by admin, or calculate automatically).
        // For the sake of this feature, admin will advance the bracket or we can add a 'finalize' endpoint.

        return "redirect:/tournaments/" + id + "/judge?success=true";
    }
}
