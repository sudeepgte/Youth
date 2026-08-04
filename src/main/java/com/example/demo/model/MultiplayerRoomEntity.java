package com.example.demo.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "multiplayer_rooms", indexes = {
    @Index(name = "idx_norm_room_id", columnList = "normalizedRoomId"),
    @Index(name = "idx_game_type", columnList = "gameType")
})
public class MultiplayerRoomEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String roomId;

    @Column(nullable = false, unique = true)
    private String normalizedRoomId;

    @Column(nullable = false)
    private String gameType; // rps, chess, snake, uno, ludo

    private String player1;
    private String player2;
    private Integer maxPlayers = 2;
    private String status = "waiting";

    @Column(columnDefinition = "LONGTEXT")
    private String roomDataJson;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    public MultiplayerRoomEntity() {}

    public MultiplayerRoomEntity(String roomId, String normalizedRoomId, String gameType, String player1, Integer maxPlayers) {
        this.roomId = roomId;
        this.normalizedRoomId = normalizedRoomId;
        this.gameType = gameType;
        this.player1 = player1;
        this.maxPlayers = maxPlayers != null ? maxPlayers : 2;
        this.status = "waiting";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getNormalizedRoomId() { return normalizedRoomId; }
    public void setNormalizedRoomId(String normalizedRoomId) { this.normalizedRoomId = normalizedRoomId; }

    public String getGameType() { return gameType; }
    public void setGameType(String gameType) { this.gameType = gameType; }

    public String getPlayer1() { return player1; }
    public void setPlayer1(String player1) { this.player1 = player1; }

    public String getPlayer2() { return player2; }
    public void setPlayer2(String player2) { this.player2 = player2; }

    public Integer getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(Integer maxPlayers) { this.maxPlayers = maxPlayers; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRoomDataJson() { return roomDataJson; }
    public void setRoomDataJson(String roomDataJson) { this.roomDataJson = roomDataJson; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
