package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.entity.Board;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
        assertEquals(board.getIntersections(), reloadedGame.getBoard().getIntersections());
        assertEquals(board.getEdges(), reloadedGame.getBoard().getEdges());
        assertEquals(board.getPorts(), reloadedGame.getBoard().getPorts());
        assertEquals(board.getHexTile_DiceNumbers(), reloadedGame.getBoard().getHexTile_DiceNumbers());
    }
}