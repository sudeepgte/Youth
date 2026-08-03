package com.example.demo.ludo;

import com.example.demo.chess.ChatMessage;
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
public class LudoWebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(LudoWebSocketController.class);

    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    private MultiplayerRoomService roomService;

    public LudoWebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping("/api/ludo/create")
    @ResponseBody
    public Map<String, Object> createRoom(@RequestBody Map<String, String> body) {
        String playerName = body.getOrDefault("playerName", "Player 1");
        String roomId = roomService.generateUniqueRoomId();
        LudoRoom room = new LudoRoom(roomId, playerName);
        roomService.registerRoom(roomId, "ludo", room, playerName, 4);

        logger.info("Ludo Room created: {} by player: {}", roomId, playerName);
        Map<String, Object> resp = new HashMap<>(room.toStateMap());
        resp.put("playerIndex", 0);
        resp.put("roomId", roomId);
        return resp;
    }

    @PostMapping("/api/ludo/join")
    @ResponseBody
    public Map<String, Object> joinRoom(@RequestBody Map<String, String> body) {
        String rawRoomId = body.get("roomId");
        String playerName = body.getOrDefault("playerName", "Player 2");

        if (rawRoomId == null || rawRoomId.isBlank()) {
            return Map.of("error", "Room code is required");
        }

        LudoRoom room = roomService.getRoom(rawRoomId, LudoRoom.class);
        if (room == null) {
            logger.warn("Ludo Join failed - Room not found for code: {}", rawRoomId);
            return Map.of("error", "Room not found");
        }

        // Allow Re-join
        for (int i = 0; i < 4; i++) {
            if (room.players.get(i).name.equalsIgnoreCase(playerName)) {
                Map<String, Object> resp = new HashMap<>(room.toStateMap());
                resp.put("playerIndex", i);
                resp.put("roomId", room.roomId);
                return resp;
            }
        }
        // Find suitable empty slot
        int slot = -1;
        long activeCount = room.players.stream().filter(p -> !p.name.isEmpty()).count();
        if (activeCount == 1 && room.players.get(2).name.isEmpty()) {
            slot = 2; // Diagonal to player 0
        } else if (activeCount == 2 && room.players.get(1).name.isEmpty()) {
            slot = 1;
        } else if (activeCount == 3 && room.players.get(3).name.isEmpty()) {
            slot = 3;
        } else {
            for (int i = 0; i < 4; i++) {
                if (room.players.get(i).name.isEmpty()) {
                    slot = i;
                    break;
                }
            }
        }
        
        if (slot == -1) return Map.of("error", "Room is full");

        room.players.get(slot).name = playerName;
        roomService.registerRoom(room.roomId, "ludo", room, room.players.get(0).name, 4);

        broadcastState(room);
        Map<String, Object> resp = new HashMap<>(room.toStateMap());
        resp.put("playerIndex", slot);
        resp.put("roomId", room.roomId);
        return resp;
    }

    @MessageMapping("/ludo/{roomId}/start")
    public void startGame(@DestinationVariable String roomId) {
        LudoRoom room = roomService.getRoom(roomId, LudoRoom.class);
        if (room == null) return;
        
        long count = room.players.stream().filter(p -> !p.name.isEmpty()).count();
        if (count > 1) {
            room.status = "active";
            room.lastTurnStartTime = System.currentTimeMillis();
            broadcastState(room);
        }
    }

    @MessageMapping("/ludo/{roomId}/leave")
    public void leaveGame(@DestinationVariable String roomId) {
        LudoRoom room = roomService.getRoom(roomId, LudoRoom.class);
        if (room == null) return;
        room.status = "opponent_left";
        broadcastState(room);
    }

    @MessageMapping("/ludo/{roomId}/roll")
    public void rollDice(@DestinationVariable String roomId, Map<String, Object> payload) {
        LudoRoom room = roomService.getRoom(roomId, LudoRoom.class);
        if (room == null) return;

        int val = (int) payload.get("val");
        room.applyRoll(val);
        
        messagingTemplate.convertAndSend("/topic/ludo/" + room.roomId + "/roll", (Object) payload);
        messagingTemplate.convertAndSend("/topic/ludo/" + RoomCodeUtil.normalize(roomId) + "/roll", (Object) payload);
        broadcastState(room);
    }

    @MessageMapping("/ludo/{roomId}/move")
    public void movePiece(@DestinationVariable String roomId, Map<String, Object> payload) {
        LudoRoom room = roomService.getRoom(roomId, LudoRoom.class);
        if (room == null) return;

        int pIdx = (int) payload.get("playerIndex");
        int pcIdx = (int) payload.get("pieceIndex");
        int newPos = (int) payload.get("newPos");

        room.applyMove(pIdx, pcIdx, newPos);
        broadcastState(room);
    }

    @MessageMapping("/ludo/{roomId}/skip")
    public void skipTurn(@DestinationVariable String roomId, Map<String, Object> payload) {
        LudoRoom room = roomService.getRoom(roomId, LudoRoom.class);
        if (room == null) return;
        if (payload != null && payload.containsKey("playerIndex")) {
            int targetPlayer = ((Number) payload.get("playerIndex")).intValue();
            if (room.currentPlayerIndex != targetPlayer) {
                return;
            }
        }
        room.skipTurn();
        broadcastState(room);
    }

    @MessageMapping("/ludo/{roomId}/chat")
    public void chat(@DestinationVariable String roomId, ChatMessage msg) {
        String norm = RoomCodeUtil.normalize(roomId);
        messagingTemplate.convertAndSend("/topic/ludo/" + roomId + "/chat", msg);
        messagingTemplate.convertAndSend("/topic/ludo/" + norm + "/chat", msg);
    }

    @MessageMapping("/ludo/{roomId}/subscribe")
    @SendTo("/topic/ludo/{roomId}")
    public Map<String, Object> subscribe(@DestinationVariable String roomId) {
        LudoRoom room = roomService.getRoom(roomId, LudoRoom.class);
        return room != null ? room.toStateMap() : Map.of("error", "Room not found");
    }

    private void broadcastState(LudoRoom room) {
        if (room == null) return;
        messagingTemplate.convertAndSend("/topic/ludo/" + room.roomId, (Object) room.toStateMap());
        messagingTemplate.convertAndSend("/topic/ludo/" + RoomCodeUtil.normalize(room.roomId), (Object) room.toStateMap());
    }
}
