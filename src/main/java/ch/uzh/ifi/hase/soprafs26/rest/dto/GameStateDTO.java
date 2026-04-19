package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class GameStateDTO {
    
    private Long gameId;
    private Integer currentTurnIndex;
    private String turnPhase;
    private Integer diceValue;
    private Long currentPlayerId;
    private String currentPlayerName;
    private Boolean gameFinished;

    public GameStateDTO() {
    }

    public GameStateDTO(Long gameId, Integer currentTurnIndex, String turnPhase, Integer diceValue,
                        Long currentPlayerId, String currentPlayerName, Boolean gameFinished) {
        this.gameId = gameId;
        this.currentTurnIndex = currentTurnIndex;
        this.turnPhase = turnPhase;
        this.diceValue = diceValue;
        this.currentPlayerId = currentPlayerId;
        this.currentPlayerName = currentPlayerName;
        this.gameFinished = gameFinished;
    }

    public Long getGameId() {
        return gameId;
    }

    public void setGameId(Long gameId) {
        this.gameId = gameId;
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

    public Integer getDiceValue() {
        return diceValue;
    }

    public void setDiceValue(Integer diceValue) {
        this.diceValue = diceValue;
    }

    public Long getCurrentPlayerId() {
        return currentPlayerId;
    }

    public void setCurrentPlayerId(Long currentPlayerId) {
        this.currentPlayerId = currentPlayerId;
    }

    public String getCurrentPlayerName() {
        return currentPlayerName;
    }

    public void setCurrentPlayerName(String currentPlayerName) {
        this.currentPlayerName = currentPlayerName;
    }

    public Boolean getGameFinished() {
        return gameFinished;
    }

    public void setGameFinished(Boolean gameFinished) {
        this.gameFinished = gameFinished;
    }
}
