package com.example.demo.controller;

import com.example.demo.model.EventRegistration;
import com.example.demo.model.User;
import com.example.demo.model.UserReward;
import com.example.demo.repository.EventRegistrationRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.UserRewardRepository;
import com.example.demo.service.SecretRewardService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping(value = "/rewards")
public class RewardController {

    @Autowired
    private EventRegistrationRepository registrationRepository;

    @Autowired
    private UserRewardRepository userRewardRepository;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private SecretRewardService secretRewardService;

    @RequestMapping(value = "/reveal/{registrationId}", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> revealReward(@PathVariable Long registrationId, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        EventRegistration reg = registrationRepository.findById(registrationId).orElse(null);
        if (reg == null) {
            response.put("success", false);
            response.put("message", "Registration not found");
            return response;
        }

        // Verify session user matches registration user (unless admin, but let's keep it simple)
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null || !userId.equals(reg.getUser().getId())) {
            response.put("success", false);
            response.put("message", "Unauthorized");
            return response;
        }

        if (!reg.isAttendanceMarked() && !"COMPLETED".equals(reg.getEvent().getStatus())) {
            response.put("success", false);
            response.put("message", "Attendance not marked yet.");
            return response;
        }
        
        // Ensure reward is assigned if not already (e.g. if event completed without attendance)
        if (!reg.isRewardRevealed()) {
            secretRewardService.assignReward(reg);
        }

        reg.setRewardRevealed(true);
        registrationRepository.save(reg);

        response.put("success", true);
        return response;
    }

    @RequestMapping(value = "/redeem/{rewardCode}", method = RequestMethod.GET)
    public String redeemPage(@PathVariable String rewardCode, Model model, HttpSession session) {
        Optional<UserReward> optionalReward = userRewardRepository.findByRewardCode(rewardCode);
        
        if (optionalReward.isEmpty()) {
            model.addAttribute("error", "Invalid reward code.");
            return "reward-redeem";
        }

        UserReward reward = optionalReward.get();
        model.addAttribute("reward", reward);
        return "reward-redeem";
    }

    @RequestMapping(value = "/redeem/{rewardCode}", method = RequestMethod.POST)
    public String confirmRedeem(@PathVariable String rewardCode, Model model, HttpSession session) {
        // Typically a business owner scans and confirms. 
        // We'll just allow any logged-in user to redeem it for now if they have the link (or require auth).
        
        Optional<UserReward> optionalReward = userRewardRepository.findByRewardCode(rewardCode);
        if (optionalReward.isEmpty()) {
            model.addAttribute("error", "Invalid reward code.");
            return "reward-redeem";
        }

        UserReward reward = optionalReward.get();
        if (!"AVAILABLE".equals(reward.getStatus())) {
            model.addAttribute("error", "Reward is already redeemed or expired.");
            model.addAttribute("reward", reward);
            return "reward-redeem";
        }

        reward.setStatus("REDEEMED");
        userRewardRepository.save(reward);

        model.addAttribute("success", "Reward successfully redeemed!");
        model.addAttribute("reward", reward);
        return "reward-redeem";
    }
}
