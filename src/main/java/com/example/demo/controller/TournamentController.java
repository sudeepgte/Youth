package com.example.demo.controller;

import com.example.demo.model.Tournament;
import com.example.demo.model.TournamentParticipant;
import com.example.demo.model.TournamentRound;
import com.example.demo.model.TournamentBattle;
import com.example.demo.model.User;
import com.example.demo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/tournaments")
public class TournamentController {

    @Autowired private TournamentRepository tournamentRepository;
    @Autowired private TournamentParticipantRepository participantRepository;
    @Autowired private TournamentRoundRepository roundRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TournamentBattleRepository tournamentBattleRepository;

    private User getUserFromSession(HttpSession session) {
        Object userObj = session.getAttribute("user");
        if (userObj instanceof User) {
            return userRepository.findById(((User) userObj).getId()).orElse(null);
        }
        return null;
    }

    @GetMapping
    public String listTournaments(Model model, HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/login";

        List<Tournament> tournaments = tournamentRepository.findAllByOrderByTournamentStartDesc();
        model.addAttribute("tournaments", tournaments);
        model.addAttribute("user", user);
        return "tournaments";
    }

    @GetMapping("/{id}")
    public String viewTournament(@PathVariable Long id, Model model, HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/login";

        Tournament t = tournamentRepository.findById(id).orElse(null);
        if (t == null) return "redirect:/tournaments";

        boolean isRegistered = participantRepository.existsByTournamentAndUser(t, user);
        long participantCount = participantRepository.countByTournament(t);
        List<TournamentRound> rounds = roundRepository.findByTournamentOrderByRoundNumberAsc(t);
        
        TournamentBattle currentMatch = null;
        if (isRegistered && ("IN_PROGRESS".equals(t.getStatus()))) {
            // Find current match
            currentMatch = t.getTournamentBattles().stream()
                .filter(tb -> "READY".equals(tb.getStatus()) || "WAITING".equals(tb.getStatus()) || "LIVE".equals(tb.getStatus()))
                .filter(tb -> (tb.getPlayer1() != null && tb.getPlayer1().getId().equals(user.getId())) || 
                              (tb.getPlayer2() != null && tb.getPlayer2().getId().equals(user.getId())))
                .findFirst().orElse(null);
        }

        model.addAttribute("tournament", t);
        model.addAttribute("user", user);
        model.addAttribute("isRegistered", isRegistered);
        model.addAttribute("participantCount", participantCount);
        model.addAttribute("rounds", rounds);
        model.addAttribute("currentMatch", currentMatch);
        
        return "tournament-view";
    }

    @PostMapping("/{id}/join")
    public String joinTournament(@PathVariable Long id, HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/login";

        Tournament t = tournamentRepository.findById(id).orElse(null);
        if (t == null) return "redirect:/tournaments";

        if (!"REGISTRATION_OPEN".equals(t.getStatus())) {
            return "redirect:/tournaments/" + id + "?error=registration_closed";
        }

        long count = participantRepository.countByTournament(t);
        if (t.getMaxParticipants() != null && count >= t.getMaxParticipants()) {
            return "redirect:/tournaments/" + id + "?error=full";
        }

        if (!participantRepository.existsByTournamentAndUser(t, user)) {
            TournamentParticipant tp = new TournamentParticipant();
            tp.setTournament(t);
            tp.setUser(user);
            tp.setSeed((int) count + 1);
            participantRepository.save(tp);
        }

        return "redirect:/tournaments/" + id;
    }
}
