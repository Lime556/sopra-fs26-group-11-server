package ch.uzh.ifi.hase.soprafs26.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DevelopmentDeckGetDTO {
    private Long id;
    private Integer knight;
    @JsonProperty("victory_point")
    private Integer victoryPoint;
    @JsonProperty("road_building")
    private Integer roadBuilding;
    @JsonProperty("year_of_plenty")
    private Integer yearOfPlenty;
    private Integer monopoly;
    private Integer remaining;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getKnight() {
        return knight;
    }

    public void setKnight(Integer knight) {
        this.knight = knight;
    }

    @JsonProperty("victory_point")
    public Integer getVictoryPoint() {
        return victoryPoint;
    }

    @JsonProperty("victory_point")
    public void setVictoryPoint(Integer victoryPoint) {
        this.victoryPoint = victoryPoint;
    }

    @JsonProperty("road_building")
    public Integer getRoadBuilding() {
        return roadBuilding;
    }

    @JsonProperty("road_building")
    public void setRoadBuilding(Integer roadBuilding) {
        this.roadBuilding = roadBuilding;
    }

    @JsonProperty("year_of_plenty")
    public Integer getYearOfPlenty() {
        return yearOfPlenty;
    }

    @JsonProperty("year_of_plenty")
    public void setYearOfPlenty(Integer yearOfPlenty) {
        this.yearOfPlenty = yearOfPlenty;
    }

    public Integer getMonopoly() {
        return monopoly;
    }

    public void setMonopoly(Integer monopoly) {
        this.monopoly = monopoly;
    }

    public Integer getRemaining() {
        return remaining;
    }

    public void setRemaining(Integer remaining) {
        this.remaining = remaining;
    }
}
