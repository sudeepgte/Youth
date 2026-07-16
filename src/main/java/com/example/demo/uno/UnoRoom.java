package com.example.demo.uno;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class UnoRoom {
    public String roomId;
    public List<UnoPlayer> players = new ArrayList<>();
    public List<UnoCard> deck = new ArrayList<>();
    public List<UnoCard> discardPile = new ArrayList<>();
    public int currentPlayerIndex = 0;
    public int direction = 1; // 1 or -1
    public String currentColor = "";
    public String status = "waiting";
    public String lastMessage = "Waiting for players...";
    public long turnStartedAt = System.currentTimeMillis();

    private static final AtomicInteger cardIdGenerator = new AtomicInteger(0);

    public UnoRoom(String roomId, String playerName) {
        this.roomId = roomId;
        addPlayer(playerName);
        initializeDeck();
    }

    public void addPlayer(String name) {
        players.add(new UnoPlayer(name, players.size()));
    }

    private void initializeDeck() {
        String[] colors = {"red", "green", "blue", "yellow"};
        String[] values = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "Skip", "Reverse", "Draw Two"};
        String[] wilds = {"Wild", "Wild Draw Four"};

        for (String c : colors) {
            for (String v : values) {
                int count = v.equals("0") ? 1 : 2;
                for (int i = 0; i < count; i++) {
                    deck.add(new UnoCard(cardIdGenerator.getAndIncrement(), c, v));
                }
            }
        }
        for (String w : wilds) {
            for (int i = 0; i < 4; i++) {
                deck.add(new UnoCard(cardIdGenerator.getAndIncrement(), "wild", w));
            }
        }
        Collections.shuffle(deck);
    }

    public void startGame() {
        status = "active";
        for (UnoPlayer p : players) {
            for (int i = 0; i < 7; i++) {
                p.hand.add(deck.remove(0));
            }
        }
        // Initial discard
        UnoCard startCard = deck.remove(0);
        while (startCard.color.equals("wild")) {
            deck.add(startCard);
            Collections.shuffle(deck);
            startCard = deck.remove(0);
        }
        discardPile.add(startCard);
        currentColor = startCard.color;
        lastMessage = "Game started! Player 1's turn.";
        turnStartedAt = System.currentTimeMillis();
    }

    public void playCard(int playerIdx, int cardId, String chosenColor) {
        UnoPlayer p = players.get(playerIdx);
        UnoCard card = p.hand.stream().filter(c -> c.id == cardId).findFirst().orElse(null);
        if (card == null) return;

        p.hand.remove(card);
        discardPile.add(card);
        currentColor = card.color.equals("wild") ? chosenColor : card.color;

        // Apply effects
        boolean skipNext = false;
        if (card.value.equals("Skip")) {
            skipNext = true;
        } else if (card.value.equals("Reverse")) {
            if (players.size() == 2) skipNext = true;
            else direction *= -1;
        } else if (card.value.equals("Draw Two")) {
            int next = getNextPlayerIndex();
            drawCardsForPlayer(next, 2);
            skipNext = true;
        } else if (card.value.equals("Wild Draw Four")) {
            int next = getNextPlayerIndex();
            drawCardsForPlayer(next, 4);
            skipNext = true;
        }

        if (p.hand.isEmpty()) {
            status = "finished";
            lastMessage = p.name + " wins!";
        } else {
            currentPlayerIndex = getNextPlayerIndex();
            if (skipNext) {
                currentPlayerIndex = getNextPlayerIndex();
            }
            if (p.hand.size() == 1) {
                lastMessage = p.name + " yelled UNO! It's " + players.get(currentPlayerIndex).name + "'s turn.";
            } else {
                lastMessage = players.get(currentPlayerIndex).name + "'s turn.";
            }
            turnStartedAt = System.currentTimeMillis();
        }
    }

    public void drawCard(int playerIdx) {
        UnoPlayer p = players.get(playerIdx);
        if (deck.isEmpty()) {
            UnoCard top = discardPile.remove(discardPile.size() - 1);
            deck.addAll(discardPile);
            Collections.shuffle(deck);
            discardPile.clear();
            discardPile.add(top);
        }
        if (!deck.isEmpty()) {
            p.hand.add(deck.remove(0));
        }
        currentPlayerIndex = getNextPlayerIndex();
        lastMessage = players.get(currentPlayerIndex).name + "'s turn.";
        turnStartedAt = System.currentTimeMillis();
    }

    private int getNextPlayerIndex() {
        int next = (currentPlayerIndex + direction) % players.size();
        if (next < 0) next += players.size();
        return next;
    }

    private void drawCardsForPlayer(int pIdx, int count) {
        for (int i = 0; i < count; i++) {
            if (deck.isEmpty()) break;
            players.get(pIdx).hand.add(deck.remove(0));
        }
    }

    public Map<String, Object> toStateMap(int forPlayerIdx) {
        Map<String, Object> map = new HashMap<>();
        map.put("roomId", roomId);
        map.put("status", status);
        map.put("currentPlayer", currentPlayerIndex);
        map.put("currentColor", currentColor);
        map.put("topCard", discardPile.isEmpty() ? null : discardPile.get(discardPile.size() - 1));
        map.put("lastMessage", lastMessage);
        map.put("direction", direction);
        map.put("turnStartedAt", turnStartedAt);
        
        List<Map<String, Object>> playersList = new ArrayList<>();
        for (int i = 0; i < players.size(); i++) {
            UnoPlayer p = players.get(i);
            Map<String, Object> pMap = new HashMap<>();
            pMap.put("name", p.name);
            pMap.put("cardCount", p.hand.size());
            if (i == forPlayerIdx) {
                pMap.put("hand", p.hand);
            }
            playersList.add(pMap);
        }
        map.put("players", playersList);
        return map;
    }

    public static class UnoPlayer {
        public String name;
        public int index;
        public List<UnoCard> hand = new ArrayList<>();

        public UnoPlayer(String name, int index) {
            this.name = name;
            this.index = index;
        }
    }
}
