package com.example.demo.rps;

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
public class RpsWebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(RpsWebSocketController.class);

    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    private MultiplayerRoomService roomService;

    public RpsWebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping("/api/rps/create")
    @ResponseBody
    public Map<String, String> createRoom(@RequestBody Map<String, String> body) {
        String playerName = body.getOrDefault("playerName", "Player 1");
        String roomId = roomService.generateUniqueRoomId();
        RpsRoom room = new RpsRoom(roomId, playerName);
        roomService.registerRoom(roomId, "rps", room, playerName, 2);
        logger.info("RPS Room created: {} by player: {}", roomId, playerName);
        return Map.of("roomId", roomId, "playerNum", "1");
    }

    @PostMapping("/api/rps/join")
    @ResponseBody
    public Map<String, String> joinRoom(@RequestBody Map<String, String> body) {
        String rawRoomId = body.get("roomId");
        String playerName = body.getOrDefault("playerName", "Player 2");

        if (rawRoomId == null || rawRoomId.isBlank()) {
            return Map.of("error", "Room code is required");
        }

        RpsRoom room = roomService.getRoom(rawRoomId, RpsRoom.class);
        if (room == null) {
            logger.warn("RPS Join failed - Room not found for code: {}", rawRoomId);
            return Map.of("error", "Room not found");
        }

        // Allow Re-join by matching player names
        if (playerName.equalsIgnoreCase(room.player1)) {
            return Map.of("roomId", room.roomId, "playerNum", "1");
        }
        if (room.player2 != null && playerName.equalsIgnoreCase(room.player2)) {
            return Map.of("roomId", room.roomId, "playerNum", "2");
        }

        if (room.player2 != null && !room.player2.isBlank()) {
            return Map.of("error", "Room is full");
        }

        room.player2 = playerName;
        roomService.registerRoom(room.roomId, "rps", room, room.player1, 2);

        messagingTemplate.convertAndSend("/topic/rps/" + room.roomId, (Object) room.toHiddenStateMap());
        messagingTemplate.convertAndSend("/topic/rps/" + RoomCodeUtil.normalize(rawRoomId), (Object) room.toHiddenStateMap());
        
        logger.info("Player {} joined RPS Room: {}", playerName, room.roomId);
        return Map.of("roomId", room.roomId, "playerNum", "2");
    }

    @MessageMapping("/rps/{roomId}/start")
    public void startGame(@DestinationVariable String roomId, Map<String, Object> payload) {
        RpsRoom room = roomService.getRoom(roomId, RpsRoom.class);
        if (room == null) return;
        if (room.player1 != null && room.player2 != null) {
            if (payload != null && payload.containsKey("gameMode")) {
                room.gameMode = (String) payload.get("gameMode");
            }
            room.status = "active";
            broadcastState(room);
        }
    }

    @MessageMapping("/rps/{roomId}/leave")
    public void leaveGame(@DestinationVariable String roomId, Map<String, Object> payload) {
        RpsRoom room = roomService.getRoom(roomId, RpsRoom.class);
        if (room == null) return;
        // Do not immediately destroy room on beforeunload page reload
        room.status = "opponent_left";
        broadcastState(room);
    }

    @MessageMapping("/rps/{roomId}/choice")
    public void makeChoice(@DestinationVariable String roomId, Map<String, Object> payload) {
        RpsRoom room = roomService.getRoom(roomId, RpsRoom.class);
        if (room == null) return;

        Object rawNum = payload.get("playerNum");
        int playerNum = (rawNum instanceof Number) ? ((Number) rawNum).intValue() : Integer.parseInt(rawNum.toString());
        String choice = (String) payload.get("choice");
        
        String previousStatus = room.status;

        room.applyChoice(playerNum, choice);
        
        if (!"finished".equals(previousStatus) && "finished".equals(room.status)) {
            roomService.registerRoom(room.roomId, "rps", room, room.player1, 2);
        }
        
        broadcastState(room);
    }

    @MessageMapping("/rps/{roomId}/playAgain")
    public void playAgain(@DestinationVariable String roomId, Map<String, Object> payload) {
        RpsRoom room = roomService.getRoom(roomId, RpsRoom.class);
        if (room == null) return;
        
        Object rawNum = payload.get("playerNum");
        int playerNum = (rawNum instanceof Number) ? ((Number) rawNum).intValue() : Integer.parseInt(rawNum.toString());
        
        if (playerNum == 1) room.p1PlayAgain = true;
        if (playerNum == 2) room.p2PlayAgain = true;
        
        if (room.p1PlayAgain && room.p2PlayAgain) {
            room.resetMatch();
        }
        
        broadcastState(room);
    }

    @MessageMapping("/rps/{roomId}/nextRound")
    public void nextRound(@DestinationVariable String roomId) {
        RpsRoom room = roomService.getRoom(roomId, RpsRoom.class);
        if (room == null) return;
        room.nextRound();
        broadcastState(room);
    }

    @MessageMapping("/rps/{roomId}/chat")
    public void chat(@DestinationVariable String roomId, ChatMessage msg) {
        String norm = RoomCodeUtil.normalize(roomId);
        messagingTemplate.convertAndSend("/topic/rps/" + roomId + "/chat", msg);
        messagingTemplate.convertAndSend("/topic/rps/" + norm + "/chat", msg);
    }

    @MessageMapping("/rps/{roomId}/subscribe")
    @SendTo("/topic/rps/{roomId}")
    public Map<String, Object> subscribe(@DestinationVariable String roomId) {
        RpsRoom room = roomService.getRoom(roomId, RpsRoom.class);
        return room != null ? room.toHiddenStateMap() : Map.of("error", "Room not found");
    }

    private void broadcastState(RpsRoom room) {
        if (room == null) return;
        messagingTemplate.convertAndSend("/topic/rps/" + room.roomId, (Object) room.toHiddenStateMap());
        messagingTemplate.convertAndSend("/topic/rps/" + RoomCodeUtil.normalize(room.roomId), (Object) room.toHiddenStateMap());
    }
}
