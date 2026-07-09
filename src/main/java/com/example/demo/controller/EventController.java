package com.example.demo.controller;

import com.example.demo.model.Event;
import com.example.demo.model.LadiesPartyDetails;
import com.example.demo.repository.LadiesPartyDetailsRepository;
import com.example.demo.model.EventRegistration;
import com.example.demo.model.User;
import com.example.demo.model.EventSeatTier;
import com.example.demo.model.EventSeat;
import com.example.demo.model.Vote;
import com.example.demo.repository.EventSeatRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.EventRepository;
import com.example.demo.repository.EventRegistrationRepository;
import com.example.demo.repository.EventSeatTierRepository;
import com.example.demo.repository.VoteRepository;
import com.example.demo.repository.LadiesPartyDetailsRepository;
import com.example.demo.repository.AdventureDetailsRepository;
import com.example.demo.repository.TrekkingDetailsRepository;
import com.example.demo.repository.BikeRidingDetailsRepository;
import com.example.demo.model.LadiesPartyDetails;
import com.example.demo.model.AdventureDetails;
import com.example.demo.model.TrekkingDetails;
import com.example.demo.model.BikeRidingDetails;
import com.example.demo.model.TrekkingDetails;
import com.example.demo.service.RewardService;
import com.example.demo.service.SecretRewardService;
import com.example.demo.repository.UserRewardRepository;
import com.example.demo.model.SecretRewardPartner;
import com.example.demo.model.UserReward;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.springframework.scheduling.annotation.Scheduled;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.net.InetAddress;

