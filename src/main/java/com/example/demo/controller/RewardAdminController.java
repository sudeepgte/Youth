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
        if (!"admin".equals(session.getAttribute("user"))) {
            return "redirect:/login";
        }
        
        RewardConfig existing = rewardService.getConfig();
        config.setId(existing.getId());
        rewardConfigRepository.save(config);
        
        return "redirect:/admin?success=rewards_updated";
    }
}
