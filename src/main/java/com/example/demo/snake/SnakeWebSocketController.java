package com.example.demo.snake;

import com.example.demo.service.MultiplayerRoomService;
import com.example.demo.util.RoomCodeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@Controller
public class SnakeWebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(SnakeWebSocketController.class);

    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    private MultiplayerRoomService roomService;

    public SnakeWebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping("/api/snake/create")
    @ResponseBody
    public Map<String, Object> createRoom(@RequestBody Map<String, String> body) {
        String playerName = body.getOrDefault("playerName", "Player 1");
        int maxPlayers = Integer.parseInt(body.getOrDefault("maxPlayers", "2"));
        String roomId = roomService.generateUniqueRoomId();
        
        SnakeRoom room = new SnakeRoom(roomId, playerName, maxPlayers);
        roomService.registerRoom(roomId, "snake", room, playerName, maxPlayers);
        
        logger.info("Snake Room created: {} by player: {}", roomId, playerName);
        Map<String, Object> resp = new HashMap<>(room.toStateMap());
        resp.put("playerIndex", 0);
        resp.put("roomId", roomId);
        return resp;
    }

    @PostMapping("/api/snake/join")
    @ResponseBody
    public Map<String, Object> joinRoom(@RequestBody Map<String, String> body) {
        String rawRoomId = body.get("roomId");
        String playerName = body.getOrDefault("playerName", "Player 2");

        if (rawRoomId == null || rawRoomId.isBlank()) {
            return Map.of("error", "Room code is required");
        }

        SnakeRoom room = roomService.getRoom(rawRoomId, SnakeRoom.class);
        if (room == null) {
            logger.warn("Snake Join failed - Room not found for code: {}", rawRoomId);
            return Map.of("error", "Room not found");
        }

        // Allow Re-join
        for (int i = 0; i < room.players.size(); i++) {
            if (room.players.get(i).equalsIgnoreCase(playerName)) {
                Map<String, Object> resp = new HashMap<>(room.toStateMap());
                resp.put("playerIndex", i);
                resp.put("roomId", room.roomId);
                return resp;
            }
        }

        if (room.players.size() >= room.maxPlayers) {
            return Map.of("error", "Room is full");
        }

        int playerIdx = room.players.size();
        room.players.add(playerName);
        roomService.registerRoom(room.roomId, "snake", room, room.players.get(0), room.maxPlayers);

        broadcastState(room);
        Map<String, Object> resp = new HashMap<>(room.toStateMap());
        resp.put("playerIndex", playerIdx);
        resp.put("roomId", room.roomId);
        return resp;
    }

    @MessageMapping("/snake/{roomId}/start")
    public void startGame(@DestinationVariable String roomId) {
        SnakeRoom room = roomService.getRoom(roomId, SnakeRoom.class);
        if (room == null) return;
        if (room.players.size() > 1) {
            room.status = "active";
            broadcastState(room);
        }
    }

    @MessageMapping("/snake/{roomId}/leave")
    public void leaveGame(@DestinationVariable String roomId) {
        SnakeRoom room = roomService.getRoom(roomId, SnakeRoom.class);
        if (room == null) return;
        room.status = "opponent_left";
        broadcastState(room);
    }

    // WebSocket: Roll Dice
    @MessageMapping("/snake/{roomId}/roll")
    public void rollDice(@DestinationVariable String roomId, SnakeRollMessage msg) {
        SnakeRoom room = roomService.getRoom(roomId, SnakeRoom.class);
        if (room == null) return;

        messagingTemplate.convertAndSend("/topic/snake/" + room.roomId + "/rollEvent", msg);
        messagingTemplate.convertAndSend("/topic/snake/" + RoomCodeUtil.normalize(roomId) + "/rollEvent", msg);

        room.applyRoll(msg.getSteps(), msg.getPlayerIndex());
        broadcastState(room);
    }

    @MessageMapping("/snake/{roomId}/subscribe")
    public void subscribe(@DestinationVariable String roomId) {
        SnakeRoom room = roomService.getRoom(roomId, SnakeRoom.class);
        if (room != null) {
            broadcastState(room);
        }
    }

    private void broadcastState(SnakeRoom room) {
        if (room == null) return;
        messagingTemplate.convertAndSend("/topic/snake/" + room.roomId, (Object) room.toStateMap());
        messagingTemplate.convertAndSend("/topic/snake/" + RoomCodeUtil.normalize(room.roomId), (Object) room.toStateMap());
    }
}
