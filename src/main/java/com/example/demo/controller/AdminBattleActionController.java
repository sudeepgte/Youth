package com.example.demo.controller;

import com.example.demo.model.Battle;
import com.example.demo.model.BattleSubmission;
import com.example.demo.model.User;
import com.example.demo.repository.BattleRepository;
import com.example.demo.repository.BattleSubmissionRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpSession;
import java.util.List;

@Controller
public class AdminBattleActionController {

    @Autowired
    private BattleRepository battleRepository;

    @Autowired
    private BattleSubmissionRepository submissionRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/admin/battles/{id}/commission")
    @Transactional
    public String updateCommission(@PathVariable Long id, @RequestParam Double adminCommissionPct, HttpSession session) {
        if (!"admin".equals(session.getAttribute("user"))) return "redirect:/login";
        Battle battle = battleRepository.findById(id).orElse(null);
        if (battle != null && !Boolean.TRUE.equals(battle.getPayoutReleased())) {
            battle.setAdminCommissionPct(adminCommissionPct);
            battleRepository.save(battle);
        }
        return "redirect:/admin/battles";
    }

    @PostMapping("/admin/battles/{id}/release")
    @Transactional
    public String releasePayout(@PathVariable Long id, HttpSession session) {
        if (!"admin".equals(session.getAttribute("user"))) return "redirect:/login";
        Battle battle = battleRepository.findById(id).orElse(null);
        if (battle == null || !"COMPLETED".equals(battle.getStatus()) || Boolean.TRUE.equals(battle.getPayoutReleased())) {
            return "redirect:/admin/battles";
        }

        double entryFee = battle.getEntryFee() != null ? battle.getEntryFee() : 0.0;
        double totalPool = entryFee * (battle.getParticipants() != null ? battle.getParticipants().size() : 0);
        double commPct = battle.getAdminCommissionPct() != null ? battle.getAdminCommissionPct() : 7.0;
        double adminCut = totalPool * (commPct / 100.0);

        User admin = userRepository.findByUsername("admin");
        if (admin != null) {
            admin.setWalletBalance((admin.getWalletBalance() != null ? admin.getWalletBalance() : 0.0) + adminCut);
            userRepository.save(admin);
        }

        List<BattleSubmission> subs = submissionRepository.findByBattleOrderByVoteCountDesc(battle);
        if (!subs.isEmpty()) {
            Double p1 = battle.getPrize1() != null ? battle.getPrize1() : 0.0;
            Double p2 = battle.getPrize2() != null ? battle.getPrize2() : 0.0;
            Double p3 = battle.getPrize3() != null ? battle.getPrize3() : 0.0;
            
            if (p1 == 0 && p2 == 0 && p3 == 0) {
                p1 = totalPool - adminCut;
            }

            if (subs.size() > 0 && p1 > 0) {
                User w1 = subs.get(0).getUser();
                w1.setWalletBalance((w1.getWalletBalance() != null ? w1.getWalletBalance() : 0.0) + p1);
                userRepository.save(w1);
            }
            if (subs.size() > 1 && p2 > 0) {
                User w2 = subs.get(1).getUser();
                w2.setWalletBalance((w2.getWalletBalance() != null ? w2.getWalletBalance() : 0.0) + p2);
                userRepository.save(w2);
            }
            if (subs.size() > 2 && p3 > 0) {
                User w3 = subs.get(2).getUser();
                w3.setWalletBalance((w3.getWalletBalance() != null ? w3.getWalletBalance() : 0.0) + p3);
                userRepository.save(w3);
            }
        } else if (battle.getWinner() != null) {
            Double p1 = battle.getPrize1() != null ? battle.getPrize1() : (totalPool - adminCut);
            User w1 = battle.getWinner();
            w1.setWalletBalance((w1.getWalletBalance() != null ? w1.getWalletBalance() : 0.0) + p1);
            userRepository.save(w1);
        }

        battle.setPayoutReleased(true);
        battleRepository.save(battle);
        
        return "redirect:/admin/battles?success=released";
    }
}
