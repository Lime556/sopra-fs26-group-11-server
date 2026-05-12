package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class GameSyncDTO {
    private Long gameId;
    private Long gameVersion;
    private Integer currentTurnIndex;
    private String turnPhase;
    private String gamePhase;
    private Integer diceValue;
    private String diceRolledAt;
    private String tradeRequestedAt;
    private String latestTradeRequest;
    private Long currentPlayerId;
    private String currentPlayerName;
    private Boolean gameFinished;
    private Boolean currentPlayerMustDiscard;
    private Boolean robberMovedAfterSevenRoll;

    public Long getGameId() {
        return gameId;
    }

    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }

    public Long getGameVersion() {
        return gameVersion;
    }

    public void setGameVersion(Long gameVersion) {
        this.gameVersion = gameVersion;
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

    public Integer getDiceValue() {
        return diceValue;
    }

    public void setDiceValue(Integer diceValue) {
        this.diceValue = diceValue;
    }

    public String getDiceRolledAt() {
        return diceRolledAt;
    }

    public void setDiceRolledAt(String diceRolledAt) {
        this.diceRolledAt = diceRolledAt;
    }

    public String getTradeRequestedAt() {
        return tradeRequestedAt;
    }

    public void setTradeRequestedAt(String tradeRequestedAt) {
        this.tradeRequestedAt = tradeRequestedAt;
    }

    public String getLatestTradeRequest() {
        return latestTradeRequest;
    }

    public void setLatestTradeRequest(String latestTradeRequest) {
        this.latestTradeRequest = latestTradeRequest;
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

    public Boolean getCurrentPlayerMustDiscard() {
        return currentPlayerMustDiscard;
    }

    public void setCurrentPlayerMustDiscard(Boolean currentPlayerMustDiscard) {
        this.currentPlayerMustDiscard = currentPlayerMustDiscard;
    }

    public Boolean getRobberMovedAfterSevenRoll() {
        return robberMovedAfterSevenRoll;
    }

    public void setRobberMovedAfterSevenRoll(Boolean robberMovedAfterSevenRoll) {
        this.robberMovedAfterSevenRoll = robberMovedAfterSevenRoll;
    }
}