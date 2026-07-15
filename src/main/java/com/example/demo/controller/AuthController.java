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

    @Autowired
    private com.example.demo.config.ActiveLoginRegistry activeLoginRegistry;



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
            if (activeLoginRegistry.isUserAlreadyLoggedIn(username, null)) {
                return "redirect:/login?error=already_logged_in";
            }
            rewardService.awardDailyLogin(user); // Zen Coins Awarded here
            
            String token = jwtUtil.generateToken(username);
            jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("jwtToken", token);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            response.addCookie(cookie);
            
            activeLoginRegistry.registerLogin(username, token);
            
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

        String tokenToRevoke = null;
        // ── 1. Blacklist the JWT from cookie (most common path) ──
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie c : request.getCookies()) {
                if ("jwtToken".equals(c.getName())) {
                    tokenToRevoke = c.getValue();
                    tokenBlacklist.blacklist(tokenToRevoke);
                    break;
                }
            }
        }

        // ── 2. Also blacklist any ?auth= token in the URL ──
        String queryToken = request.getParameter("auth");
        if (queryToken != null && !queryToken.isBlank()) {
            tokenToRevoke = queryToken;
            tokenBlacklist.blacklist(queryToken);
        }

        if (tokenToRevoke != null) {
            try {
                String username = jwtUtil.extractUsername(tokenToRevoke);
                activeLoginRegistry.removeLogin(username);
            } catch (Exception e) {}
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

    @RequestMapping(value = "/forgot-password", method = RequestMethod.GET)
    public String showForgotPasswordForm() {
        return "forgot-password";
    }

    @RequestMapping(value = "/forgot-password", method = RequestMethod.POST)
    public String resetPassword(
            @org.springframework.web.bind.annotation.RequestParam String username,
            @org.springframework.web.bind.annotation.RequestParam String email,
            @org.springframework.web.bind.annotation.RequestParam String newPassword,
            @org.springframework.web.bind.annotation.RequestParam String confirmPassword,
            Model model) {

        if (!newPassword.equals(confirmPassword)) {
            return "redirect:/forgot-password?error=mismatch";
        }

        if (newPassword.length() < 8 || !newPassword.matches(".*[A-Z].*") || !newPassword.matches(".*[a-z].*") || !newPassword.matches(".*\\d.*") || !newPassword.matches(".*[@$!%*?&].*")) {
            return "redirect:/forgot-password?error=weak_password";
        }

        User user = userRepository.findByUsername(username);
        if (user == null || !email.equalsIgnoreCase(user.getEmail())) {
            return "redirect:/forgot-password?error=not_found";
        }

        user.setPassword(newPassword);
        userRepository.save(user);

        return "redirect:/forgot-password?success=true";
    }
}
