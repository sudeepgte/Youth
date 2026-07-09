package com.example.demo.snake;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class SnakeWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final Map<String, SnakeRoom> rooms = new ConcurrentHashMap<>();

    public SnakeWebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping("/api/snake/create")
    @ResponseBody
    public Map<String, Object> createRoom(@RequestBody Map<String, String> body) {
        String roomId = generateRoomId();
        String playerName = body.get("playerName");
        int maxPlayers = Integer.parseInt(body.getOrDefault("maxPlayers", "2"));
        
        SnakeRoom room = new SnakeRoom(roomId, playerName, maxPlayers);
        rooms.put(roomId, room);
        
        Map<String, Object> resp = new HashMap<>(room.toStateMap());
        resp.put("playerIndex", 0);
        return resp;
    }

    @PostMapping("/api/snake/join")
    @ResponseBody
    public Map<String, Object> joinRoom(@RequestBody Map<String, String> body) {
        String roomId = body.get("roomId").toUpperCase();
        String playerName = body.get("playerName");

        SnakeRoom room = rooms.get(roomId);
        if (room == null) return Map.of("error", "Room not found");

        // Allow Re-join
        for (int i = 0; i < room.players.size(); i++) {
            if (room.players.get(i).equals(playerName)) {
                Map<String, Object> resp = new HashMap<>(room.toStateMap());
                resp.put("playerIndex", i);
                return resp;
            }
        }

        if (room.players.size() >= room.maxPlayers) return Map.of("error", "Room is full");

        int playerIdx = room.players.size();
        room.players.add(playerName);
        
        // keep status as waiting

        messagingTemplate.convertAndSend("/topic/snake/" + roomId, (Object) room.toStateMap());
        Map<String, Object> resp = new HashMap<>(room.toStateMap());
        resp.put("playerIndex", playerIdx);
        return resp;
    }

    @MessageMapping("/snake/{roomId}/start")
    public void startGame(@DestinationVariable String roomId) {
        SnakeRoom room = rooms.get(roomId);
        if (room == null) return;
        if (room.players.size() > 1) {
            room.status = "active";
            messagingTemplate.convertAndSend("/topic/snake/" + roomId, (Object) room.toStateMap());
        }
    }

    // WebSocket: Roll Dice (Sends roll value, then updates state)
    @MessageMapping("/snake/{roomId}/roll")
    public void rollDice(@DestinationVariable String roomId, SnakeRollMessage msg) {
        SnakeRoom room = rooms.get(roomId);
        if (room == null) return;

        // Broadcast the roll animation event first
        messagingTemplate.convertAndSend("/topic/snake/" + roomId + "/rollEvent", msg);

        // Apply to room state
        room.applyRoll(msg.getSteps(), msg.getPlayerIndex());

        // Broadcast new state
        messagingTemplate.convertAndSend("/topic/snake/" + roomId, (Object) room.toStateMap());
    }

    @MessageMapping("/snake/{roomId}/subscribe")
    public void subscribe(@DestinationVariable String roomId) {
        SnakeRoom room = rooms.get(roomId);
        if (room != null) {
            messagingTemplate.convertAndSend("/topic/snake/" + roomId, (Object) room.toStateMap());
        }
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
