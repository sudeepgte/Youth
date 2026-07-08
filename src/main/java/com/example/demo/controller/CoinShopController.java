package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;

@Controller
@RequestMapping(value = "/shop")
public class CoinShopController {

    @Autowired
    private UserRepository userRepository;

    private User getUserFromSession(HttpSession session) {
        Object userIdObj = session.getAttribute("userId");
        if (userIdObj != null) {
            return userRepository.findById((Long) userIdObj).orElse(null);
        }
        return null;
    }

    @GetMapping
    public String showShop(HttpSession session, Model model) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/login";

        model.addAttribute("user", user);
        return "shop";
    }

    @RequestMapping(value = "/buy-discount", method = RequestMethod.POST)
    public String buyDiscount(HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/login";

        if (user.getCoins() >= 100) {
            user.setCoins(user.getCoins() - 100);
            user.setHasDiscount(true);
            userRepository.save(user);
            return "redirect:/shop?success=discount";
        }
        return "redirect:/shop?error=insufficient_coins";
    }

    @RequestMapping(value = "/buy-boost", method = RequestMethod.POST)
    public String buyBoost(HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/login";

        if (user.getCoins() >= 200) {
            user.setCoins(user.getCoins() - 200);
            user.setProfileBoostUntil(LocalDateTime.now().plusDays(3));
            userRepository.save(user);
            return "redirect:/shop?success=boost";
        }
        return "redirect:/shop?error=insufficient_coins";
    }

    @RequestMapping(value = "/buy-badge", method = RequestMethod.POST)
    public String buyBadge(HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/login";

        if (user.getCoins() >= 500) {
            user.setCoins(user.getCoins() - 500);
            user.setPremium(true);
            userRepository.save(user);
            return "redirect:/shop?success=badge";
        }
        return "redirect:/shop?error=insufficient_coins";
    }

    @RequestMapping(value = "/buy-free-entry", method = RequestMethod.POST)
    public String buyFreeEntry(HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/login";

        if (user.getCoins() >= 300) {
            user.setCoins(user.getCoins() - 300);
            user.setHasFreeEntry(true);
            userRepository.save(user);
            return "redirect:/shop?success=free_entry";
        }
        return "redirect:/shop?error=insufficient_coins";
    }
}
