package ch.uzh.ifi.hase.soprafs26.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.entity.Board;
import ch.uzh.ifi.hase.soprafs26.entity.City;
import ch.uzh.ifi.hase.soprafs26.entity.Edge;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Intersection;
import ch.uzh.ifi.hase.soprafs26.entity.Player;
import ch.uzh.ifi.hase.soprafs26.entity.Road;
import ch.uzh.ifi.hase.soprafs26.entity.Settlement;
import ch.uzh.ifi.hase.soprafs26.entity.TurnPhase;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;

class GameServiceTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private GameService gameService;

    private Game testGame;
    private Player testPlayer;
    private final Long gameId = 1L;
    private final Long playerId = 1L;
    private final String token = "test-token";

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        testPlayer = new Player();
        testPlayer.setId(playerId);
        testPlayer.setWood(10); testPlayer.setBrick(10); testPlayer.setWool(10); testPlayer.setWheat(10); testPlayer.setOre(10);

        Board board = new Board();
        board.setEdges(new ArrayList<>());
        board.setIntersections(new ArrayList<>());

        testGame = new Game();
        testGame.setId(gameId);
        testGame.setBoard(board);
        testGame.setPlayers(List.of(testPlayer));
        testGame.setTurnPhase(TurnPhase.ACTION.toString());

        when(userService.authenticate(token)).thenReturn(new User());
        when(gameRepository.findById(gameId)).thenReturn(Optional.of(testGame));
    }

    @Test
    void addRoadToPlayer_throwsConflict_whenLimitReached() {
        // Setup: Add 15 roads to the board for this player
        for (int i = 0; i < 15; i++) {
            Edge edge = new Edge();
            Road road = new Road();
            road.setOwnerPlayerId(playerId);
            edge.setRoad(road);
            testGame.getBoard().getEdges().add(edge);
        }

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> 
            gameService.addRoadToPlayer(gameId, token, playerId, 99)
        );
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Maximum number of roads reached.", exception.getReason());
    }

    @Test
    void addSettlementToPlayer_throwsConflict_whenLimitReached() {
        // Setup: Add 5 settlements
        for (int i = 0; i < 5; i++) {
            Intersection intersection = new Intersection();
            Settlement settlement = new Settlement();
            settlement.setOwnerPlayerId(playerId);
            intersection.setBuilding(settlement);
            testGame.getBoard().getIntersections().add(intersection);
        }

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> 
            gameService.addSettlementToPlayer(gameId, token, playerId, 99)
        );
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void upgradeSettlementToCity_throwsConflict_whenLimitReached() {
        // Setup: Add 4 cities
        for (int i = 0; i < 4; i++) {
            Intersection intersection = new Intersection();
            City city = new City();
            city.setOwnerPlayerId(playerId);
            intersection.setBuilding(city);
            testGame.getBoard().getIntersections().add(intersection);
        }

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> 
            gameService.upgradeSettlementToCity(gameId, token, playerId, 99)
        );
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }
}