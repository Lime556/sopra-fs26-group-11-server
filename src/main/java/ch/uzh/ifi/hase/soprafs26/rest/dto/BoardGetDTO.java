package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.List;

import ch.uzh.ifi.hase.soprafs26.entity.Edge;
import ch.uzh.ifi.hase.soprafs26.entity.Intersection;

public class BoardGetDTO {
    private Long id;
    private List<String> hexTiles;
    private List<Intersection> intersections;
    private List<Edge> edges;
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

    public List<Intersection> getIntersections() {
        return intersections;
    }

    public void setIntersections(List<Intersection> intersections) {
        this.intersections = intersections;
    }

    public List<Edge> getEdges() {
        return edges;
    }

    public void setEdges(List<Edge> edges) {
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
