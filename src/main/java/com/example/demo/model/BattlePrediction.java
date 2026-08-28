package com.example.demo.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "battle_predictions")
public class BattlePrediction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "battle_id", nullable = false)
    private Battle battle;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bettor_id", nullable = false)
    private User bettor;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "predicted_winner_id", nullable = false)
    private User predictedWinner;
    @Column(nullable = false)
    private Double amount;
    private LocalDateTime createdAt = LocalDateTime.now();
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Battle getBattle() { return battle; }
    public void setBattle(Battle battle) { this.battle = battle; }
    public User getBettor() { return bettor; }
    public void setBettor(User bettor) { this.bettor = bettor; }
    public User getPredictedWinner() { return predictedWinner; }
    public void setPredictedWinner(User predictedWinner) { this.predictedWinner = predictedWinner; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}