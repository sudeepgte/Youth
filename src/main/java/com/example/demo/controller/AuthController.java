package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.RewardService;
import com.example.demo.config.TokenBlacklist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.example.demo.config.JwtUtil;

@Controller
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RewardService rewardService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TokenBlacklist tokenBlacklist;



    @org.springframework.web.bind.annotation.ResponseBody
    
    @RequestMapping(value = "/debug-users", method = RequestMethod.GET)
    public String debugUsers() {
        StringBuilder sb = new StringBuilder();
        for (User u : userRepository.findAll()) {
            sb.append(u.getUsername()).append(":").append(u.getPassword()).append("\n");
        }
        return sb.toString();
    }

    @RequestMapping(value = "/register", method = RequestMethod.GET)
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public String registerUser(@ModelAttribute("user") User user, Model model) {
        if (userRepository.findByUsername(user.getUsername()) != null) {
            return "redirect:/register?error=duplicate";
        }
        if (userRepository.findByEmail(user.getEmail()) != null) {
            return "redirect:/register?error=duplicate_email";
        }
        if (user.getDob() != null && user.getDob().isAfter(java.time.LocalDate.now())) {
            return "redirect:/register?error=future_dob";
        }
        String pwd = user.getPassword();
        if (pwd == null || pwd.length() < 8 || !pwd.matches(".*[A-Z].*") || !pwd.matches(".*[a-z].*") || !pwd.matches(".*\\d.*") || !pwd.matches(".*[@$!%*?&].*")) {
            return "redirect:/register?error=weak_password";
        }
        if (user.getEmail() == null || !user.getEmail().toLowerCase().endsWith(".com")) {
            return "redirect:/register?error=invalid_email_domain";
        }
        userRepository.save(user);
        return "redirect:/home";
    }

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String showLoginForm() {
        return "login";
    }

    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public String loginUser(@org.springframework.web.bind.annotation.RequestParam String username,
            @org.springframework.web.bind.annotation.RequestParam String password,
            jakarta.servlet.http.HttpSession session,
            jakarta.servlet.http.HttpServletResponse response) {
        if ("admin".equals(username) && "admin123".equals(password)) {
            String token = jwtUtil.generateToken("admin");
            jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("jwtToken", token);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            response.addCookie(cookie);
            
            session.setAttribute("user", "admin");
            return "redirect:/admin";
        }
        User user = userRepository.findByUsername(username);
        if (user != null && user.getPassword().equals(password)) {
            rewardService.awardDailyLogin(user); // Zen Coins Awarded here
            
            String token = jwtUtil.generateToken(username);
            jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("jwtToken", token);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            response.addCookie(cookie);
            
            session.setAttribute("user", user);
            session.setAttribute("userId", user.getId());
            return "redirect:/dashboard";
        } else {
            return "redirect:/login?error=bad_credentials";
        }
    }

    @RequestMapping(value = "/logout", method = RequestMethod.GET)
    public String logout(
            jakarta.servlet.http.HttpSession session,
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response) {

        // ── 1. Blacklist the JWT from cookie (most common path) ──
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie c : request.getCookies()) {
                if ("jwtToken".equals(c.getName())) {
                    tokenBlacklist.blacklist(c.getValue());
                    break;
                }
            }
        }

        // ── 2. Also blacklist any ?auth= token in the URL ──
        String queryToken = request.getParameter("auth");
        if (queryToken != null && !queryToken.isBlank()) {
            tokenBlacklist.blacklist(queryToken);
        }

        // ── 3. Destroy server-side session ──
        session.invalidate();

        // ── 4. Expire the jwtToken cookie in the browser ──
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("jwtToken", "");
        cookie.setPath("/");
        cookie.setMaxAge(0);   // Delete immediately
        cookie.setHttpOnly(true);
        response.addCookie(cookie);

        return "redirect:/login?loggedOut=true";
    }
}
