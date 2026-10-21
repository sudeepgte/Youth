package com.example.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "tournament_judge_scores")
public class TournamentJudgeScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_battle_id")
    private TournamentBattle tournamentBattle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id")
    private TournamentParticipant participant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "judge_id")
    private User judge;

    private Integer techniqueScore = 0;
    private Integer creativityScore = 0;
    private Integer performanceScore = 0;
    private Integer executionScore = 0;
    private Integer audienceImpactScore = 0;
    
    private Integer totalScore = 0;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public TournamentBattle getTournamentBattle() { return tournamentBattle; }
    public void setTournamentBattle(TournamentBattle tournamentBattle) { this.tournamentBattle = tournamentBattle; }

    public TournamentParticipant getParticipant() { return participant; }
    public void setParticipant(TournamentParticipant participant) { this.participant = participant; }

    public User getJudge() { return judge; }
    public void setJudge(User judge) { this.judge = judge; }

    public Integer getTechniqueScore() { return techniqueScore != null ? techniqueScore : 0; }
    public void setTechniqueScore(Integer techniqueScore) { this.techniqueScore = techniqueScore; this.updateTotal(); }

    public Integer getCreativityScore() { return creativityScore != null ? creativityScore : 0; }
    public void setCreativityScore(Integer creativityScore) { this.creativityScore = creativityScore; this.updateTotal(); }

    public Integer getPerformanceScore() { return performanceScore != null ? performanceScore : 0; }
    public void setPerformanceScore(Integer performanceScore) { this.performanceScore = performanceScore; this.updateTotal(); }

    public Integer getExecutionScore() { return executionScore != null ? executionScore : 0; }
    public void setExecutionScore(Integer executionScore) { this.executionScore = executionScore; this.updateTotal(); }

    public Integer getAudienceImpactScore() { return audienceImpactScore != null ? audienceImpactScore : 0; }
    public void setAudienceImpactScore(Integer audienceImpactScore) { this.audienceImpactScore = audienceImpactScore; this.updateTotal(); }

    public Integer getTotalScore() { return totalScore != null ? totalScore : 0; }
    public void setTotalScore(Integer totalScore) { this.totalScore = totalScore; }

    private void updateTotal() {
        this.totalScore = getTechniqueScore() + getCreativityScore() + getPerformanceScore() + getExecutionScore() + getAudienceImpactScore();
    }
}
