package com.example.demo.music.room;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class MusicRoomWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MusicRoomRepository roomRepository;
    private final MusicRoomService roomService;
    private final MusicRoomSubmissionRepository submissionRepository;
    private final UserRepository userRepository;

    public MusicRoomWebSocketController(
            SimpMessagingTemplate messagingTemplate,
            MusicRoomRepository roomRepository,
            MusicRoomService roomService,
            MusicRoomSubmissionRepository submissionRepository,
            UserRepository userRepository) {
        this.messagingTemplate = messagingTemplate;
        this.roomRepository = roomRepository;
        this.roomService = roomService;
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
    }

    public void broadcastRoomEvent(String code, Map<String, Object> payload) {
        messagingTemplate.convertAndSend("/topic/music-room/" + code, MessageBuilder.withPayload(payload).build());
    }

    private void broadcastState(MusicRoom room) {
        if (room == null) return;
        broadcastRoomEvent(room.getCode(), buildRoomState(room));
    }

    private Map<String, Object> buildRoomState(MusicRoom room) {
        Map<String, Object> state = new HashMap<>();
        state.put("type", "state");
        state.put("code", room.getCode());
        state.put("name", room.getName());
        state.put("phase", room.getPhase());
        state.put("status", room.getPhase());
        state.put("active", room.isActive());
        state.put("submissionsLocked", room.isSubmissionsLocked());
        state.put("votingLocked", room.isVotingLocked());
        state.put("hostId", room.getHost() != null ? room.getHost().getId() : null);
        state.put("hostUsername", room.getHost() != null ? room.getHost().getUsername() : null);
        if (room.getCountdownEndsAt() != null) {
            state.put("countdownEndsAt", room.getCountdownEndsAt().toString());
        }
        if (room.getWinnerSubmission() != null) {
            state.put("winnerSubmissionId", room.getWinnerSubmission().getId());
        }

        Map<Long, Long> voteCounts = roomService.voteCounts(room);
        List<Map<String, Object>> submissions = new ArrayList<>();
        for (MusicRoomSubmission s : roomService.listSubmissions(room)) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", s.getId());
            row.put("trackId", s.getTrack() != null ? s.getTrack().getId() : null);
            row.put("trackTitle", s.getTrack() != null ? s.getTrack().getTitle() : "Track");
            row.put("artist", s.getTrack() != null ? s.getTrack().getArtistName() : null);
            row.put("submittedBy", s.getSubmittedBy() != null ? s.getSubmittedBy().getUsername() : null);
            row.put("votes", voteCounts.getOrDefault(s.getId(), 0L));
            submissions.add(row);
        }
        state.put("submissions", submissions);
        return state;
    }

    private User resolveUser(Principal principal) {
        if (principal == null || principal.getName() == null) return null;
        try {
            Long userId = Long.parseLong(principal.getName());
            return userRepository.findById(userId).orElse(null);
        } catch (NumberFormatException e) {
            return userRepository.findByUsername(principal.getName());
        }
    }

    private MusicRoom requireRoom(String code) {
        return roomRepository.findByCode(code.trim().toUpperCase()).orElse(null);
    }

    private boolean isHost(MusicRoom room, User user) {
        return room != null && user != null && room.getHost() != null
                && room.getHost().getId().equals(user.getId());
    }

    @MessageMapping("/music-room/{code}/subscribe")
    @Transactional(readOnly = true)
    public void subscribe(@DestinationVariable String code, Principal principal) {
        MusicRoom room = requireRoom(code);
        if (room == null) {
            broadcastRoomEvent(code.trim().toUpperCase(), Map.of("type", "error", "message", "Room not found"));
            return;
        }
        broadcastState(room);
    }

    @MessageMapping("/music-room/{code}/vote")
    @Transactional
    public void vote(@DestinationVariable String code, @Payload Map<String, Object> payload, Principal principal) {
        User user = resolveUser(principal);
        MusicRoom room = requireRoom(code);
        if (user == null || room == null) return;
        if (room.isVotingLocked() || "SUBMIT".equalsIgnoreCase(room.getPhase()) || "ENDED".equalsIgnoreCase(room.getPhase())) {
            return;
        }
        Object rawId = payload.get("submissionId");
        if (rawId == null) return;
        Long submissionId = Long.parseLong(rawId.toString());
        MusicRoomSubmission submission = submissionRepository.findById(submissionId).orElse(null);
        if (submission == null || submission.getRoom() == null || !submission.getRoom().getId().equals(room.getId())) {
            return;
        }
        if (roomService.vote(room, submission, user)) {
            broadcastState(room);
        }
    }

    @MessageMapping("/music-room/{code}/lock-submissions")
    @Transactional
    public void lockSubmissions(@DestinationVariable String code, @Payload Map<String, Object> payload, Principal principal) {
        User user = resolveUser(principal);
        MusicRoom room = requireRoom(code);
        if (!isHost(room, user)) return;
        boolean locked = payload.get("locked") == null || Boolean.parseBoolean(payload.get("locked").toString());
        room.setSubmissionsLocked(locked);
        roomRepository.save(room);
        broadcastState(room);
    }

    @MessageMapping("/music-room/{code}/lock-voting")
    @Transactional
    public void lockVoting(@DestinationVariable String code, @Payload Map<String, Object> payload, Principal principal) {
        User user = resolveUser(principal);
        MusicRoom room = requireRoom(code);
        if (!isHost(room, user)) return;
        boolean locked = payload.get("locked") == null || Boolean.parseBoolean(payload.get("locked").toString());
        room.setVotingLocked(locked);
        roomRepository.save(room);
        broadcastState(room);
    }

    @MessageMapping("/music-room/{code}/start-voting")
    @Transactional
    public void startVoting(@DestinationVariable String code, @Payload Map<String, Object> payload, Principal principal) {
        User user = resolveUser(principal);
        MusicRoom room = requireRoom(code);
        if (!isHost(room, user)) return;
        int seconds = 300;
        if (payload != null && payload.get("seconds") != null) {
            try {
                seconds = Integer.parseInt(payload.get("seconds").toString());
            } catch (NumberFormatException ignored) {}
        }
        int safe = Math.max(30, Math.min(seconds, 1800));
        room.setPhase("VOTE");
        room.setSubmissionsLocked(true);
        room.setVotingLocked(false);
        room.setCountdownEndsAt(LocalDateTime.now().plusSeconds(safe));
        roomRepository.save(room);
        broadcastState(room);
    }

    @MessageMapping("/music-room/{code}/end-room")
    @Transactional
    public void endRoom(@DestinationVariable String code, Principal principal) {
        User user = resolveUser(principal);
        MusicRoom room = requireRoom(code);
        if (!isHost(room, user)) return;
        room.setPhase("ENDED");
        room.setVotingLocked(true);
        room.setActive(false);
        roomRepository.save(room);
        broadcastState(room);
    }

    @MessageMapping("/music-room/{code}/declare-winner")
    @Transactional
    public void declareWinner(@DestinationVariable String code, @Payload Map<String, Object> payload, Principal principal) {
        User user = resolveUser(principal);
        MusicRoom room = requireRoom(code);
        if (!isHost(room, user) || payload == null || payload.get("submissionId") == null) return;
        Long submissionId = Long.parseLong(payload.get("submissionId").toString());
        MusicRoomSubmission submission = submissionRepository.findById(submissionId).orElse(null);
        if (submission == null || submission.getRoom() == null || !submission.getRoom().getId().equals(room.getId())) {
            return;
        }
        room.setWinnerSubmission(submission);
        room.setPhase("ENDED");
        room.setVotingLocked(true);
        room.setActive(false);
        roomRepository.save(room);
        broadcastState(room);
    }
}
