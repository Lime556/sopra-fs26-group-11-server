package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.entity.Board;
import ch.uzh.ifi.hase.soprafs26.entity.Edge;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Intersection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@DataJpaTest
public class GameRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private GameRepository gameRepository;

    @Test
    public void saveGame_persistsBoardState() {
        Board board = new Board();
        board.generateBoard();

        Game game = new Game();
        game.setBoard(board);
        game.setCurrentTurnIndex(2);
        game.setDiceValue(6);

        Game persistedGame = gameRepository.save(game);
        entityManager.flush();
        entityManager.clear();

        Game reloadedGame = gameRepository.findById(persistedGame.getId()).orElseThrow();

        assertNotNull(reloadedGame.getBoard());
        assertEquals(board.getHexTiles(), reloadedGame.getBoard().getHexTiles());
        assertEquals(board.getPorts(), reloadedGame.getBoard().getPorts());
        assertEquals(board.getHexTile_DiceNumbers(), reloadedGame.getBoard().getHexTile_DiceNumbers());

        assertEquals(board.getIntersections().size(), reloadedGame.getBoard().getIntersections().size());
        for (int i = 0; i < board.getIntersections().size(); i++) {
            Intersection expected = board.getIntersections().get(i);
            Intersection actual = reloadedGame.getBoard().getIntersections().get(i);

            assertNotNull(actual);
            assertEquals(expected.getId(), actual.getId());
            assertEquals(expected.isOccupied(), actual.isOccupied());
            assertNull(actual.getBuilding());
        }

        assertEquals(board.getEdges().size(), reloadedGame.getBoard().getEdges().size());
        for (int i = 0; i < board.getEdges().size(); i++) {
            Edge expected = board.getEdges().get(i);
            Edge actual = reloadedGame.getBoard().getEdges().get(i);

            assertNotNull(actual);
            assertEquals(expected.getId(), actual.getId());
            assertEquals(expected.getIntersectionAId(), actual.getIntersectionAId());
            assertEquals(expected.getIntersectionBId(), actual.getIntersectionBId());
            assertEquals(expected.isOccupied(), actual.isOccupied());
            assertNull(actual.getRoad());
        }
    }
}