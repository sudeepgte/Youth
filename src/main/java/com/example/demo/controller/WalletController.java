package com.example.demo.controller;

import com.example.demo.model.CoinTransaction;
import com.example.demo.model.Notification;
import com.example.demo.model.User;
import com.example.demo.model.WalletRequest;
import com.example.demo.model.WalletTransaction;
import com.example.demo.repository.CoinTransactionRepository;
import com.example.demo.repository.NotificationRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.WalletRequestRepository;
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
import java.util.UUID;

@Controller
@RequestMapping("/wallet")
public class WalletController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    @Autowired
    private WalletRequestRepository walletRequestRepository;

    @Autowired
    private CoinTransactionRepository coinTransactionRepository;

    @Autowired
    private NotificationRepository notificationRepository;

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

    @org.springframework.beans.factory.annotation.Value("${razorpay.key.id:rzp_test_youthiansMockKey}")
    private String razorpayKeyId;

    @org.springframework.beans.factory.annotation.Value("${razorpay.key.secret:youthiansMockSecretKey123}")
    private String razorpayKeySecret;

    private boolean verifyRazorpaySignature(String orderId, String paymentId, String signature, String secret) {
        if (orderId == null || paymentId == null || signature == null || secret == null) {
            return false;
        }
        try {
            String payload = orderId + "|" + paymentId;
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString().equalsIgnoreCase(signature.trim());
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isAdminSession(HttpSession session) {
        if (session == null) return false;
        Object u = session.getAttribute("user");
        if ("admin".equals(u)) return true;
        if (u instanceof User && "admin".equalsIgnoreCase(((User) u).getUsername())) return true;
        return "admin".equals(session.getAttribute("admin"));
    }

    @PostMapping("/create-order")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> createDepositOrder(@RequestParam Double amount, HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null) {
            return org.springframework.http.ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        if (amount == null || Double.isNaN(amount) || Double.isInfinite(amount) || amount < 10.0 || amount > 50000.0) {
            return org.springframework.http.ResponseEntity.badRequest().body(Map.of("error", "Deposit amount must be between Rs. 10 and Rs. 50,000"));
        }
        String orderId = "order_w_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        session.setAttribute("wallet_order_" + orderId, amount);
        return org.springframework.http.ResponseEntity.ok(Map.of(
            "orderId", orderId,
            "amount", amount,
            "keyId", razorpayKeyId
        ));
    }

    @PostMapping("/verify-payment")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> verifyWebPayment(
            @RequestParam String razorpay_payment_id,
            @RequestParam String razorpay_order_id,
            @RequestParam String razorpay_signature,
            @RequestParam Double amount,
            HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null) {
            return org.springframework.http.ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        String refId = "RZP-" + razorpay_payment_id.trim();
        if (walletTransactionRepository.existsByReferenceId(refId)) {
            return org.springframework.http.ResponseEntity.status(409).body(Map.of("error", "Duplicate payment receipt: already processed"));
        }

        boolean isValid = verifyRazorpaySignature(razorpay_order_id, razorpay_payment_id, razorpay_signature, razorpayKeySecret);
        if (!isValid) {
            return org.springframework.http.ResponseEntity.badRequest().body(Map.of("error", "Payment verification failed: Invalid signature"));
        }

        user = userRepository.findById(user.getId()).orElse(user);
        user.addWalletBalance(amount);
        userRepository.save(user);

        WalletTransaction tx = new WalletTransaction(user, amount, "DEPOSIT", "Web Razorpay Deposit (Ref: " + refId + ")");
        tx.setStatus("COMPLETED");
        tx.setReferenceId(refId);
        walletTransactionRepository.save(tx);

        notificationRepository.save(new Notification(user, null,
            "Deposit of Rs. " + String.format("%.2f", amount) + " added to your wallet! (Ref: " + refId + ")", "WALLET"));

        return org.springframework.http.ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Payment verified and credited successfully",
            "balance", user.getWalletBalance()
        ));
    }

    @GetMapping
    public String viewWallet(HttpSession session, Model model) {
        User user = getUserFromSession(session);
        if (user == null) {
            return "redirect:/login";
        }
        user = userRepository.findById(user.getId()).orElse(user);
        List<WalletTransaction> transactions = walletTransactionRepository.findByUserOrderByTimestampDesc(user);
        List<WalletRequest> incomingRequests = walletRequestRepository.findByTargetUserAndStatusOrderByCreatedAtDesc(user, "PENDING");
        List<WalletRequest> sentRequests = walletRequestRepository.findByRequesterOrderByCreatedAtDesc(user);

        model.addAttribute("user", user);
        model.addAttribute("transactions", transactions);
        model.addAttribute("incomingRequests", incomingRequests);
        model.addAttribute("sentRequests", sentRequests);
        model.addAttribute("razorpayKeyId", razorpayKeyId);
        return "wallet";
    }

    @PostMapping("/add")
    public String addFunds(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", "Direct self-deposits are not permitted. You can earn coins through battles and tournaments, or request them from friends.");
        return "redirect:/wallet";
    }

    @PostMapping("/buy-coins")
    public String buyCoins(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", "Direct coin purchases are disabled. Earn coins through platform activities, battles, or request from peers.");
        return "redirect:/wallet";
    }

    @PostMapping("/send")
    public String sendFunds(
            @RequestParam("recipientUsername") String recipientUsername,
            @RequestParam("amount") Double amount,
            @RequestParam(value = "currency", defaultValue = "COINS") String currency,
            @RequestParam(value = "note", required = false) String note,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        User sender = getUserFromSession(session);
        if (sender == null) return "redirect:/login";

        sender = userRepository.findById(sender.getId()).orElse(sender);

        if (recipientUsername == null || recipientUsername.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please provide a valid recipient username.");
            return "redirect:/wallet";
        }

        String targetName = recipientUsername.trim();
        if (targetName.equalsIgnoreCase(sender.getUsername())) {
            redirectAttributes.addFlashAttribute("error", "You cannot send funds to yourself.");
            return "redirect:/wallet";
        }

        User recipient = userRepository.findByUsername(targetName);
        if (recipient == null) {
            redirectAttributes.addFlashAttribute("error", "User @" + targetName + " not found.");
            return "redirect:/wallet";
        }

        if (amount == null || amount <= 0 || amount.isNaN() || amount.isInfinite()) {
            redirectAttributes.addFlashAttribute("error", "Please enter a valid amount.");
            return "redirect:/wallet";
        }

        boolean isCoins = "COINS".equalsIgnoreCase(currency);
        String noteClean = (note != null && !note.trim().isEmpty()) ? " Note: " + note.trim() : "";

        if (isCoins) {
            int coinAmount = amount.intValue();
            if (coinAmount <= 0) {
                redirectAttributes.addFlashAttribute("error", "Minimum coin transfer is 1 Coin.");
                return "redirect:/wallet";
            }
            if (sender.getCoins() < coinAmount) {
                redirectAttributes.addFlashAttribute("error", "Insufficient coins! You have " + sender.getCoins() + " coins.");
                return "redirect:/wallet";
            }
            sender.setCoins(sender.getCoins() - coinAmount);
            recipient.addCoins(coinAmount);
            userRepository.save(sender);
            userRepository.save(recipient);

            coinTransactionRepository.save(new CoinTransaction(sender, -coinAmount, "Transfer to @" + recipient.getUsername(), "P2P Sent" + noteClean));
            coinTransactionRepository.save(new CoinTransaction(recipient, coinAmount, "Transfer from @" + sender.getUsername(), "P2P Received" + noteClean));

            notificationRepository.save(new Notification(recipient, sender, "@" + sender.getUsername() + " sent you " + coinAmount + " Coins!" + noteClean, "TRANSFER"));
            redirectAttributes.addFlashAttribute("success", "Successfully sent " + coinAmount + " Coins to @" + recipient.getUsername() + "!");
        } else {
            // INR Cash
            if (amount < 1.0) {
                redirectAttributes.addFlashAttribute("error", "Minimum transfer amount is Rs. 1.00.");
                return "redirect:/wallet";
            }
            if (amount > 10000.0) {
                redirectAttributes.addFlashAttribute("error", "Maximum transfer limit is Rs. 10,000.00 per transaction.");
                return "redirect:/wallet";
            }
            if (sender.getWalletBalance() < amount) {
                redirectAttributes.addFlashAttribute("error", "Insufficient wallet balance. Available: Rs. " + String.format("%.2f", sender.getWalletBalance()));
                return "redirect:/wallet";
            }
            sender.deductWalletBalance(amount);
            recipient.addWalletBalance(amount);
            userRepository.save(sender);
            userRepository.save(recipient);

            String ref = "TX-" + System.currentTimeMillis();
            WalletTransaction debit = new WalletTransaction(sender, amount, "DEBIT", "Transferred to @" + recipient.getUsername() + noteClean);
            debit.setReferenceId(ref);
            debit.setStatus("COMPLETED");
            walletTransactionRepository.save(debit);

            WalletTransaction credit = new WalletTransaction(recipient, amount, "CREDIT", "Received from @" + sender.getUsername() + noteClean);
            credit.setReferenceId(ref);
            credit.setStatus("COMPLETED");
            walletTransactionRepository.save(credit);

            notificationRepository.save(new Notification(recipient, sender, "@" + sender.getUsername() + " sent you Rs. " + String.format("%.2f", amount) + "!" + noteClean, "TRANSFER"));
            redirectAttributes.addFlashAttribute("success", "Successfully transferred Rs. " + String.format("%.2f", amount) + " to @" + recipient.getUsername() + "!");
        }

        return "redirect:/wallet";
    }

    @PostMapping("/request")
    public String requestFunds(
            @RequestParam("targetUsername") String targetUsername,
            @RequestParam("amount") Double amount,
            @RequestParam(value = "currency", defaultValue = "COINS") String currency,
            @RequestParam(value = "note", required = false) String note,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        User requester = getUserFromSession(session);
        if (requester == null) return "redirect:/login";

        requester = userRepository.findById(requester.getId()).orElse(requester);

        if (targetUsername == null || targetUsername.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please provide a valid username to request from.");
            return "redirect:/wallet";
        }

        String targetName = targetUsername.trim();
        if (targetName.equalsIgnoreCase(requester.getUsername())) {
            redirectAttributes.addFlashAttribute("error", "You cannot request funds from yourself.");
            return "redirect:/wallet";
        }

        User targetUser = userRepository.findByUsername(targetName);
        if (targetUser == null) {
            redirectAttributes.addFlashAttribute("error", "User @" + targetName + " not found.");
            return "redirect:/wallet";
        }

        if (amount == null || amount <= 0 || amount.isNaN() || amount.isInfinite()) {
            redirectAttributes.addFlashAttribute("error", "Please enter a valid amount.");
            return "redirect:/wallet";
        }

        WalletRequest req = new WalletRequest(requester, targetUser, amount, currency, note != null ? note.trim() : "");
        walletRequestRepository.save(req);

        String currencySymbol = "COINS".equalsIgnoreCase(currency) ? "Coins" : "Rs.";
        String amtStr = "COINS".equalsIgnoreCase(currency) ? String.valueOf(amount.intValue()) : String.format("%.2f", amount);
        notificationRepository.save(new Notification(targetUser, requester, "@" + requester.getUsername() + " requested " + amtStr + " " + currencySymbol + " from you: \"" + (note != null && !note.trim().isEmpty() ? note.trim() : "Funds Request") + "\"", "WALLET_REQ"));

        redirectAttributes.addFlashAttribute("success", "Request sent to @" + targetUser.getUsername() + " for " + amtStr + " " + currencySymbol + "!");
        return "redirect:/wallet";
    }

    @PostMapping("/request/accept/{id}")
    public String acceptRequest(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/login";

        user = userRepository.findById(user.getId()).orElse(user);

        WalletRequest req = walletRequestRepository.findById(id).orElse(null);
        if (req == null || !req.getTargetUser().getId().equals(user.getId())) {
            redirectAttributes.addFlashAttribute("error", "Request not found or unauthorized.");
            return "redirect:/wallet";
        }

        if (!"PENDING".equals(req.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "Request is already " + req.getStatus().toLowerCase() + ".");
            return "redirect:/wallet";
        }

        User requester = userRepository.findById(req.getRequester().getId()).orElse(null);
        if (requester == null) {
            redirectAttributes.addFlashAttribute("error", "Requester account no longer exists.");
            return "redirect:/wallet";
        }

        boolean isCoins = "COINS".equalsIgnoreCase(req.getCurrency());
        if (isCoins) {
            int coinAmt = req.getAmount().intValue();
            if (user.getCoins() < coinAmt) {
                redirectAttributes.addFlashAttribute("error", "Insufficient coins! You need " + coinAmt + " coins to fulfill this request.");
                return "redirect:/wallet";
            }
            user.setCoins(user.getCoins() - coinAmt);
            requester.addCoins(coinAmt);
            userRepository.save(user);
            userRepository.save(requester);

            coinTransactionRepository.save(new CoinTransaction(user, -coinAmt, "Fulfilled request from @" + requester.getUsername(), "Request Accepted"));
            coinTransactionRepository.save(new CoinTransaction(requester, coinAmt, "Received requested coins from @" + user.getUsername(), "Request Fulfilled"));
        } else {
            Double cashAmt = req.getAmount();
            if (user.getWalletBalance() < cashAmt) {
                redirectAttributes.addFlashAttribute("error", "Insufficient wallet balance to fulfill this request!");
                return "redirect:/wallet";
            }
            user.deductWalletBalance(cashAmt);
            requester.addWalletBalance(cashAmt);
            userRepository.save(user);
            userRepository.save(requester);

            String ref = "REQ-" + System.currentTimeMillis();
            WalletTransaction debit = new WalletTransaction(user, cashAmt, "DEBIT", "Fulfilled request from @" + requester.getUsername());
            debit.setReferenceId(ref);
            debit.setStatus("COMPLETED");
            walletTransactionRepository.save(debit);

            WalletTransaction credit = new WalletTransaction(requester, cashAmt, "CREDIT", "Received requested funds from @" + user.getUsername());
            credit.setReferenceId(ref);
            credit.setStatus("COMPLETED");
            walletTransactionRepository.save(credit);
        }

        req.setStatus("ACCEPTED");
        walletRequestRepository.save(req);

        String amtStr = isCoins ? (req.getAmount().intValue() + " Coins") : ("Rs. " + String.format("%.2f", req.getAmount()));
        notificationRepository.save(new Notification(requester, user, "@" + user.getUsername() + " accepted your request and sent you " + amtStr + "!", "WALLET_REQ"));

        redirectAttributes.addFlashAttribute("success", "Request accepted! " + amtStr + " successfully transferred to @" + requester.getUsername());
        return "redirect:/wallet";
    }

    @PostMapping("/request/decline/{id}")
    public String declineRequest(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/login";

        WalletRequest req = walletRequestRepository.findById(id).orElse(null);
        if (req == null || !req.getTargetUser().getId().equals(user.getId())) {
            redirectAttributes.addFlashAttribute("error", "Request not found or unauthorized.");
            return "redirect:/wallet";
        }

        if (!"PENDING".equals(req.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "Request is already " + req.getStatus().toLowerCase() + ".");
            return "redirect:/wallet";
        }

        req.setStatus("DECLINED");
        walletRequestRepository.save(req);

        notificationRepository.save(new Notification(req.getRequester(), user, "@" + user.getUsername() + " declined your request for funds.", "WALLET_REQ"));

        redirectAttributes.addFlashAttribute("success", "Request declined.");
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

        if (amount == null || amount.isNaN() || amount.isInfinite() || amount < 100.0) {
            redirectAttributes.addFlashAttribute("error", "Minimum withdrawal amount is Rs. 100.00.");
            return "redirect:/wallet";
        }

        if (amount > 50000.0) {
            redirectAttributes.addFlashAttribute("error", "Maximum withdrawal limit is Rs. 50,000.00 per transaction.");
            return "redirect:/wallet";
        }

        if (accountDetails == null || accountDetails.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please provide valid transfer account details (UPI ID or Bank Account).");
            return "redirect:/wallet";
        }

        Double currentBalance = user.getWalletBalance() != null ? user.getWalletBalance() : 0.0;
        if (currentBalance < amount) {
            redirectAttributes.addFlashAttribute("error", "Insufficient balance. Your available balance is \u20B9" + String.format("%.2f", currentBalance));
            redirectAttributes.addFlashAttribute("error", "Insufficient balance. Your available balance is Rs. " + String.format("%.2f", currentBalance));
            return "redirect:/wallet";
        }

        // Platform charge: 5% processing fee deducted from payout
        double platformFeePct = 5.0;
        double fee = Math.round((amount * (platformFeePct / 100.0)) * 100.0) / 100.0;
        double netPayout = Math.round((amount - fee) * 100.0) / 100.0;

        user.deductWalletBalance(amount);
        userRepository.save(user);

        String accountClean = accountDetails.trim();
        String refId = "WTH-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 9000 + 1000);

        String details = "Withdrawal to " + accountClean + " (Gross: Rs. " + String.format("%.2f", amount) 
                + ", Platform Fee (5%): Rs. " + String.format("%.2f", fee) 
                + ", Net Payout: Rs. " + String.format("%.2f", netPayout) + ")";

        WalletTransaction tx = new WalletTransaction(
            user, 
            amount, 
            "WITHDRAWAL", 
            details
        );
        tx.setStatus("PENDING");
        tx.setReferenceId(refId);
        tx.setFee(fee);
        tx.setNetAmount(netPayout);
        walletTransactionRepository.save(tx);

        redirectAttributes.addFlashAttribute("success", "Withdrawal of \u20B9" + String.format("%.2f", amount) + " to " + accountClean + " processed successfully. Ref: " + refId);
        notificationRepository.save(new Notification(user, null, 
            "Withdrawal request of Rs. " + String.format("%.2f", amount) + " submitted. Platform charge (5%): Rs. " 
            + String.format("%.2f", fee) + ". Net payout of Rs. " + String.format("%.2f", netPayout) 
            + " to " + accountClean + " is PENDING review.", "WALLET"));

        redirectAttributes.addFlashAttribute("success", 
            "Withdrawal request submitted! Gross: Rs. " + String.format("%.2f", amount) 
            + " | Platform Charge (5%): Rs. " + String.format("%.2f", fee) 
            + " | Net Payout: Rs. " + String.format("%.2f", netPayout) 
            + " to " + accountClean + " (Ref: " + refId + ").");
        return "redirect:/wallet";
    }

    // ── ADMIN APPROVE & REJECT ENDPOINTS ──

    @PostMapping("/admin/approve/{txId}")
    public String adminApproveDeposit(@PathVariable Long txId, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isAdminSession(session)) {
            return "redirect:/login";
        }

        WalletTransaction tx = walletTransactionRepository.findById(txId).orElse(null);
        if (tx == null) {
            redirectAttributes.addFlashAttribute("error", "Transaction not found.");
            return "redirect:/admin";
        }

        if (!"PENDING".equalsIgnoreCase(tx.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "Transaction is already " + tx.getStatus().toLowerCase());
            return "redirect:/admin";
        }

        User user = tx.getUser();
        if ("WITHDRAWAL".equalsIgnoreCase(tx.getType())) {
            // Withdrawal approval: Balance was already deducted when requested; mark approved without re-crediting! (DEF-10)
            tx.setStatus("APPROVED");
            walletTransactionRepository.save(tx);
            if (user != null) {
                notificationRepository.save(new Notification(user, null, "Your withdrawal of Rs. " + String.format("%.2f", tx.getAmount()) + " (Ref: " + tx.getReferenceId() + ") has been approved and processed!", "WALLET"));
            }
            redirectAttributes.addFlashAttribute("success", "Approved withdrawal of Rs. " + String.format("%.2f", tx.getAmount()) + " for user @" + (user != null ? user.getUsername() : "") + ".");
        } else {
            // Deposit approval: Credit funds to user wallet
            if (user != null) {
                user.addWalletBalance(tx.getAmount());
                userRepository.save(user);
                notificationRepository.save(new Notification(user, null, "Your deposit of Rs. " + String.format("%.2f", tx.getAmount()) + " (Ref: " + tx.getReferenceId() + ") has been approved and credited!", "WALLET"));
            }
            tx.setStatus("COMPLETED");
            walletTransactionRepository.save(tx);
            redirectAttributes.addFlashAttribute("success", "Approved deposit of Rs. " + String.format("%.2f", tx.getAmount()) + " for user @" + (user != null ? user.getUsername() : "") + ".");
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/reject/{txId}")
    public String adminRejectDeposit(@PathVariable Long txId, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isAdminSession(session)) {
            return "redirect:/login";
        }

        WalletTransaction tx = walletTransactionRepository.findById(txId).orElse(null);
        if (tx == null) {
            redirectAttributes.addFlashAttribute("error", "Transaction not found.");
            return "redirect:/admin";
        }

        if (!"PENDING".equalsIgnoreCase(tx.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "Transaction is already " + tx.getStatus().toLowerCase());
            return "redirect:/admin";
        }

        User user = tx.getUser();
        if ("WITHDRAWAL".equalsIgnoreCase(tx.getType())) {
            // Withdrawal rejection: Refund the previously deducted balance back to user (DEF-10)
            if (user != null) {
                user.addWalletBalance(tx.getAmount());
                userRepository.save(user);
                notificationRepository.save(new Notification(user, null, "Your withdrawal request of Rs. " + String.format("%.2f", tx.getAmount()) + " (Ref: " + tx.getReferenceId() + ") was rejected. Funds have been refunded to your wallet balance.", "WALLET"));
            }
        } else {
            if (user != null) {
                notificationRepository.save(new Notification(tx.getUser(), null, "Your deposit request of Rs. " + String.format("%.2f", tx.getAmount()) + " (Ref: " + tx.getReferenceId() + ") was rejected by Admin.", "WALLET"));
            }
        }

        tx.setStatus("REJECTED");
        walletTransactionRepository.save(tx);
        redirectAttributes.addFlashAttribute("success", "Rejected transaction for user @" + (user != null ? user.getUsername() : "") + ".");
        return "redirect:/admin";
    }
}
