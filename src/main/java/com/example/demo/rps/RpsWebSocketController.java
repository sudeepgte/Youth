package com.example.demo.rps;

import com.example.demo.chess.ChatMessage;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class RpsWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final Map<String, RpsRoom> rooms = new ConcurrentHashMap<>();

    public RpsWebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping("/api/rps/create")
    @ResponseBody
    public Map<String, String> createRoom(@RequestBody Map<String, String> body) {
        String roomId = generateRoomId();
        RpsRoom room = new RpsRoom(roomId, body.get("playerName"));
        rooms.put(roomId, room);
        return Map.of("roomId", roomId, "playerNum", "1");
    }

    @PostMapping("/api/rps/join")
    @ResponseBody
    public Map<String, String> joinRoom(@RequestBody Map<String, String> body) {
        String roomId = body.get("roomId").toUpperCase();
        String playerName = body.get("playerName");

        RpsRoom room = rooms.get(roomId);
        if (room == null) return Map.of("error", "Room not found");

        // Allow Re-join
        if (playerName.equals(room.player1)) {
            return Map.of("roomId", roomId, "playerNum", "1");
        }
        if (playerName.equals(room.player2)) {
            return Map.of("roomId", roomId, "playerNum", "2");
        }

        if (room.player2 != null) return Map.of("error", "Room is full");

        room.player2 = playerName;
        // Keep status as "waiting" so lobby is shown for both

        messagingTemplate.convertAndSend("/topic/rps/" + roomId, (Object) room.toHiddenStateMap());
        return Map.of("roomId", roomId, "playerNum", "2");
    }

    @MessageMapping("/rps/{roomId}/start")
    public void startGame(@DestinationVariable String roomId) {
        RpsRoom room = rooms.get(roomId);
        if (room == null) return;
        if (room.player1 != null && room.player2 != null) {
            room.status = "active";
            messagingTemplate.convertAndSend("/topic/rps/" + roomId, (Object) room.toHiddenStateMap());
        }
    }

    @MessageMapping("/rps/{roomId}/choice")
    public void makeChoice(@DestinationVariable String roomId, Map<String, Object> payload) {
        RpsRoom room = rooms.get(roomId);
        if (room == null) return;

        Object rawNum = payload.get("playerNum");
        int playerNum = (rawNum instanceof Number) ? ((Number) rawNum).intValue() : Integer.parseInt(rawNum.toString());
        String choice = (String) payload.get("choice");

        room.applyChoice(playerNum, choice);
        messagingTemplate.convertAndSend("/topic/rps/" + roomId, (Object) room.toHiddenStateMap());
    }

    @MessageMapping("/rps/{roomId}/nextRound")
    public void nextRound(@DestinationVariable String roomId) {
        RpsRoom room = rooms.get(roomId);
        if (room == null) return;
        room.nextRound();
        messagingTemplate.convertAndSend("/topic/rps/" + roomId, (Object) room.toHiddenStateMap());
    }

    @MessageMapping("/rps/{roomId}/chat")
    public void chat(@DestinationVariable String roomId, ChatMessage msg) {
        messagingTemplate.convertAndSend("/topic/rps/" + roomId + "/chat", msg);
    }

    @MessageMapping("/rps/{roomId}/subscribe")
    @SendTo("/topic/rps/{roomId}")
    public Map<String, Object> subscribe(@DestinationVariable String roomId) {
        RpsRoom room = rooms.get(roomId);
        return room != null ? room.toHiddenStateMap() : Map.of("error", "Room not found");
    }

    private String generateRoomId() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(6);
        Random rnd = new Random();
        for (int i = 0; i < 6; i++)
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }
}