@Controller
@RequestMapping(value = "/events")
public class EventController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventRegistrationRepository eventRegistrationRepository;

    @Autowired
    private EventSeatRepository eventSeatRepository;

    @Autowired
    private EventSeatTierRepository eventSeatTierRepository;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private RewardService rewardService;

    @Autowired
    private LadiesPartyDetailsRepository ladiesPartyDetailsRepository;

    @Autowired
    private AdventureDetailsRepository adventureDetailsRepository;

    @Autowired
    private TrekkingDetailsRepository trekkingDetailsRepository;

    @Autowired
    private BikeRidingDetailsRepository bikeRidingDetailsRepository;

    @Autowired
    private SecretRewardService secretRewardService;

    @Autowired
    private UserRewardRepository userRewardRepository;

    @Autowired
    private HttpServletRequest httpServletRequest;

    private User getUserFromSession(HttpSession session) {
        Object authUser = httpServletRequest.getAttribute("authenticatedUser");
        if (authUser instanceof User) {
            return (User) authUser;
        }
        if ("admin".equals(authUser)) return null; // Admin string handled separately
        
        Object sessionUser = session.getAttribute("user");
        if (sessionUser instanceof User) {
            return (User) sessionUser;
        }
        if ("admin".equals(sessionUser)) return null; // Admin string handled separately
        Object userIdObj = session.getAttribute("userId");
        if (userIdObj != null) {
            try {
                Long userId = null;
                if (userIdObj instanceof Number) {
                    userId = ((Number) userIdObj).longValue();
                } else if (userIdObj instanceof String) {
                    userId = Long.parseLong((String) userIdObj);
                }
                if (userId != null) {
                    return userRepository.findById(userId).orElse(null);
                }
            } catch (Exception e) {
            }
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────
    //  PUBLIC: List Events  (Student View)
    // ─────────────────────────────────────────────────────────
    @GetMapping
    public String listEvents(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            Model model, HttpSession session) {
        resolveExpiredPolls(); // Maintenance: Auto-pick winners

        User user = getUserFromSession(session);
        boolean adminViewing = isAdmin(session);
        if (user != null) {
            user = userRepository.findById(user.getId()).orElse(user);
            session.setAttribute("user", user);
            session.setAttribute("userId", user.getId());
        }

        // Redirect only if neither student nor admin
        if (user == null && !adminViewing) return "redirect:/login";

        // ── Step 1: Base query — search by location/venue/title keyword ──
        List<Event> allItems;
        boolean hasSearch = search != null && !search.trim().isEmpty();
        if (hasSearch) {
            allItems = eventRepository.searchByVenueOrTitle(search.trim());
        } else {
            allItems = eventRepository.findAll();
        }

        // ── Step 2: Further filter by category (both filters work together) ──
        boolean hasCategory = category != null && !category.trim().isEmpty() && !category.equalsIgnoreCase("All");
        if (hasCategory) {
            final String cat = category.trim();
            allItems = allItems.stream()
                    .filter(e -> cat.equalsIgnoreCase(e.getCategory()))
                    .collect(Collectors.toList());
        }

        // Separate Polls from Registered Events
        List<Event> votingPolls = allItems.stream()
                .filter(e -> "VOTING".equals(e.getStatus()))
                .filter(e -> e.getVotingEndDate() == null || e.getVotingEndDate().isAfter(LocalDateTime.now()))
                .collect(Collectors.toList());

        LocalDateTime eightDaysAgo = LocalDateTime.now().minusDays(8);
        List<Event> regularEvents = allItems.stream()
                .filter(e -> {
                    String status = e.getStatus();
                    if ("VOTING".equals(status) || "REJECTED".equals(status)) return false;
                    
                    if (e.getDateTime() != null && e.getDateTime().isBefore(eightDaysAgo)) {
                        return false;
                    }
                    
                    return true;
                })
                .collect(Collectors.toList());

        // Trending = first 3 UPCOMING regular events (always from full list, unaffected by search)
        List<Event> trending = eventRepository.findAll().stream()
                .filter(e -> "UPCOMING".equals(e.getStatus()) || e.getStatus() == null)
                .filter(e -> { String s = e.getStatus(); return s == null || (!"VOTING".equals(s) && !"REJECTED".equals(s)); })
                .limit(3).collect(Collectors.toList());

        model.addAttribute("events", regularEvents);
        model.addAttribute("votingPolls", votingPolls);
        model.addAttribute("trending", trending);
        model.addAttribute("activeCategory", category != null ? category : "All");
        model.addAttribute("activeSearch", search != null ? search : "");
        model.addAttribute("user", user);
        model.addAttribute("isAdmin", adminViewing);
        
        Set<Long> votedPollIds = new HashSet<>();
        if (user != null) {
            List<Vote> userVotes = voteRepository.findByUserId(user.getId());
            votedPollIds = userVotes.stream().map(Vote::getPollId).collect(Collectors.toSet());
        }
        model.addAttribute("votedPollIds", votedPollIds);

        return "events";
    }

    @RequestMapping(value = "/{id}/poll-vote", method = RequestMethod.POST)
    public String castPollVote(@PathVariable Long id, HttpSession session, HttpServletRequest request) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/login";

        Event event = eventRepository.findById(id).orElse(null);
        if (event == null || !"VOTING".equals(event.getStatus())) return "redirect:/events?error=invalid_event";

        String referer = request.getHeader("Referer");
        String redirectBase = (referer != null && !referer.contains("/login")) ? referer : "/events";
        if (redirectBase.contains("?")) redirectBase = redirectBase.split("\\?")[0];

        // Check for duplicate vote using VoteRepository
        if (voteRepository.existsByUserIdAndPollId(user.getId(), id)) {
            return "redirect:" + redirectBase + "?alreadyVotedInPoll=true";
        }

        // Save new vote
        Vote vote = new Vote(user.getId(), id);
        voteRepository.save(vote);

        event.setPollVotes(event.getPollVotes() + 1);
        eventRepository.save(event);

        User dbUser = userRepository.findById(user.getId()).orElse(user);
        rewardService.awardVoting(dbUser); // Award coins for voting 🗳️
        userRepository.save(dbUser);
        
        return "redirect:" + redirectBase + "?voteSuccess=true";
    }

    private void resolveExpiredPolls() {
        LocalDateTime now = LocalDateTime.now();
        List<Event> expiredPolls = eventRepository.findAll().stream()
                .filter(e -> "VOTING".equals(e.getStatus()))
                .filter(e -> e.getVotingEndDate() != null && e.getVotingEndDate().isBefore(now))
                .collect(Collectors.toList());

        if (expiredPolls.isEmpty()) return;

        // Group by category to pick one winner per category poll
        Map<String, List<Event>> groups = expiredPolls.stream()
                .collect(Collectors.groupingBy(e -> e.getCategory() != null ? e.getCategory() : "Misc"));

        for (List<Event> group : groups.values()) {
            Event winner = group.stream()
                    .max(Comparator.comparingInt(Event::getPollVotes))
                    .orElse(null);
            
            if (winner != null) {
                winner.setStatus("UPCOMING");
                winner.setVotingStatus("CLOSED");
                eventRepository.save(winner);
                
                // Reject others in same expired group
                for (Event e : group) {
                    if (!e.getId().equals(winner.getId())) {
                        e.setStatus("REJECTED");
                        e.setVotingStatus("CLOSED");
                        eventRepository.save(e);
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    //  PUBLIC: Event Detail Page
    // ─────────────────────────────────────────────────────────
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public String eventDetails(@PathVariable Long id, Model model, HttpSession session) {
        User user = getUserFromSession(session);
        boolean adminViewing = isAdmin(session);

        if (user != null) {
            user = userRepository.findById(user.getId()).orElse(user);
            session.setAttribute("user", user);
            session.setAttribute("userId", user.getId());
        }

        Event event = eventRepository.findById(id).orElse(null);
        if (event == null) return "redirect:/events";

        List<EventRegistration> userRegs = new java.util.ArrayList<>();
        if (user != null) {
            userRegs = eventRegistrationRepository.findByEventAndUser(event, user);
            
            // Fix for missing ticket IDs in legacy records
            for (EventRegistration reg : userRegs) {
                if (reg.getTicketId() == null || reg.getTicketId().isBlank()) {
                    reg.setTicketId(UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                    eventRegistrationRepository.save(reg);
                }
            }
        }
        boolean isRegistered = !userRegs.isEmpty();
        long dbRegistrationCount = eventRegistrationRepository.countByEvent(event);
        long registrationCount = dbRegistrationCount;
        
        // Use -1 for unlimited spots
        Integer totalCapacity = event.getMaxParticipants();
        if (event.getSeatTiers() != null && !event.getSeatTiers().isEmpty() && event.getTotalRows() > 0) {
            totalCapacity = event.getSeatTiers().stream().mapToInt(EventSeatTier::getCapacity).sum();
            // sync maxParticipants for UI consistency if needed
        }

        int spotsLeft = (totalCapacity != null && totalCapacity > 0)
                ? Math.max(0, totalCapacity - (int) registrationCount) : -1;
        model.addAttribute("totalCapacity", totalCapacity); // For UI display

        // Calculate minimum starting price for available seats
        Double minStartingPrice = null;
        if (event.getSeatTiers() != null && !event.getSeatTiers().isEmpty() && event.getTotalRows() > 0) {
            for (EventSeatTier tier : event.getSeatTiers()) {
                int capacity = tier.getCapacity() != null ? tier.getCapacity() : 0;
                int registered = tier.getRegisteredCount() != null ? tier.getRegisteredCount() : 0;
                if (capacity - registered > 0 && tier.getPrice() != null) {
                    if (minStartingPrice == null || tier.getPrice() < minStartingPrice) {
                        minStartingPrice = tier.getPrice();
                    }
                }
            }
        } else {
            // For non-seated events, parse the base price
            try {
                if (spotsLeft != 0 && event.getPrice() != null && !event.getPrice().equalsIgnoreCase("Free")) {
                    minStartingPrice = Double.parseDouble(event.getPrice().replaceAll("[^0-9.]", ""));
                }
            } catch (Exception e) {
                // Ignore parsing errors
            }
        }
        
        if (minStartingPrice != null) {
            String formattedPrice = "₹" + String.format("%,.0f", minStartingPrice);
            model.addAttribute("startingPriceFormatted", formattedPrice);
        } else {
            model.addAttribute("startingPriceFormatted", "Sold Out");
        }

        // Related events (upcoming, same category or venue, exclude current)
        List<Event> allUpcoming = eventRepository.findAll().stream()
                .filter(e -> "UPCOMING".equals(e.getStatus()) || e.getStatus() == null)
                .filter(e -> e.getDateTime() == null || e.getDateTime().isAfter(LocalDateTime.now().minusDays(1)))
                .filter(e -> !e.getId().equals(id))
                .collect(Collectors.toList());

        List<Event> candidatesForRelated = allUpcoming.stream()
                .filter(e -> (e.getCategory() != null && e.getCategory().equalsIgnoreCase(event.getCategory())) ||
                             (e.getVenue() != null && e.getVenue().equalsIgnoreCase(event.getVenue())))
                .collect(Collectors.toList());

        candidatesForRelated.sort((e1, e2) -> {
            boolean e1Cat = e1.getCategory() != null && e1.getCategory().equalsIgnoreCase(event.getCategory());
            boolean e2Cat = e2.getCategory() != null && e2.getCategory().equalsIgnoreCase(event.getCategory());
            if (e1Cat && !e2Cat) return -1;
            if (!e1Cat && e2Cat) return 1;

            LocalDateTime d1 = e1.getDateTime();
            LocalDateTime d2 = e2.getDateTime();
            if (d1 == null && d2 == null) return 0;
            if (d1 == null) return 1;
            if (d2 == null) return -1;
            return d1.compareTo(d2);
        });

        List<Event> related = candidatesForRelated.stream().limit(3).collect(Collectors.toList());

        // For Secret Voting System (when event is ONGOING) or Finalist voting (when event is VOTING)
        boolean hasVoted = false;
        List<EventRegistration> candidates = new java.util.ArrayList<>();
        if ("ONGOING".equals(event.getStatus()) || "COMPLETED".equals(event.getStatus()) || "VOTING".equals(event.getStatus())) {
            if (user != null) {
                User dbUser = userRepository.findById(user.getId()).orElse(user);
                hasVoted = dbUser.getVotedEvents().contains(id);
            }
            if ("VOTING".equals(event.getStatus())) {
                candidates = eventRegistrationRepository.findByEvent(event).stream()
                        .filter(EventRegistration::isFinalist)
                        .collect(Collectors.toList());
            } else {
                candidates = eventRegistrationRepository.findByEvent(event).stream()
                        .filter(EventRegistration::isAttendanceMarked)
                        .collect(Collectors.toList());
            }
        }

        List<EventSeat> seats = event.getSeats();
        seats.sort(Comparator.comparing(EventSeat::getRowLabel).thenComparing(EventSeat::getSeatNumber));

        // Fetch category-specific details based on the event's category
        if ("House Party".equalsIgnoreCase(event.getCategory())) {
            ladiesPartyDetailsRepository.findByEvent(event).ifPresent(lpd -> model.addAttribute("housePartyDetails", lpd));
        } else if ("Adventure".equalsIgnoreCase(event.getCategory())) {
            adventureDetailsRepository.findByEvent(event).ifPresent(ad -> model.addAttribute("adventureDetails", ad));
        } else if ("Trekking".equalsIgnoreCase(event.getCategory())) {
            trekkingDetailsRepository.findByEvent(event).ifPresent(td -> model.addAttribute("trekkingDetails", td));
        } else if ("Bike Riding".equalsIgnoreCase(event.getCategory())) {
            bikeRidingDetailsRepository.findByEvent(event).ifPresent(bd -> model.addAttribute("bikeRidingDetails", bd));
        }

        model.addAttribute("event", event);
        model.addAttribute("seats", seats);
        model.addAttribute("user", user);
        model.addAttribute("isAdmin", adminViewing);
        model.addAttribute("isRegistered", isRegistered);
        model.addAttribute("userRegs", userRegs);
        model.addAttribute("registrationCount", registrationCount);
        model.addAttribute("spotsLeft", spotsLeft);
        model.addAttribute("related", related);
        model.addAttribute("hasVoted", hasVoted);
        model.addAttribute("candidates", candidates);
        return "event-registration";
    }

    // ─────────────────────────────────────────────────────────
    //  PUBLIC VOTING
    // ─────────────────────────────────────────────────────────
    @RequestMapping(value = "/{id}/vote/{regId}", method = RequestMethod.POST)
    public String castVote(@PathVariable Long id, @PathVariable Long regId, HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/login";

        Event event = eventRepository.findById(id).orElse(null);
        if (event == null || !("ONGOING".equals(event.getStatus()) || "VOTING".equals(event.getStatus()))) return "redirect:/events/" + id;

        User dbUser = userRepository.findById(user.getId()).orElse(null);
        if (dbUser == null) return "redirect:/login";

        if (dbUser.getVotedEvents().contains(id)) {
            return "redirect:/events/" + id + "?alreadyVoted=true";
        }

        EventRegistration reg = eventRegistrationRepository.findById(regId).orElse(null);
        if (reg != null && reg.getEvent().getId().equals(id)) {
            reg.setPublicVotes(reg.getPublicVotes() + 1);
            eventRegistrationRepository.save(reg);

            dbUser.getVotedEvents().add(id);
            userRepository.save(dbUser);
        }

        return "redirect:/events/" + id + "?voteSuccess=true";
    }

    // ─────────────────────────────────────────────────────────
    //  SEATING: Hold seats for 5 minutes
    // ─────────────────────────────────────────────────────────
    @RequestMapping(value = "/{id}/hold-seats", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> holdSeats(@PathVariable Long id, @RequestBody List<Long> seatIds, HttpSession session) {
        Map<String, Object> response = new java.util.HashMap<>();
        User user = getUserFromSession(session);
        if (user == null) {
            response.put("success", false);
            response.put("message", "Please login to select seats.");
            return response;
        }

        Event event = eventRepository.findById(id).orElse(null);
        if (event == null) {
            response.put("success", false);
            response.put("message", "Event not found.");
            return response;
        }

        List<EventSeat> seatsToHold = eventSeatRepository.findAllById(seatIds);
        LocalDateTime now = LocalDateTime.now();

        // Check availability
        for (EventSeat seat : seatsToHold) {
            boolean isHoldExpired = "HOLD".equals(seat.getStatus()) && seat.getHoldExpiresAt() != null && seat.getHoldExpiresAt().isBefore(now);
            if (!"AVAILABLE".equals(seat.getStatus()) && !isHoldExpired) {
                response.put("success", false);
                response.put("message", "Seat " + seat.getSeatIdentifier() + " is currently unavailable.");
                return response;
            }
        }

        // Apply HOLD
        for (EventSeat seat : seatsToHold) {
            seat.setStatus("HOLD");
            seat.setHoldExpiresAt(now.plusMinutes(5));
            seat.setBookedByUser(user);
        }
        eventSeatRepository.saveAll(seatsToHold);

        response.put("success", true);
        return response;
    }

    @Scheduled(fixedRate = 30000) // Run every 30 seconds
    public void releaseExpiredHolds() {
        LocalDateTime now = LocalDateTime.now();
        List<EventSeat> expiredSeats = eventSeatRepository.findByStatusAndHoldExpiresAtBefore("HOLD", now);
        if (!expiredSeats.isEmpty()) {
            for (EventSeat seat : expiredSeats) {
                seat.setStatus("AVAILABLE");
                seat.setHoldExpiresAt(null);
                seat.setBookedByUser(null);
            }
            eventSeatRepository.saveAll(expiredSeats);
            System.out.println("Released " + expiredSeats.size() + " expired seat holds.");
        }
    }

    // ─────────────────────────────────────────────────────────
    //  REGISTER: Entry point — decides Free vs Paid
    // ─────────────────────────────────────────────────────────
    @RequestMapping(value = "/{id}/register", method = RequestMethod.POST)
    public String initiateRegistration(
            @PathVariable Long id,
            @RequestParam(required = false) String fullName,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String college,
            @RequestParam(required = false) String yearOfStudy,
            @RequestParam(required = false) String selectedTier,
            @RequestParam(required = false) List<Long> selectedSeatIds,
            @RequestParam(defaultValue = "1") Integer quantity,
            @RequestParam(required = false) Integer lp_age,
            @RequestParam(required = false) String lp_city,
            @RequestParam(required = false) String lp_emergencyContactName,
            @RequestParam(required = false) String lp_emergencyContactMobile,
            @RequestParam(required = false) String lp_dietaryPreference,
            @RequestParam(required = false) String lp_specialRequests,
            HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/login";

        Event event = eventRepository.findById(id).orElse(null);
        if (event == null) return "redirect:/events";

        // If seat map is used, quantity is the number of seats selected
        if (selectedSeatIds != null && !selectedSeatIds.isEmpty()) {
            quantity = selectedSeatIds.size();
        }
        
        if ("House Party".equalsIgnoreCase(event.getCategory())) {
            LadiesPartyDetails lpd = ladiesPartyDetailsRepository.findByEvent(event).orElse(null);
        }

        // Store reg info in session temporarily for paid flow
        session.setAttribute("regFullName", fullName);
        session.setAttribute("regEmail", email);
        session.setAttribute("regPhone", phone);
        session.setAttribute("regCollege", college);
        session.setAttribute("regYear", yearOfStudy);
        session.setAttribute("regTier", selectedTier);
        session.setAttribute("regSeatIds", selectedSeatIds);
        session.setAttribute("regQuantity", quantity);
        session.setAttribute("lp_age", lp_age);
        session.setAttribute("lp_city", lp_city);
        session.setAttribute("lp_emergencyContactName", lp_emergencyContactName);
        session.setAttribute("lp_emergencyContactMobile", lp_emergencyContactMobile);
        session.setAttribute("lp_dietaryPreference", lp_dietaryPreference);
        session.setAttribute("lp_specialRequests", lp_specialRequests);

        if ("Paid".equalsIgnoreCase(event.getEntryFeeType()) || (event.getPrice() != null && !event.getPrice().equalsIgnoreCase("Free"))) {
            User dbUser = userRepository.findById(user.getId()).orElse(user);
            if (dbUser.isHasFreeEntry()) {
                dbUser.setHasFreeEntry(false);
                userRepository.save(dbUser);
                return completeRegistration(event, dbUser, "FREE", fullName, email, phone, college, yearOfStudy, selectedTier, selectedSeatIds, quantity, session);
            }
            return "redirect:/events/" + id + "/payment";
        } else {
            return completeRegistration(event, user, "FREE", fullName, email, phone, college, yearOfStudy, selectedTier, selectedSeatIds, quantity, session);
        }
    }

    // ─────────────────────────────────────────────────────────
    //  PAYMENT: Show payment gateway page
    // ─────────────────────────────────────────────────────────
    @RequestMapping(value = "/{id}/payment", method = RequestMethod.GET)
    public String showPaymentPage(@PathVariable Long id, Model model, HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/login";

        Event event = eventRepository.findById(id).orElse(null);
        if (event == null) return "redirect:/events";
        
        Integer quantity = (Integer) session.getAttribute("regQuantity");
        if (quantity == null) quantity = 1;
        String selectedTier = (String) session.getAttribute("regTier");

        User dbUser = userRepository.findById(user.getId()).orElse(user);
        
        // Calculate discounted price if applicable
        String originalPrice = event.getPrice();
        double priceVal = 0.0;
        
        if (selectedTier != null && !selectedTier.isBlank() && event.getSeatTiers() != null) {
            for (EventSeatTier tier : event.getSeatTiers()) {
                if (selectedTier.equalsIgnoreCase(tier.getTierName())) {
                    priceVal = tier.getPrice() != null ? tier.getPrice() : 0.0;
                    break;
                }
            }
        } else {
            priceVal = parsePrice(originalPrice);
        }
        
        double finalPrice = priceVal * quantity;
        boolean isDiscounted = false;

        if (dbUser.isHasDiscount()) {
            finalPrice = finalPrice * 0.5;
            isDiscounted = true;
        }

        model.addAttribute("event", event);
        model.addAttribute("user", dbUser);
        model.addAttribute("isDiscounted", isDiscounted);
        model.addAttribute("quantity", quantity);
        model.addAttribute("basePrice", "₹" + String.format("%,.0f", priceVal));
        model.addAttribute("finalPrice", "₹" + String.format("%,.0f", finalPrice));
        return "payment";
    }

    // ─────────────────────────────────────────────────────────
    //  PAYMENT: Process payment (simulate success)
    // ─────────────────────────────────────────────────────────
    @RequestMapping(value = "/{id}/payment/confirm", method = RequestMethod.POST)
    @SuppressWarnings("unchecked")
    public String confirmPayment(
            @PathVariable Long id,
            @RequestParam(required = false) String cardName,
            @RequestParam(required = false) String cardNumber,
            HttpSession session) {

        User user = getUserFromSession(session);
        if (user == null) return "redirect:/login";

        Event event = eventRepository.findById(id).orElse(null);
        if (event == null) return "redirect:/events";

        // Retrieve registration info stored in session
        String fullName    = (String) session.getAttribute("regFullName");
        String email       = (String) session.getAttribute("regEmail");
        String phone       = (String) session.getAttribute("regPhone");
        String college     = (String) session.getAttribute("regCollege");
        String yearOfStudy = (String) session.getAttribute("regYear");
        String selectedTier = (String) session.getAttribute("regTier");
        List<Long> selectedSeatIds = (List<Long>) session.getAttribute("regSeatIds");
        Integer quantity = (Integer) session.getAttribute("regQuantity");
        if (quantity == null) quantity = 1;

        User dbUser = userRepository.findById(user.getId()).orElse(user);
        String paymentStatus = "PAID";
        if (dbUser.isHasDiscount()) {
            dbUser.setHasDiscount(false);
            userRepository.save(dbUser);
            paymentStatus = "PAID (DISCOUNTED)";
        }

        return completeRegistration(event, dbUser, paymentStatus, fullName, email, phone, college, yearOfStudy, selectedTier, selectedSeatIds, quantity, session);
    }

    // ─────────────────────────────────────────────────────────
    //  TICKET: Show confirmation + ticket
    // ─────────────────────────────────────────────────────────
    @RequestMapping(value = "/ticket/{ticketId}", method = RequestMethod.GET)
    public String showTicket(@PathVariable String ticketId, Model model, HttpSession session, HttpServletRequest request) {
        User user = getUserFromSession(session);
        boolean adminViewing = isAdmin(session);
        
        // Publicly accessible for QR code verification
        // (Previously restricted to logged-in users only)

        EventRegistration reg = eventRegistrationRepository.findByTicketId(ticketId).orElse(null);
        if (reg == null) return "redirect:/events";

        // Construct full URL and a text summary for the QR code
        String serverName = request.getServerName();
        String port = String.valueOf(request.getServerPort());
        
        // Localhost Fix: Try to get actual IP if running locally so phone can scan
        if (serverName.equals("localhost") || serverName.equals("127.0.0.1")) {
            try {
                serverName = InetAddress.getLocalHost().getHostAddress();
            } catch (Exception e) {
                // fallback to localhost if IP detection fails
            }
        }
        
        String baseUrl = request.getScheme() + "://" + serverName + ":" + port;
        String fullUrl = baseUrl + "/events/ticket/" + ticketId;
        
        // Formatted summary for the scanner
        String qrData = "ZENTRIX VERIFIED TICKET\n" +
                        "----------------------\n" +
                        "ID: " + ticketId + "\n" +
                        "NAME: " + reg.getFullName() + "\n" +
                        "EVENT: " + reg.getEvent().getTitle() + "\n" +
                        "STATUS: " + reg.getRegistrationStatus() + "\n" +
                        "----------------------\n" +
                        "Link: " + fullUrl;
        
        model.addAttribute("ticketUrl", qrData);

        // Fetch Booked Seats for this user and event
        if (reg.getUser() != null) {
            List<EventSeat> bookedSeats = eventSeatRepository.findByEventAndBookedByUser(reg.getEvent(), reg.getUser());
            model.addAttribute("bookedSeats", bookedSeats);
            
            // Add seat info to QR data if seats exist
            if (!bookedSeats.isEmpty()) {
                String seatsStr = bookedSeats.stream().map(EventSeat::getSeatIdentifier).collect(Collectors.joining(", "));
                qrData += "\nSEATS: " + seatsStr;
                model.addAttribute("ticketUrl", qrData); // update with seats
            }
        }

        model.addAttribute("registration", reg);
        model.addAttribute("event", reg.getEvent());
        
        if (reg.getEvent().isEnableSecretRewards() && reg.getUser() != null) {
            userRewardRepository.findByUserAndEvent(reg.getUser(), reg.getEvent())
                    .ifPresent(reward -> model.addAttribute("userReward", reward));
        }

        boolean canSeePII = adminViewing || (user != null && reg.getUser() != null && reg.getUser().getId().equals(user.getId()));
        model.addAttribute("canSeePII", canSeePII);
        
        model.addAttribute("user", reg.getUser() != null ? reg.getUser() : user);
        model.addAttribute("isAdmin", adminViewing);
        return "ticket";
    }

    // ─────────────────────────────────────────────────────────
    //  ADMIN: Manage Events
    // ─────────────────────────────────────────────────────────
    @RequestMapping(value = "/admin/manage", method = RequestMethod.GET)
    public String adminManageEvents(Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";

        List<Event> events = eventRepository.findAll();
        model.addAttribute("events", events);
        model.addAttribute("totalEvents", events.size());
        model.addAttribute("upcomingCount", eventRepository.countByStatus("UPCOMING"));
        model.addAttribute("ongoingCount", eventRepository.countByStatus("ONGOING"));
        model.addAttribute("completedCount", eventRepository.countByStatus("COMPLETED"));
        model.addAttribute("votingCount", eventRepository.countByStatus("VOTING"));
        return "admin-events";
    }

    @RequestMapping(value = "/admin/create", method = RequestMethod.GET)
    public String showCreateEventForm(Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";
        model.addAttribute("event", new Event());
        return "admin-create-event";
    }

    @RequestMapping(value = "/admin/create", method = RequestMethod.POST)
    public String createEvent(
            @ModelAttribute("formEvent") Event formEvent,
            @RequestParam String title,
            @RequestParam String category,
            @RequestParam String description,
            @RequestParam String dateTime,
            @RequestParam(required = false) String venue,
            @RequestParam String entryFeeType,
            @RequestParam(required = false) String price,
            @RequestParam Integer maxParticipants,
            @RequestParam(required = false, defaultValue = "Offline") String eventMode,
            @RequestParam(required = false) String meetingLink,
            @RequestParam(required = false) MultipartFile imageFile,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String votingStartDate,
            @RequestParam(required = false) String votingEndDate,
            @RequestParam(required = false, defaultValue = "0") Integer totalRows,
            @RequestParam(required = false, defaultValue = "0") Integer seatsPerRow,
            @RequestParam(required = false, defaultValue = "Standard") String seatLayoutType,
            @RequestParam(required = false, defaultValue = "0") Integer vipSeatCount,
            @RequestParam(required = false, defaultValue = "0.0") Double vipPrice,
            @RequestParam(required = false, defaultValue = "0.0") Double regularPrice,
            @RequestParam(required = false, defaultValue = "false") boolean finalVotingEnabled,
            HttpServletRequest request,
            HttpSession session) {

        if (!isAdmin(session)) return "redirect:/login";

        Event event = new Event();
        event.setTitle(title);
        event.setCategory(category);
        event.setDescription(description);
        event.setVenue(venue);
        event.setEntryFeeType(entryFeeType);
        event.setMaxParticipants(maxParticipants);
        event.setStatus(status != null ? status : "UPCOMING");
        event.setOrganizer("Zentrix Admin");
        event.setEventMode(eventMode);
        event.setMeetingLink(meetingLink);
        event.setFinalVotingEnabled(finalVotingEnabled);

        // Secret Rewards Binding
        event.setEnableSecretRewards(formEvent.isEnableSecretRewards());
        if (formEvent.isEnableSecretRewards() && formEvent.getSecretRewards() != null) {
            for (SecretRewardPartner partner : formEvent.getSecretRewards()) {
                if (partner.getBusinessName() != null && !partner.getBusinessName().isBlank()) {
                    partner.setRemainingQuantity(partner.getQuantity());
                    partner.setEvent(event);
                    event.getSecretRewards().add(partner);
                }
            }
        }

        if ("Free".equals(entryFeeType)) {
            event.setPrice("Free");
        } else {
            event.setPrice(price != null && !price.isBlank() ? "\u20b9" + price : "Paid");
        }

        try {
            event.setDateTime(LocalDateTime.parse(dateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")));
        } catch (Exception ignored) {}

        // Handle File Upload
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String originalFilename = imageFile.getOriginalFilename();
                String extension = "";
                if(originalFilename != null && originalFilename.contains(".")) {
                    extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                }
                String newFilename = UUID.randomUUID().toString() + extension;
                Path uploadDir = Paths.get("src/main/resources/static/uploads");
                if (!Files.exists(uploadDir)) {
                    Files.createDirectories(uploadDir);
                }
                Path filePath = uploadDir.resolve(newFilename);
                Files.copy(imageFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                event.setImageUrl("/uploads/" + newFilename);
            } catch (IOException e) {
                event.setImageUrl(getDefaultImage(category));
            }
        } else {
            event.setImageUrl(getDefaultImage(category));
        }

        if ("VOTING".equals(event.getStatus())) {
            try {
                if (votingStartDate != null && !votingStartDate.isBlank())
                    event.setVotingStartDate(LocalDateTime.parse(votingStartDate));
                if (votingEndDate != null && !votingEndDate.isBlank())
                    event.setVotingEndDate(LocalDateTime.parse(votingEndDate));
            } catch (Exception ignored) {}
        }

        event.setTotalRows(totalRows);
        event.setSeatsPerRow(seatsPerRow);
        event.setSeatLayoutType(seatLayoutType);
        event.setVipSeatCount(vipSeatCount);
        event.setVipPrice(vipPrice);
        event.setRegularPrice(regularPrice);

        // Populate seatTiers for backward compatibility/pricing display logic
        event.getSeatTiers().clear();
        if (vipPrice > 0 || vipSeatCount > 0) {
            EventSeatTier vipTier = new EventSeatTier();
            vipTier.setTierName("VIP");
            vipTier.setPrice(vipPrice);
            vipTier.setCapacity(vipSeatCount);
            vipTier.setEvent(event);
            event.getSeatTiers().add(vipTier);
        }
        
        EventSeatTier regTier = new EventSeatTier();
        regTier.setTierName("REGULAR");
        regTier.setPrice(regularPrice);
        regTier.setCapacity((totalRows * seatsPerRow) - vipSeatCount);
        regTier.setEvent(event);
        event.getSeatTiers().add(regTier);

        // Generate the grid!
        generateSeatGrid(event);

        eventRepository.save(event);
        
        if ("House Party".equalsIgnoreCase(category)) {
            LadiesPartyDetails lpd = new LadiesPartyDetails();
            lpd.setEvent(event);
            lpd.setTheme(request.getParameter("lp_theme"));
            lpd.setPartyType(request.getParameter("lp_partyType"));
            lpd.setDressCode(request.getParameter("lp_dressCode"));
            lpd.setAgeGroup(request.getParameter("lp_ageGroup"));
            lpd.setVenueName(request.getParameter("lp_venueName"));
            lpd.setActivities(request.getParameter("lp_activities"));
            
            ladiesPartyDetailsRepository.save(lpd);
        }

        if ("Adventure".equalsIgnoreCase(category)) {
            AdventureDetails ad = new AdventureDetails();
            ad.setEvent(event);
            ad.setAdventureType(request.getParameter("adv_type"));
            ad.setDifficultyLevel(request.getParameter("adv_difficulty"));
            ad.setAdventureDuration(request.getParameter("adv_duration"));
            
            try {
                if(request.getParameter("adv_distance") != null && !request.getParameter("adv_distance").isBlank())
                    ad.setDistanceCovered(Double.parseDouble(request.getParameter("adv_distance")));
            } catch(Exception e) {}
            
            ad.setElevation(request.getParameter("adv_elevation"));
            ad.setFitnessLevelRequired(request.getParameter("adv_fitness"));
            
            try {
                if(request.getParameter("adv_minAge") != null && !request.getParameter("adv_minAge").isBlank())
                    ad.setMinAge(Integer.parseInt(request.getParameter("adv_minAge")));
            } catch(Exception e) {}
            
            try {
                if(request.getParameter("adv_maxAge") != null && !request.getParameter("adv_maxAge").isBlank())
                    ad.setMaxAge(Integer.parseInt(request.getParameter("adv_maxAge")));
            } catch(Exception e) {}
            
            String[] safetyEquipments = request.getParameterValues("adv_safetyEquipment");
            if (safetyEquipments != null && safetyEquipments.length > 0) {
                ad.setSafetyEquipment(String.join(", ", safetyEquipments));
            }
            
            ad.setThingsToBring(request.getParameter("adv_thingsToBring"));
            ad.setMedicalCertificateRequired("Yes".equalsIgnoreCase(request.getParameter("adv_medical")));
            ad.setProfessionalGuideAvailable("Yes".equalsIgnoreCase(request.getParameter("adv_guide")));
            ad.setInsuranceIncluded("Yes".equalsIgnoreCase(request.getParameter("adv_insurance")));
            ad.setEmergencyRescueSupport("Yes".equalsIgnoreCase(request.getParameter("adv_rescue")));
            ad.setFoodIncluded(request.getParameter("adv_food"));
            ad.setStayIncluded(request.getParameter("adv_stay"));
            ad.setTransportationIncluded("Yes".equalsIgnoreCase(request.getParameter("adv_transport")));
            ad.setPhotographyIncluded("Yes".equalsIgnoreCase(request.getParameter("adv_photography")));
            
            adventureDetailsRepository.save(ad);
        }

        if ("Trekking".equalsIgnoreCase(category)) {
            TrekkingDetails td = new TrekkingDetails();
            td.setEvent(event);
            td.setTrekType(request.getParameter("trek_type"));
            td.setTrekDifficulty(request.getParameter("trek_difficulty"));
            
            try {
                if(request.getParameter("trek_distance") != null && !request.getParameter("trek_distance").isBlank())
                    td.setTrekDistance(Double.parseDouble(request.getParameter("trek_distance")));
            } catch(Exception e) {}
            
            td.setEstimatedDuration(request.getParameter("trek_duration"));
            td.setMaxElevation(request.getParameter("trek_elevation"));
            td.setTrailType(request.getParameter("trek_trailType"));
            td.setFitnessLevel(request.getParameter("trek_fitness"));
            
            try {
                if(request.getParameter("trek_minAge") != null && !request.getParameter("trek_minAge").isBlank())
                    td.setMinAge(Integer.parseInt(request.getParameter("trek_minAge")));
            } catch(Exception e) {}
            
            try {
                if(request.getParameter("trek_maxAge") != null && !request.getParameter("trek_maxAge").isBlank())
                    td.setMaxAge(Integer.parseInt(request.getParameter("trek_maxAge")));
            } catch(Exception e) {}
            
            td.setReportingPoint(request.getParameter("trek_reportingPoint"));
            td.setReportingTime(request.getParameter("trek_reportingTime"));
            td.setDepartureTime(request.getParameter("trek_departureTime"));
            td.setReturnTime(request.getParameter("trek_returnTime"));
            
            String[] inclusions = request.getParameterValues("trek_inclusions");
            if (inclusions != null && inclusions.length > 0) {
                td.setTrekInclusions(String.join(", ", inclusions));
            }
            
            td.setParticipantsMustCarry(request.getParameter("trek_mustCarry"));
            td.setMedicalCertificateRequired("Yes".equalsIgnoreCase(request.getParameter("trek_medical")));
            td.setEmergencyRescueSupport("Yes".equalsIgnoreCase(request.getParameter("trek_rescue")));
            td.setForestPermissionRequired("Yes".equalsIgnoreCase(request.getParameter("trek_forest")));
            td.setMobileNetworkAvailability(request.getParameter("trek_network"));
            td.setWashroomFacility("Yes".equalsIgnoreCase(request.getParameter("trek_washroom")));
            td.setDrinkingWaterAvailability("Yes".equalsIgnoreCase(request.getParameter("trek_drinkingWater")));
            td.setTrekSchedule(request.getParameter("trek_schedule"));
            
            trekkingDetailsRepository.save(td);
        }

        if ("Bike Riding".equalsIgnoreCase(category)) {
            BikeRidingDetails bd = new BikeRidingDetails();
            bd.setEvent(event);
            bd.setRideType(request.getParameter("bike_type"));
            
            String[] bikeTypes = request.getParameterValues("bike_allowed");
            if (bikeTypes != null && bikeTypes.length > 0) {
                bd.setBikeTypeAllowed(String.join(", ", bikeTypes));
            }
            
            bd.setMinEngineCapacity(request.getParameter("bike_engine"));
            bd.setRidingExperience(request.getParameter("bike_experience"));
            bd.setStartPoint(request.getParameter("bike_startPoint"));
            bd.setDestination(request.getParameter("bike_destination"));
            
            try {
                if(request.getParameter("bike_distance") != null && !request.getParameter("bike_distance").isBlank())
                    bd.setTotalDistance(Double.parseDouble(request.getParameter("bike_distance")));
            } catch(Exception e) {}
            
            bd.setEstimatedDuration(request.getParameter("bike_duration"));
            bd.setReportingTime(request.getParameter("bike_reportingTime"));
            bd.setRideStartTime(request.getParameter("bike_startTime"));
            bd.setEstimatedFinishTime(request.getParameter("bike_finishTime"));
            
            String[] safetyGears = request.getParameterValues("bike_safety");
            if (safetyGears != null && safetyGears.length > 0) {
                bd.setSafetyGearMandatory(String.join(", ", safetyGears));
            }
            
            String[] supports = request.getParameterValues("bike_support");
            if (supports != null && supports.length > 0) {
                bd.setSupportAvailable(String.join(", ", supports));
            }
            
            String[] inclusions = request.getParameterValues("bike_inclusions");
            if (inclusions != null && inclusions.length > 0) {
                bd.setRideInclusions(String.join(", ", inclusions));
            }
            
            bd.setRiderRequirements(request.getParameter("bike_requirements"));
            bd.setFuelPolicy(request.getParameter("bike_fuel"));
            bd.setRideDifficulty(request.getParameter("bike_difficulty"));
            bd.setRoadType(request.getParameter("bike_road"));
            
            bikeRidingDetailsRepository.save(bd);
        }

        return "redirect:/events/admin/manage?created=true";
    }

    @RequestMapping(value = "/admin/edit/{id}", method = RequestMethod.GET)
    public String showEditEventForm(@PathVariable Long id, Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";
        Event event = eventRepository.findById(id).orElse(null);
        if (event == null) return "redirect:/events/admin/manage";
        model.addAttribute("event", event);
        return "admin-create-event";
    }

    @RequestMapping(value = "/admin/edit/{id}", method = RequestMethod.POST)
    public String updateEvent(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam String category,
            @RequestParam String description,
            @RequestParam String dateTime,
            @RequestParam(required = false) String venue,
            @RequestParam String entryFeeType,
            @RequestParam(required = false) String price,
            @RequestParam Integer maxParticipants,
            @RequestParam(required = false, defaultValue = "Offline") String eventMode,
            @RequestParam(required = false) String meetingLink,
            @RequestParam String status,
            @RequestParam(required = false) MultipartFile imageFile,
            @RequestParam(required = false) String votingStartDate,
            @RequestParam(required = false) String votingEndDate,
            @RequestParam(required = false, defaultValue = "0") Integer totalRows,
            @RequestParam(required = false, defaultValue = "0") Integer seatsPerRow,
            @RequestParam(required = false, defaultValue = "Standard") String seatLayoutType,
            @RequestParam(required = false, defaultValue = "0") Integer vipSeatCount,
            @RequestParam(required = false, defaultValue = "0.0") Double vipPrice,
            @RequestParam(required = false, defaultValue = "0.0") Double regularPrice,
            @RequestParam(required = false, defaultValue = "false") boolean finalVotingEnabled,
            HttpSession session) {

        if (!isAdmin(session)) return "redirect:/login";
        Event event = eventRepository.findById(id).orElse(null);
        if (event == null) return "redirect:/events/admin/manage";

        event.setTitle(title);
        event.setCategory(category);
        event.setDescription(description);
        event.setVenue(venue);
        event.setEntryFeeType(entryFeeType);
        event.setMaxParticipants(maxParticipants);
        event.setStatus(status);
        event.setMeetingLink(meetingLink);
        event.setFinalVotingEnabled(finalVotingEnabled);

        if ("Free".equals(entryFeeType)) {
            event.setPrice("Free");
        } else {
            event.setPrice(price != null && !price.isBlank() ? "\u20b9" + price : "Paid");
        }

        if ("VOTING".equals(status)) {
            try {
                if (votingStartDate != null && !votingStartDate.isBlank())
                    event.setVotingStartDate(LocalDateTime.parse(votingStartDate));
                if (votingEndDate != null && !votingEndDate.isBlank())
                    event.setVotingEndDate(LocalDateTime.parse(votingEndDate));
            } catch (Exception ignored) {}
        }

        try {
            event.setDateTime(LocalDateTime.parse(dateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")));
        } catch (Exception ignored) {}

        // Handle File Upload for edit
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String originalFilename = imageFile.getOriginalFilename();
                String extension = "";
                if(originalFilename != null && originalFilename.contains(".")) {
                    extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                }
                String newFilename = UUID.randomUUID().toString() + extension;
                Path uploadDir = Paths.get("src/main/resources/static/uploads");
                if (!Files.exists(uploadDir)) {
                    Files.createDirectories(uploadDir);
                }
                Path filePath = uploadDir.resolve(newFilename);
                Files.copy(imageFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                event.setImageUrl("/uploads/" + newFilename);
            } catch (IOException e) {
                // Keep existing
            }
        }

        // Update Seat Grid Configuration
        boolean reGenGrid = false;
        if (!event.getTotalRows().equals(totalRows) || !event.getSeatsPerRow().equals(seatsPerRow) || 
            !event.getVipSeatCount().equals(vipSeatCount) || !event.getVipPrice().equals(vipPrice) || 
            !event.getRegularPrice().equals(regularPrice)) {
            reGenGrid = true;
        }

        event.setTotalRows(totalRows);
        event.setSeatsPerRow(seatsPerRow);
        event.setSeatLayoutType(seatLayoutType);
        event.setVipSeatCount(vipSeatCount);
        event.setVipPrice(vipPrice);
        event.setRegularPrice(regularPrice);

        // Update tiers for backward compatibility
        event.getSeatTiers().clear();
        if (vipPrice > 0 || vipSeatCount > 0) {
            EventSeatTier vipTier = new EventSeatTier();
            vipTier.setTierName("VIP");
            vipTier.setPrice(vipPrice);
            vipTier.setCapacity(vipSeatCount);
            vipTier.setEvent(event);
            event.getSeatTiers().add(vipTier);
        }
        
        EventSeatTier regTier = new EventSeatTier();
        regTier.setTierName("REGULAR");
        regTier.setPrice(regularPrice);
        regTier.setCapacity((totalRows * seatsPerRow) - vipSeatCount);
        regTier.setEvent(event);
        event.getSeatTiers().add(regTier);

        if (reGenGrid) {
            generateSeatGrid(event);
        }

        eventRepository.save(event);
        return "redirect:/events/admin/manage?updated=true";
    }

    @Transactional
    @RequestMapping(value = "/admin/delete/{id}", method = RequestMethod.POST)
    public String deleteEvent(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";
        
        Event event = eventRepository.findById(id).orElse(null);
        if (event != null) {
            // Manual cleanup to be 100% sure and avoid FK constraint errors in all environments
            List<EventRegistration> registrations = eventRegistrationRepository.findByEvent(event);
            if (!registrations.isEmpty()) {
                eventRegistrationRepository.deleteAll(registrations);
            }
            
            // Seats are cascaded in model, but manual is safer given the errors seen
            List<EventSeat> seats = eventSeatRepository.findByEvent(event);
            if (!seats.isEmpty()) {
                eventSeatRepository.deleteAll(seats);
            }
            
            // Explicitly clear voting logs so they don't block deletion (FK)
            userRepository.deleteFromVotedEvents(id);
            voteRepository.deleteByPollId(id);
            
            eventRepository.delete(event);
        }
        
        return "redirect:/events/admin/manage?deleted=true";
    }

    // ─────────────────────────────────────────────────────────
    //  ADMIN: Attendance Dashboard
    // ─────────────────────────────────────────────────────────
    @RequestMapping(value = "/admin/{id}/attendance", method = RequestMethod.GET)
    public String adminAttendancePage(@PathVariable Long id, Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";

        Event event = eventRepository.findById(id).orElse(null);
        if (event == null) return "redirect:/events/admin/manage";

        List<EventRegistration> registrations = eventRegistrationRepository.findByEvent(event);
        long attendedCount = eventRegistrationRepository.countByEventAndAttendanceMarked(event, true);

        model.addAttribute("event", event);
        model.addAttribute("registrations", registrations);
        model.addAttribute("attendedCount", attendedCount);
        model.addAttribute("totalCount", registrations.size());
        return "admin-attendance";
    }

    /** Mark event as ONGOING */
    @RequestMapping(value = "/admin/{id}/start", method = RequestMethod.POST)
    public String startEvent(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";
        Event event = eventRepository.findById(id).orElse(null);
        if (event != null) {
            event.setStatus("ONGOING");
            eventRepository.save(event);
        }
        return "redirect:/events/admin/" + id + "/attendance?started=true";
    }

    /** Mark event as COMPLETED and Assign XP/Results */
    @RequestMapping(value = "/admin/{id}/complete", method = RequestMethod.POST)
    public String completeEvent(
            @PathVariable Long id,
            @RequestParam(required = false) List<Long> winnerIds,
            @RequestParam(required = false) List<Long> runnerIds,
            @RequestParam(defaultValue = "100") int winnerPoints,
            @RequestParam(defaultValue = "50") int runnerPoints,
            @RequestParam(defaultValue = "10") int defaultPoints,
            HttpSession session,
            jakarta.servlet.http.HttpServletRequest request) {
        if (!isAdmin(session)) return "redirect:/login";

        Event event = eventRepository.findById(id).orElse(null);
        if (event == null) return "redirect:/events/admin/manage";

        event.setStatus("COMPLETED");
        eventRepository.save(event);

        List<EventRegistration> registrations = eventRegistrationRepository.findByEvent(event);

        // Find max public votes for fair 40% weighting algorithm
        int maxVotes = 0;
        for (EventRegistration r : registrations) {
            if (r.isAttendanceMarked() && r.getPublicVotes() != null && r.getPublicVotes() > maxVotes) {
                maxVotes = r.getPublicVotes();
            }
        }

        // Process results for all registrations
        for (EventRegistration reg : registrations) {
            String position = "Participant";
            int points = 0;

            if (reg.isAttendanceMarked()) {
                // Secret Voting Weight System
                String jScoreStr = request.getParameter("judgeScore_" + reg.getId());
                double jScore = 0.0;
                if (jScoreStr != null && !jScoreStr.isEmpty()) {
                    try { jScore = Double.parseDouble(jScoreStr); } catch (Exception ignored) {}
                }
                reg.setJudgeScore(jScore);
                
                int pVotes = reg.getPublicVotes() != null ? reg.getPublicVotes() : 0;
                double publicScore = maxVotes > 0 ? ((double) pVotes / maxVotes) * 100.0 : 0.0;
                double finalScore = (jScore * 0.5) + (publicScore * 0.5);
                reg.setFinalScore(finalScore);

                // Assign Rank
                if (winnerIds != null && winnerIds.contains(reg.getId())) {
                    position = "Winner";
                    points = winnerPoints;
                } else if (runnerIds != null && runnerIds.contains(reg.getId())) {
                    position = "Runner";
                    points = runnerPoints;
                } else {
                    points = defaultPoints; // Basic participation points
                }

                reg.setPosition(position);
                reg.setPointsEarned(points);

                // Update User XP & Coins
                User u = reg.getUser();
                if (u != null) {
                    u.setXp((u.getXp() == null ? 0 : u.getXp()) + points);
                    u.setLevel(calculateLevel(u.getXp()));
                    
                    // Zen Coins Award depending on position
                    if ("Winner".equals(position)) {
                        rewardService.awardWinner(u);
                    } else if ("Runner".equals(position)) {
                        rewardService.awardRunner(u);
                    }
                    
                    userRepository.save(u);
                }
            } else {
                reg.setPosition("Absent");
                reg.setPointsEarned(0);
            }
        }
        eventRegistrationRepository.saveAll(registrations);

        return "redirect:/events/admin/" + id + "/attendance?completed=true";
    }

    @RequestMapping(value = "/admin/{id}/nominate-finalists", method = RequestMethod.POST)
    public String nominateFinalists(
            @PathVariable Long id,
            @RequestParam(required = false) Long finalist1_id,
            @RequestParam(required = false) String finalist1_media,
            @RequestParam(required = false) String finalist1_desc,
            @RequestParam(required = false) Long finalist2_id,
            @RequestParam(required = false) String finalist2_media,
            @RequestParam(required = false) String finalist2_desc,
            @RequestParam(required = false) Long finalist3_id,
            @RequestParam(required = false) String finalist3_media,
            @RequestParam(required = false) String finalist3_desc,
            HttpSession session) {

        if (!isAdmin(session)) return "redirect:/login";

        Event event = eventRepository.findById(id).orElse(null);
        if (event == null) return "redirect:/events/admin/manage";

        // Set the event status to Voting
        event.setStatus("VOTING");
        // Auto set voting end date to 2 days from now if not set
        if (event.getVotingEndDate() == null) {
            event.setVotingEndDate(LocalDateTime.now().plusDays(2));
        }
        eventRepository.save(event);

        // Map finalists input
        Long[] fIds = {finalist1_id, finalist2_id, finalist3_id};
        String[] fMedias = {finalist1_media, finalist2_media, finalist3_media};
        String[] fDescs = {finalist1_desc, finalist2_desc, finalist3_desc};

        for (int i = 0; i < 3; i++) {
            if (fIds[i] != null) {
                EventRegistration reg = eventRegistrationRepository.findById(fIds[i]).orElse(null);
                if (reg != null) {
                    reg.setFinalist(true);
                    reg.setFinalistMediaUrl(fMedias[i]);
                    reg.setFinalistDescription(fDescs[i]);
                    eventRegistrationRepository.save(reg);
                }
            }
        }

        return "redirect:/events/admin/manage";
    }

    private String calculateLevel(int xp) {
        if (xp >= 1000) return "Platinum";
        if (xp >= 500) return "Gold";
        if (xp >= 200) return "Silver";
        if (xp >= 50) return "Bronze";
        return "Novice";
    }

    /** Mark attendance for a specific ticket ID (offline scan) */
    @RequestMapping(value = "/admin/{id}/attendance/mark", method = RequestMethod.POST)
    public String markAttendance(@PathVariable Long id,
                                  @RequestParam String ticketId,
                                  HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";

        EventRegistration reg = eventRegistrationRepository.findByTicketId(ticketId.trim().toUpperCase()).orElse(null);
        if (reg == null || !reg.getEvent().getId().equals(id)) {
            return "redirect:/events/admin/" + id + "/attendance?error=invalid";
        }
        if (reg.isAttendanceMarked()) {
            return "redirect:/events/admin/" + id + "/attendance?error=already";
        }
        reg.setAttendanceMarked(true);
        reg.setAttendedAt(LocalDateTime.now());
        eventRegistrationRepository.save(reg);
        
        secretRewardService.assignReward(reg);
        rewardService.awardAttendance(reg.getUser()); // award coins for attending 🪙
        return "redirect:/events/admin/" + id + "/attendance?marked=" + ticketId;
    }

    /** Mark ALL registered participants as attended */
    @RequestMapping(value = "/admin/{id}/attendance/mark-all", method = RequestMethod.POST)
    public String markAllAttendance(@PathVariable Long id, 
                                     @RequestParam(required = false) String next, 
                                     HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";
        Event event = eventRepository.findById(id).orElse(null);
        if (event != null) {
            List<EventRegistration> regs = eventRegistrationRepository.findByEvent(event);
            for (EventRegistration reg : regs) {
                if (!reg.isAttendanceMarked()) {
                    reg.setAttendanceMarked(true);
                    reg.setAttendedAt(LocalDateTime.now());
                    secretRewardService.assignReward(reg);
                    rewardService.awardAttendance(reg.getUser()); // Coins for attending 🪙
                }
            }
            eventRegistrationRepository.saveAll(regs);
        }
        String redirect = "redirect:/events/admin/" + id + "/attendance?markedAll=true";
        if ("declare".equals(next)) redirect += "&declare=true";
        return redirect;
    }

    // ─────────────────────────────────────────────────────────
    //  STUDENT: Join Online Event (auto-marks attendance)
    // ─────────────────────────────────────────────────────────
    @RequestMapping(value = "/{id}/join", method = RequestMethod.POST)
    public String joinOnlineEvent(@PathVariable Long id, HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/login";

        Event event = eventRepository.findById(id).orElse(null);
        if (event == null) return "redirect:/events";

        List<EventRegistration> regs = eventRegistrationRepository.findByEventAndUser(event, user);
        EventRegistration reg = regs.isEmpty() ? null : regs.get(0);
        if (reg != null && !reg.isAttendanceMarked()) {
            reg.setAttendanceMarked(true);
            reg.setAttendedAt(LocalDateTime.now());
            eventRegistrationRepository.save(reg);
            secretRewardService.assignReward(reg);
            rewardService.awardAttendance(user); // Zen Coins for joining 🪙
        }

        // Redirect to the meeting link
        String link = event.getMeetingLink();
        if (link != null && !link.isBlank()) {
            return "redirect:" + link;
        }
        return "redirect:/events/" + id + "?joined=true";
    }

    // ─────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────
    private String completeRegistration(Event event, User user, String paymentStatus,
                                         String fullName, String email, String phone,
                                         String college, String yearOfStudy, String selectedTier,
                                         List<Long> selectedSeatIds, Integer quantity, HttpSession session) {
        
        if (quantity == null || quantity < 1) quantity = 1;

        // Enforce seat limit in backend
        if (event.getMaxParticipants() != null && event.getMaxParticipants() > 0) {
            long currentCount = eventRegistrationRepository.countByEvent(event);
            if (currentCount + quantity > event.getMaxParticipants()) {
                return "redirect:/events/" + event.getId() + "?error=full";
            }
        }

        // Tier-specific check
        double basePrice = parsePrice(event.getPrice());
        if (selectedTier != null && !selectedTier.isBlank() && event.getSeatTiers() != null) {
            EventSeatTier selectedTierObj = null;
            for (EventSeatTier t : event.getSeatTiers()) {
                if (selectedTier.equalsIgnoreCase(t.getTierName())) {
                    selectedTierObj = t;
                    if (t.getPrice() != null) basePrice = t.getPrice();
                    break;
                }
            }
            
            if (selectedTierObj != null) {
                if (selectedTierObj.getRegisteredCount() + quantity > selectedTierObj.getCapacity()) {
                    return "redirect:/events/" + event.getId() + "?error=tier_full";
                }
                selectedTierObj.setRegisteredCount(selectedTierObj.getRegisteredCount() + quantity);
            }
        }

        // Individual Seats Check & Update
        if (selectedSeatIds != null && !selectedSeatIds.isEmpty()) {
            List<EventSeat> seatsToBook = eventSeatRepository.findAllById(selectedSeatIds);
            for (EventSeat seat : seatsToBook) {
                seat.setStatus("BOOKED");
                seat.setBookedByUser(user);
            }
            eventSeatRepository.saveAll(seatsToBook);
            quantity = selectedSeatIds.size(); // Ensure quantity matches seats selected
        }
        
        double totalPrice = basePrice * quantity;
        if (paymentStatus.contains("DISCOUNTED")) {
            totalPrice = totalPrice * 0.5;
        }

        EventRegistration reg = new EventRegistration();
        reg.setEvent(event);
        reg.setUser(user);
        reg.setPaymentStatus(paymentStatus);
        reg.setRegistrationStatus("REGISTERED");
        reg.setSelectedTier(selectedTier);
        reg.setTicketId(generateTicketId(event));
        reg.setFullName(fullName);
        reg.setEmail(email);
        reg.setPhone(phone);
        reg.setCollege(college);
        reg.setYearOfStudy(yearOfStudy);
        reg.setQuantity(quantity);
        reg.setTotalPrice(totalPrice);
        
        try {
            if (session.getAttribute("lp_age") != null) reg.setAge((Integer) session.getAttribute("lp_age"));
            if (session.getAttribute("lp_city") != null) reg.setCity((String) session.getAttribute("lp_city"));
            if (session.getAttribute("lp_emergencyContactName") != null) reg.setEmergencyContactName((String) session.getAttribute("lp_emergencyContactName"));
            if (session.getAttribute("lp_emergencyContactMobile") != null) reg.setEmergencyContactMobile((String) session.getAttribute("lp_emergencyContactMobile"));
            if (session.getAttribute("lp_dietaryPreference") != null) reg.setDietaryPreference((String) session.getAttribute("lp_dietaryPreference"));
            if (session.getAttribute("lp_specialRequests") != null) reg.setSpecialRequests((String) session.getAttribute("lp_specialRequests"));
            
            session.removeAttribute("lp_age");
            session.removeAttribute("lp_city");
            session.removeAttribute("lp_emergencyContactName");
            session.removeAttribute("lp_emergencyContactMobile");
            session.removeAttribute("lp_dietaryPreference");
            session.removeAttribute("lp_specialRequests");
        } catch (Exception e) {}
        
        eventRegistrationRepository.save(reg);
        eventRepository.save(event); // Save the incremented tier count
        rewardService.awardRegistration(user); // Award coins for registering 🍪
        return "redirect:/events/ticket/" + reg.getTicketId() + "?success=true";
    }

    private void generateSeatGrid(Event event) {
        if (event.getTotalRows() <= 0 || event.getSeatsPerRow() <= 0) return;

        // Clear existing seats if we're re-generating
        event.getSeats().clear();
        
        int vipCount = event.getVipSeatCount();
        int assignedVip = 0;
        
        for (int row = 0; row < event.getTotalRows(); row++) {
            String rowLabel = String.valueOf((char) ('A' + row));
            
            for (int seatNum = 1; seatNum <= event.getSeatsPerRow(); seatNum++) {
                EventSeat seat = new EventSeat();
                seat.setEvent(event);
                seat.setRowLabel(rowLabel);
                seat.setSeatNumber(seatNum);
                
                if (assignedVip < vipCount) {
                    seat.setSeatType("VIP");
                    seat.setPrice(event.getVipPrice());
                    assignedVip++;
                } else {
                    seat.setSeatType("REGULAR");
                    seat.setPrice(event.getRegularPrice());
                }
                
                seat.setStatus("AVAILABLE");
                event.getSeats().add(seat);
            }
        }
    }

    private Double parsePrice(String price) {
        if (price == null || price.equalsIgnoreCase("Free")) return 0.0;
        try {
            String clean = price.replaceAll("[^0-9.]", "");
            if (clean.isEmpty()) return 0.0;
            return Double.parseDouble(clean);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private String generateTicketId(Event event) {
        String prefix = "ZTX";
        String fullCat = event.getCategory() != null ? event.getCategory().toUpperCase() : "EV";
        String cat = fullCat.length() >= 2 ? fullCat.substring(0, 2) : (fullCat + "X").substring(0, 2);
        String uid = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return prefix + "-" + cat + "-" + uid;
    }



    private boolean isAdmin(HttpSession session) {
        Object authUser = httpServletRequest.getAttribute("authenticatedUser");
        if ("admin".equals(authUser)) return true;
        return "admin".equals(session.getAttribute("user"));
    }

    private String getDefaultImage(String category) {
        return switch (category != null ? category : "") {
            case "Gaming"   -> "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=800&auto=format&fit=crop";
            case "Talent"   -> "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=800&auto=format&fit=crop";
            case "Tech"     -> "https://images.unsplash.com/photo-1518770660439-4636190af475?w=800&auto=format&fit=crop";
            case "Sports"   -> "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?w=800&auto=format&fit=crop";
            case "Cultural" -> "https://images.unsplash.com/photo-1533174072545-7a4b6ad7a6c3?w=800&auto=format&fit=crop";
            default         -> "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=800&auto=format&fit=crop";
        };
    }
}
