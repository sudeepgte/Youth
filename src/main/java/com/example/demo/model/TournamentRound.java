package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tournament_rounds")
public class TournamentRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tournament_id")
    private Tournament tournament;

    private Integer roundNumber;
    
    private String name; // e.g., "Round of 16", "Quarter Final"

    @JsonIgnore
    @OneToMany(mappedBy = "round", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TournamentBattle> battles = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Tournament getTournament() { return tournament; }
    public void setTournament(Tournament tournament) { this.tournament = tournament; }

    public Integer getRoundNumber() { return roundNumber; }
    public void setRoundNumber(Integer roundNumber) { this.roundNumber = roundNumber; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<TournamentBattle> getBattles() { return battles; }
    public void setBattles(List<TournamentBattle> battles) { this.battles = battles; }
}
