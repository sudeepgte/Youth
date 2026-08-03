package com.example.demo.snake;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SnakeRoom {
    public String roomId;
    public List<String> players = new ArrayList<>();
    public int[] positions;
    public int turn = 0; // 0-based index
    public String status = "waiting"; // waiting, active, finished
    public int maxPlayers = 2;

    private static final Map<Integer, Integer> MAPS = new HashMap<>();
    static {
        // Ladders
        MAPS.put(2, 38); MAPS.put(7, 14); MAPS.put(8, 31); MAPS.put(15, 26);
        MAPS.put(21, 42); MAPS.put(28, 84); MAPS.put(36, 44); MAPS.put(51, 67);
        MAPS.put(71, 91); MAPS.put(78, 98);
        // Snakes
        MAPS.put(16, 6); MAPS.put(46, 25); MAPS.put(49, 11); MAPS.put(62, 19);
        MAPS.put(64, 60); MAPS.put(74, 53); MAPS.put(89, 68); MAPS.put(92, 88);
        MAPS.put(95, 75); MAPS.put(99, 80);
    }

    public SnakeRoom(String roomId, String hostName, int maxPlayers) {
        this.roomId = roomId;
        this.maxPlayers = maxPlayers;
        this.players.add(hostName);
        this.positions = new int[4]; // Max possible
        for(int i=0; i<4; i++) positions[i] = 1;
    }

    public void applyRoll(int steps, int playerIdx) {
        if (!status.equals("active") || turn != playerIdx) return;

        int targetPos = positions[playerIdx] + steps;
        if (targetPos <= 100) {
            if (MAPS.containsKey(targetPos)) targetPos = MAPS.get(targetPos);
            positions[playerIdx] = targetPos;
            if (targetPos == 100) {
                status = "finished";
                return;
            }
        }
        // Next Turn
        turn = (turn + 1) % players.size();
    }

    public Map<String, Object> toStateMap() {
        Map<String, Object> state = new HashMap<>();
        state.put("roomId", roomId);
        state.put("status", status);
        state.put("turn", turn);
        state.put("maxPlayers", maxPlayers);
        
        List<Map<String, Object>> playerList = new ArrayList<>();
        for(int i=0; i<players.size(); i++) {
            Map<String, Object> p = new HashMap<>();
            p.put("name", players.get(i));
            p.put("pos", positions[i]);
            playerList.add(p);
        }
        state.put("players", playerList);
        return state;
    }
}
