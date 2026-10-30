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
        if (session == null) return false;
        Object userAttr = session.getAttribute("user");
        if ("admin".equals(userAttr)) return true;
        if (userAttr instanceof User && "admin".equalsIgnoreCase(((User) userAttr).getUsername())) return true;
        return "admin".equals(session.getAttribute("admin"));
    }

    @GetMapping
    public String listTournaments(Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";
        List<Tournament> tournaments = tournamentRepository.findAllByOrderByIdDesc();
        model.addAttribute("tournaments", tournaments);
        return "admin-tournaments";
    }

    @GetMapping("/create")
    public String createTournamentForm(@RequestParam(required = false, defaultValue = "ONLINE") String mode, Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";
        Tournament t = new Tournament();
        t.setMode(mode);
        model.addAttribute("tournament", t);
        model.addAttribute("users", userRepository.findAll());
        return "admin-tournament-create";
    }

    @PostMapping("/create")
    public String createTournament(@ModelAttribute Tournament tournament, 
                                   @RequestParam(required = false) List<Long> judgeIds,
                                   HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";
        if (judgeIds != null && !judgeIds.isEmpty()) {
            tournament.setJudges(userRepository.findAllById(judgeIds));
        }
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

    @GetMapping("/{id}/checkin")
    public String checkinTournament(@PathVariable Long id, Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";
        Tournament t = tournamentRepository.findById(id).orElse(null);
        if (t == null) return "redirect:/admin/tournaments";

        List<TournamentParticipant> participants = participantRepository.findByTournament(t);
        model.addAttribute("tournament", t);
        model.addAttribute("participants", participants);
        
        long checkedInCount = participants.stream().filter(TournamentParticipant::getCheckedIn).count();
        model.addAttribute("checkedInCount", checkedInCount);
        model.addAttribute("registeredCount", participants.size());
        
        return "admin-tournament-checkin";
    }

    @PostMapping("/{id}/checkin/scan")
    public String scanCheckin(@PathVariable Long id, @RequestParam String qrPassCode, HttpSession session, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) return "redirect:/login";
        String finalCode = qrPassCode;
        if (finalCode != null && finalCode.contains("/")) {
            finalCode = finalCode.substring(finalCode.lastIndexOf("/") + 1);
        }
        Tournament t = tournamentRepository.findById(id).orElse(null);
        if (t == null) return "redirect:/admin/tournaments";

        List<TournamentParticipant> participants = participantRepository.findByTournament(t);
        final String searchCode = finalCode;
        TournamentParticipant match = participants.stream()
            .filter(p -> p.getQrPassCode() != null && p.getQrPassCode().equals(searchCode))
            .findFirst().orElse(null);
            
        if (match != null) {
            match.setCheckedIn(true);
            participantRepository.save(match);
            redirectAttributes.addFlashAttribute("successMessage", "Successfully checked in: " + match.getUser().getUsername() + " (" + match.getParticipantNumber() + ")");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid QR code or participant not found in this tournament.");
        }
        
        return "redirect:/admin/tournaments/" + id + "/checkin";
    }
}
