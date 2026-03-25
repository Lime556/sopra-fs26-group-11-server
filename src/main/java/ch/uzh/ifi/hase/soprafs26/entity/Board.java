package ch.uzh.ifi.hase.soprafs26.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Board {

    private static final int STANDARD_INTERSECTION_COUNT = 54;
    private static final int STANDARD_EDGE_COUNT = 72;

    // Board state location in array == tile on board
    private static final List<String> STANDARD_HEX_TILES = Arrays.asList(
        "SHEEP", "WHEAT", "WOOD",
        "BRICK", "ORE", "SHEEP", "WHEAT",
        "WOOD", "DESERT", "WOOD", "WHEAT", "BRICK",
        "ORE", "WOOD", "ORE", "SHEEP",
        "BRICK", "WHEAT", "SHEEP"
    );

    // Fixed port order (around the board): 4x generic and 5x resource-specific.
    private static final List<String> STANDARD_PORTS = Arrays.asList(
        "STANDARD", "BRICK", "STONE", "WHEAT", "WOOD", "STANDARD", "SHEEP", "STANDARD", "STANDARD"
    );

    // Numbers according to die value to generate the board, -1 for desert tile
    private static final List<Integer> STANDARD_HEX_TILE_DICE_NUMBERS = Arrays.asList(
        10, 2, 9,
        12, 6, 4, 10,
        9, -1, 11, 3, 8,
        8, 3, 4, 5,
        5, 6, 11
    );

    private List<String> hexTiles;

    private List<Boolean> intersections;

    private List<Boolean> edges;

    private List<String> ports;

    private List<Integer> hexTile_DiceNumbers;

    public List<String> getHexTiles() {
        return hexTiles;
    }

    public List<Boolean> getIntersections() {
        return intersections;
    }

    public List<Boolean> getEdges() {
        return edges;
    }

    public List<String> getPorts() {
        return ports;
    }

    public void setIntersections(List<Boolean> intersections) {
        this.intersections = intersections;
    }

    public void setEdges(List<Boolean> edges) {
        this.edges = edges;
    }

    public void setPorts(List<String> ports) {
        this.ports = ports;
    }

    public void setHexTile_DiceNumbers(List<Integer> hexTile_DiceNumbers) {
        this.hexTile_DiceNumbers = hexTile_DiceNumbers;
    }

    public List<Integer> getHexTile_DiceNumbers() {
        return hexTile_DiceNumbers;
    }

    public List<String> generateBoard() {
        this.hexTiles = new ArrayList<>(STANDARD_HEX_TILES);
        this.intersections = new ArrayList<>(Collections.nCopies(STANDARD_INTERSECTION_COUNT, false)); // false indicates unoccupied intersection
        this.edges = new ArrayList<>(Collections.nCopies(STANDARD_EDGE_COUNT, false)); // false indicates unoccupied edge
        this.ports = new ArrayList<>(STANDARD_PORTS);
        this.hexTile_DiceNumbers = new ArrayList<>(STANDARD_HEX_TILE_DICE_NUMBERS);
        return this.hexTiles;
    }

}
