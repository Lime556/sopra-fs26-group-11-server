package ch.uzh.ifi.hase.soprafs26.board;

import org.junit.jupiter.api.Test;

import ch.uzh.ifi.hase.soprafs26.entity.Board;
import ch.uzh.ifi.hase.soprafs26.entity.Edge;
import ch.uzh.ifi.hase.soprafs26.entity.Intersection;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
