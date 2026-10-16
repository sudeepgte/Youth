package com.example.demo.controller;
import com.example.demo.model.Battle;
import com.example.demo.model.BattlePrediction;
import com.example.demo.model.User;
import com.example.demo.repository.BattlePredictionRepository;
import com.example.demo.repository.BattleRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;
@Controller
@RequestMapping("/battles")
public class BattlePredictionController {
    @Autowired private BattleRepository battleRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private BattlePredictionRepository predictionRepository;
    private User getUserFromSession(HttpSession session) {
        Object userObj = session.getAttribute("user");
        if (userObj instanceof User) {
            return (User) userObj;
        }
        return null;
    }
    @PostMapping("/{id}/predict")
    public String placePrediction(@PathVariable Long id, @RequestParam Long predictedWinnerId, @RequestParam Double amount, HttpSession session, RedirectAttributes redirectAttributes) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/login";
        user = userRepository.findById(user.getId()).orElse(null);
        if (user == null) return "redirect:/login";
        Battle battle = battleRepository.findById(id).orElse(null);
        if (battle == null) return "redirect:/battles";
        if (!"ACTIVE".equals(battle.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "You can only bet on active battles.");
            return "redirect:/battles/" + id + "/live";
        }
        if (predictionRepository.existsByBattleAndBettor(battle, user)) {
            redirectAttributes.addFlashAttribute("error", "You have already placed a bet on this battle.");
            return "redirect:/battles/" + id + "/live";
        }
        if (amount <= 0 || user.getWalletBalance() < amount) {
            redirectAttributes.addFlashAttribute("error", "Insufficient wallet balance.");
            return "redirect:/battles/" + id + "/live";
        }
        User predictedWinner = userRepository.findById(predictedWinnerId).orElse(null);
        if (predictedWinner == null) {
            redirectAttributes.addFlashAttribute("error", "Invalid player selected.");
            return "redirect:/battles/" + id + "/live";
        }
        user.deductWalletBalance(amount);
        userRepository.save(user);
        BattlePrediction prediction = new BattlePrediction();
        prediction.setBattle(battle);
        prediction.setBettor(user);
        prediction.setPredictedWinner(predictedWinner);
        prediction.setAmount(amount);
        predictionRepository.save(prediction);
        redirectAttributes.addFlashAttribute("message", "Bet placed successfully! Good luck.");
        return "redirect:/battles/" + id + "/live";
    }
}