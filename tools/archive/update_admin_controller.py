import os

path = 'src/main/java/com/example/demo/controller/AdminTournamentController.java'
with open(path, 'r') as f:
    content = f.read()

new_create = '''    @PostMapping("/create")
    public String createTournament(@ModelAttribute Tournament tournament, @RequestParam(required = false) List<Long> judgeIds, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        tournament.setStatus("DRAFT");
        if (judgeIds != null && !judgeIds.isEmpty()) {
            List<User> judges = userRepository.findAllById(judgeIds);
            tournament.setJudges(judges);
        }
        tournamentService.createTournament(tournament);
        return "redirect:/admin/tournaments";
    }'''

old_create = '''    @PostMapping("/create")
    public String createTournament(@ModelAttribute Tournament tournament, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        tournament.setStatus("DRAFT");
        tournamentService.createTournament(tournament);
        return "redirect:/admin/tournaments";
    }'''

content = content.replace(old_create, new_create)
content = content.replace('model.addAttribute("tournament", new Tournament());', 'model.addAttribute("tournament", new Tournament());\n        model.addAttribute("users", userRepository.findAll());')

new_methods = '''
    @GetMapping("/{id}/checkin")
    public String checkinTournament(@PathVariable Long id, Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/admin/login";
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
        if (!isAdmin(session)) return "redirect:/admin/login";
        Tournament t = tournamentRepository.findById(id).orElse(null);
        if (t == null) return "redirect:/admin/tournaments";

        List<TournamentParticipant> participants = participantRepository.findByTournament(t);
        TournamentParticipant match = participants.stream()
            .filter(p -> p.getQrPassCode() != null && p.getQrPassCode().equals(qrPassCode))
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
}'''

# Replace last brace
content = content[:content.rfind('}')] + new_methods

with open(path, 'w') as f:
    f.write(content)
