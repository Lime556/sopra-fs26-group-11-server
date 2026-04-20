package ch.uzh.ifi.hase.soprafs26.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Board implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final int EXPECTED_INTERSECTION_COUNT = 54;
    private static final int EXPECTED_EDGE_COUNT = 72;

    private static final double HEX_SIZE = 58.0;
    private static final double SQRT_3 = Math.sqrt(3.0);
    private static final double ORIGIN_X = 150.0;
    private static final double ORIGIN_Y = 130.0;
    private static final double HEX_SPACING_X = HEX_SIZE * SQRT_3;
    private static final double HEX_SPACING_Y = HEX_SIZE * 1.5;

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
        "STANDARD", "BRICK", "STONE", "WHEAT", "WOOD",
        "STANDARD", "SHEEP", "STANDARD", "STANDARD"
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
    private List<Intersection> intersections;
    private List<Edge> edges;
    private List<String> ports;
    private List<Boat> boats;
    private List<Integer> hexTile_DiceNumbers;

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

    public List<Boat> getBoats() {
        return boats;
    }

    public void setBoats(List<Boat> boats) {
        this.boats = boats;
    }

    public List<Integer> getHexTile_DiceNumbers() {
        return hexTile_DiceNumbers;
    }

    public void setHexTile_DiceNumbers(List<Integer> hexTile_DiceNumbers) {
        this.hexTile_DiceNumbers = hexTile_DiceNumbers;
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

    private void createDefaultIntersectionsAndEdges() {
        Map<String, Integer> intersectionKeyToId = new LinkedHashMap<>();
        Map<String, Intersection> intersectionsByKey = new LinkedHashMap<>();
        Map<String, Edge> edgesByKey = new LinkedHashMap<>();

        int nextIntersectionId = 0;
        int nextEdgeId = 0;

        for (int hexId = 1; hexId <= 19; hexId++) {
            double[] center = toPixel(hexId);

            String[] cornerKeys = new String[6];
            for (int cornerIndex = 0; cornerIndex < 6; cornerIndex++) {
                double[] point = getCornerPoint(center[0], center[1], cornerIndex);
                String cornerKey = formatPoint(point[0], point[1]);
                cornerKeys[cornerIndex] = cornerKey;

                if (!intersectionKeyToId.containsKey(cornerKey)) {
                    Intersection intersection = new Intersection();
                    intersection.setId(nextIntersectionId);
                    intersection.setBuilding(null);

                    intersectionKeyToId.put(cornerKey, nextIntersectionId);
                    intersectionsByKey.put(cornerKey, intersection);
                    nextIntersectionId++;
                }
            }

            for (int edgeIndex = 0; edgeIndex < 6; edgeIndex++) {
                String cornerAKey = cornerKeys[edgeIndex];
                String cornerBKey = cornerKeys[(edgeIndex + 1) % 6];

                int intersectionAId = intersectionKeyToId.get(cornerAKey);
                int intersectionBId = intersectionKeyToId.get(cornerBKey);

                String edgeKey = createCanonicalEdgeKey(intersectionAId, intersectionBId);

                if (!edgesByKey.containsKey(edgeKey)) {
                    Edge edge = new Edge();
                    edge.setId(nextEdgeId);
                    edge.setIntersectionAId(Math.min(intersectionAId, intersectionBId));
                    edge.setIntersectionBId(Math.max(intersectionAId, intersectionBId));
                    edge.setRoad(null);

                    edgesByKey.put(edgeKey, edge);
                    nextEdgeId++;
                }
            }
        }

        this.intersections = new ArrayList<>(intersectionsByKey.values());
        this.edges = new ArrayList<>(edgesByKey.values());

        if (this.intersections.size() != EXPECTED_INTERSECTION_COUNT) {
            throw new IllegalStateException(
                "Board generation created " + this.intersections.size()
                    + " intersections instead of " + EXPECTED_INTERSECTION_COUNT + "."
            );
        }

        if (this.edges.size() != EXPECTED_EDGE_COUNT) {
            throw new IllegalStateException(
                "Board generation created " + this.edges.size()
                    + " edges instead of " + EXPECTED_EDGE_COUNT + "."
            );
        }
    }

    private String createCanonicalEdgeKey(int intersectionAId, int intersectionBId) {
        int min = Math.min(intersectionAId, intersectionBId);
        int max = Math.max(intersectionAId, intersectionBId);
        return min + "|" + max;
    }

    private double[] toPixel(int hexId) {
        double[] coordinates = boardCoordinatesForHex(hexId);
        return new double[] {
            ORIGIN_X + coordinates[0] * HEX_SPACING_X,
            ORIGIN_Y + coordinates[1] * HEX_SPACING_Y
        };
    }

    private double[] boardCoordinatesForHex(int hexId) {
        return switch (hexId) {
            case 1 -> new double[] {1, 0};
            case 2 -> new double[] {2, 0};
            case 3 -> new double[] {3, 0};
            case 4 -> new double[] {0.5, 1};
            case 5 -> new double[] {1.5, 1};
            case 6 -> new double[] {2.5, 1};
            case 7 -> new double[] {3.5, 1};
            case 8 -> new double[] {0, 2};
            case 9 -> new double[] {1, 2};
            case 10 -> new double[] {2, 2};
            case 11 -> new double[] {3, 2};
            case 12 -> new double[] {4, 2};
            case 13 -> new double[] {0.5, 3};
            case 14 -> new double[] {1.5, 3};
            case 15 -> new double[] {2.5, 3};
            case 16 -> new double[] {3.5, 3};
            case 17 -> new double[] {1, 4};
            case 18 -> new double[] {2, 4};
            case 19 -> new double[] {3, 4};
            default -> throw new IllegalArgumentException("Unsupported hex id: " + hexId);
        };
    }

    private double[] getCornerPoint(double centerX, double centerY, int cornerIndex) {
        double angle = (Math.PI / 3.0) * cornerIndex + Math.PI / 6.0;
        return new double[] {
            centerX + HEX_SIZE * Math.cos(angle),
            centerY + HEX_SIZE * Math.sin(angle)
        };
    }

    private String formatPoint(double x, double y) {
        return Math.round(x) + ":" + Math.round(y);
    }

    public List<String> generateBoard() {
        this.hexTiles = new ArrayList<>(STANDARD_HEX_TILES);
        createDefaultIntersectionsAndEdges();
        this.ports = new ArrayList<>(STANDARD_PORTS);
        this.boats = createDefaultBoats(this.ports);
        this.hexTile_DiceNumbers = new ArrayList<>(STANDARD_HEX_TILE_DICE_NUMBERS);
        return this.hexTiles;
    }
}