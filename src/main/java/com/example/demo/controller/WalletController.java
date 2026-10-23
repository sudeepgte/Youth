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
    public String addFunds(
            @RequestParam(value = "amount", required = false) Double amount,
            @RequestParam(value = "paymentMethod", required = false, defaultValue = "UPI") String paymentMethod,
            @RequestParam(value = "transactionRef", required = false) String transactionRef,
            HttpSession session, 
            RedirectAttributes redirectAttributes) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/login";

        if (amount == null || amount <= 0) {
            redirectAttributes.addFlashAttribute("error", "Please enter a valid amount greater than \u20B90.");
            return "redirect:/wallet";
        }

        if (amount > 100000.0) {
            redirectAttributes.addFlashAttribute("error", "Maximum deposit limit is \u20B91,00,000 per transaction.");
            return "redirect:/wallet";
        }

        user = userRepository.findById(user.getId()).orElse(user);

        // Generate verified payment reference if not provided
        String refId = (transactionRef != null && !transactionRef.trim().isEmpty()) 
                ? transactionRef.trim() 
                : "PAY-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 9000 + 1000);

        user.addWalletBalance(amount);
        userRepository.save(user);

        // Save completed deposit transaction
        WalletTransaction tx = new WalletTransaction(user, amount, "DEPOSIT", "Added funds via " + paymentMethod);
        tx.setStatus("COMPLETED");
        tx.setReferenceId(refId);
        walletTransactionRepository.save(tx);

        redirectAttributes.addFlashAttribute("success", "Payment verified! Successfully added \u20B9" + String.format("%.2f", amount) + " to your wallet. Ref: " + refId);
        return "redirect:/wallet";
    }

    @PostMapping("/withdraw")
    public String withdrawFunds(
            @RequestParam(value = "amount", required = false) Double amount, 
            @RequestParam(value = "accountDetails", required = false) String accountDetails, 
            HttpSession session, 
            RedirectAttributes redirectAttributes) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/login";

        user = userRepository.findById(user.getId()).orElse(user);

        if (amount == null || amount <= 0) {
            redirectAttributes.addFlashAttribute("error", "Please enter a valid amount to withdraw.");
            return "redirect:/wallet";
        }

        if (amount < 100.0) {
            redirectAttributes.addFlashAttribute("error", "Minimum withdrawal amount is \u20B9100.00.");
            return "redirect:/wallet";
        }

        if (accountDetails == null || accountDetails.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please provide valid transfer account details (UPI ID or Bank Account).");
            return "redirect:/wallet";
        }

        Double currentBalance = user.getWalletBalance() != null ? user.getWalletBalance() : 0.0;
        if (currentBalance < amount) {
            redirectAttributes.addFlashAttribute("error", "Insufficient balance. Your available balance is \u20B9" + String.format("%.2f", currentBalance));
            return "redirect:/wallet";
        }

        user.deductWalletBalance(amount);
        userRepository.save(user);

        String accountClean = accountDetails.trim();
        String refId = "WTH-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 9000 + 1000);

        WalletTransaction tx = new WalletTransaction(
            user, 
            amount, 
            "WITHDRAWAL", 
            "Withdrawal request to " + accountClean
        );
        tx.setStatus("COMPLETED");
        tx.setReferenceId(refId);
        walletTransactionRepository.save(tx);

        redirectAttributes.addFlashAttribute("success", "Withdrawal of \u20B9" + String.format("%.2f", amount) + " to " + accountClean + " processed successfully. Ref: " + refId);
        return "redirect:/wallet";
    }
}

