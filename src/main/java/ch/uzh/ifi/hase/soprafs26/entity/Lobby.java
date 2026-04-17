package ch.uzh.ifi.hase.soprafs26.entity;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;


@Entity
@Table(name = "lobbies")
public class Lobby implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int capacity;

    @Column
    private String password;

    @OneToMany(mappedBy = "lobby", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<LobbyParticipant> participants = new HashSet<>();

    @OneToOne
    @JoinColumn(name = "host_participant_id")
    private LobbyParticipant hostParticipant;

    @Column
    private Long gameId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getCapacity() {
        return capacity;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Set<LobbyParticipant> getParticipants() {
        return participants;
    }

    public void setParticipants(Set<LobbyParticipant> participants) {
        this.participants = participants;
    }

    @Transient
    public int getCurrentParticipants() {
        return participants == null ? 0 : participants.size();
    }

    public LobbyParticipant getHostParticipant() {
        return hostParticipant;
    }

    public void setHostParticipant(LobbyParticipant hostParticipant) {
        this.hostParticipant = hostParticipant;
    }

    public Long getGameId() {
        return gameId;
    }

    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }
}
