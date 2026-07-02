package com.example.demo.controller;

import com.example.demo.model.CouponClaim;
import com.example.demo.model.User;
import com.example.demo.repository.CouponClaimRepository;
import com.example.demo.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/redeem")
public class CouponController {

    @Autowired
    private CouponClaimRepository couponClaimRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<?> redeemCoupon(@RequestBody Map<String, String> payload, HttpSession session) {
        Object userObj = session.getAttribute("user");
        if (!(userObj instanceof User)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        User sessionUser = (User) userObj;
        
        // Fetch fresh user from DB
        User user = userRepository.findById(sessionUser.getId()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User not found"));
        }

        String code = payload.get("code");
        if (code == null || code.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Coupon code is required."));
        }
        
        code = code.trim().toUpperCase();

        if (!"ZENTRIX-ELITE-2025".equals(code)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid coupon code."));
        }

        if (couponClaimRepository.existsByUserAndCouponCode(user, code)) {
            return ResponseEntity.badRequest().body(Map.of("error", "You have already claimed this coupon!"));
        }

        // Award XP and Coins
        user.setXp((user.getXp() == null ? 0 : user.getXp()) + 500);
        user.addCoins(100);
        
        userRepository.save(user);

        // Save claim
        CouponClaim claim = new CouponClaim(user, code);
        couponClaimRepository.save(claim);

        // Update session
        session.setAttribute("user", user);

        return ResponseEntity.ok(Map.of(
            "message", "Success! You received +500 XP and +100 Coins.",
            "xp", user.getXp(),
            "coins", user.getCoins()
        ));
    }
}
