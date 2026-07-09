package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/wallet")
public class WalletController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HttpServletRequest httpServletRequest;

    private User getUserFromSession(HttpSession session) {
        Object authUser = httpServletRequest.getAttribute("authenticatedUser");
        if (authUser instanceof User) return (User) authUser;
        Object sessionUser = session.getAttribute("user");
        if (sessionUser instanceof User) return (User) sessionUser;
        Object userIdObj = session.getAttribute("userId");
        if (userIdObj != null) {
            try {
                Long userId = userIdObj instanceof Number ? ((Number) userIdObj).longValue() : Long.parseLong(userIdObj.toString());
                return userRepository.findById(userId).orElse(null);
            } catch (Exception e) { /* ignore */ }
        }
        return null;
    }

    @GetMapping
    public String viewWallet(HttpSession session, Model model) {
        User user = getUserFromSession(session);
        if (user == null) {
            return "redirect:/login";
        }
        user = userRepository.findById(user.getId()).orElse(user);
        model.addAttribute("user", user);
        return "wallet";
    }

    @PostMapping("/add")
    public String addFunds(@RequestParam("amount") Double amount, HttpSession session, RedirectAttributes redirectAttributes) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/login";
        
        if (amount != null && amount > 0) {
            user = userRepository.findById(user.getId()).orElse(user);
            user.addWalletBalance(amount);
            userRepository.save(user);
            redirectAttributes.addFlashAttribute("success", "Successfully added ₹" + amount + " to your wallet.");
        }
        return "redirect:/wallet";
    }

    @PostMapping("/withdraw")
    public String withdrawFunds(@RequestParam("amount") Double amount, HttpSession session, RedirectAttributes redirectAttributes) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/login";

        user = userRepository.findById(user.getId()).orElse(user);
        
        if (amount != null && amount > 0) {
            if (user.getWalletBalance() >= amount) {
                user.deductWalletBalance(amount);
                userRepository.save(user);
                redirectAttributes.addFlashAttribute("success", "Successfully withdrew ₹" + amount + " from your wallet.");
            } else {
                redirectAttributes.addFlashAttribute("error", "Insufficient balance.");
            }
        }
        return "redirect:/wallet";
    }
}
