package ch.uzh.ifi.hase.soprafs26.board;

import org.junit.jupiter.api.Test;

import ch.uzh.ifi.hase.soprafs26.entity.Board;
import ch.uzh.ifi.hase.soprafs26.entity.Edge;
import ch.uzh.ifi.hase.soprafs26.entity.Intersection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BoardGenerationTest {

	@Test
	public void generateBoard_randomizedPorts_areCreatedCorrectly() {
		Board board = new Board();

		List<String> generatedTiles = board.generateBoard();

		assertEquals(19, generatedTiles.size());
		assertEquals(19, board.getHexTile_DiceNumbers().size());
		assertEquals(generatedTiles, board.getHexTiles());
		assertEquals(9, board.getPorts().size());
		assertEquals(1, Collections.frequency(board.getPorts(), "WOOD"));
		assertEquals(1, Collections.frequency(board.getPorts(), "BRICK"));
		assertEquals(1, Collections.frequency(board.getPorts(), "SHEEP"));
		assertEquals(1, Collections.frequency(board.getPorts(), "WHEAT"));
		assertEquals(1, Collections.frequency(board.getPorts(), "ORE"));
		assertEquals(4, Collections.frequency(board.getPorts(), "STANDARD"));

		assertEquals(1, Collections.frequency(generatedTiles, "DESERT"));
		assertEquals(4, Collections.frequency(generatedTiles, "SHEEP"));
		assertEquals(4, Collections.frequency(generatedTiles, "WHEAT"));
		assertEquals(4, Collections.frequency(generatedTiles, "WOOD"));
		assertEquals(3, Collections.frequency(generatedTiles, "BRICK"));
		assertEquals(3, Collections.frequency(generatedTiles, "ORE"));

		List<Integer> diceNumbers = board.getHexTile_DiceNumbers();
		assertEquals(1, Collections.frequency(diceNumbers, -1));
		assertEquals(1, Collections.frequency(diceNumbers, 2));
		assertEquals(2, Collections.frequency(diceNumbers, 3));
		assertEquals(2, Collections.frequency(diceNumbers, 4));
		assertEquals(2, Collections.frequency(diceNumbers, 5));
		assertEquals(2, Collections.frequency(diceNumbers, 6));
		assertEquals(2, Collections.frequency(diceNumbers, 8));
		assertEquals(2, Collections.frequency(diceNumbers, 9));
		assertEquals(2, Collections.frequency(diceNumbers, 10));
		assertEquals(2, Collections.frequency(diceNumbers, 11));
		assertEquals(1, Collections.frequency(diceNumbers, 12));
		assertEquals(0, Collections.frequency(diceNumbers, 7));

		assertPortsRespectBaseGameSpacing(board);
		assertPortsDoNotShareCorners(board);
	}

	@Test
	public void generateBoard_initialOccupancy_isEmpty() {
		Board board = new Board();
		board.generateBoard();

		assertEquals(54, board.getIntersections().size());
		assertEquals(72, board.getEdges().size());

		for (Intersection intersection : board.getIntersections()) {
			assertFalse(intersection.isOccupied());
		}

		for (Edge edge : board.getEdges()) {
			assertFalse(edge.isOccupied());
		}
	}

	@Test
	public void generateBoard_desertTile_matchesNegativeOneDiceValue() {
		Board board = new Board();
		board.generateBoard();

		List<String> tiles = board.getHexTiles();
		List<Integer> diceValues = board.getHexTile_DiceNumbers();

		int desertTileCount = 0;
		int negativeOneCount = 0;
		int desertIndex = -1;
		int negativeOneIndex = -1;

		for (int i = 0; i < tiles.size(); i++) {
			if ("DESERT".equals(tiles.get(i))) {
				desertTileCount++;
				desertIndex = i;
			}
			if (diceValues.get(i) == -1) {
				negativeOneCount++;
				negativeOneIndex = i;
			}
		}

		assertEquals(1, desertTileCount);
		assertEquals(1, negativeOneCount);
		assertTrue(desertIndex >= 0);
		assertEquals(desertIndex, negativeOneIndex);
	}

	@Test
	public void generateBoard_redNumbers_areNotAdjacent() {
		Board board = new Board();
		board.generateBoard();

		for (int firstHexId = 1; firstHexId <= 19; firstHexId++) {
			if (!isRedNumber(board.getHexTile_DiceNumbers().get(firstHexId - 1))) {
				continue;
			}

			for (int secondHexId = firstHexId + 1; secondHexId <= 19; secondHexId++) {
				if (!isRedNumber(board.getHexTile_DiceNumbers().get(secondHexId - 1))) {
					continue;
				}

				assertFalse(
					areAdjacentHexes(firstHexId, secondHexId),
					"Red numbers must not be adjacent: " + firstHexId + " and " + secondHexId
				);
			}
		}
	}

	@Test
	public void generateBoard_intersections_haveStableIds() {
		Board board = new Board();
		board.generateBoard();

		assertEquals(54, board.getIntersections().size());

		for (int i = 0; i < board.getIntersections().size(); i++) {
			assertEquals(i, board.getIntersections().get(i).getId());
			assertFalse(board.getIntersections().get(i).isOccupied());
		}
	}

	@Test
	public void generateBoard_edges_haveStableTopology() {
		Board board = new Board();
		board.generateBoard();

		assertEquals(72, board.getEdges().size());

		assertEquals(0, board.getEdges().get(0).getId());
		assertEquals(0, board.getEdges().get(0).getIntersectionAId());
		assertEquals(1, board.getEdges().get(0).getIntersectionBId());

		assertEquals(1, board.getEdges().get(1).getId());
		assertEquals(1, board.getEdges().get(1).getIntersectionAId());
		assertEquals(2, board.getEdges().get(1).getIntersectionBId());

		assertEquals(5, board.getEdges().get(5).getId());
		assertEquals(0, board.getEdges().get(5).getIntersectionAId());
		assertEquals(5, board.getEdges().get(5).getIntersectionBId());

		assertEquals(71, board.getEdges().get(71).getId());
		assertEquals(46, board.getEdges().get(71).getIntersectionAId());
		assertEquals(52, board.getEdges().get(71).getIntersectionBId());

		for (Edge edge : board.getEdges()) {
			assertFalse(edge.isOccupied());
			assertNotNull(edge.getIntersectionAId());
			assertNotNull(edge.getIntersectionBId());
		}
	}

	@Test
	public void coordinateHelpers_nullAndValidIds_returnExpectedMappings() {
		Board board = new Board();
		board.generateBoard();

		assertTrue(board.getHexCoordinatesForIntersection(null).isEmpty());
		assertTrue(board.getHexCoordinatesForEdge(null).isEmpty());
		assertTrue(board.getAdjacentHexIdsForIntersection(null).isEmpty());

		List<Map<String, Integer>> intersectionCoordinates = board.getHexCoordinatesForIntersection(0);
		List<Map<String, Integer>> edgeCoordinates = board.getHexCoordinatesForEdge(0);
		List<Integer> adjacentHexIds = board.getAdjacentHexIdsForIntersection(0);

		assertFalse(intersectionCoordinates.isEmpty());
		assertFalse(edgeCoordinates.isEmpty());
		assertFalse(adjacentHexIds.isEmpty());
		assertTrue(intersectionCoordinates.get(0).containsKey("hexId"));
		assertTrue(intersectionCoordinates.get(0).containsKey("corner"));
		assertTrue(edgeCoordinates.get(0).containsKey("hexId"));
		assertTrue(edgeCoordinates.get(0).containsKey("edge"));
	}

	@Test
	public void buildIntersectionToHexIdsMap_containsAllGeneratedIntersections() {
		Board board = new Board();
		board.generateBoard();

		Map<Integer, List<Integer>> mapping = board.buildIntersectionToHexIdsMap();

		assertEquals(54, mapping.size());
		assertTrue(mapping.values().stream().allMatch(hexIds -> !hexIds.isEmpty()));
		assertTrue(mapping.values().stream().flatMap(List::stream).allMatch(hexId -> hexId >= 1 && hexId <= 19));
	}

	@Test
	public void privateAdjacencyHelpers_coverTrueFalseAndErrorBranches() {
		Board board = new Board();

		assertTrue((Boolean) invokePrivate(board, "isRedNumber", new Class<?>[] {int.class}, 6));
		assertTrue((Boolean) invokePrivate(board, "isRedNumber", new Class<?>[] {int.class}, 8));
		assertFalse((Boolean) invokePrivate(board, "isRedNumber", new Class<?>[] {int.class}, 5));

		assertTrue((Boolean) invokePrivate(board, "areAdjacentHexes", new Class<?>[] {int.class, int.class}, 1, 2));
		assertTrue((Boolean) invokePrivate(board, "areAdjacentHexes", new Class<?>[] {int.class, int.class}, 1, 5));
		assertFalse((Boolean) invokePrivate(board, "areAdjacentHexes", new Class<?>[] {int.class, int.class}, 1, 19));

		List<Integer> adjacentRedNumbers = List.of(
			6, 8, 2,
			3, 4, 5, 9,
			10, 11, 12, 3, 4,
			5, 9, 10, 11,
			12, 3, 4
		);
		assertTrue((Boolean) invokePrivate(board, "hasAdjacentRedNumbers", new Class<?>[] {List.class}, adjacentRedNumbers));

		List<Integer> separatedRedNumbers = List.of(
			6, 2, 3,
			4, 5, 9, 10,
			11, 12, 3, 4, 5,
			9, 10, 11, 12,
			8, 3, 4
		);
		assertFalse((Boolean) invokePrivate(board, "hasAdjacentRedNumbers", new Class<?>[] {List.class}, separatedRedNumbers));

		assertThrows(AssertionError.class,
			() -> invokePrivate(board, "boardCoordinatesForHex", new Class<?>[] {int.class}, 99));
	}

	private boolean isRedNumber(int diceNumber) {
		return diceNumber == 6 || diceNumber == 8;
	}

	private boolean areAdjacentHexes(int firstHexId, int secondHexId) {
		double[] firstCoordinates = boardCoordinatesForHex(firstHexId);
		double[] secondCoordinates = boardCoordinatesForHex(secondHexId);

		double deltaX = Math.abs(firstCoordinates[0] - secondCoordinates[0]);
		double deltaY = Math.abs(firstCoordinates[1] - secondCoordinates[1]);

		return (deltaX == 1.0 && deltaY == 0.0) || (deltaX == 0.5 && deltaY == 1.0);
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

	@SuppressWarnings("unchecked")
	private <T> T invokePrivate(Object target, String methodName, Class<?>[] parameterTypes, Object... args) {
		try {
			Method method = target.getClass().getDeclaredMethod(methodName, parameterTypes);
			method.setAccessible(true);
			return (T) method.invoke(target, args);
		} catch (Exception exception) {
			throw new AssertionError("Failed to invoke private method " + methodName, exception);
		}
	}

	private void assertPortsDoNotShareCorners(Board board) {
		Set<String> occupiedCornerKeys = new HashSet<>();

		for (var port : board.getBoats()) {
			double[] center = invokePrivate(board, "toPixel", new Class<?>[] {int.class}, port.getHexId());
			double[] firstCornerPoint = invokePrivate(board, "getCornerPoint", new Class<?>[] {double.class, double.class, int.class}, center[0], center[1], port.getFirstCorner());
			double[] secondCornerPoint = invokePrivate(board, "getCornerPoint", new Class<?>[] {double.class, double.class, int.class}, center[0], center[1], port.getSecondCorner());
			String firstCornerKey = invokePrivate(board, "formatPoint", new Class<?>[] {double.class, double.class}, firstCornerPoint[0], firstCornerPoint[1]);
			String secondCornerKey = invokePrivate(board, "formatPoint", new Class<?>[] {double.class, double.class}, secondCornerPoint[0], secondCornerPoint[1]);

			assertTrue(occupiedCornerKeys.add(firstCornerKey));
			assertTrue(occupiedCornerKeys.add(secondCornerKey));
		}
	}

	private void assertPortsRespectBaseGameSpacing(Board board) {
		List<CoastalPortPosition> orderedCandidates = loadOrderedCoastalPortCandidates(board);
		List<Integer> selectedIndices = new ArrayList<>();

		for (var port : board.getBoats()) {
			selectedIndices.add(findCandidateIndex(orderedCandidates, port));
		}

		for (int i = 0; i < selectedIndices.size(); i++) {
			int currentIndex = selectedIndices.get(i);
			int nextIndex = selectedIndices.get((i + 1) % selectedIndices.size());
			int spacing = (nextIndex - currentIndex + orderedCandidates.size()) % orderedCandidates.size();

			assertTrue(spacing >= 3, "Ports must leave at least two coastal edges between neighboring ports.");
		}
	}

	private List<CoastalPortPosition> loadOrderedCoastalPortCandidates(Board board) {
		List<?> rawCandidates = invokePrivate(board, "buildCoastalPortCandidates", new Class<?>[] {});
		List<CoastalPortPosition> orderedCandidates = new ArrayList<>();

		for (Object candidate : rawCandidates) {
			int hexId = readPrivateIntField(candidate, "hexId");
			int firstCorner = readPrivateIntField(candidate, "firstCorner");
			int secondCorner = readPrivateIntField(candidate, "secondCorner");
			double[] center = invokePrivate(board, "toPixel", new Class<?>[] {int.class}, hexId);
			double[] firstCornerPoint = invokePrivate(board, "getCornerPoint", new Class<?>[] {double.class, double.class, int.class}, center[0], center[1], firstCorner);
			double[] secondCornerPoint = invokePrivate(board, "getCornerPoint", new Class<?>[] {double.class, double.class, int.class}, center[0], center[1], secondCorner);

			orderedCandidates.add(new CoastalPortPosition(
				hexId,
				firstCorner,
				secondCorner,
				(firstCornerPoint[0] + secondCornerPoint[0]) / 2.0,
				(firstCornerPoint[1] + secondCornerPoint[1]) / 2.0
			));
		}

		double centerX = orderedCandidates.stream().mapToDouble(candidate -> candidate.midpointX).average().orElse(0.0);
		double centerY = orderedCandidates.stream().mapToDouble(candidate -> candidate.midpointY).average().orElse(0.0);

		orderedCandidates.sort(
			Comparator.comparingDouble((CoastalPortPosition candidate) -> Math.atan2(candidate.midpointY - centerY, candidate.midpointX - centerX))
				.thenComparingInt(candidate -> candidate.hexId)
				.thenComparingInt(candidate -> candidate.firstCorner)
				.thenComparingInt(candidate -> candidate.secondCorner)
		);

		for (int i = 0; i < orderedCandidates.size(); i++) {
			orderedCandidates.get(i).index = i;
		}

		return orderedCandidates;
	}

	private int findCandidateIndex(List<CoastalPortPosition> orderedCandidates, Object port) {
		int hexId = (Integer) invokePrivate(port, "getHexId", new Class<?>[] {});
		int firstCorner = (Integer) invokePrivate(port, "getFirstCorner", new Class<?>[] {});
		int secondCorner = (Integer) invokePrivate(port, "getSecondCorner", new Class<?>[] {});

		for (CoastalPortPosition candidate : orderedCandidates) {
			if (candidate.hexId == hexId && candidate.firstCorner == firstCorner && candidate.secondCorner == secondCorner) {
				return candidate.index;
			}
		}

		throw new AssertionError("Could not match generated port to a coastal edge candidate.");
	}

	private int readPrivateIntField(Object target, String fieldName) {
		try {
			Field field = target.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			return field.getInt(target);
		} catch (Exception exception) {
			throw new AssertionError("Failed to read private field " + fieldName, exception);
		}
	}

	private static final class CoastalPortPosition {
		private final int hexId;
		private final int firstCorner;
		private final int secondCorner;
		private final double midpointX;
		private final double midpointY;
		private int index;

		private CoastalPortPosition(int hexId, int firstCorner, int secondCorner, double midpointX, double midpointY) {
			this.hexId = hexId;
			this.firstCorner = firstCorner;
			this.secondCorner = secondCorner;
			this.midpointX = midpointX;
			this.midpointY = midpointY;
		}
	}
}
