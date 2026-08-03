package com.example.demo.chess;

import com.example.demo.service.MultiplayerRoomService;
import com.example.demo.util.RoomCodeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@Controller
public class ChessWebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(ChessWebSocketController.class);

    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    private MultiplayerRoomService roomService;

    public ChessWebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // REST: Create Room
    @PostMapping("/api/chess/create")
    @ResponseBody
    public Map<String, Object> createRoom(@RequestBody Map<String, String> body) {
        String playerName = body.getOrDefault("playerName", "Player 1");
        String roomId = roomService.generateUniqueRoomId();
        ChessRoom room = new ChessRoom(roomId, playerName);
        roomService.registerRoom(roomId, "chess", room, playerName, 2);

        logger.info("Chess Room created: {} by player: {}", roomId, playerName);
        Map<String, Object> resp = new HashMap<>(room.toStateMap());
        resp.put("color", "w");
        resp.put("roomId", roomId);
        return resp;
    }

    // REST: Join Room
    @PostMapping("/api/chess/join")
    @ResponseBody
    public Map<String, Object> joinRoom(@RequestBody Map<String, String> body) {
        String rawRoomId = body.get("roomId");
        String playerName = body.getOrDefault("playerName", "Player 2");

        if (rawRoomId == null || rawRoomId.isBlank()) {
            return Map.of("error", "Room code is required");
        }

        ChessRoom room = roomService.getRoom(rawRoomId, ChessRoom.class);
        if (room == null) {
            logger.warn("Chess Join failed - Room not found for code: {}", rawRoomId);
            return Map.of("error", "Room not found");
        }

        String color = "";
        if (playerName.equalsIgnoreCase(room.whitePlayer)) {
            color = "w";
        } else if (room.blackPlayer != null && playerName.equalsIgnoreCase(room.blackPlayer)) {
            color = "b";
        } else if (room.blackPlayer == null || room.blackPlayer.isBlank()) {
            room.blackPlayer = playerName;
            color = "b";
            roomService.registerRoom(room.whitePlayer != null ? room.whitePlayer : rawRoomId, "chess", room, room.whitePlayer, 2);
        } else {
            return Map.of("error", "Room is full");
        }

        broadcastState(room);

        Map<String, Object> resp = new HashMap<>(room.toStateMap());
        resp.put("color", color);
        resp.put("roomId", room.whitePlayer != null ? room.toStateMap().get("roomId") : rawRoomId);
        return resp;
    }

    // WebSocket: Start Game
    @MessageMapping("/chess/{roomId}/start")
    public void startGame(@DestinationVariable String roomId) {
        ChessRoom room = roomService.getRoom(roomId, ChessRoom.class);
        if (room == null) return;
        if (room.whitePlayer != null && room.blackPlayer != null) {
            room.status = "active";
            broadcastState(room);
        }
    }

    @MessageMapping("/chess/{roomId}/leave")
    public void leaveGame(@DestinationVariable String roomId) {
        ChessRoom room = roomService.getRoom(roomId, ChessRoom.class);
        if (room == null) return;
        room.status = "opponent_left";
        broadcastState(room);
    }

    // WebSocket: Make Move
    @MessageMapping("/chess/{roomId}/move")
    public void makeMove(@DestinationVariable String roomId, MoveMessage msg) {
        ChessRoom room = roomService.getRoom(roomId, ChessRoom.class);
        if (room == null) return;
        room.applyMove(msg);
        broadcastState(room);
    }

    // WebSocket: Chat
    @MessageMapping("/chess/{roomId}/chat")
    public void chat(@DestinationVariable String roomId, ChatMessage msg) {
        String norm = RoomCodeUtil.normalize(roomId);
        messagingTemplate.convertAndSend("/topic/chess/" + roomId + "/chat", (Object) msg);
        messagingTemplate.convertAndSend("/topic/chess/" + norm + "/chat", (Object) msg);
    }

    // WebSocket: Subscribe to room state
    @MessageMapping("/chess/{roomId}/subscribe")
    @SendTo("/topic/chess/{roomId}")
    public Map<String, Object> subscribe(@DestinationVariable String roomId) {
        ChessRoom room = roomService.getRoom(roomId, ChessRoom.class);
        return room != null ? room.toStateMap() : Map.of("error", "Room not found");
    }

    private void broadcastState(ChessRoom room) {
        if (room == null) return;
        messagingTemplate.convertAndSend("/topic/chess/" + room.toStateMap().get("roomId"), (Object) room.toStateMap());
        String norm = RoomCodeUtil.normalize(String.valueOf(room.toStateMap().get("roomId")));
        messagingTemplate.convertAndSend("/topic/chess/" + norm, (Object) room.toStateMap());
    }
}
