package com.example.demo.controller;

import com.example.demo.model.ContactMessage;
import com.example.demo.repository.ContactMessageRepository;
import com.example.demo.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.regex.Pattern;

@Controller
public class ContactController {

    private static final Logger logger = LoggerFactory.getLogger(ContactController.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    @Autowired
    private ContactMessageRepository contactMessageRepository;

    @Autowired
    private EmailService emailService;

    @Value("${youthians.support.email:kishorhavanur07@gmail.com}")
    private String configuredSupportEmail;

    @PostMapping("/contact")
    public String submitContactForm(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "subject", required = false) String subject,
            @RequestParam(value = "message", required = false) String message,
            HttpServletRequest request
    ) {
        String referer = request.getHeader("Referer");
        boolean isSupportPage = (referer != null && referer.contains("/support"));
        String successRedirect = isSupportPage ? "redirect:/support?success=message_sent" : "redirect:/home?success=message_sent#contact";

        // Validate presence of required fields
        if (name == null || name.isBlank() ||
            email == null || email.isBlank() ||
            subject == null || subject.isBlank() ||
            message == null || message.isBlank()) {
            logger.warn("Support enquiry rejected: Missing required fields");
            return isSupportPage ? "redirect:/support?error=missing_fields" : "redirect:/home?error=missing_fields#contact";
        }

        // Validate sender email format
        String trimmedEmail = email.trim();
        if (!EMAIL_PATTERN.matcher(trimmedEmail).matches()) {
            logger.warn("Support enquiry rejected: Invalid sender email format: {}", trimmedEmail);
            return isSupportPage ? "redirect:/support?error=invalid_email" : "redirect:/home?error=invalid_email#contact";
        }

        try {
            ContactMessage contactMessage = new ContactMessage(name.trim(), trimmedEmail, subject.trim(), message.trim());
            contactMessageRepository.save(contactMessage);

            logger.info("Saved support enquiry to database and dispatching emails for [{}] from: {} ({}) | Subject: {}",
                    configuredSupportEmail, name.trim(), trimmedEmail, subject.trim());

            // 1. Send email notification to Admin's email (with Reply-To set to the user's email)
            emailService.sendSupportEnquiryNotification(name.trim(), trimmedEmail, subject.trim(), message.trim());

            // 2. Send automated acknowledgment email to the user
            emailService.sendEnquiryAcknowledgement(name.trim(), trimmedEmail, subject.trim(), message.trim());

            return successRedirect;
        } catch (Exception e) {
            logger.error("Failed to deliver and save support enquiry", e);
            return isSupportPage ? "redirect:/support?error=system_error" : "redirect:/home?error=system_error#contact";
        }
    }

    @PostMapping("/admin/support/reply")
    public String replyToEnquiry(
            @RequestParam("toEmail") String toEmail,
            @RequestParam("recipientName") String recipientName,
            @RequestParam("subject") String subject,
            @RequestParam("replyMessage") String replyMessage,
            HttpSession session
    ) {
        Object u = session.getAttribute("user");
        boolean isAdmin = "admin".equals(u)
                || (u instanceof com.example.demo.model.User && "admin".equalsIgnoreCase(((com.example.demo.model.User) u).getUsername()))
                || "admin".equals(session.getAttribute("admin"));
        if (!isAdmin) {
            return "redirect:/login";
        }

        boolean sent = emailService.sendDirectReply(toEmail, recipientName, subject, replyMessage);
        return "redirect:/admin?reply=" + (sent ? "success" : "failed") + "#support-inquiries-section";
    }
}


