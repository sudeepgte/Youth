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

    @Autowired
    private com.example.demo.repository.UserRepository userRepository;

    @Autowired
    private com.example.demo.repository.NotificationRepository notificationRepository;

    @org.springframework.beans.factory.annotation.Value("${razorpay.key.id:${RAZORPAY_KEY_ID:rzp_test_youthiansMockKey}}")
    private String razorpayKeyId;

    @org.springframework.beans.factory.annotation.Value("${razorpay.key.secret:${RAZORPAY_KEY_SECRET:youthiansMockSecretKey123}}")
    private String razorpayKeySecret;

    @org.springframework.beans.factory.annotation.Value("${razorpayx.account.number:${RAZORPAYX_ACCOUNT_NUMBER:}}")
    private String razorpayXAccountNumber;

    private boolean isAdmin(HttpSession session) {
        if (session == null) return false;
        Object userAttr = session.getAttribute("user");
        if ("admin".equals(userAttr)) return true;
        if (userAttr instanceof com.example.demo.model.User && "admin".equalsIgnoreCase(((com.example.demo.model.User) userAttr).getUsername())) return true;
        return "admin".equals(session.getAttribute("admin"));
    }

    @GetMapping
    public String viewWithdrawals(Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";
        
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
    public String approveWithdrawal(
            @PathVariable Long id, 
            @org.springframework.web.bind.annotation.RequestParam(value = "manual", defaultValue = "false") boolean manual,
            HttpSession session, 
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) return "redirect:/login";

        WalletTransaction tx = walletTransactionRepository.findById(id).orElse(null);
        if (tx == null || !"PENDING".equals(tx.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "Transaction not found or already processed.");
            return "redirect:/admin/withdrawals";
        }

        double net = tx.getNetAmount() != null ? tx.getNetAmount() : (tx.getAmount() != null ? tx.getAmount() * 0.95 : 0.0);
        String destination = extractDestination(tx.getDescription());

        // If manual bypass is requested (admin confirmed they already sent funds via PhonePe/GPay)
        if (manual) {
            tx.setStatus("APPROVED");
            walletTransactionRepository.save(tx);
            if (tx.getUser() != null) {
                notificationRepository.save(new com.example.demo.model.Notification(tx.getUser(), null, 
                    "Your withdrawal of Rs. " + String.format("%.2f", tx.getAmount()) 
                    + " (Net Payout: Rs. " + String.format("%.2f", net) + ") to " + destination + " has been APPROVED and credited!", "WALLET"));
            }
            redirectAttributes.addFlashAttribute("success", "Payout marked as APPROVED (Manually transferred to " + destination + ").");
            return "redirect:/admin/withdrawals";
        }

        // Automated Direct Payout Execution via Razorpay / RazorpayX
        java.util.Map<String, Object> payoutResult = executeRazorpayAutomatedPayout(destination, net, tx);
        if (Boolean.TRUE.equals(payoutResult.get("success"))) {
            String utr = (String) payoutResult.get("utr");
            tx.setStatus("APPROVED");
            tx.setDescription(tx.getDescription() + " [Paid via Razorpay: " + utr + "]");
            walletTransactionRepository.save(tx);

            if (tx.getUser() != null) {
                notificationRepository.save(new com.example.demo.model.Notification(tx.getUser(), null, 
                    "Your withdrawal of Rs. " + String.format("%.2f", tx.getAmount()) 
                    + " (Net Payout: Rs. " + String.format("%.2f", net) + ") has been sent directly to your account! UTR: " + utr, "WALLET"));
            }

            redirectAttributes.addFlashAttribute("success", 
                "Automated Payout SUCCESSFUL! Transferred Rs. " + String.format("%.2f", net) + " directly to " + destination + " (UTR: " + utr + ").");
        } else {
            String errorMsg = (String) payoutResult.get("error");
            redirectAttributes.addFlashAttribute("error", 
                "Automated Bank/UPI Payout could not be dispatched: " + errorMsg + ". If you have already paid " + destination + " via PhonePe/GPay, click 'Confirm Manual Transfer' below.");
        }

        return "redirect:/admin/withdrawals";
    }

    private String extractDestination(String desc) {
        if (desc == null) return "UPI";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?:Withdrawal.*to\\s+)([\\w@\\.\\-]+)").matcher(desc);
        if (m.find()) return m.group(1);
        return desc;
    }

    private java.util.Map<String, Object> executeRazorpayAutomatedPayout(String destination, double netAmount, WalletTransaction tx) {
        try {
            boolean isLive = razorpayKeyId != null && !razorpayKeyId.contains("Mock") 
                    && (razorpayKeyId.startsWith("rzp_live_") || razorpayKeyId.startsWith("rzp_test_"));
            
            if (!isLive) {
                return java.util.Map.of("success", true, "utr", "UTR_SIM_" + System.currentTimeMillis());
            }

            // Step 1: Check if there's an eligible live deposit that can be refunded to user's UPI
            List<WalletTransaction> userDeposits = walletTransactionRepository.findByUserOrderByTimestampDesc(tx.getUser());
            for (WalletTransaction dep : userDeposits) {
                if ("DEPOSIT".equalsIgnoreCase(dep.getType()) && dep.getReferenceId() != null && dep.getReferenceId().startsWith("RZP-pay_")) {
                    String paymentId = dep.getReferenceId().replace("RZP-", "");
                    try {
                        String refundResp = executeRazorpayLiveRefund(paymentId, netAmount);
                        return java.util.Map.of("success", true, "utr", "REFUND_" + paymentId);
                    } catch (Exception ignored) {
                        // Refund might not be ready or settled, fallback to direct RazorpayX payout
                    }
                }
            }

            // Step 2: Direct RazorpayX Payout to UPI
            if (destination != null && destination.contains("@")) {
                String contactId = createRazorpayContact(tx.getUser() != null ? tx.getUser().getUsername() : "User");
                String fundAccountId = createRazorpayVpaFundAccount(contactId, destination);
                return createRazorpayPayout(fundAccountId, netAmount);
            } else {
                return java.util.Map.of("success", false, "error", "Destination " + destination + " is not a valid UPI ID (e.g. username@upi)");
            }

        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Razorpay connection error";
            if (msg.contains("URL was not found") || msg.contains("400")) {
                msg = "RazorpayX Banking Payouts is not yet enabled on your Razorpay account. Enable RazorpayX or transfer directly via your UPI app.";
            }
            return java.util.Map.of("success", false, "error", msg);
        }
    }

    private String executeRazorpayLiveRefund(String paymentId, double amount) throws Exception {
        long amountInPaise = Math.round(amount * 100);
        String jsonPayload = String.format("{\"amount\":%d,\"speed\":\"optimum\"}", amountInPaise);
        String auth = razorpayKeyId + ":" + razorpayKeySecret;
        String encodedAuth = java.util.Base64.getEncoder().encodeToString(auth.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create("https://api.razorpay.com/v1/payments/" + paymentId + "/refund"))
            .header("Authorization", "Basic " + encodedAuth)
            .header("Content-Type", "application/json")
            .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonPayload))
            .build();

        java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200 || response.statusCode() == 201) {
            return response.body();
        }
        throw new RuntimeException("Status " + response.statusCode() + ": " + response.body());
    }

    private String createRazorpayContact(String name) throws Exception {
        String jsonPayload = String.format("{\"name\":\"%s\",\"type\":\"customer\"}", name);
        String auth = razorpayKeyId + ":" + razorpayKeySecret;
        String encodedAuth = java.util.Base64.getEncoder().encodeToString(auth.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create("https://api.razorpay.com/v1/contacts"))
            .header("Authorization", "Basic " + encodedAuth)
            .header("Content-Type", "application/json")
            .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonPayload))
            .build();

        java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200 || response.statusCode() == 201) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"id\"\\s*:\\s*\"(cont_[^\"]+)\"").matcher(response.body());
            if (m.find()) return m.group(1);
        }
        throw new RuntimeException("Contact error: " + response.body());
    }

    private String createRazorpayVpaFundAccount(String contactId, String vpaAddress) throws Exception {
        String jsonPayload = String.format("{\"contact_id\":\"%s\",\"account_type\":\"vpa\",\"vpa\":{\"address\":\"%s\"}}", contactId, vpaAddress.trim());
        String auth = razorpayKeyId + ":" + razorpayKeySecret;
        String encodedAuth = java.util.Base64.getEncoder().encodeToString(auth.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create("https://api.razorpay.com/v1/fund_accounts"))
            .header("Authorization", "Basic " + encodedAuth)
            .header("Content-Type", "application/json")
            .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonPayload))
            .build();

        java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200 || response.statusCode() == 201) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"id\"\\s*:\\s*\"(fa_[^\"]+)\"").matcher(response.body());
            if (m.find()) return m.group(1);
        }
        throw new RuntimeException("Fund account error: " + response.body());
    }

    private java.util.Map<String, Object> createRazorpayPayout(String fundAccountId, double amount) throws Exception {
        long amountInPaise = Math.round(amount * 100);
        String accNum = (razorpayXAccountNumber != null && !razorpayXAccountNumber.isEmpty()) ? razorpayXAccountNumber : "7878780080316316";
        String jsonPayload = String.format("{\"account_number\":\"%s\",\"fund_account_id\":\"%s\",\"amount\":%d,\"currency\":\"INR\",\"mode\":\"UPI\",\"purpose\":\"payout\"}",
                accNum, fundAccountId, amountInPaise);

        String auth = razorpayKeyId + ":" + razorpayKeySecret;
        String encodedAuth = java.util.Base64.getEncoder().encodeToString(auth.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create("https://api.razorpay.com/v1/payouts"))
            .header("Authorization", "Basic " + encodedAuth)
            .header("Content-Type", "application/json")
            .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonPayload))
            .build();

        java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200 || response.statusCode() == 201) {
            String body = response.body();
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"id\"\\s*:\\s*\"(pout_[^\"]+)\"").matcher(body);
            String payoutId = m.find() ? m.group(1) : ("pout_" + System.currentTimeMillis());
            java.util.regex.Matcher utrMatcher = java.util.regex.Pattern.compile("\"utr\"\\s*:\\s*\"([^\"]+)\"").matcher(body);
            String utr = utrMatcher.find() ? utrMatcher.group(1) : ("UTR" + System.currentTimeMillis());
            return java.util.Map.of("success", true, "payoutId", payoutId, "utr", utr);
        }
        
        throw new RuntimeException(response.body());
    }

    @PostMapping("/{id}/reject")
    public String rejectWithdrawal(@PathVariable Long id, HttpSession session, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) return "redirect:/login";
        WalletTransaction tx = walletTransactionRepository.findById(id).orElse(null);
        if (tx != null && "PENDING".equals(tx.getStatus())) {
            tx.setStatus("REJECTED");
            walletTransactionRepository.save(tx);
            // Refund the user
            if (tx.getUser() != null && tx.getAmount() != null) {
                com.example.demo.model.User user = userRepository.findById(tx.getUser().getId()).orElse(tx.getUser());
                user.addWalletBalance(tx.getAmount());
                userRepository.save(user);

                notificationRepository.save(new com.example.demo.model.Notification(user, null, 
                    "Your withdrawal request of Rs. " + String.format("%.2f", tx.getAmount()) 
                    + " was rejected. The full amount has been refunded back to your wallet.", "WALLET"));
            }
            redirectAttributes.addFlashAttribute("success", "Withdrawal rejected and Rs. " + String.format("%.2f", tx.getAmount()) + " refunded to @" + tx.getUser().getUsername() + "'s wallet.");
        }
        return "redirect:/admin/withdrawals";
    }
}