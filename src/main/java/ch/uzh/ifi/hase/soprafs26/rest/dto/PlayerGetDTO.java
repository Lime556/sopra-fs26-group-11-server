package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.List;
import java.util.Map;

public class PlayerGetDTO {

    private Long id;
    private String name;
    private int victoryPoints;
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
    private List<String> developmentCards;
    private Integer knightsPlayed;
    private Integer freeRoadBuildsRemaining;

    // New fields for building and road locations
    private List<Map<String, Integer>> settlementsOnCorners;
    private List<Map<String, Integer>> citiesOnCorners;
    private List<Map<String, Integer>> roadsOnEdges;

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

    public int getVictoryPoints() {
        return victoryPoints;
    }

    public void setVictoryPoints(int victoryPoints) {
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

    public List<String> getDevelopmentCards() {
        return developmentCards;
    }

    public void setDevelopmentCards(List<String> developmentCards) {
        this.developmentCards = developmentCards;
    }

    public Integer getKnightsPlayed() {
        return knightsPlayed;
    }

    public void setKnightsPlayed(Integer knightsPlayed) {
        this.knightsPlayed = knightsPlayed;
    }

    public Integer getFreeRoadBuildsRemaining() {
        return freeRoadBuildsRemaining;
    }

    public void setFreeRoadBuildsRemaining(Integer freeRoadBuildsRemaining) {
        this.freeRoadBuildsRemaining = freeRoadBuildsRemaining;
    }

    public List<Map<String, Integer>> getSettlementsOnCorners() {
        return settlementsOnCorners;
    }

    public void setSettlementsOnCorners(List<Map<String, Integer>> settlementsOnCorners) {
        this.settlementsOnCorners = settlementsOnCorners;
    }

    public List<Map<String, Integer>> getCitiesOnCorners() {
        return citiesOnCorners;
    }

    public void setCitiesOnCorners(List<Map<String, Integer>> citiesOnCorners) {
        this.citiesOnCorners = citiesOnCorners;
    }

    public List<Map<String, Integer>> getRoadsOnEdges() {
        return roadsOnEdges;
    }

    public void setRoadsOnEdges(List<Map<String, Integer>> roadsOnEdges) {
        this.roadsOnEdges = roadsOnEdges;
    }
}