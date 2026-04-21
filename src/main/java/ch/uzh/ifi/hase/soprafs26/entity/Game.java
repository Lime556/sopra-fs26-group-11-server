package ch.uzh.ifi.hase.soprafs26.entity;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "Game")
public class Game{

	@Id
	@GeneratedValue
	private Long id;

    @Convert(converter = PlayerListJsonConverter.class)
    @Column(columnDefinition = "CLOB")
    private List<Player> players;

    @Convert(converter = BoardJsonConverter.class)
    @Column(columnDefinition = "CLOB")
    private Board board;

    @Column
    private Integer currentTurnIndex;

    @Column
    private String turnPhase;

    @Transient
    private Player currentPlayer;

    @Transient
    private Robber robber;

    @Transient
    private Dice dice;

    @Column
    private Integer diceValue;

    @Column
    private Integer robberTileIndex;

    @Column
    private Integer developmentKnightRemaining;

    @Column
    private Integer developmentVictoryPointRemaining;

    @Column
    private Integer developmentRoadBuildingRemaining;

    @Column
    private Integer developmentYearOfPlentyRemaining;

    @Column
    private Integer developmentMonopolyRemaining;

    @Transient
    private DevelopmentDeck developmentDeck;

    @Transient
    private Player longestRoad;

    @Transient
    private Player largestArmy;

    @Column
    private Integer targetVictoryPoints;

    @Column
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime finishedAt;

    @Column
    private Long winnerPlayerId;

    @Transient
    private Player winner;

    @Transient
    private List<String> eventLog;

    @Convert(converter = StringListJsonConverter.class)
    @Column(columnDefinition = "CLOB")
    private List<String> chatMessages;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

    public List<Player> getPlayers() {
        return players;
    }

    public void setPlayers(List<Player> players) {
        this.players = players;
    }

    public Board getBoard() {
        return board;
    }

    public void setBoard(Board board) {
        this.board = board;
    }

    public Integer getCurrentTurnIndex() {
        return currentTurnIndex;
    }

    public void setCurrentTurnIndex(Integer currentTurnIndex) {
        this.currentTurnIndex = currentTurnIndex;
    }

    public String getTurnPhase() {
        return turnPhase != null ? turnPhase : TurnPhase.ROLL_DICE.toString();
    }

    public void setTurnPhase(String turnPhase) {
        this.turnPhase = turnPhase;
    }

    public void setTurnPhase(TurnPhase turnPhase) {
        this.turnPhase = turnPhase != null ? turnPhase.toString() : TurnPhase.ROLL_DICE.toString();
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public void setCurrentPlayer(Player currentPlayer) {
        this.currentPlayer = currentPlayer;
    }

    public Robber getRobber() {
        return robber;
    }

    public void setRobber(Robber robber) {
        this.robber = robber;
    }

    public Dice getDice() {
        return dice;
    }

    public void setDice(Dice dice) {
        this.dice = dice;
    }

    public Integer getDiceValue() {
        return diceValue;
    }

    public void setDiceValue(Integer diceValue) {
        this.diceValue = diceValue;
    }

    public Integer getRobberTileIndex() {
        return robberTileIndex;
    }

    public void setRobberTileIndex(Integer robberTileIndex) {
        this.robberTileIndex = robberTileIndex;
    }

    public Integer getDevelopmentKnightRemaining() {
        return developmentKnightRemaining;
    }

    public void setDevelopmentKnightRemaining(Integer developmentKnightRemaining) {
        this.developmentKnightRemaining = developmentKnightRemaining;
    }

    public Integer getDevelopmentVictoryPointRemaining() {
        return developmentVictoryPointRemaining;
    }

    public void setDevelopmentVictoryPointRemaining(Integer developmentVictoryPointRemaining) {
        this.developmentVictoryPointRemaining = developmentVictoryPointRemaining;
    }

    public Integer getDevelopmentRoadBuildingRemaining() {
        return developmentRoadBuildingRemaining;
    }

    public void setDevelopmentRoadBuildingRemaining(Integer developmentRoadBuildingRemaining) {
        this.developmentRoadBuildingRemaining = developmentRoadBuildingRemaining;
    }

    public Integer getDevelopmentYearOfPlentyRemaining() {
        return developmentYearOfPlentyRemaining;
    }

    public void setDevelopmentYearOfPlentyRemaining(Integer developmentYearOfPlentyRemaining) {
        this.developmentYearOfPlentyRemaining = developmentYearOfPlentyRemaining;
    }

    public Integer getDevelopmentMonopolyRemaining() {
        return developmentMonopolyRemaining;
    }

    public void setDevelopmentMonopolyRemaining(Integer developmentMonopolyRemaining) {
        this.developmentMonopolyRemaining = developmentMonopolyRemaining;
    }

    public DevelopmentDeck getDevelopmentDeck() {
        return developmentDeck;
    }

    public void setDevelopmentDeck(DevelopmentDeck developmentDeck) {
        this.developmentDeck = developmentDeck;
    }

    public Player getLongestRoad() {
        return longestRoad;
    }

    public void setLongestRoad(Player longestRoad) {
        this.longestRoad = longestRoad;
    }

    public Player getLargestArmy() {
        return largestArmy;
    }

    public void setLargestArmy(Player largestArmy) {
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

    public Long getWinnerPlayerId() {
        return winnerPlayerId;
    }

    public void setWinnerPlayerId(Long winnerPlayerId) {
        this.winnerPlayerId = winnerPlayerId;
    }

    public Player getWinner() {
        if (winner == null && winnerPlayerId != null && players != null) {
            winner = players.stream()
                    .filter(player -> player != null && winnerPlayerId.equals(player.getId()))
                    .findFirst()
                    .orElse(null);
        }
        return winner;
    }

    public void setWinner(Player winner) {
        this.winner = winner;
        this.winnerPlayerId = winner == null ? null : winner.getId();
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
