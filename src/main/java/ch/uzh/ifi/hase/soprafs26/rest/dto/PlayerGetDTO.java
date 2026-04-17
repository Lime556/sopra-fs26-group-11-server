package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.List;

public class PlayerGetDTO {
    private Long id;
    private String name;
    private Integer victoryPoints;
    private Integer settlementPoints;
    private Integer cityPoints;
    private Integer developmentCardVictoryPoints;
    private Boolean hasLongestRoad;
    private Boolean hasLargestArmy;
    private List<String> roadsOnEdges;
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

    public List<String> getRoadsOnEdges() {
        return roadsOnEdges;
    }

    public void setRoadsOnEdges(List<String> roadsOnEdges) {
        this.roadsOnEdges = roadsOnEdges;
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
}
