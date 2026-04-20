package ch.uzh.ifi.hase.soprafs26.entity;

import java.io.Serializable;

import jakarta.persistence.*;

@Entity
@Table(name = "players")
public class Player implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Long gameId;

    @Column(nullable = false)
    private String color;

    @Column(nullable = false)
    private int victoryPoints;
  
    private String name;

    private Integer settlementPoints;

    private Integer cityPoints;

    private Integer developmentCardVictoryPoints;

    private Boolean hasLongestRoad;

    private Boolean hasLargestArmy;

    private Integer wood;

    private Integer brick;

    private Integer wool;

    private Integer wheat;

    private Integer ore;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Long getGameId() {
        return gameId;
    }

    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public int getVictoryPoints() {
        return victoryPoints;
    }

    public void setVictoryPoints(int victoryPoints) {
        this.victoryPoints = victoryPoints;
    }
  
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getSettlementPoints() {
        return settlementPoints;
    }

    public void setSettlementPoints(Integer settlementPoints) {
        this.settlementPoints = settlementPoints;
    }

    public Integer getCityPoints() {
        return cityPoints;
    }

    public void setCityPoints(Integer cityPoints) {
        this.cityPoints = cityPoints;
    }

    public Integer getDevelopmentCardVictoryPoints() {
        return developmentCardVictoryPoints;
    }

    public void setDevelopmentCardVictoryPoints(Integer developmentCardVictoryPoints) {
        this.developmentCardVictoryPoints = developmentCardVictoryPoints;
    }

    public Boolean getHasLongestRoad() {
        return hasLongestRoad;
    }

    public void setHasLongestRoad(Boolean hasLongestRoad) {
        this.hasLongestRoad = hasLongestRoad;
    }

    public Boolean getHasLargestArmy() {
        return hasLargestArmy;
    }

    public void setHasLargestArmy(Boolean hasLargestArmy) {
        this.hasLargestArmy = hasLargestArmy;
    }

    public Integer getWood() {
        return wood;
    }

    public void setWood(Integer wood) {
        this.wood = wood;
    }

    public Integer getBrick() {
        return brick;
    }

    public void setBrick(Integer brick) {
        this.brick = brick;
    }

    public Integer getWool() {
        return wool;
    }

    public void setWool(Integer wool) {
        this.wool = wool;
    }

    public Integer getWheat() {
        return wheat;
    }

    public void setWheat(Integer wheat) {
        this.wheat = wheat;
    }

    public Integer getOre() {
        return ore;
    }

    public void setOre(Integer ore) {
        this.ore = ore;
    }

    public int calculateVictoryPoints() {
        int score = safeValue(settlementPoints)
                + safeValue(cityPoints)
                + safeValue(developmentCardVictoryPoints);

        if (Boolean.TRUE.equals(hasLongestRoad)) {
            score += 2;
        }
        if (Boolean.TRUE.equals(hasLargestArmy)) {
            score += 2;
        }

        return score;
    }

    public void recalculateVictoryPoints() {
        this.victoryPoints = calculateVictoryPoints();
    }

    private int safeValue(Integer value) {
        return value == null ? 0 : value;
    }
}
