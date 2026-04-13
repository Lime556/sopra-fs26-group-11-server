package ch.uzh.ifi.hase.soprafs26.entity;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "Player")
public class Player implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    private Integer victoryPoints;

    private Integer settlementPoints;

    private Integer cityPoints;

    private Integer developmentCardVictoryPoints;

    private Boolean hasLongestRoad;

    private Boolean hasLargestArmy;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getVictoryPoints() {
        return victoryPoints;
    }

    public void setVictoryPoints(Integer victoryPoints) {
        this.victoryPoints = victoryPoints;
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
