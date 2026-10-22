package com.example.demo.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${youthians.admin.email:kishorhavanur07@gmail.com}")
    private String adminEmail;

    @Value("${spring.mail.username:kishorhavanur07@gmail.com}")
    private String mailFrom;

    /**
     * Sends the user's support enquiry to the admin inbox.
     * Sets Reply-To to the user's email, so when the admin hits Reply in Gmail,
     * the response goes directly to the user.
     */
    public void sendSupportEnquiryNotification(String senderName, String senderEmail, String subject, String messageText) {
        CompletableFuture.runAsync(() -> {
            if (mailSender == null) {
                logger.warn("JavaMailSender is not configured. Skipping email dispatch.");
                return;
            }

            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setFrom(mailFrom, "Youthians Support System");
                helper.setTo(adminEmail);
                helper.setReplyTo(senderEmail, senderName);
                helper.setSubject("[Youthians Support Enquiry] " + subject + " - from " + senderName);

                String htmlContent = "<div style=\"font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; border: 1px solid #e2e8f0; border-radius: 12px; overflow: hidden;\">"
                        + "<div style=\"background: linear-gradient(135deg, #3b82f6, #2563eb); color: #ffffff; padding: 24px; text-align: center;\">"
                        + "<h2 style=\"margin: 0; font-size: 22px;\">New Support Enquiry</h2>"
                        + "<p style=\"margin: 5px 0 0 0; opacity: 0.9; font-size: 13px;\">Youthians Platform</p>"
                        + "</div>"
                        + "<div style=\"padding: 24px;\">"
                        + "<table style=\"width: 100%; border-collapse: collapse; margin-bottom: 20px;\">"
                        + "<tr><td style=\"padding: 8px 0; font-weight: bold; width: 120px; color: #64748b;\">Submitter:</td><td style=\"padding: 8px 0;\">" + senderName + "</td></tr>"
                        + "<tr><td style=\"padding: 8px 0; font-weight: bold; color: #64748b;\">Email:</td><td style=\"padding: 8px 0;\"><a href=\"mailto:" + senderEmail + "\" style=\"color: #2563eb; text-decoration: none;\">" + senderEmail + "</a></td></tr>"
                        + "<tr><td style=\"padding: 8px 0; font-weight: bold; color: #64748b;\">Subject:</td><td style=\"padding: 8px 0; font-weight: bold;\">" + subject + "</td></tr>"
                        + "</table>"
                        + "<div style=\"background: #f8fafc; border-left: 4px solid #3b82f6; padding: 16px; border-radius: 4px; margin-bottom: 24px;\">"
                        + "<div style=\"font-size: 11px; font-weight: bold; color: #64748b; text-transform: uppercase; margin-bottom: 8px;\">Enquiry Message</div>"
                        + "<div style=\"white-space: pre-wrap; color: #0f172a;\">" + messageText + "</div>"
                        + "</div>"
                        + "<div style=\"background: #dbeafe; color: #1e40af; padding: 12px 16px; border-radius: 8px; font-size: 13px;\">"
                        + "💡 <strong>Direct Reply Enabled:</strong> Simply click <em>Reply</em> in your email client to respond directly to <strong>" + senderName + " (" + senderEmail + ")</strong>."
                        + "</div>"
                        + "</div>"
                        + "<div style=\"background: #f1f5f9; padding: 14px; text-align: center; font-size: 12px; color: #64748b;\">"
                        + "Youthians Support Notification • Generated automatically by Youthians Platform"
                        + "</div>"
                        + "</div>";

                helper.setText(htmlContent, true);
                mailSender.send(message);

                logger.info("Successfully delivered support enquiry notification to admin [{}] with Reply-To [{}]", adminEmail, senderEmail);
            } catch (Exception e) {
                logger.error("Failed to send support enquiry email notification: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * Sends an automated acknowledgment email to the user confirming their enquiry was received.
     */
    public void sendEnquiryAcknowledgement(String senderName, String senderEmail, String subject, String messageText) {
        CompletableFuture.runAsync(() -> {
            if (mailSender == null) {
                return;
            }

            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setFrom(mailFrom, "Youthians Support");
                helper.setTo(senderEmail);
                helper.setReplyTo(adminEmail, "Youthians Support");
                helper.setSubject("[Youthians Support] We received your message: " + subject);

                String htmlContent = "<div style=\"font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; border: 1px solid #e2e8f0; border-radius: 12px; overflow: hidden;\">"
                        + "<div style=\"background: linear-gradient(135deg, #3b82f6, #0ea5e9); color: #ffffff; padding: 24px; text-align: center;\">"
                        + "<h2 style=\"margin: 0; font-size: 22px;\">Thank You For Contacting Us</h2>"
                        + "<p style=\"margin: 5px 0 0 0; opacity: 0.9; font-size: 13px;\">Youthians Support Team</p>"
                        + "</div>"
                        + "<div style=\"padding: 24px;\">"
                        + "<p>Hi <strong>" + senderName + "</strong>,</p>"
                        + "<p>Thank you for reaching out to us. We have received your inquiry regarding <strong>\"" + subject + "\"</strong>.</p>"
                        + "<p>Our support team is reviewing your message and will respond directly to this email shortly.</p>"
                        + "<div style=\"background: #f8fafc; border-left: 4px solid #0ea5e9; padding: 14px; border-radius: 4px; margin: 20px 0;\">"
                        + "<div style=\"font-size: 11px; font-weight: bold; color: #64748b; text-transform: uppercase; margin-bottom: 6px;\">Summary of your message:</div>"
                        + "<div style=\"white-space: pre-wrap; color: #0f172a;\">" + messageText + "</div>"
                        + "</div>"
                        + "<p style=\"margin-bottom: 0;\">Best regards,<br><strong>Youthians Support Team</strong></p>"
                        + "</div>"
                        + "<div style=\"background: #f1f5f9; padding: 14px; text-align: center; font-size: 12px; color: #64748b;\">"
                        + "Youthians • " + adminEmail
                        + "</div>"
                        + "</div>";

                helper.setText(htmlContent, true);
                mailSender.send(message);

                logger.info("Successfully sent enquiry acknowledgment email to user [{}]", senderEmail);
            } catch (Exception e) {
                logger.error("Failed to send user acknowledgment email: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * Sends an email reply from the Admin directly to the user.
     */
    public boolean sendDirectReply(String toEmail, String recipientName, String subject, String replyBody) {
        if (mailSender == null) {
            logger.warn("JavaMailSender is not configured. Cannot send direct reply.");
            return false;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailFrom, "Youthians Support");
            helper.setTo(toEmail);
            helper.setReplyTo(adminEmail, "Youthians Support");
            helper.setSubject("Re: " + subject);

            String htmlContent = "<div style=\"font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; border: 1px solid #e2e8f0; border-radius: 12px; overflow: hidden;\">"
                    + "<div style=\"background: linear-gradient(135deg, #3b82f6, #2563eb); color: #ffffff; padding: 20px 24px;\">"
                    + "<h2 style=\"margin: 0; font-size: 20px;\">Response to Your Support Request</h2>"
                    + "</div>"
                    + "<div style=\"padding: 24px;\">"
                    + "<p>Hi <strong>" + recipientName + "</strong>,</p>"
                    + "<div style=\"white-space: pre-wrap; color: #0f172a; margin: 20px 0; font-size: 15px;\">" + replyBody + "</div>"
                    + "<hr style=\"border: none; border-top: 1px solid #e2e8f0; margin: 20px 0;\">"
                    + "<p style=\"font-size: 13px; color: #64748b;\">If you have any further questions, simply reply directly to this email.</p>"
                    + "<p style=\"margin-bottom: 0;\">Best regards,<br><strong>Youthians Support Team</strong></p>"
                    + "</div>"
                    + "</div>";

            helper.setText(htmlContent, true);
            mailSender.send(message);

            logger.info("Admin successfully sent direct email reply to [{}]", toEmail);
            return true;
        } catch (Exception e) {
            logger.error("Failed to send direct email reply to [{}]: {}", toEmail, e.getMessage(), e);
            return false;
        }
    }
}
