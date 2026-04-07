package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.List;

public class BoardGetDTO {
    private Long id;
    private List<String> hexTiles;
    private List<Boolean> intersections;
    private List<Boolean> edges;
    private List<String> ports;
    private List<BoatGetDTO> boats;
    private List<Integer> hexTile_DiceNumbers;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<String> getHexTiles() {
        return hexTiles;
    }

    public void setHexTiles(List<String> hexTiles) {
        this.hexTiles = hexTiles;
    }

    public List<Boolean> getIntersections() {
        return intersections;
    }

    public void setIntersections(List<Boolean> intersections) {
        this.intersections = intersections;
    }

    public List<Boolean> getEdges() {
        return edges;
    }

    public void setEdges(List<Boolean> edges) {
        this.edges = edges;
    }

    public List<String> getPorts() {
        return ports;
    }

    public void setPorts(List<String> ports) {
        this.ports = ports;
    }

    public List<BoatGetDTO> getBoats() {
        return boats;
    }

    public void setBoats(List<BoatGetDTO> boats) {
        this.boats = boats;
    }

    public List<Integer> getHexTile_DiceNumbers() {
        return hexTile_DiceNumbers;
    }

    public void setHexTile_DiceNumbers(List<Integer> hexTile_DiceNumbers) {
        this.hexTile_DiceNumbers = hexTile_DiceNumbers;
    }
}
