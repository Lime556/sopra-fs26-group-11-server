package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.time.LocalDateTime;
import java.util.List;

public class GameHeartbeatDTO {
    private Long id;
    private Long gameVersion;
    private List<PlayerGetDTO> players;
    private Integer currentTurnIndex;
    private String turnPhase;
    private String gamePhase;
    private PlayerGetDTO winner;
    private LocalDateTime finishedAt;
    private Boolean gameFinished;
    private Boolean robberMovedAfterSevenRoll;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGameVersion() {
        return gameVersion;
    }

    public void setGameVersion(Long gameVersion) {
        this.gameVersion = gameVersion;
    }

    public List<PlayerGetDTO> getPlayers() {
        return players;
    }

    public void setPlayers(List<PlayerGetDTO> players) {
        this.players = players;
    }

    public Integer getCurrentTurnIndex() {
        return currentTurnIndex;
    }

    public void setCurrentTurnIndex(Integer currentTurnIndex) {
        this.currentTurnIndex = currentTurnIndex;
    }

    public String getTurnPhase() {
        return turnPhase;
    }

    public void setTurnPhase(String turnPhase) {
        this.turnPhase = turnPhase;
    }

    public String getGamePhase() {
        return gamePhase;
    }

    public void setGamePhase(String gamePhase) {
        this.gamePhase = gamePhase;
    }

    public PlayerGetDTO getWinner() {
        return winner;
    }

    public void setWinner(PlayerGetDTO winner) {
        this.winner = winner;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public Boolean getGameFinished() {
        return gameFinished;
    }

    public void setGameFinished(Boolean gameFinished) {
        this.gameFinished = gameFinished;
    }

    public Boolean getRobberMovedAfterSevenRoll() {
        return robberMovedAfterSevenRoll;
    }

    public void setRobberMovedAfterSevenRoll(Boolean robberMovedAfterSevenRoll) {
        this.robberMovedAfterSevenRoll = robberMovedAfterSevenRoll;
    }
}
