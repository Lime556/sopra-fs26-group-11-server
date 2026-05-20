package ch.uzh.ifi.hase.soprafs26.board;

import org.junit.jupiter.api.Test;

import ch.uzh.ifi.hase.soprafs26.entity.Board;
import ch.uzh.ifi.hase.soprafs26.entity.Edge;
import ch.uzh.ifi.hase.soprafs26.entity.Intersection;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BoardGenerationTest {

	@Test
	public void generateBoard_staticLayout_isCreatedCorrectly() {
		Board board = new Board();

		List<String> generatedTiles = board.generateBoard();

		List<String> expectedPorts = List.of(
				"STANDARD", "BRICK", "STONE", "WHEAT", "WOOD",
				"STANDARD", "SHEEP", "STANDARD", "STANDARD"
		);

		assertEquals(19, generatedTiles.size());
		assertEquals(19, board.getHexTile_DiceNumbers().size());
		assertEquals(generatedTiles, board.getHexTiles());

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

		assertEquals(expectedPorts, board.getPorts());
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
}
