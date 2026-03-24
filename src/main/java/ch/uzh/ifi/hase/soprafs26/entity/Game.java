package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Game")
public class Game{

	@Id
	@GeneratedValue
	private Long id;

    @Players
    private List<Player> players;

    @Board
    private Board board;

    @CurrentTurnIndex
    private Integer currentTurnIndex;

    @CurrentPlayer
    private Player currentPlayer;

    @Robber
    private Robber robber;

    @Dice
    private Dice dice;

    @DiceValue
    private Integer diceValue;

    @DevelopmentDeck
    private DevelopmentDeck developmentDeck;

    @LongestRoad
    private Player longestRoad;

    @LargestArmy
    private Player largestArmy;

    @TargetVictoryPoints
    private Integer targetVictoryPoints;

    @StartedAt
    private LocalDateTime startedAt;

    @FinishedAt
    private LocalDateTime finishedAt;

    @Winner
    private Player winner;

    @EventLog
    private List<String> eventLog;

    @ChatMessages
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

    public Player getWinner() {
        return winner;
    }

    public void setWinner(Player winner) {
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
