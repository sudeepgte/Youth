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
import java.util.stream.Collectors;

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
        if (session == null) return false;
        Object userAttr = session.getAttribute("user");
        if ("admin".equals(userAttr)) return true;
        if (userAttr instanceof User && "admin".equalsIgnoreCase(((User) userAttr).getUsername())) return true;
        return "admin".equals(session.getAttribute("admin"));
    }

    @GetMapping
    public String listTournaments(Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";
        List<Tournament> tournaments = tournamentRepository.findAllByOrderByIdDesc().stream()
                .filter(t -> !"ONLINE".equalsIgnoreCase(t.getMode()))
                .collect(Collectors.toList());
        model.addAttribute("tournaments", tournaments);
        return "admin-tournaments";
    }

    @GetMapping("/create")
    public String createTournamentForm(@RequestParam(required = false, defaultValue = "OFFLINE") String mode, Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";
        Tournament t = new Tournament();
        t.setMode("OFFLINE");
        t.setBattleDurationMinutes(10);
        t.setMaxParticipants(16);
        t.setEntryFee(0.0);
        t.setAdminCommissionPercent(0.0);
        t.setPrize1Percent(60.0);
        t.setPrize2Percent(30.0);
        model.addAttribute("tournament", t);

        User adminUser = userRepository.findByUsername("admin");
        model.addAttribute("adminUser", adminUser);
        model.addAttribute("users", userRepository.findAll());
        return "admin-tournament-create";
    }

    @PostMapping("/create")
    public String createTournament(@ModelAttribute Tournament tournament, 
                                   @RequestParam(required = false) List<Long> judgeIds,
                                   HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";
        tournament.setMode("OFFLINE");

        // Date validation: Registration start cannot be in the past (yesterday or older)
        LocalDateTime now = LocalDateTime.now();
        if (tournament.getRegistrationStart() != null && tournament.getRegistrationStart().isBefore(now.minusMinutes(10))) {
            return "redirect:/admin/tournaments/create?error=date_past";
        }
        if (tournament.getRegistrationStart() != null && tournament.getRegistrationEnd() != null && 
            tournament.getRegistrationEnd().isBefore(tournament.getRegistrationStart())) {
            return "redirect:/admin/tournaments/create?error=invalid_range";
        }
        if (tournament.getRegistrationEnd() != null && tournament.getTournamentStart() != null && 
            tournament.getTournamentStart().isBefore(tournament.getRegistrationEnd())) {
            return "redirect:/admin/tournaments/create?error=kickoff_before_reg";
        }

        // Judges: Admin is always the primary judge, plus any selected additional judges
        User adminUser = userRepository.findByUsername("admin");
        List<User> judgesList = new java.util.ArrayList<>();
        if (adminUser != null) {
            judgesList.add(adminUser);
        }
        if (judgeIds != null && !judgeIds.isEmpty()) {
            List<User> selected = userRepository.findAllById(judgeIds);
            for (User u : selected) {
                if (adminUser == null || !u.getId().equals(adminUser.getId())) {
                    judgesList.add(u);
                }
            }
        }
        tournament.setJudges(judgesList);

        if (tournament.getStatus() == null || tournament.getStatus().trim().isEmpty() || "DRAFT".equalsIgnoreCase(tournament.getStatus())) {
            tournament.setStatus("REGISTRATION_OPEN");
        }
        tournamentService.createTournament(tournament);
        return "redirect:/admin/tournaments";
    }

    @GetMapping("/{id}/manage")
    public String manageTournament(@PathVariable Long id, Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";
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
        if (!isAdmin(session)) return "redirect:/login";
        Tournament t = tournamentRepository.findById(id).orElse(null);
        if (t != null) {
            t.setStatus(status);
            tournamentRepository.save(t);
        }
        return "redirect:/admin/tournaments/" + id + "/manage";
    }

    @PostMapping("/{id}/start")
    public String startTournament(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";
        try {
            tournamentService.startTournament(id);
        } catch (Exception e) {
            return "redirect:/admin/tournaments/" + id + "/manage?error=" + e.getMessage();
        }
        return "redirect:/admin/tournaments/" + id + "/manage";
    }
}
