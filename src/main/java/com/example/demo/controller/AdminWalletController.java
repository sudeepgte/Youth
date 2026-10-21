package com.example.demo.controller;

import com.example.demo.model.WalletTransaction;
import com.example.demo.repository.WalletTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import jakarta.servlet.http.HttpSession;

import java.util.List;

@Controller
@RequestMapping("/admin/withdrawals")
public class AdminWalletController {

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    private boolean isAdmin(HttpSession session) {
        Object userAttr = session.getAttribute("user");
        return "admin".equals(userAttr);
    }

    @GetMapping
    public String viewWithdrawals(Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        
        List<WalletTransaction> pendingWithdrawals = walletTransactionRepository.findAll().stream()
                .filter(tx -> "WITHDRAWAL".equals(tx.getType()) && "PENDING".equals(tx.getStatus()))
                .toList();
                
        List<WalletTransaction> history = walletTransactionRepository.findAll().stream()
                .filter(tx -> "WITHDRAWAL".equals(tx.getType()) && !"PENDING".equals(tx.getStatus()))
                .toList();
                
        model.addAttribute("pendingWithdrawals", pendingWithdrawals);
        model.addAttribute("history", history);
        return "admin-withdrawals";
    }

    @PostMapping("/{id}/approve")
    public String approveWithdrawal(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        WalletTransaction tx = walletTransactionRepository.findById(id).orElse(null);
        if (tx != null && "PENDING".equals(tx.getStatus())) {
            tx.setStatus("APPROVED");
            walletTransactionRepository.save(tx);
        }
        return "redirect:/admin/withdrawals";
    }

    @PostMapping("/{id}/reject")
    public String rejectWithdrawal(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        WalletTransaction tx = walletTransactionRepository.findById(id).orElse(null);
        if (tx != null && "PENDING".equals(tx.getStatus())) {
            tx.setStatus("REJECTED");
            walletTransactionRepository.save(tx);
            // Refund the user
            if (tx.getUser() != null && tx.getAmount() != null) {
                tx.getUser().addWalletBalance(tx.getAmount());
            }
        }
        return "redirect:/admin/withdrawals";
    }
}