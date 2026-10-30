package com.example.demo.service;

import com.example.demo.model.Event;
import com.example.demo.model.EventRegistration;
import com.example.demo.model.TournamentParticipant;
import com.example.demo.model.User;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${youthians.admin.email:kishorhavanur07@gmail.com}")
    private String adminEmail;

    @Value("${spring.mail.username:kishorhavanur07@gmail.com}")
    private String mailFrom;

    @Value("${app.base-url:http://localhost:8009}")
    private String baseUrl;

    // Cache to prevent duplicate profile search emails within 6 hours
    private final ConcurrentHashMap<String, Long> profileSearchCooldown = new ConcurrentHashMap<>();

    // Cache to prevent duplicate post comment emails within 2 minutes
    private final ConcurrentHashMap<String, Long> commentCooldown = new ConcurrentHashMap<>();

    // -------------------------------------------------------------
    // Generic Master HTML Email Dispatcher
    // -------------------------------------------------------------
    public void sendHtmlEmail(String toEmail, String recipientName, String subject, String headerTitle, String bodyHtml, String actionText, String actionUrl) {
        CompletableFuture.runAsync(() -> {
            if (mailSender == null) {
                logger.warn("JavaMailSender is not configured. Skipping email dispatch to [{}]", toEmail);
                return;
            }
            if (toEmail == null || toEmail.trim().isEmpty() || !toEmail.contains("@")) {
                logger.debug("Invalid email address [{}]. Skipping dispatch.", toEmail);
                return;
            }

            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setFrom(mailFrom, "Youthians Platform");
                helper.setTo(toEmail.trim());
                helper.setReplyTo(adminEmail, "Youthians Support");
                helper.setSubject(subject);

                String actionBtn = "";
                if (actionText != null && !actionText.isBlank() && actionUrl != null && !actionUrl.isBlank()) {
                    String fullUrl = actionUrl.startsWith("http") ? actionUrl : (baseUrl + actionUrl);
                    actionBtn = "<div style=\"text-align: center; margin: 28px 0 16px 0;\">"
                            + "<a href=\"" + fullUrl + "\" style=\"display: inline-block; background: linear-gradient(135deg, #3b82f6, #1d4ed8); color: #ffffff; text-decoration: none; padding: 12px 28px; border-radius: 30px; font-weight: 700; font-size: 14px; box-shadow: 0 4px 12px rgba(37,99,235,0.3);\">"
                            + actionText + " &rarr;</a></div>";
                }

                String greetingName = (recipientName != null && !recipientName.isBlank()) ? recipientName : "Youthian";

                String html = "<div style=\"font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f1f5f9; padding: 30px 12px;\">"
                        + "<div style=\"max-width: 580px; margin: 0 auto; background: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 16px rgba(15,23,42,0.06); border: 1px solid #e2e8f0;\">"
                        + "  <div style=\"background: linear-gradient(135deg, #3b82f6 0%, #2563eb 50%, #1d4ed8 100%); color: #ffffff; padding: 26px 24px; text-align: center;\">"
                        + "    <h1 style=\"margin: 0; font-size: 24px; font-weight: 800; letter-spacing: -0.5px;\">Youthians</h1>"
                        + "    <p style=\"margin: 6px 0 0 0; opacity: 0.92; font-size: 14px; font-weight: 600;\">" + headerTitle + "</p>"
                        + "  </div>"
                        + "  <div style=\"padding: 28px 24px; color: #1e293b; font-size: 15px; line-height: 1.6;\">"
                        + "    <p style=\"margin-top: 0; font-weight: 700; font-size: 16px; color: #0f172a;\">Hi " + greetingName + ",</p>"
                        +      bodyHtml
                        +      actionBtn
                        + "  </div>"
                        + "  <div style=\"background: #f8fafc; border-top: 1px solid #e2e8f0; padding: 16px; text-align: center; font-size: 12px; color: #64748b;\">"
                        + "    You received this notification from your <strong>Youthians</strong> account.<br>"
                        + "    Questions? Contact <a href=\"mailto:" + adminEmail + "\" style=\"color: #2563eb; text-decoration: none;\">" + adminEmail + "</a>"
                        + "  </div>"
                        + "</div>"
                        + "</div>";

                helper.setText(html, true);
                mailSender.send(message);

                logger.info("Email [{}] sent successfully to [{}]", subject, toEmail);
            } catch (Exception e) {
                logger.error("Failed to send email to [{}]: {}", toEmail, e.getMessage());
            }
        });
    }

    // -------------------------------------------------------------
    // 1. Profile Searched / Viewed Notification
    // -------------------------------------------------------------
    public void sendProfileSearchNotification(User targetUser, User searcher) {
        if (targetUser == null || searcher == null || targetUser.getEmail() == null) return;
        if (targetUser.getId() != null && targetUser.getId().equals(searcher.getId())) return;

        // Rate limit: 1 email per visitor per target every 6 hours
        String cooldownKey = searcher.getId() + "_" + targetUser.getId();
        long now = System.currentTimeMillis();
        Long lastSent = profileSearchCooldown.get(cooldownKey);
        if (lastSent != null && (now - lastSent) < (6 * 60 * 60 * 1000L)) {
            return;
        }
        profileSearchCooldown.put(cooldownKey, now);

        String searcherName = searcher.getUsername();
        String searcherCollege = searcher.getCollegeName() != null ? (" from " + searcher.getCollegeName()) : "";
        String body = "<p>Someone is interested in your profile on Youthians!</p>"
                + "<div style=\"background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 18px; margin: 18px 0;\">"
                + "  <p style=\"margin: 0; font-size: 15px;\"><strong>@" + searcherName + "</strong>" + searcherCollege + " searched and viewed your profile.</p>"
                + "</div>"
                + "<p>Connect back, see their posts, or follow them to stay in touch!</p>";

        sendHtmlEmail(
                targetUser.getEmail(),
                targetUser.getUsername(),
                "👀 @" + searcherName + " viewed your profile on Youthians!",
                "Profile Activity Alert",
                body,
                "View @" + searcherName + "'s Profile",
                "/profile/" + searcherName
        );
    }

    // -------------------------------------------------------------
    // 2. New Event Added / Broadcast Notification
    // -------------------------------------------------------------
    public void sendEventCreatedBroadcast(Event event, List<User> users) {
        if (event == null || users == null || users.isEmpty()) return;

        String eventDate = (event.getDateTime() != null) ? event.getDateTime().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")) : "TBD";
        String venue = (event.getVenue() != null && !event.getVenue().isBlank()) ? event.getVenue() : "Youthians Arena";
        String category = (event.getCategory() != null) ? event.getCategory() : "Featured Event";

        String body = "<p>A brand new event has just been published on Youthians! Secure your spot early before passes run out.</p>"
                + "<div style=\"background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 20px; margin: 18px 0;\">"
                + "  <h3 style=\"margin: 0 0 10px 0; font-size: 18px; color: #0f172a;\">" + event.getTitle() + "</h3>"
                + "  <p style=\"margin: 4px 0; color: #475569; font-size: 14px;\"><strong>Category:</strong> " + category + "</p>"
                + "  <p style=\"margin: 4px 0; color: #475569; font-size: 14px;\"><strong>Date & Time:</strong> " + eventDate + "</p>"
                + "  <p style=\"margin: 4px 0; color: #475569; font-size: 14px;\"><strong>Venue:</strong> " + venue + "</p>"
                + "</div>";

        for (User u : users) {
            if (u.getEmail() != null && !u.getEmail().isBlank()) {
                sendHtmlEmail(
                        u.getEmail(),
                        u.getUsername(),
                        "🎉 New Event Added: " + event.getTitle() + " is now live!",
                        "New Event Announcement",
                        body,
                        "Book Your Tickets Now",
                        "/events/" + event.getId()
                );
            }
        }
    }

    // -------------------------------------------------------------
    // 3. Post or Reel Shared Notification
    // -------------------------------------------------------------
    public void sendPostOrReelSharedNotification(User recipient, User sender, String title, String link, boolean isReel) {
        if (recipient == null || recipient.getEmail() == null || sender == null) return;

        String itemType = isReel ? "reel" : "post";
        String displayTitle = (title != null && !title.isBlank()) ? ("\"" + (title.length() > 60 ? title.substring(0, 57) + "..." : title) + "\"") : ("a " + itemType);

        String body = "<p><strong>@" + sender.getUsername() + "</strong> just shared a " + itemType + " with you on Youthians!</p>"
                + "<div style=\"background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 18px; margin: 18px 0;\">"
                + "  <p style=\"margin: 0; font-size: 15px; font-weight: 600; color: #0f172a;\">" + displayTitle + "</p>"
                + "</div>"
                + "<p>Check it out now to like, comment, and see what's trending.</p>";

        sendHtmlEmail(
                recipient.getEmail(),
                recipient.getUsername(),
                (isReel ? "🎬 @" : "📸 @") + sender.getUsername() + " shared a " + itemType + " with you!",
                "Shared Content Alert",
                body,
                "Watch " + (isReel ? "Reel" : "Post"),
                link != null ? link : "/dashboard"
        );
    }

    // -------------------------------------------------------------
    // 4. Event Ticket Booking Confirmation
    // -------------------------------------------------------------
    public void sendTicketConfirmation(EventRegistration registration) {
        if (registration == null) return;
        String email = registration.getEmail();
        if ((email == null || email.isBlank()) && registration.getUser() != null) {
            email = registration.getUser().getEmail();
        }
        if (email == null || email.isBlank()) return;

        Event event = registration.getEvent();
        String eventTitle = event != null ? event.getTitle() : "Youthians Event";
        String eventDate = (event != null && event.getDateTime() != null) ? event.getDateTime().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy, hh:mm a")) : "TBD";
        String venue = (event != null && event.getVenue() != null) ? event.getVenue() : "Youthians Venue";
        String ticketId = registration.getTicketId() != null ? registration.getTicketId() : "N/A";
        String tier = registration.getSelectedTier() != null ? registration.getSelectedTier() : "Standard";
        int qty = registration.getQuantity() != null ? registration.getQuantity() : 1;
        double price = registration.getTotalPrice() != null ? registration.getTotalPrice() : 0.0;

        String body = "<p>Your booking has been confirmed! Here are your ticket and event admission details:</p>"
                + "<div style=\"background: #f0fdf4; border: 2px dashed #86efac; border-radius: 14px; padding: 22px; margin: 20px 0;\">"
                + "  <h3 style=\"margin: 0 0 12px 0; color: #166534; font-size: 20px;\">🎟️ TICKET ID: " + ticketId + "</h3>"
                + "  <table style=\"width: 100%; border-collapse: collapse; font-size: 14px; color: #1e293b;\">"
                + "    <tr><td style=\"padding: 6px 0; font-weight: bold; width: 120px;\">Event:</td><td>" + eventTitle + "</td></tr>"
                + "    <tr><td style=\"padding: 6px 0; font-weight: bold;\">Date & Time:</td><td>" + eventDate + "</td></tr>"
                + "    <tr><td style=\"padding: 6px 0; font-weight: bold;\">Venue:</td><td>" + venue + "</td></tr>"
                + "    <tr><td style=\"padding: 6px 0; font-weight: bold;\">Pass Tier:</td><td>" + tier + " (Qty: " + qty + ")</td></tr>"
                + "    <tr><td style=\"padding: 6px 0; font-weight: bold;\">Total Paid:</td><td>₹" + String.format("%.2f", price) + "</td></tr>"
                + "  </table>"
                + "</div>"
                + "<p style=\"font-size: 13px; color: #64748b;\">Please present this Ticket ID or your digital pass at the entrance.</p>";

        sendHtmlEmail(
                email,
                registration.getFullName() != null ? registration.getFullName() : (registration.getUser() != null ? registration.getUser().getUsername() : "Guest"),
                "🎟️ Booking Confirmed: " + eventTitle + " (Ticket: " + ticketId + ")",
                "Ticket Admission Pass",
                body,
                "View Your Digital Pass",
                "/events/ticket/" + ticketId
        );
    }

    // -------------------------------------------------------------
    // 5. Event Check-in / Attendance Marked
    // -------------------------------------------------------------
    public void sendEventCheckInNotification(EventRegistration registration) {
        if (registration == null) return;
        String email = registration.getEmail();
        if ((email == null || email.isBlank()) && registration.getUser() != null) {
            email = registration.getUser().getEmail();
        }
        if (email == null || email.isBlank()) return;

        Event event = registration.getEvent();
        String eventTitle = event != null ? event.getTitle() : "Event";

        String body = "<p>Welcome to <strong>" + eventTitle + "</strong>! You have been successfully checked in.</p>"
                + "<div style=\"background: #ecfdf5; border: 1px solid #a7f3d0; border-radius: 12px; padding: 18px; margin: 18px 0; color: #065f46;\">"
                + "  <h4 style=\"margin: 0 0 6px 0;\">✓ Checked-In Successfully</h4>"
                + "  <p style=\"margin: 0; font-size: 14px;\">Attendance verified. Enjoy the event and participate in live polls & challenges!</p>"
                + "</div>";

        sendHtmlEmail(
                email,
                registration.getFullName() != null ? registration.getFullName() : "Guest",
                "✅ Check-in Confirmed: Welcome to " + eventTitle + "!",
                "Event Check-In",
                body,
                "View Event Arena",
                "/events/" + (event != null ? event.getId() : "")
        );
    }

    // -------------------------------------------------------------
    // 6. Tournament Registration Confirmation
    // -------------------------------------------------------------
    public void sendTournamentRegistrationConfirmation(TournamentParticipant participant) {
        if (participant == null || participant.getUser() == null || participant.getUser().getEmail() == null) return;

        String tournamentName = participant.getTournament() != null ? participant.getTournament().getName() : "Youthians Tournament";
        String participantNo = participant.getParticipantNumber() != null ? participant.getParticipantNumber() : ("#" + participant.getId());
        String mode = participant.getTournament() != null ? participant.getTournament().getMode() : "ONLINE";

        String body = "<p>You are officially registered for <strong>" + tournamentName + "</strong>!</p>"
                + "<div style=\"background: #f8fafc; border: 2px dashed #93c5fd; border-radius: 14px; padding: 20px; margin: 20px 0;\">"
                + "  <h3 style=\"margin: 0 0 10px 0; color: #1e40af;\">🏆 BATTLE PASS: " + participantNo + "</h3>"
                + "  <p style=\"margin: 4px 0;\"><strong>Tournament Mode:</strong> " + mode + "</p>"
                + "  <p style=\"margin: 4px 0;\"><strong>Participant Seed:</strong> #" + participant.getSeed() + "</p>"
                + "</div>"
                + "<p>Prepare your strategies and check the bracket schedule as rounds begin.</p>";

        sendHtmlEmail(
                participant.getUser().getEmail(),
                participant.getUser().getUsername(),
                "🏆 Registered: " + tournamentName + " (" + participantNo + ")",
                "Tournament Pass Confirmed",
                body,
                "View Bracket & Matches",
                "/tournaments/" + (participant.getTournament() != null ? participant.getTournament().getId() : "")
        );
    }

    // -------------------------------------------------------------
    // 7. Tournament Next Match Ready
    // -------------------------------------------------------------
    public void sendTournamentMatchReadyNotification(User player, String tournamentName, String opponentName, String roundName) {
        if (player == null || player.getEmail() == null) return;

        String body = "<p>Your next match in <strong>" + tournamentName + "</strong> has been generated!</p>"
                + "<div style=\"background: #fff7ed; border: 1px solid #fed7aa; border-radius: 12px; padding: 18px; margin: 18px 0; color: #9a3412;\">"
                + "  <h4 style=\"margin: 0 0 8px 0; font-size: 16px;\">⚔️ Match: " + (roundName != null ? roundName : "Next Round") + "</h4>"
                + "  <p style=\"margin: 0; font-size: 15px;\">Opponent: <strong>" + (opponentName != null ? opponentName : "TBD") + "</strong></p>"
                + "</div>"
                + "<p>Get ready and join the battle arena on time to avoid forfeiture.</p>";

        sendHtmlEmail(
                player.getEmail(),
                player.getUsername(),
                "⚔️ Match Ready in " + tournamentName + " vs " + opponentName,
                "Tournament Match Scheduled",
                body,
                "Go to Match Room",
                "/tournaments"
        );
    }

    // -------------------------------------------------------------
    // 8. Tournament Winner / Podium Notification
    // -------------------------------------------------------------
    public void sendTournamentWinnerNotification(User user, String tournamentName, String position, String prize) {
        if (user == null || user.getEmail() == null) return;

        String body = "<p>Congratulations! You achieved top standing in <strong>" + tournamentName + "</strong>!</p>"
                + "<div style=\"background: #fefce8; border: 1px solid #fef08a; border-radius: 14px; padding: 22px; margin: 20px 0; text-align: center;\">"
                + "  <div style=\"font-size: 36px; margin-bottom: 8px;\">🥇</div>"
                + "  <h3 style=\"margin: 0 0 6px 0; color: #854d0e; font-size: 20px;\">" + position + "</h3>"
                + "  <p style=\"margin: 0; font-weight: 700; color: #a16207;\">Prize: " + (prize != null ? prize : "Zen Coins & Trophy") + "</p>"
                + "</div>";

        sendHtmlEmail(
                user.getEmail(),
                user.getUsername(),
                "🥇 Victory! You placed " + position + " in " + tournamentName,
                "Tournament Triumph",
                body,
                "View Your Achievements",
                "/achievements"
        );
    }

    // -------------------------------------------------------------
    // 9. 1v1 Battle Invitation Notification
    // -------------------------------------------------------------
    public void sendBattleInviteNotification(User targetUser, User challenger, String category, Long battleId) {
        if (targetUser == null || targetUser.getEmail() == null || challenger == null) return;

        String body = "<p><strong>@" + challenger.getUsername() + "</strong> has challenged you to an arena battle!</p>"
                + "<div style=\"background: #fef2f2; border: 1px solid #fecaca; border-radius: 12px; padding: 18px; margin: 18px 0;\">"
                + "  <p style=\"margin: 0; color: #991b1b; font-size: 15px;\"><strong>Battle Category:</strong> " + (category != null ? category : "Head-to-Head") + "</p>"
                + "</div>"
                + "<p>Accept the challenge to fight for XP, Zen Coins, and leaderboard rank.</p>";

        sendHtmlEmail(
                targetUser.getEmail(),
                targetUser.getUsername(),
                "⚔️ @" + challenger.getUsername() + " challenged you to a Battle!",
                "Battle Arena Challenge",
                body,
                "Enter Battle Arena",
                battleId != null ? ("/battles/" + battleId) : "/battles"
        );
    }

    // -------------------------------------------------------------
    // 10. Battle Prediction Win Notification
    // -------------------------------------------------------------
    public void sendBattlePredictionWinNotification(User user, String battleTitle, double coinsWon) {
        if (user == null || user.getEmail() == null) return;

        String body = "<p>Your battle prediction was accurate! You called the outcome correctly.</p>"
                + "<div style=\"background: #f0fdf4; border: 1px solid #bbf7d0; border-radius: 12px; padding: 18px; margin: 18px 0;\">"
                + "  <p style=\"margin: 0 0 6px 0; color: #166534;\"><strong>Battle:</strong> " + (battleTitle != null ? battleTitle : "Live Battle") + "</p>"
                + "  <p style=\"margin: 0; font-size: 18px; font-weight: 800; color: #15803d;\">💰 Payout: +" + String.format("%.0f", coinsWon) + " Zen Coins</p>"
                + "</div>"
                + "<p>Coins have been credited to your platform wallet.</p>";

        sendHtmlEmail(
                user.getEmail(),
                user.getUsername(),
                "🎉 Prediction Win! You earned " + String.format("%.0f", coinsWon) + " Zen Coins",
                "Prediction Payout",
                body,
                "View Your Wallet",
                "/wallet"
        );
    }

    // -------------------------------------------------------------
    // 11. Post Comment / Reply Notification
    // -------------------------------------------------------------
    public void sendPostCommentNotification(User postOwner, User commenter, String postSnippet, String commentText) {
        if (postOwner == null || postOwner.getEmail() == null || commenter == null) return;
        if (postOwner.getId() != null && postOwner.getId().equals(commenter.getId())) return;

        // Rate limit: 1 email per user pair per 2 minutes
        String key = commenter.getId() + "_" + postOwner.getId();
        long now = System.currentTimeMillis();
        Long lastSent = commentCooldown.get(key);
        if (lastSent != null && (now - lastSent) < (2 * 60 * 1000L)) {
            return;
        }
        commentCooldown.put(key, now);

        String snippet = (commentText != null && commentText.length() > 60) ? (commentText.substring(0, 57) + "...") : commentText;

        String body = "<p><strong>@" + commenter.getUsername() + "</strong> commented on your post:</p>"
                + "<div style=\"background: #f8fafc; border-left: 4px solid #3b82f6; border-radius: 4px; padding: 14px; margin: 18px 0; font-style: italic; color: #334155;\">"
                + "  \"" + snippet + "\""
                + "</div>";

        sendHtmlEmail(
                postOwner.getEmail(),
                postOwner.getUsername(),
                "💬 @" + commenter.getUsername() + " commented on your post",
                "New Comment Alert",
                body,
                "View Post & Reply",
                "/dashboard"
        );
    }

    // -------------------------------------------------------------
    // 12. Post Collaboration Invite Notification
    // -------------------------------------------------------------
    public void sendCollaborationInviteNotification(User invitee, User author, String postContent) {
        if (invitee == null || invitee.getEmail() == null || author == null) return;

        String body = "<p><strong>@" + author.getUsername() + "</strong> invited you to collaborate as co-author on a post!</p>"
                + "<div style=\"background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 16px; margin: 18px 0;\">"
                + "  <p style=\"margin: 0; color: #475569;\">\"" + (postContent != null && postContent.length() > 80 ? postContent.substring(0, 77) + "..." : postContent) + "\"</p>"
                + "</div>"
                + "<p>Accepting will feature this post on both profiles.</p>";

        sendHtmlEmail(
                invitee.getEmail(),
                invitee.getUsername(),
                "🤝 @" + author.getUsername() + " invited you to collaborate!",
                "Collaboration Invitation",
                body,
                "Review Collaboration",
                "/profile"
        );
    }

    // -------------------------------------------------------------
    // 13. New Follower / Follow Request Notification
    // -------------------------------------------------------------
    public void sendNewFollowerNotification(User targetUser, User follower, boolean isRequest) {
        if (targetUser == null || targetUser.getEmail() == null || follower == null) return;

        String title = isRequest ? ("@" + follower.getUsername() + " requested to follow you") : ("@" + follower.getUsername() + " started following you");
        String body = "<p><strong>@" + follower.getUsername() + "</strong> " + (isRequest ? "has requested to follow your private profile." : "is now following your updates on Youthians.") + "</p>"
                + "<div style=\"background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 16px; margin: 18px 0;\">"
                + "  <p style=\"margin: 0;\">College: <strong>" + (follower.getCollegeName() != null ? follower.getCollegeName() : "Youthian Campus") + "</strong></p>"
                + "</div>";

        sendHtmlEmail(
                targetUser.getEmail(),
                targetUser.getUsername(),
                "👤 " + title,
                "New Connection",
                body,
                isRequest ? "Review Follow Requests" : "View Profile",
                "/profile"
        );
    }

    // -------------------------------------------------------------
    // 14. Follow Request Accepted Notification
    // -------------------------------------------------------------
    public void sendFollowAcceptedNotification(User requester, User approver) {
        if (requester == null || requester.getEmail() == null || approver == null) return;

        String body = "<p>Great news! <strong>@" + approver.getUsername() + "</strong> accepted your follow request.</p>"
                + "<p>You can now see their private posts, reels, and stories in your feed.</p>";

        sendHtmlEmail(
                requester.getEmail(),
                requester.getUsername(),
                "✓ @" + approver.getUsername() + " accepted your follow request",
                "Follow Request Accepted",
                body,
                "Visit @" + approver.getUsername() + "'s Profile",
                "/profile/" + approver.getUsername()
        );
    }

    // -------------------------------------------------------------
    // 15. Unread Chat Message Notification
    // -------------------------------------------------------------
    public void sendUnreadMessageNotification(User recipient, User sender, String messageSnippet) {
        if (recipient == null || recipient.getEmail() == null || sender == null) return;

        String snippet = (messageSnippet != null && messageSnippet.length() > 60) ? (messageSnippet.substring(0, 57) + "...") : messageSnippet;

        String body = "<p>You received a new direct message from <strong>@" + sender.getUsername() + "</strong>:</p>"
                + "<div style=\"background: #f8fafc; border-left: 4px solid #2563eb; border-radius: 4px; padding: 14px; margin: 18px 0; color: #1e293b;\">"
                + "  \"" + snippet + "\""
                + "</div>";

        sendHtmlEmail(
                recipient.getEmail(),
                recipient.getUsername(),
                "✉️ New message from @" + sender.getUsername(),
                "Direct Message",
                body,
                "Open Chat & Reply",
                "/messages"
        );
    }

    // -------------------------------------------------------------
    // 16. Withdrawal Status Email
    // -------------------------------------------------------------
    public void sendWithdrawalStatusEmail(User user, Double amount, String status, String note) {
        if (user == null || user.getEmail() == null) return;

        boolean approved = "APPROVED".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status);
        String color = approved ? "#166534" : "#991b1b";
        String bg = approved ? "#f0fdf4" : "#fef2f2";

        String body = "<p>Your withdrawal request has been updated:</p>"
                + "<div style=\"background: " + bg + "; border-radius: 12px; padding: 18px; margin: 18px 0; color: " + color + ";\">"
                + "  <h3 style=\"margin: 0 0 8px 0;\">Status: " + status + "</h3>"
                + "  <p style=\"margin: 4px 0; font-size: 16px;\">Amount: <strong>₹" + String.format("%.2f", amount != null ? amount : 0.0) + "</strong></p>"
                + (note != null && !note.isBlank() ? ("  <p style=\"margin: 4px 0; font-size: 14px;\">Note: " + note + "</p>") : "")
                + "</div>";

        sendHtmlEmail(
                user.getEmail(),
                user.getUsername(),
                "💸 Withdrawal " + status + ": ₹" + String.format("%.2f", amount != null ? amount : 0.0),
                "Wallet Payout Notice",
                body,
                "Check Wallet Balance",
                "/wallet"
        );
    }

    // -------------------------------------------------------------
    // 17. Coin Purchase / Deposit Confirmation
    // -------------------------------------------------------------
    public void sendCoinPurchaseConfirmation(User user, int coins, double amount, String txnId) {
        if (user == null || user.getEmail() == null) return;

        String body = "<p>Your coin top-up transaction was successful!</p>"
                + "<div style=\"background: #eff6ff; border: 1px solid #bfdbfe; border-radius: 12px; padding: 18px; margin: 18px 0;\">"
                + "  <h3 style=\"margin: 0 0 8px 0; color: #1e40af;\">🪙 +" + coins + " Zen Coins Credited</h3>"
                + "  <p style=\"margin: 4px 0;\">Amount Paid: <strong>₹" + String.format("%.2f", amount) + "</strong></p>"
                + "  <p style=\"margin: 4px 0; font-size: 13px; color: #64748b;\">Txn ID: " + (txnId != null ? txnId : "N/A") + "</p>"
                + "</div>";

        sendHtmlEmail(
                user.getEmail(),
                user.getUsername(),
                "💳 Payment Receipt: +" + coins + " Coins Added",
                "Wallet Deposit Receipt",
                body,
                "View Wallet",
                "/wallet"
        );
    }

    // -------------------------------------------------------------
    // 18. Funds / Coin Transfer Received
    // -------------------------------------------------------------
    public void sendFundsTransferNotification(User recipient, User sender, Double amount) {
        if (recipient == null || recipient.getEmail() == null || sender == null) return;

        String body = "<p><strong>@" + sender.getUsername() + "</strong> just sent you funds!</p>"
                + "<div style=\"background: #f0fdf4; border: 1px solid #bbf7d0; border-radius: 12px; padding: 18px; margin: 18px 0;\">"
                + "  <h3 style=\"margin: 0 0 6px 0; color: #166534;\">💰 Received: ₹" + String.format("%.2f", amount != null ? amount : 0.0) + "</h3>"
                + "  <p style=\"margin: 0; color: #15803d;\">Funds have been added to your current balance.</p>"
                + "</div>";

        sendHtmlEmail(
                recipient.getEmail(),
                recipient.getUsername(),
                "💰 @" + sender.getUsername() + " sent you ₹" + String.format("%.2f", amount != null ? amount : 0.0),
                "Funds Transfer Received",
                body,
                "Check Balance",
                "/wallet"
        );
    }

    // -------------------------------------------------------------
    // 19. Reward / Secret Voucher Claimed
    // -------------------------------------------------------------
    public void sendRewardClaimedEmail(User user, String rewardTitle, String voucherCode) {
        if (user == null || user.getEmail() == null) return;

        String body = "<p>You unlocked a special reward voucher!</p>"
                + "<div style=\"background: #fdf4ff; border: 2px dashed #f0abfc; border-radius: 14px; padding: 20px; margin: 20px 0; text-align: center;\">"
                + "  <h3 style=\"margin: 0 0 8px 0; color: #86198f;\">🎁 " + (rewardTitle != null ? rewardTitle : "Reward Voucher") + "</h3>"
                + "  <div style=\"display: inline-block; background: #fae8ff; border: 1px solid #d8b4fe; padding: 10px 24px; border-radius: 8px; font-size: 20px; font-weight: 800; letter-spacing: 2px; color: #701a75;\">"
                +      (voucherCode != null ? voucherCode : "PROMO2026")
                + "  </div>"
                + "</div>"
                + "<p style=\"font-size: 13px; color: #64748b;\">Present this code at partner outlets to redeem your benefits.</p>";

        sendHtmlEmail(
                user.getEmail(),
                user.getUsername(),
                "🎁 Your Reward Voucher: " + rewardTitle,
                "Special Voucher Code",
                body,
                "View Reward Details",
                "/reward-redeem"
        );
    }

    // -------------------------------------------------------------
    // 20. Security / Password Changed Notification
    // -------------------------------------------------------------
    public void sendPasswordChangedNotification(User user) {
        if (user == null || user.getEmail() == null) return;

        String body = "<p>Your Youthians account password was recently changed.</p>"
                + "<div style=\"background: #fffbeb; border-left: 4px solid #f59e0b; padding: 14px; border-radius: 4px; margin: 18px 0;\">"
                + "  <p style=\"margin: 0; font-size: 14px; color: #92400e;\">"
                + "    If you initiated this change, no further action is needed. If you did not make this change, please reset your password immediately."
                + "  </p>"
                + "</div>";

        sendHtmlEmail(
                user.getEmail(),
                user.getUsername(),
                "🔒 Security Alert: Your Password Was Changed",
                "Security Notice",
                body,
                "Account Settings",
                "/settings"
        );
    }

    // -------------------------------------------------------------
    // 21. Welcome Email
    // -------------------------------------------------------------
    public void sendWelcomeEmail(User user) {
        if (user == null || user.getEmail() == null) return;

        String body = "<div style=\"text-align: center; margin-bottom: 24px;\">"
                + "  <h2 style=\"color: #1e293b; font-size: 20px; font-weight: 800; margin-bottom: 8px;\">ðŸŽ‰ Congratulations on Registering!</h2>"
                + "  <p style=\"color: #64748b; font-size: 14px; margin: 0;\">Your Youthians account is active and ready to go.</p>"
                + "</div>"
                + "<p>Welcome to <strong>Youthians</strong> â€” the ultimate campus hub for events, talent battles, tournaments, and creative connections!</p>"
                + "<div style=\"background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 18px; margin: 18px 0;\">"
                + "  <p style=\"margin-top: 0; font-weight: 700; color: #0f172a;\">Here is what you can do right away:</p>"
                + "  <ul style=\"margin: 0; padding-left: 20px; color: #334155; line-height: 1.8;\">"
                + "    <li><strong>Discover Events:</strong> Book passes for college festivals & cultural meets</li>"
                + "    <li><strong>Battle Arena:</strong> Challenge rivals in 1v1 talent battles</li>"
                + "    <li><strong>Tournaments:</strong> Register for esports, sports & gaming championships</li>"
                + "    <li><strong>Reels & Feed:</strong> Share your campus moments and connect with friends</li>"
                + "  </ul>"
                + "</div>";

        sendHtmlEmail(
                user.getEmail(),
                user.getUsername(),
                "ðŸŽ‰ Congratulations! You have successfully registered on Youthians, @" + user.getUsername(),
                "Registration Successful",
                body,
                "Go to Your Dashboard",
                "/dashboard"
        );
    }

    // -------------------------------------------------------------
    // Support Enquiry & Direct Replies
    // -------------------------------------------------------------
    public void sendSupportEnquiryNotification(String senderName, String senderEmail, String subject, String messageText) {
        CompletableFuture.runAsync(() -> {
            if (mailSender == null) return;
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
                        + "</div>"
                        + "<div style=\"padding: 24px;\">"
                        + "<p><strong>Submitter:</strong> " + senderName + " (" + senderEmail + ")</p>"
                        + "<p><strong>Subject:</strong> " + subject + "</p>"
                        + "<div style=\"background: #f8fafc; border-left: 4px solid #3b82f6; padding: 16px; margin: 20px 0;\">"
                        + messageText
                        + "</div>"
                        + "</div>"
                        + "</div>";

                helper.setText(htmlContent, true);
                mailSender.send(message);
            } catch (Exception e) {
                logger.error("Failed to send support enquiry email: {}", e.getMessage());
            }
        });
    }

    public void sendEnquiryAcknowledgement(String senderName, String senderEmail, String subject, String messageText) {
        sendHtmlEmail(
                senderEmail,
                senderName,
                "[Youthians Support] We received your message: " + subject,
                "Support Request Received",
                "<p>Thank you for reaching out. We have received your inquiry regarding <strong>\"" + subject + "\"</strong>.</p>"
                        + "<div style=\"background: #f8fafc; border-left: 4px solid #0ea5e9; padding: 14px; margin: 18px 0;\">"
                        + messageText
                        + "</div>"
                        + "<p>Our team is reviewing your message and will get back to you shortly.</p>",
                null,
                null
        );
    }

    public boolean sendDirectReply(String toEmail, String recipientName, String subject, String replyBody) {
        sendHtmlEmail(
                toEmail,
                recipientName,
                "Re: " + subject,
                "Support Response",
                "<p>" + replyBody + "</p>",
                "Visit Support Center",
                "/support"
        );
        return true;
    }
}
