package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.time.LocalDateTime;
import java.util.List;

public class GameHistoryEntryDTO {

    private Long gameId;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private boolean won;
    private int playerVictoryPoints;
    private List<String> playerNames;

    public Long getGameId() {
        return gameId;
    }

    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public boolean isWon() {
        return won;
    }

    public void setWon(boolean won) {
        this.won = won;
    }

    public int getPlayerVictoryPoints() {
        return playerVictoryPoints;
    }

    public void setPlayerVictoryPoints(int playerVictoryPoints) {
        this.playerVictoryPoints = playerVictoryPoints;
    }

    public List<String> getPlayerNames() {
        return playerNames;
    }

    public void setPlayerNames(List<String> playerNames) {
        this.playerNames = playerNames;
    }
}
