package com.example.demo.rps;

import java.util.Map;

public class RpsRoom {
    public String roomId;
    public String player1;
    public String player2;
    public String p1Choice = "";
    public String p2Choice = "";
    public int p1Score = 0;
    public int p2Score = 0;
    public String status = "waiting";
    public String lastResult = "";
    
    // New Match state variables
    public int currentRound = 1;
    public int maxRounds = 10;
    public String gameMode = "best_of_10"; // best_of_10 or max_10
    public String matchWinner = ""; // p1, p2, or draw
    public boolean p1PlayAgain = false;
    public boolean p2PlayAgain = false;

    public RpsRoom(String roomId, String player1) {
        this.roomId = roomId;
        this.player1 = player1;
    }

    public void applyChoice(int playerNum, String choice) {
        if ("finished".equals(status)) return; // Prevent moves after match ends
        
        if (playerNum == 1) {
            p1Choice = choice;
        } else {
            p2Choice = choice;
        }

        // If both chosen, determine winner
        if (!p1Choice.isEmpty() && !p2Choice.isEmpty()) {
            calculateResult();
        }
    }

    private void calculateResult() {
        if (p1Choice.equals(p2Choice)) {
            lastResult = "draw";
        } else if (
            (p1Choice.equals("Rock") && p2Choice.equals("Scissors")) ||
            (p1Choice.equals("Paper") && p2Choice.equals("Rock")) ||
            (p1Choice.equals("Scissors") && p2Choice.equals("Paper"))
        ) {
            lastResult = "p1";
            p1Score++;
        } else {
            lastResult = "p2";
            p2Score++;
        }
        
        checkMatchCompletion();
    }
    
    private void checkMatchCompletion() {
        if ("best_of_10".equals(gameMode)) {
            if (p1Score == 6) {
                status = "finished";
                matchWinner = "p1";
            } else if (p2Score == 6) {
                status = "finished";
                matchWinner = "p2";
            } else if (currentRound >= maxRounds) {
                status = "finished";
                if (p1Score > p2Score) matchWinner = "p1";
                else if (p2Score > p1Score) matchWinner = "p2";
                else matchWinner = "draw";
            }
        } else if ("max_10".equals(gameMode)) {
            if (currentRound >= maxRounds) {
                status = "finished";
                if (p1Score > p2Score) matchWinner = "p1";
                else if (p2Score > p1Score) matchWinner = "p2";
                else matchWinner = "draw";
            }
        }
    }

    public void nextRound() {
        if ("finished".equals(status)) return;
        p1Choice = "";
        p2Choice = "";
        lastResult = "";
        currentRound++;
    }
    
    public void resetMatch() {
        p1Score = 0;
        p2Score = 0;
        currentRound = 1;
        matchWinner = "";
        p1PlayAgain = false;
        p2PlayAgain = false;
        p1Choice = "";
        p2Choice = "";
        lastResult = "";
        status = "active";
    }

    public Map<String, Object> toStateMap() {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("roomId", roomId);
        map.put("player1", player1 != null ? player1 : "");
        map.put("player2", player2 != null ? player2 : "");
        map.put("p1Choice", p1Choice);
        map.put("p2Choice", p2Choice);
        map.put("p1Score", p1Score);
        map.put("p2Score", p2Score);
        map.put("status", status);
        map.put("lastResult", lastResult);
        map.put("currentRound", currentRound);
        map.put("maxRounds", maxRounds);
        map.put("gameMode", gameMode);
        map.put("matchWinner", matchWinner);
        map.put("p1PlayAgain", p1PlayAgain);
        map.put("p2PlayAgain", p2PlayAgain);
        return map;
    }

    // Version of state that hides choices if only one player has chosen
    public Map<String, Object> toHiddenStateMap() {
        boolean bothChosen = !p1Choice.isEmpty() && !p2Choice.isEmpty();
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("roomId", roomId);
        map.put("player1", player1 != null ? player1 : "");
        map.put("player2", player2 != null ? player2 : "");
        map.put("p1Chosen", !p1Choice.isEmpty());
        map.put("p2Chosen", !p2Choice.isEmpty());
        map.put("p1Choice", bothChosen ? p1Choice : "");
        map.put("p2Choice", bothChosen ? p2Choice : "");
        map.put("p1Score", p1Score);
        map.put("p2Score", p2Score);
        map.put("status", status);
        map.put("lastResult", lastResult);
        map.put("currentRound", currentRound);
        map.put("maxRounds", maxRounds);
        map.put("gameMode", gameMode);
        map.put("matchWinner", matchWinner);
        map.put("p1PlayAgain", p1PlayAgain);
        map.put("p2PlayAgain", p2PlayAgain);
        return map;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"roomId\":\"").append(roomId != null ? roomId : "").append("\",");
        sb.append("\"player1\":\"").append(player1 != null ? player1 : "").append("\",");
        sb.append("\"player2\":\"").append(player2 != null ? player2 : "").append("\",");
        sb.append("\"p1Score\":").append(p1Score).append(",");
        sb.append("\"p2Score\":").append(p2Score).append(",");
        sb.append("\"status\":\"").append(status != null ? status : "").append("\",");
        sb.append("\"currentRound\":").append(currentRound).append(",");
        sb.append("\"maxRounds\":").append(maxRounds).append(",");
        sb.append("\"gameMode\":\"").append(gameMode != null ? gameMode : "").append("\",");
        sb.append("\"matchWinner\":\"").append(matchWinner != null ? matchWinner : "").append("\"");
        sb.append("}");
        return sb.toString();
    }
}
