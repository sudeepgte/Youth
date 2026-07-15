package com.example.demo.chess;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class ChessWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    // In-memory room store (replace with Redis/DB for production)
    private final Map<String, ChessRoom> rooms = new ConcurrentHashMap<>();

    public ChessWebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // REST: Create Room
    @PostMapping("/api/chess/create")
    @ResponseBody
    public Map<String, Object> createRoom(@RequestBody Map<String, String> body) {
        String roomId = generateRoomId();
        ChessRoom room = new ChessRoom(roomId, body.get("playerName"));
        rooms.put(roomId, room);
        Map<String, Object> resp = new HashMap<>(room.toStateMap());
        resp.put("color", "w");
        return resp;
    }

    // REST: Join Room
    @PostMapping("/api/chess/join")
    @ResponseBody
    public Map<String, Object> joinRoom(@RequestBody Map<String, String> body) {
        String roomId = body.get("roomId").toUpperCase();
        String playerName = body.get("playerName");

        ChessRoom room = rooms.get(roomId);
        if (room == null)
            return Map.of("error", "Room not found");

        String color = "";
        if (playerName.equals(room.whitePlayer)) {
            color = "w";
        } else if (playerName.equals(room.blackPlayer)) {
            color = "b";
        } else if (room.blackPlayer == null) {
            room.blackPlayer = playerName;
            // keep status as waiting
            color = "b";
        } else {
            return Map.of("error", "Room is full");
        }

        // Notify others
        messagingTemplate.convertAndSend("/topic/chess/" + roomId, (Object) room.toStateMap());
        
        Map<String, Object> resp = new HashMap<>(room.toStateMap());
        resp.put("color", color);
        return resp;
    }

    // WebSocket: Start Game
    @MessageMapping("/chess/{roomId}/start")
    public void startGame(@DestinationVariable String roomId) {
        ChessRoom room = rooms.get(roomId);
        if (room == null) return;
        if (room.whitePlayer != null && room.blackPlayer != null) {
            room.status = "active";
            messagingTemplate.convertAndSend("/topic/chess/" + roomId, (Object) room.toStateMap());
        }
    }

    @MessageMapping("/chess/{roomId}/leave")
    public void leaveGame(@DestinationVariable String roomId) {
        ChessRoom room = rooms.get(roomId);
        if (room == null) return;
        room.status = "opponent_left";
        messagingTemplate.convertAndSend("/topic/chess/" + roomId, (Object) room.toStateMap());
        rooms.remove(roomId);
    }

    // WebSocket: Make Move
    @MessageMapping("/chess/{roomId}/move")
    public void makeMove(@DestinationVariable String roomId, MoveMessage msg) {
        ChessRoom room = rooms.get(roomId);
        if (room == null)
            return;
        room.applyMove(msg);
        messagingTemplate.convertAndSend("/topic/chess/" + roomId, (Object) room.toStateMap());
    }

    // WebSocket: Chat
    @MessageMapping("/chess/{roomId}/chat")
    public void chat(@DestinationVariable String roomId, ChatMessage msg) {
        messagingTemplate.convertAndSend("/topic/chess/" + roomId + "/chat", (Object) msg);
    }

    // WebSocket: Subscribe to room state
    @MessageMapping("/chess/{roomId}/subscribe")
    @SendTo("/topic/chess/{roomId}")
    public Map<String, Object> subscribe(@DestinationVariable String roomId) {
        ChessRoom room = rooms.get(roomId);
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
