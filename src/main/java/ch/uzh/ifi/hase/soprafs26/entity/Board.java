package ch.uzh.ifi.hase.soprafs26.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Board implements Serializable {

    private static final long serialVersionUID = 1L;

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

    private List<Boat> boats;

    private List<Integer> hexTile_DiceNumbers;

    public List<String> getHexTiles() {
        return hexTiles;
    }

    public void setHexTiles(List<String> hexTiles) {
        this.hexTiles = hexTiles;
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

    public List<Boat> getBoats() {
        return boats;
    }

    public void setBoats(List<Boat> boats) {
        this.boats = boats;
    }

    public void setHexTile_DiceNumbers(List<Integer> hexTile_DiceNumbers) {
        this.hexTile_DiceNumbers = hexTile_DiceNumbers;
    }

    public List<Integer> getHexTile_DiceNumbers() {
        return hexTile_DiceNumbers;
    }

    private List<Boat> createDefaultBoats(List<String> portTypes) {
        List<Boat> defaultBoats = new ArrayList<>();
        int[][] anchors = {
            {16, 0, 5},
            {2, 4, 3},
            {3, 4, 5},
            {4, 3, 4},
            {7, 4, 5},
            {12, 5, 0},
            {13, 2, 3},
            {19, 0, 1},
            {18, 1, 2}
        };

        for (int i = 0; i < anchors.length; i++) {
            Boat boat = new Boat();
            boat.setId(i + 1);
            boat.setBoatType(portTypes != null && i < portTypes.size() ? portTypes.get(i) : "STANDARD");
            boat.setHexId(anchors[i][0]);
            boat.setFirstCorner(anchors[i][1]);
            boat.setSecondCorner(anchors[i][2]);
            defaultBoats.add(boat);
        }

        return defaultBoats;
    }

    public List<String> generateBoard() {
        this.hexTiles = new ArrayList<>(STANDARD_HEX_TILES);
        this.intersections = new ArrayList<>(Collections.nCopies(STANDARD_INTERSECTION_COUNT, false)); // false indicates unoccupied intersection
        this.edges = new ArrayList<>(Collections.nCopies(STANDARD_EDGE_COUNT, false)); // false indicates unoccupied edge
        this.ports = new ArrayList<>(STANDARD_PORTS);
        this.boats = createDefaultBoats(this.ports);
        this.hexTile_DiceNumbers = new ArrayList<>(STANDARD_HEX_TILE_DICE_NUMBERS);
        return this.hexTiles;
    }

}
