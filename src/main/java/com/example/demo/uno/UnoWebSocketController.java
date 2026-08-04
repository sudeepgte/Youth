package com.example.demo.uno;

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
public class UnoWebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(UnoWebSocketController.class);

    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    private MultiplayerRoomService roomService;

    public UnoWebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping("/api/uno/create")
    @ResponseBody
    public Map<String, Object> createRoom(@RequestBody Map<String, String> body) {
        String playerName = body.getOrDefault("playerName", "Player 1");
        String roomId = roomService.generateUniqueRoomId();
        UnoRoom room = new UnoRoom(roomId, playerName);
        roomService.registerRoom(roomId, "uno", room, playerName, 4);

        logger.info("Uno Room created: {} by player: {}", roomId, playerName);
        Map<String, Object> resp = new HashMap<>(room.toStateMap(0));
        resp.put("playerIndex", 0);
        resp.put("roomId", roomId);
        return resp;
    }

    @PostMapping("/api/uno/join")
    @ResponseBody
    public Map<String, Object> joinRoom(@RequestBody Map<String, String> body) {
        String rawRoomId = body.get("roomId");
        String playerName = body.getOrDefault("playerName", "Player 2");

        if (rawRoomId == null || rawRoomId.isBlank()) {
            return Map.of("error", "Room code is required");
        }

        UnoRoom room = roomService.getRoom(rawRoomId, UnoRoom.class);
        if (room == null) {
            logger.warn("Uno Join failed - Room not found for code: {}", rawRoomId);
            return Map.of("error", "Room not found");
        }

        // Allow Re-join
        for (int i = 0; i < room.players.size(); i++) {
            if (room.players.get(i).name.equalsIgnoreCase(playerName)) {
                Map<String, Object> resp = new HashMap<>(room.toStateMap(i));
                resp.put("playerIndex", i);
                resp.put("roomId", room.roomId);
                return resp;
            }
        }

        if (room.players.size() >= 4) {
            return Map.of("error", "Room is full");
        }

        int playerIndex = room.players.size();
        room.addPlayer(playerName);
        roomService.registerRoom(room.roomId, "uno", room, room.players.get(0).name, 4);

        broadcastState(room);
        Map<String, Object> resp = new HashMap<>(room.toStateMap(playerIndex));
        resp.put("playerIndex", playerIndex);
        resp.put("roomId", room.roomId);
        return resp;
    }

    @MessageMapping("/uno/{roomId}/start")
    public void startGame(@DestinationVariable String roomId) {
        UnoRoom room = roomService.getRoom(roomId, UnoRoom.class);
        if (room == null) return;
        if (room.players.size() > 1 && room.status.equals("waiting")) {
            room.startGame();
            broadcastState(room);
        }
    }

    @MessageMapping("/uno/{roomId}/restart")
    public void restartGame(@DestinationVariable String roomId) {
        UnoRoom room = roomService.getRoom(roomId, UnoRoom.class);
        if (room == null) return;
        if (room.status.equals("finished")) {
            room.resetGame();
            broadcastState(room);
        }
    }

    @MessageMapping("/uno/{roomId}/leave")
    public void leaveGame(@DestinationVariable String roomId) {
        UnoRoom room = roomService.getRoom(roomId, UnoRoom.class);
        if (room == null) return;
        room.status = "opponent_left";
        broadcastState(room);
    }

    @MessageMapping("/uno/{roomId}/play")
    public void playCard(@DestinationVariable String roomId, Map<String, Object> payload) {
        UnoRoom room = roomService.getRoom(roomId, UnoRoom.class);
        if (room == null) return;

        int pIdx = (int) payload.get("playerIndex");
        int cardId = (int) payload.get("cardId");
        String chosenColor = (String) payload.get("chosenColor");

        room.playCard(pIdx, cardId, chosenColor);
        broadcastState(room);
    }

    @MessageMapping("/uno/{roomId}/draw")
    public void drawCard(@DestinationVariable String roomId, Map<String, Object> payload) {
        UnoRoom room = roomService.getRoom(roomId, UnoRoom.class);
        if (room == null) return;

        int pIdx = (int) payload.get("playerIndex");
        room.drawCard(pIdx);
        broadcastState(room);
    }

    @MessageMapping("/uno/{roomId}/call-uno")
    public void callUno(@DestinationVariable String roomId, Map<String, Object> payload) {
        UnoRoom room = roomService.getRoom(roomId, UnoRoom.class);
        if (room == null) return;

        int pIdx = (int) payload.get("playerIndex");
        room.callUno(pIdx);
        broadcastState(room);
    }

    @MessageMapping("/uno/{roomId}/skip")
    public void skipTurn(@DestinationVariable String roomId, Map<String, Object> payload) {
        UnoRoom room = roomService.getRoom(roomId, UnoRoom.class);
        if (room == null) return;

        int pIdx = (int) payload.get("playerIndex");
        room.skipTurn(pIdx);
        broadcastState(room);
    }

    @MessageMapping("/uno/{roomId}/catch-uno")
    public void catchUno(@DestinationVariable String roomId, Map<String, Object> payload) {
        UnoRoom room = roomService.getRoom(roomId, UnoRoom.class);
        if (room == null) return;

        int catcherIdx = (int) payload.get("catcherIndex");
        int targetIdx = (int) payload.get("targetIndex");
        room.catchUno(catcherIdx, targetIdx);
        broadcastState(room);
    }

    @MessageMapping("/uno/{roomId}/subscribe")
    public void subscribe(@DestinationVariable String roomId) {
        UnoRoom room = roomService.getRoom(roomId, UnoRoom.class);
        if (room != null) broadcastState(room);
    }

    private void broadcastState(UnoRoom room) {
        if (room == null) return;
        String norm = RoomCodeUtil.normalize(room.roomId);
        for (int i = 0; i < room.players.size(); i++) {
            messagingTemplate.convertAndSend("/topic/uno/" + room.roomId + "/player/" + i, (Object) room.toStateMap(i));
            messagingTemplate.convertAndSend("/topic/uno/" + norm + "/player/" + i, (Object) room.toStateMap(i));
        }
        messagingTemplate.convertAndSend("/topic/uno/" + room.roomId, (Object) Map.of("status", room.status, "playerCount", room.players.size()));
        messagingTemplate.convertAndSend("/topic/uno/" + norm, (Object) Map.of("status", room.status, "playerCount", room.players.size()));
    }
}
