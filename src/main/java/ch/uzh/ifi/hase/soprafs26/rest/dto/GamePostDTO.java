package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.time.LocalDateTime;
import java.util.List;

public class GamePostDTO {

	private Long id;
    private List<LobbyGetDTO> players;
    private board.BoardGetDTO board;
    private Integer currentTurnIndex;
    private PlayerGetDTO currentPlayer;
    private robber.RobberGetDTO robber;
    private dice.DiceGetDTO dice;
    private Integer diceValue;
    private developmentDeck.DevelopmentDeckGetDTO developmentDeck;
    private longestRoad.PlayerGetDTO longestRoad;
    private largestArmy.PlayerGetDTO largestArmy;
    private Integer targetVictoryPoints;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private PlayerGetDTO winner;
    private List<String> eventLog;
    private List<String> chatMessages;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<LobbyGetDTO> getPlayers() {
        return players;
    }

    public void setPlayers(List<LobbyGetDTO> players) {
        this.players = players;
    }

    public board.BoardGetDTO getBoard() {
        return board;
    }

    public void setBoard(board.BoardGetDTO board) {
        this.board = board;
    }

    public Integer getCurrentTurnIndex() {
        return currentTurnIndex;
    }

    public void setCurrentTurnIndex(Integer currentTurnIndex) {
        this.currentTurnIndex = currentTurnIndex;
    }

    public PlayerGetDTO getCurrentPlayer() {
        return currentPlayer;
    }

    public void setCurrentPlayer(PlayerGetDTO currentPlayer) {
        this.currentPlayer = currentPlayer;
    }

    public robber.RobberGetDTO getRobber() {
        return robber;
    }

    public void setRobber(robber.RobberGetDTO robber) {
        this.robber = robber;
    }

    public dice.DiceGetDTO getDice() {
        return dice;
    }

    public void setDice(dice.DiceGetDTO dice) {
        this.dice = dice;
    }

    public Integer getDiceValue() {
        return diceValue;
    }

    public void setDiceValue(Integer diceValue) {
        this.diceValue = diceValue;
    }

    public developmentDeck.DevelopmentDeckGetDTO getDevelopmentDeck() {
        return developmentDeck;
    }

    public void setDevelopmentDeck(developmentDeck.DevelopmentDeckGetDTO developmentDeck) {
        this.developmentDeck = developmentDeck;
    }

    public longestRoad.PlayerGetDTO getLongestRoad() {
        return longestRoad;
    }

    public void setLongestRoad(longestRoad.PlayerGetDTO longestRoad) {
        this.longestRoad = longestRoad;
    }

    public largestArmy.PlayerGetDTO getLargestArmy() {
        return largestArmy;
    }

    public void setLargestArmy(largestArmy.PlayerGetDTO largestArmy) {
        this.largestArmy = largestArmy;
    }

    public Integer getTargetVictoryPoints() {
        return targetVictoryPoints;
    }

    public void setTargetVictoryPoints(Integer targetVictoryPoints) {
        this.targetVictoryPoints = targetVictoryPoints;
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

    public PlayerGetDTO getWinner() {
        return winner;
    }

    public void setWinner(PlayerGetDTO winner) {
        this.winner = winner;
    }

    public List<String> getEventLog() {
        return eventLog;
    }

    public void setEventLog(List<String> eventLog) {
        this.eventLog = eventLog;
    }

    public List<String> getChatMessages() {
        return chatMessages;
    }

    public void setChatMessages(List<String> chatMessages) {
        this.chatMessages = chatMessages;
    }
}
