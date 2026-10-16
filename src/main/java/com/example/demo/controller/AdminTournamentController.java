package com.example.demo.controller;

import com.example.demo.model.Tournament;
import com.example.demo.model.TournamentParticipant;
import com.example.demo.model.TournamentRound;
import com.example.demo.model.User;
import com.example.demo.repository.*;
import com.example.demo.service.TournamentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin/tournaments")
public class AdminTournamentController {

    @Autowired private TournamentRepository tournamentRepository;
    @Autowired private TournamentParticipantRepository participantRepository;
    @Autowired private TournamentRoundRepository roundRepository;
    @Autowired private TournamentBattleRepository tournamentBattleRepository;
    @Autowired private TournamentService tournamentService;
    @Autowired private UserRepository userRepository;

    private boolean isAdmin(HttpSession session) {
        Object userAttr = session.getAttribute("user");
        if ("admin".equals(userAttr)) {
            return true;
        }
        return false;
    }

    @GetMapping
    public String listTournaments(Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        List<Tournament> tournaments = tournamentRepository.findAllByOrderByTournamentStartDesc();
        model.addAttribute("tournaments", tournaments);
        return "admin-tournaments";
    }

    @GetMapping("/create")
    public String createTournamentForm(Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        model.addAttribute("tournament", new Tournament());
        return "admin-tournament-create";
    }

    @PostMapping("/create")
    public String createTournament(@ModelAttribute Tournament tournament, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        tournament.setStatus("DRAFT");
        tournamentService.createTournament(tournament);
        return "redirect:/admin/tournaments";
    }

    @GetMapping("/{id}/manage")
    public String manageTournament(@PathVariable Long id, Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        Tournament t = tournamentRepository.findById(id).orElse(null);
        if (t == null) return "redirect:/admin/tournaments";

        List<TournamentParticipant> participants = participantRepository.findByTournament(t);
        List<TournamentRound> rounds = roundRepository.findByTournamentOrderByRoundNumberAsc(t);

        model.addAttribute("tournament", t);
        model.addAttribute("participants", participants);
        model.addAttribute("rounds", rounds);
        return "admin-tournament-manage";
    }

    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable Long id, @RequestParam String status, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        Tournament t = tournamentRepository.findById(id).orElse(null);
        if (t != null) {
            t.setStatus(status);
            tournamentRepository.save(t);
        }
        return "redirect:/admin/tournaments/" + id + "/manage";
    }

    @PostMapping("/{id}/start")
    public String startTournament(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        try {
            tournamentService.startTournament(id);
        } catch (Exception e) {
            return "redirect:/admin/tournaments/" + id + "/manage?error=" + e.getMessage();
        }
        return "redirect:/admin/tournaments/" + id + "/manage";
    }
}
