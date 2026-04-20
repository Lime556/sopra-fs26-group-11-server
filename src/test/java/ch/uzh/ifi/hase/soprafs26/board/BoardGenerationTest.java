package ch.uzh.ifi.hase.soprafs26.board;

import org.junit.jupiter.api.Test;

import ch.uzh.ifi.hase.soprafs26.entity.Board;
import ch.uzh.ifi.hase.soprafs26.entity.Edge;
import ch.uzh.ifi.hase.soprafs26.entity.Intersection;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BoardGenerationTest {

	@Test
	public void generateBoard_staticLayout_isCreatedCorrectly() {
		Board board = new Board();

		List<String> generatedTiles = board.generateBoard();

		List<String> expectedTiles = Arrays.asList(
				"SHEEP", "WHEAT", "WOOD",
				"BRICK", "ORE", "SHEEP", "WHEAT",
				"WOOD", "DESERT", "WOOD", "WHEAT", "BRICK",
				"ORE", "WOOD", "ORE", "SHEEP",
				"BRICK", "WHEAT", "SHEEP"
		);

		List<Integer> expectedDiceNumbers = Arrays.asList(
				10, 2, 9,
				12, 6, 4, 10,
				9, -1, 11, 3, 8,
				8, 3, 4, 5,
				5, 6, 11
		);

		List<String> expectedPorts = Arrays.asList(
				"STANDARD", "BRICK", "STONE", "WHEAT", "WOOD",
				"STANDARD", "SHEEP", "STANDARD", "STANDARD"
		);

		assertEquals(expectedTiles, generatedTiles);
		assertEquals(expectedTiles, board.getHexTiles());
		assertEquals(expectedDiceNumbers, board.getHexTile_DiceNumbers());
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
}
