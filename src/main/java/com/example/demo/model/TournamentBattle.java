package com.example.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "tournament_battles")
public class TournamentBattle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tournament_id")
    private Tournament tournament;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "round_id")
    private TournamentRound round;

    private Integer matchNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "player1_id")
    private User player1;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "player2_id")
    private User player2;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "winner_id")
    private User winner;

    // WAITING, READY, LIVE, COMPLETED
    private String status = "WAITING";

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "next_match_id")
    private TournamentBattle nextBattle;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "battle_id")
    private Battle battle;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Tournament getTournament() { return tournament; }
    public void setTournament(Tournament tournament) { this.tournament = tournament; }

    public TournamentRound getRound() { return round; }
    public void setRound(TournamentRound round) { this.round = round; }

    public Integer getMatchNumber() { return matchNumber; }
    public void setMatchNumber(Integer matchNumber) { this.matchNumber = matchNumber; }

    public User getPlayer1() { return player1; }
    public void setPlayer1(User player1) { this.player1 = player1; }

    public User getPlayer2() { return player2; }
    public void setPlayer2(User player2) { this.player2 = player2; }

    public User getWinner() { return winner; }
    public void setWinner(User winner) { this.winner = winner; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public TournamentBattle getNextBattle() { return nextBattle; }
    public void setNextBattle(TournamentBattle nextBattle) { this.nextBattle = nextBattle; }

    public Battle getBattle() { return battle; }
    public void setBattle(Battle battle) { this.battle = battle; }
}
