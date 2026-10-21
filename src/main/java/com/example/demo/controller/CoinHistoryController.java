package com.example.demo.controller;

import com.example.demo.model.CoinTransaction;
import com.example.demo.model.User;
import com.example.demo.repository.CoinTransactionRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpSession;
import java.util.List;

@RestController
@RequestMapping("/api/coins")
public class CoinHistoryController {

    @Autowired
    private CoinTransactionRepository coinTransactionRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/history")
    public ResponseEntity<?> getHistory(HttpSession session) {
        Object userObj = session.getAttribute("user");
        if (!(userObj instanceof User)) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        User sessionUser = (User) userObj;
        User user = userRepository.findById(sessionUser.getId()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        
        List<CoinTransaction> history = coinTransactionRepository.findByUserOrderByTimestampDesc(user);
        return ResponseEntity.ok(history);
    }
}