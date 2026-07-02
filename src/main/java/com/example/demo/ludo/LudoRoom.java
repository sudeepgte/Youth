package com.example.demo.ludo;

import java.util.*;

public class LudoRoom {
    public String roomId;
    public List<LudoPlayer> players = new ArrayList<>();
    public int currentPlayerIndex = 0;
    public int diceValue = 1;
    public boolean diceRolled = false;
    public String status = "waiting";

    public LudoRoom(String roomId, String playerName) {
        this.roomId = roomId;
        this.players.add(new LudoPlayer(playerName, "RED", 0));
        // Initialize other slots as empty
        this.players.add(new LudoPlayer("", "BLUE", 1));
        this.players.add(new LudoPlayer("", "GREEN", 2));
        this.players.add(new LudoPlayer("", "YELLOW", 3));
    }

    public static class LudoPlayer {
        public String name;
        public String color;
        public int index;
        public int[] pieces = {-1, -1, -1, -1};

        public LudoPlayer(String name, String color, int index) {
            this.name = name;
            this.color = color;
            this.index = index;
        }
    }

    public Map<String, Object> toStateMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("roomId", roomId);
        map.put("players", players);
        map.put("currentPlayer", currentPlayerIndex);
        map.put("diceValue", diceValue);
        map.put("diceRolled", diceRolled);
        map.put("status", status);
        return map;
    }

    public void applyRoll(int val) {
        this.diceValue = val;
        this.diceRolled = true;
    }

    private static final int[] STARTS = {0, 13, 26, 39};
    private static final Set<Integer> SAFE_SQUARES = new HashSet<>(Arrays.asList(0, 8, 13, 21, 26, 34, 39, 47));

    public void applyMove(int playerIdx, int pieceIdx, int newPos) {
        players.get(playerIdx).pieces[pieceIdx] = newPos;
        
        boolean captured = false;
        if (newPos >= 0 && newPos < 52) {
            int globalIdx = (STARTS[playerIdx] + newPos) % 52;
            if (!SAFE_SQUARES.contains(globalIdx)) {
                for (int i = 0; i < players.size(); i++) {
                    if (i == playerIdx) continue;
                    LudoPlayer opponent = players.get(i);
                    for (int j = 0; j < 4; j++) {
                        if (opponent.pieces[j] >= 0 && opponent.pieces[j] < 52) {
                            int oppGlobalIdx = (STARTS[i] + opponent.pieces[j]) % 52;
                            if (oppGlobalIdx == globalIdx) {
                                opponent.pieces[j] = -1;
                                captured = true;
                            }
                        }
                    }
                }
            }
        }

        diceRolled = false;

        // Check if current player won
        boolean allHome = true;
        for (int p : players.get(playerIdx).pieces) {
            if (p < 57) {
                allHome = false;
                break;
            }
        }
        if (allHome) {
            this.status = "finished";
            return;
        }
        
        // Handle next turn if not a 6 and no capture
        if (diceValue != 6 && !captured) {
            nextTurn();
        }
    }

    private void nextTurn() {
        int original = currentPlayerIndex;
        do {
            currentPlayerIndex = (currentPlayerIndex + 1) % 4;
        } while (players.get(currentPlayerIndex).name.isEmpty() && currentPlayerIndex != original);
    }

    public void skipTurn() {
        diceRolled = false;
        nextTurn();
    }
}
