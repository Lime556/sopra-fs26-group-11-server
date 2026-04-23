package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.entity.Board;
import ch.uzh.ifi.hase.soprafs26.entity.Edge;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Intersection;
import ch.uzh.ifi.hase.soprafs26.entity.Player;
import ch.uzh.ifi.hase.soprafs26.entity.Settlement;
import ch.uzh.ifi.hase.soprafs26.entity.Road;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;

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

    @Test
    public void saveGame_persistsSettlementState() {
        Board board = new Board();
        board.generateBoard();

        Game game = new Game();
        game.setGamePhase("SETUP");
        game.setCurrentTurnIndex(0);
        game.setBoard(board);

        Player player = new Player();
        player.setId(10L);
        player.setName("Alice");
        player.setSettlementPoints(1);
        game.setPlayers(List.of(player));

        Intersection intersection = board.getIntersections().get(0);
        Settlement settlement = new Settlement();
        settlement.setOwnerPlayerId(10L);
        settlement.setIntersectionId(intersection.getId());
        intersection.setBuilding(settlement);

        Game persistedGame = gameRepository.save(game);
        entityManager.flush();
        entityManager.clear();

        Game reloadedGame = gameRepository.findById(persistedGame.getId()).orElseThrow();

        assertNotNull(reloadedGame.getBoard());
        Intersection reloadedIntersection = reloadedGame.getBoard().getIntersections().get(0);
        assertNotNull(reloadedIntersection.getBuilding());
        assertEquals(10L, ((Settlement) reloadedIntersection.getBuilding()).getOwnerPlayerId());
        assertEquals(1, reloadedGame.getPlayers().get(0).getSettlementPoints());
    }

    @Test
    public void saveGame_persistsRoadState() {
        Board board = new Board();
        board.generateBoard();

        Game game = new Game();
        game.setGamePhase("SETUP");
        game.setCurrentTurnIndex(0);
        game.setBoard(board);

        Player player = new Player();
        player.setId(10L);
        player.setName("Alice");
        game.setPlayers(List.of(player));

        Edge edge = board.getEdges().get(0);
        Road road = new Road();
        road.setOwnerPlayerId(10L);
        road.setEdgeId(edge.getId());
        edge.setRoad(road);

        Game persistedGame = gameRepository.save(game);
        entityManager.flush();
        entityManager.clear();

        Game reloadedGame = gameRepository.findById(persistedGame.getId()).orElseThrow();

        assertNotNull(reloadedGame.getBoard());
        Edge reloadedEdge = reloadedGame.getBoard().getEdges().get(0);
        assertNotNull(reloadedEdge.getRoad());
        assertEquals(10L, reloadedEdge.getRoad().getOwnerPlayerId());
    }

    @Test
    public void saveGame_persistsMultipleSettlementsAndRoads() {
        Board board = new Board();
        board.generateBoard();

        Game game = new Game();
        game.setGamePhase("SETUP");
        game.setCurrentTurnIndex(1);
        game.setBoard(board);

        Player player1 = new Player();
        player1.setId(10L);
        player1.setName("Alice");
        player1.setSettlementPoints(2);

        Player player2 = new Player();
        player2.setId(11L);
        player2.setName("Bob");
        player2.setSettlementPoints(1);

        game.setPlayers(List.of(player1, player2));

        Intersection intersection1 = board.getIntersections().get(0);
        Settlement settlement1 = new Settlement();
        settlement1.setOwnerPlayerId(10L);
        settlement1.setIntersectionId(intersection1.getId());
        intersection1.setBuilding(settlement1);

        Intersection intersection2 = board.getIntersections().get(5);
        Settlement settlement2 = new Settlement();
        settlement2.setOwnerPlayerId(10L);
        settlement2.setIntersectionId(intersection2.getId());
        intersection2.setBuilding(settlement2);

        Intersection intersection3 = board.getIntersections().get(10);
        Settlement settlement3 = new Settlement();
        settlement3.setOwnerPlayerId(11L);
        settlement3.setIntersectionId(intersection3.getId());
        intersection3.setBuilding(settlement3);

        Edge edge1 = board.getEdges().get(0);
        Road road1 = new Road();
        road1.setOwnerPlayerId(10L);
        road1.setEdgeId(edge1.getId());
        edge1.setRoad(road1);

        Edge edge2 = board.getEdges().get(5);
        Road road2 = new Road();
        road2.setOwnerPlayerId(11L);
        road2.setEdgeId(edge2.getId());
        edge2.setRoad(road2);

        Game persistedGame = gameRepository.save(game);
        entityManager.flush();
        entityManager.clear();

        Game reloadedGame = gameRepository.findById(persistedGame.getId()).orElseThrow();

        Intersection reloadedIntersection1 = reloadedGame.getBoard().getIntersections().get(0);
        assertNotNull(reloadedIntersection1.getBuilding());
        assertEquals(10L, ((Settlement) reloadedIntersection1.getBuilding()).getOwnerPlayerId());

        Intersection reloadedIntersection2 = reloadedGame.getBoard().getIntersections().get(5);
        assertNotNull(reloadedIntersection2.getBuilding());
        assertEquals(10L, ((Settlement) reloadedIntersection2.getBuilding()).getOwnerPlayerId());

        Intersection reloadedIntersection3 = reloadedGame.getBoard().getIntersections().get(10);
        assertNotNull(reloadedIntersection3.getBuilding());
        assertEquals(11L, ((Settlement) reloadedIntersection3.getBuilding()).getOwnerPlayerId());

        Edge reloadedEdge1 = reloadedGame.getBoard().getEdges().get(0);
        assertNotNull(reloadedEdge1.getRoad());
        assertEquals(10L, reloadedEdge1.getRoad().getOwnerPlayerId());

        Edge reloadedEdge2 = reloadedGame.getBoard().getEdges().get(5);
        assertNotNull(reloadedEdge2.getRoad());
        assertEquals(11L, reloadedEdge2.getRoad().getOwnerPlayerId());

        assertEquals(2, reloadedGame.getPlayers().get(0).getSettlementPoints());
        assertEquals(1, reloadedGame.getPlayers().get(1).getSettlementPoints());
    }
} 