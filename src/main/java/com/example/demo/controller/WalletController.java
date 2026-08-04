package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.model.WalletTransaction;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.WalletTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Controller
@RequestMapping("/wallet")
public class WalletController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

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
        List<WalletTransaction> transactions = walletTransactionRepository.findByUserOrderByTimestampDesc(user);
        model.addAttribute("user", user);
        model.addAttribute("transactions", transactions);
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
            
            // Save transaction
            WalletTransaction tx = new WalletTransaction(user, amount, "DEPOSIT", "Added funds via payment");
            walletTransactionRepository.save(tx);
            
            redirectAttributes.addFlashAttribute("success", "Successfully added ₹" + amount + " to your wallet.");
        }
        return "redirect:/wallet";
    }

    @PostMapping("/withdraw")
    public String withdrawFunds(@RequestParam("amount") Double amount, 
                                @RequestParam(value="accountDetails", required=false) String accountDetails, 
                                HttpSession session, RedirectAttributes redirectAttributes) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/login";

        user = userRepository.findById(user.getId()).orElse(user);
        
        if (amount != null && amount > 0) {
            if (user.getWalletBalance() >= amount) {
                user.deductWalletBalance(amount);
                userRepository.save(user);
                
                String accountStr = accountDetails != null && !accountDetails.isEmpty() ? " to " + accountDetails : "";
                
                // Save transaction
                WalletTransaction tx = new WalletTransaction(
                    user, 
                    amount, 
                    "WITHDRAWAL", 
                    "Withdrawal request" + accountStr
                );
                walletTransactionRepository.save(tx);
                
                redirectAttributes.addFlashAttribute("success", "Successfully initiated withdrawal of ₹" + amount + accountStr + ". It will be processed shortly.");
            } else {
                redirectAttributes.addFlashAttribute("error", "Insufficient balance.");
            }
        }
        return "redirect:/wallet";
    }
}
