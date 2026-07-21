package com.example.demo.ludo;

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
public class LudoWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final Map<String, LudoRoom> rooms = new ConcurrentHashMap<>();

    public LudoWebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping("/api/ludo/create")
    @ResponseBody
    public Map<String, Object> createRoom(@RequestBody Map<String, String> body) {
        String roomId = generateRoomId();
        LudoRoom room = new LudoRoom(roomId, body.get("playerName"));
        rooms.put(roomId, room);
        Map<String, Object> resp = new HashMap<>(room.toStateMap());
        resp.put("playerIndex", 0);
        return resp;
    }

    @PostMapping("/api/ludo/join")
    @ResponseBody
    public Map<String, Object> joinRoom(@RequestBody Map<String, String> body) {
        String roomId = body.get("roomId").toUpperCase();
        String playerName = body.get("playerName");

        LudoRoom room = rooms.get(roomId);
        if (room == null) return Map.of("error", "Room not found");

        // Allow Re-join
        for (int i = 0; i < 4; i++) {
            if (room.players.get(i).name.equals(playerName)) {
                Map<String, Object> resp = new HashMap<>(room.toStateMap());
                resp.put("playerIndex", i);
                return resp;
            }
        }
        
        // Find first empty slot
        int slot = -1;
        for (int i = 0; i < 4; i++) {
            if (room.players.get(i).name.isEmpty()) {
                slot = i;
                break;
            }
        }
        
        if (slot == -1) return Map.of("error", "Room is full");

        room.players.get(slot).name = playerName;
        // keep status as waiting

        messagingTemplate.convertAndSend("/topic/ludo/" + roomId, (Object) room.toStateMap());
        Map<String, Object> resp = new HashMap<>(room.toStateMap());
        resp.put("playerIndex", slot);
        return resp;
    }

    @MessageMapping("/ludo/{roomId}/start")
    public void startGame(@DestinationVariable String roomId) {
        LudoRoom room = rooms.get(roomId);
        if (room == null) return;
        
        long count = room.players.stream().filter(p -> !p.name.isEmpty()).count();
        if (count > 1) {
            room.status = "active";
            room.lastTurnStartTime = System.currentTimeMillis();
            messagingTemplate.convertAndSend("/topic/ludo/" + roomId, (Object) room.toStateMap());
        }
    }

    @MessageMapping("/ludo/{roomId}/leave")
    public void leaveGame(@DestinationVariable String roomId) {
        LudoRoom room = rooms.get(roomId);
        if (room == null) return;
        room.status = "opponent_left";
        messagingTemplate.convertAndSend("/topic/ludo/" + roomId, (Object) room.toStateMap());
        rooms.remove(roomId);
    }

    @MessageMapping("/ludo/{roomId}/roll")
    public void rollDice(@DestinationVariable String roomId, Map<String, Object> payload) {
        LudoRoom room = rooms.get(roomId);
        if (room == null) return;

        int val = (int) payload.get("val");
        room.applyRoll(val);
        
        messagingTemplate.convertAndSend("/topic/ludo/" + roomId + "/roll", (Object) payload);
        messagingTemplate.convertAndSend("/topic/ludo/" + roomId, (Object) room.toStateMap());
    }

    @MessageMapping("/ludo/{roomId}/move")
    public void movePiece(@DestinationVariable String roomId, Map<String, Object> payload) {
        LudoRoom room = rooms.get(roomId);
        if (room == null) return;

        int pIdx = (int) payload.get("playerIndex");
        int pcIdx = (int) payload.get("pieceIndex");
        int newPos = (int) payload.get("newPos");

        room.applyMove(pIdx, pcIdx, newPos);
        messagingTemplate.convertAndSend("/topic/ludo/" + roomId, (Object) room.toStateMap());
    }

    @MessageMapping("/ludo/{roomId}/skip")
    public void skipTurn(@DestinationVariable String roomId, Map<String, Object> payload) {
        LudoRoom room = rooms.get(roomId);
        if (room == null) return;
        if (payload != null && payload.containsKey("playerIndex")) {
            int targetPlayer = ((Number) payload.get("playerIndex")).intValue();
            if (room.currentPlayerIndex != targetPlayer) {
                return; // Ignore duplicate or late skip requests
            }
        }
        room.skipTurn();
        messagingTemplate.convertAndSend("/topic/ludo/" + roomId, (Object) room.toStateMap());
    }

    @MessageMapping("/ludo/{roomId}/chat")
    public void chat(@DestinationVariable String roomId, ChatMessage msg) {
        messagingTemplate.convertAndSend("/topic/ludo/" + roomId + "/chat", msg);
    }

    @MessageMapping("/ludo/{roomId}/subscribe")
    @SendTo("/topic/ludo/{roomId}")
    public Map<String, Object> subscribe(@DestinationVariable String roomId) {
        LudoRoom room = rooms.get(roomId);
        return room != null ? room.toStateMap() : Map.of("error", "Room not found");
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
