package com.example.demo.controller;

import com.example.demo.model.RewardConfig;
import com.example.demo.service.RewardService;
import com.example.demo.repository.RewardConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping(value = "/admin/rewards")
public class RewardAdminController {

    @Autowired
    private RewardService rewardService;

    @Autowired
    private RewardConfigRepository rewardConfigRepository;

    @RequestMapping(value = "/update", method = RequestMethod.POST)
    public String updateRewards(@ModelAttribute RewardConfig config, HttpSession session) {
        Object u = session.getAttribute("user");
        boolean isAdmin = "admin".equals(u)
                || (u instanceof com.example.demo.model.User && "admin".equalsIgnoreCase(((com.example.demo.model.User) u).getUsername()))
                || "admin".equals(session.getAttribute("admin"));
        if (!isAdmin) {
            return "redirect:/login";
        }
        
        RewardConfig existing = rewardService.getConfig();
        config.setId(existing.getId());
        rewardConfigRepository.save(config);
        
        return "redirect:/admin?success=rewards_updated";
    }
}
