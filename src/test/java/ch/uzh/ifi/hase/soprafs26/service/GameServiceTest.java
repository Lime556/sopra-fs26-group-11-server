package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Board;
import ch.uzh.ifi.hase.soprafs26.entity.Edge;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Intersection;
import ch.uzh.ifi.hase.soprafs26.entity.Player;
import ch.uzh.ifi.hase.soprafs26.entity.Road;
import ch.uzh.ifi.hase.soprafs26.entity.Settlement;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GamePostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PlayerGetDTO;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class GameServiceTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GameService gameService;

    private User user;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        user = new User();
        user.setId(1L);
        user.setToken("valid-token");
        user.setEmail("user@email.com");

        Mockito.when(gameRepository.save(Mockito.any())).thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(userRepository.findByToken("valid-token")).thenReturn(user);
    }

    @Test
    public void createGame_withoutBoard_generatesBoard() {
        Game createdGame = gameService.createGame("valid-token", null);

        assertNotNull(createdGame.getBoard());
        assertEquals(19, createdGame.getBoard().getHexTiles().size());
    }

    @Test
    public void createGame_missingToken_throwsUnauthorized() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> gameService.createGame(null, null));

        assertEquals(401, exception.getStatusCode().value());
    }

    @Test
    public void createGame_recalculatesVictoryPointsAndWinner() {
        GamePostDTO gamePostDTO = new GamePostDTO();
        gamePostDTO.setTargetVictoryPoints(10);

        PlayerGetDTO alice = new PlayerGetDTO();
        alice.setId(10L);
        alice.setName("Alice");
        alice.setSettlementPoints(3);
        alice.setCityPoints(4);
        alice.setDevelopmentCardVictoryPoints(1);
        alice.setHasLongestRoad(true);

        PlayerGetDTO bob = new PlayerGetDTO();
        bob.setId(11L);
        bob.setName("Bob");
        bob.setSettlementPoints(2);
        bob.setCityPoints(2);
        bob.setDevelopmentCardVictoryPoints(0);
        bob.setHasLargestArmy(true);

        gamePostDTO.setPlayers(List.of(alice, bob));

        Game createdGame = gameService.createGame("valid-token", gamePostDTO);

        assertNotNull(createdGame.getPlayers());
        assertEquals(2, createdGame.getPlayers().size());
        assertEquals(8, createdGame.getPlayers().get(0).getVictoryPoints());
        assertNull(createdGame.getWinner());
        assertNull(createdGame.getFinishedAt());
    }

    @Test
    public void updateGameState_detectsNewWinnerAndFinishesGame() {
        Game existingGame = new Game();
        existingGame.setId(99L);
        existingGame.setTargetVictoryPoints(10);

        Player alice = new Player();
        alice.setId(10L);
        alice.setName("Alice");
        alice.setSettlementPoints(2);
        alice.setCityPoints(2);
        alice.setDevelopmentCardVictoryPoints(0);

        existingGame.setPlayers(List.of(alice));

        Mockito.when(gameRepository.findById(99L)).thenReturn(Optional.of(existingGame));

        GamePostDTO update = new GamePostDTO();
        PlayerGetDTO updatedAlice = new PlayerGetDTO();
        updatedAlice.setId(10L);
        updatedAlice.setName("Alice");
        updatedAlice.setSettlementPoints(4);
        updatedAlice.setCityPoints(4);
        updatedAlice.setDevelopmentCardVictoryPoints(0);
        updatedAlice.setHasLargestArmy(true);
        update.setPlayers(List.of(updatedAlice));

        Game updatedGame = gameService.updateGameState(99L, "valid-token", update);

        assertEquals(10, updatedGame.getPlayers().get(0).getVictoryPoints());
        assertNotNull(updatedGame.getWinner());
        assertEquals(10L, updatedGame.getWinner().getId());
        assertNotNull(updatedGame.getFinishedAt());
    }

    @Test
    public void updateGameState_withoutPlayerAtTarget_hasNoWinner() {
        Game existingGame = new Game();
        existingGame.setId(100L);
        existingGame.setTargetVictoryPoints(10);

        Mockito.when(gameRepository.findById(100L)).thenReturn(Optional.of(existingGame));

        GamePostDTO update = new GamePostDTO();
        PlayerGetDTO bob = new PlayerGetDTO();
        bob.setId(20L);
        bob.setName("Bob");
        bob.setSettlementPoints(3);
        bob.setCityPoints(2);
        update.setPlayers(List.of(bob));

        Game updatedGame = gameService.updateGameState(100L, "valid-token", update);

        assertEquals(5, updatedGame.getPlayers().get(0).getVictoryPoints());
        assertNull(updatedGame.getWinner());
        assertNull(updatedGame.getFinishedAt());
    }

    @Test
    public void endTurn_resetsGameStateAndClearsDiceValue() {
        Game testGame = new Game();
        testGame.setId(150L);
        testGame.setCurrentTurnIndex(0);
        testGame.setTurnPhase("ACTION");
        testGame.setDiceValue(7);

        Player alice = new Player();
        alice.setId(30L);
        alice.setName("Alice");

        Player bob = new Player();
        bob.setId(31L);
        bob.setName("Bob");

        testGame.setPlayers(List.of(alice, bob));

        Mockito.when(gameRepository.findById(150L)).thenReturn(Optional.of(testGame));

        Game updatedGame = gameService.endTurn(150L, "valid-token");

        assertEquals(1, updatedGame.getCurrentTurnIndex());
        assertEquals("ROLL_DICE", updatedGame.getTurnPhase());
        assertNull(updatedGame.getDiceValue());
        assertEquals(31L, gameService.getCurrentPlayer(updatedGame).getId());
    }

    @Test
    public void endTurn_withMultiplePlayers_transitionsCorrectly() {
        Game testGame = new Game();
        testGame.setId(151L);
        testGame.setCurrentTurnIndex(1);
        testGame.setTurnPhase("ACTION");
        testGame.setDiceValue(9);

        Player alice = new Player();
        alice.setId(40L);
        alice.setName("Alice");

        Player bob = new Player();
        bob.setId(41L);
        bob.setName("Bob");

        Player charlie = new Player();
        charlie.setId(42L);
        charlie.setName("Charlie");

        testGame.setPlayers(List.of(alice, bob, charlie));

        Mockito.when(gameRepository.findById(151L)).thenReturn(Optional.of(testGame));

        Game updatedGame = gameService.endTurn(151L, "valid-token");

        assertEquals(2, updatedGame.getCurrentTurnIndex());
        assertEquals("ROLL_DICE", updatedGame.getTurnPhase());
        assertNull(updatedGame.getDiceValue());
        assertEquals(42L, gameService.getCurrentPlayer(updatedGame).getId());
    }







    // Tests for settlement placement and city upgrade logic
    @Test
    public void addSettlementToPlayer_adjacentBuilding_throwsConflict() {
        Game game = new Game();
        game.setId(5L);
    
        Player player = new Player();
        player.setId(1L);
        player.setWood(1);
        player.setBrick(1);
        player.setWool(1);
        player.setWheat(1);
    
        Board board = new Board();
        board.generateBoard();
    
        // build already exists at neighboring intersection 1
        Intersection occupiedNeighbor = findIntersection(board, 1);
        ch.uzh.ifi.hase.soprafs26.entity.Settlement existingSettlement =
            new ch.uzh.ifi.hase.soprafs26.entity.Settlement();
        existingSettlement.setOwnerPlayerId(2L);
        existingSettlement.setIntersectionId(1);
        occupiedNeighbor.setBuilding(existingSettlement);
    
        game.setBoard(board);
        game.setPlayers(List.of(player));
    
        Mockito.when(gameRepository.findById(5L)).thenReturn(Optional.of(game));
    
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> gameService.addSettlementToPlayer(5L, "valid-token", 1L, 0)
        );
    
        assertEquals(409, exception.getStatusCode().value());
    }

    @Test
    public void upgradeSettlementToCity_withoutSettlement_throwsConflict() {
        Game game = new Game();
        game.setId(201L);
    
        Player player = new Player();
        player.setId(10L);
        player.setWheat(2);
        player.setOre(3);
        player.setSettlementPoints(0);
        player.setCityPoints(0);
    
        Board board = new Board();
        board.generateBoard();
    
        game.setBoard(board);
        game.setPlayers(List.of(player));
    
        Mockito.when(gameRepository.findById(201L)).thenReturn(Optional.of(game));
    
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> gameService.upgradeSettlementToCity(201L, "valid-token", 10L, 3)
        );
    
        assertEquals(409, exception.getStatusCode().value());
    }

    @Test
    public void upgradeSettlementToCity_notEnoughResources_throwsConflict() {
    Game game = new Game();
    game.setId(202L);

    Player player = new Player();
    player.setId(10L);
    player.setWheat(1);
    player.setOre(2);
    player.setSettlementPoints(1);
    player.setCityPoints(0);

    Board board = new Board();
    board.generateBoard();

    Intersection intersection = findIntersection(board, 3);
    Settlement settlement = new Settlement();
    settlement.setOwnerPlayerId(10L);
    settlement.setIntersectionId(3);
    intersection.setBuilding(settlement);

    game.setBoard(board);
    game.setPlayers(List.of(player));

    Mockito.when(gameRepository.findById(202L)).thenReturn(Optional.of(game));

    ResponseStatusException exception = assertThrows(
        ResponseStatusException.class,
        () -> gameService.upgradeSettlementToCity(202L, "valid-token", 10L, 3)
    );

    assertEquals(409, exception.getStatusCode().value());
    }

    @Test
    public void addRoadToPlayer_validRequest_deductsResourcesAndPlacesRoad() {
        Game game = new Game();
        game.setId(210L);
    
        Player player = new Player();
        player.setId(10L);
        player.setWood(2);
        player.setBrick(2);
        player.setSettlementPoints(0);
        player.setCityPoints(0);
        player.setDevelopmentCardVictoryPoints(0);
    
        Board board = new Board();
        board.generateBoard();
    
        Edge targetEdge = findEdge(board, 0, 1);

        Intersection ownedIntersection = findIntersection(board, 0);
        Settlement settlement = new Settlement();
        settlement.setOwnerPlayerId(10L);
        settlement.setIntersectionId(0);
        ownedIntersection.setBuilding(settlement);
    
        game.setBoard(board);
        game.setPlayers(List.of(player));
    
        Mockito.when(gameRepository.findById(210L)).thenReturn(Optional.of(game));
        Mockito.when(gameRepository.save(Mockito.any(Game.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    
        Game result = gameService.addRoadToPlayer(210L, "valid-token", 10L, targetEdge.getId());
    
        Player updatedPlayer = result.getPlayers().get(0);
        Edge updatedEdge = findEdge(result.getBoard(), 0, 1);
    
        assertEquals(1, updatedPlayer.getWood());
        assertEquals(1, updatedPlayer.getBrick());
        assertNotNull(updatedEdge.getRoad());
        assertEquals(10L, updatedEdge.getRoad().getOwnerPlayerId());
    }
    
    @Test
    public void addSettlementToPlayer_validRequest_deductsResourcesAndPlacesSettlement() {
        Game game = new Game();
        game.setId(220L);
    
        Player player = new Player();
        player.setId(10L);
        player.setWood(2);
        player.setBrick(2);
        player.setWool(2);
        player.setWheat(2);
        player.setSettlementPoints(0);
        player.setCityPoints(0);
        player.setDevelopmentCardVictoryPoints(0);
    
        Board board = new Board();
        board.generateBoard();

        placeRoad(findEdge(board, 2, 3), 10L);

        game.setBoard(board);
        game.setPlayers(List.of(player));
    
        Mockito.when(gameRepository.findById(220L)).thenReturn(Optional.of(game));
        Mockito.when(gameRepository.save(Mockito.any(Game.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    
        Game result = gameService.addSettlementToPlayer(220L, "valid-token", 10L, 3);
    
        Player updatedPlayer = result.getPlayers().get(0);
        Intersection updatedIntersection = findIntersection(result.getBoard(), 3);
    
        assertEquals(1, updatedPlayer.getWood());
        assertEquals(1, updatedPlayer.getBrick());
        assertEquals(1, updatedPlayer.getWool());
        assertEquals(1, updatedPlayer.getWheat());
        assertEquals(1, updatedPlayer.getSettlementPoints());
        assertNotNull(updatedIntersection.getBuilding());
        assertEquals("Settlement", updatedIntersection.getBuilding().getClass().getSimpleName());
    }

    @Test
    public void upgradeSettlementToCity_validRequest_updatesResourcesAndPoints() {
        Game game = new Game();
        game.setId(200L);
    
        Player player = new Player();
        player.setId(10L);
        player.setSettlementPoints(1);
        player.setCityPoints(0);
        player.setDevelopmentCardVictoryPoints(0);
        player.setWheat(3);
        player.setOre(4);
    
        Board board = new Board();
        board.generateBoard();
    
        Intersection intersection = findIntersection(board, 3);
        Settlement settlement = new Settlement();
        settlement.setOwnerPlayerId(10L);
        settlement.setIntersectionId(3);
        intersection.setBuilding(settlement);
    
        game.setBoard(board);
        game.setPlayers(List.of(player));
    
        Mockito.when(gameRepository.findById(200L)).thenReturn(Optional.of(game));
        Mockito.when(gameRepository.save(Mockito.any(Game.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    
        Game result = gameService.upgradeSettlementToCity(200L, "valid-token", 10L, 3);
    
        Player updatedPlayer = result.getPlayers().get(0);
        Intersection updatedIntersection = findIntersection(result.getBoard(), 3);
    
        assertEquals(1, updatedPlayer.getWheat());
        assertEquals(1, updatedPlayer.getOre());
        assertEquals(0, updatedPlayer.getSettlementPoints());
        assertEquals(1, updatedPlayer.getCityPoints());
        assertNotNull(updatedIntersection.getBuilding());
        assertEquals("City", updatedIntersection.getBuilding().getClass().getSimpleName());
    }
    









    // Tests for longest road and victory point recalculation logic
    @Test
    public void recalculateVictoryState_fiveConnectedRoads_setsLongestRoad() {
        Game game = new Game();
        game.setId(1L);
        game.setTargetVictoryPoints(10);
    
        Player playerA = new Player();
        playerA.setId(1L);
        playerA.setSettlementPoints(0);
        playerA.setCityPoints(0);
        playerA.setDevelopmentCardVictoryPoints(0);
        playerA.setHasLongestRoad(false);
        playerA.setHasLargestArmy(false);
    
        Player playerB = new Player();
        playerB.setId(2L);
        playerB.setSettlementPoints(0);
        playerB.setCityPoints(0);
        playerB.setDevelopmentCardVictoryPoints(0);
        playerB.setHasLongestRoad(false);
        playerB.setHasLargestArmy(false);
    
        Board board = new Board();
        board.generateBoard();
    
        placeRoad(findEdge(board, 0, 1), 1L);
        placeRoad(findEdge(board, 1, 2), 1L);
        placeRoad(findEdge(board, 2, 3), 1L);
        placeRoad(findEdge(board, 3, 4), 1L);
        placeRoad(findEdge(board, 4, 5), 1L);
    
        game.setBoard(board);
        game.setPlayers(List.of(playerA, playerB));
    
        Mockito.when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        Mockito.when(gameRepository.save(Mockito.any(Game.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    
        Game result = gameService.updateGameState(1L, "valid-token", new GamePostDTO());
    
        Player updatedA = result.getPlayers().get(0);
        Player updatedB = result.getPlayers().get(1);
    
        assertEquals(true, updatedA.getHasLongestRoad());
        assertEquals(false, updatedB.getHasLongestRoad());
        assertEquals(2, updatedA.getVictoryPoints());
    }

    @Test
    public void recalculateVictoryState_fourConnectedRoads_doesNotSetLongestRoad() {
        Game game = new Game();
        game.setId(2L);
        game.setTargetVictoryPoints(10);
    
        Player playerA = new Player();
        playerA.setId(1L);
        playerA.setSettlementPoints(0);
        playerA.setCityPoints(0);
        playerA.setDevelopmentCardVictoryPoints(0);
        playerA.setHasLongestRoad(false);
    
        Player playerB = new Player();
        playerB.setId(2L);
        playerB.setSettlementPoints(0);
        playerB.setCityPoints(0);
        playerB.setDevelopmentCardVictoryPoints(0);
        playerB.setHasLongestRoad(false);
    
        Board board = new Board();
        board.generateBoard();
    
        placeRoad(findEdge(board, 0, 1), 1L);
        placeRoad(findEdge(board, 1, 2), 1L);
        placeRoad(findEdge(board, 2, 3), 1L);
        placeRoad(findEdge(board, 3, 4), 1L);
    
        game.setBoard(board);
        game.setPlayers(List.of(playerA, playerB));
    
        Mockito.when(gameRepository.findById(2L)).thenReturn(Optional.of(game));
        Mockito.when(gameRepository.save(Mockito.any(Game.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    
        Game result = gameService.updateGameState(2L, "valid-token", new GamePostDTO());
    
        Player updatedA = result.getPlayers().get(0);
        Player updatedB = result.getPlayers().get(1);
    
        assertEquals(false, updatedA.getHasLongestRoad());
        assertEquals(false, updatedB.getHasLongestRoad());
        assertEquals(0, updatedA.getVictoryPoints());
    }

    @Test
    public void recalculateVictoryState_branchingRoadNetwork_countsLongestPathNotAllEdges() {
        Game game = new Game();
        game.setId(3L);
        game.setTargetVictoryPoints(10);
    
        Player playerA = new Player();
        playerA.setId(1L);
        playerA.setSettlementPoints(0);
        playerA.setCityPoints(0);
        playerA.setDevelopmentCardVictoryPoints(0);
        playerA.setHasLongestRoad(false);
    
        Player playerB = new Player();
        playerB.setId(2L);
        playerB.setSettlementPoints(0);
        playerB.setCityPoints(0);
        playerB.setDevelopmentCardVictoryPoints(0);
        playerB.setHasLongestRoad(false);
    
        Board board = new Board();
        board.generateBoard();
    
        // branch at intersection 1:
        // 0-1-2 and 1-14-19
        // total roads = 4, but longest simple path = 3
        placeRoad(findEdge(board, 0, 1), 1L);
        placeRoad(findEdge(board, 1, 2), 1L);
        placeRoad(findEdge(board, 1, 14), 1L);
        placeRoad(findEdge(board, 14, 19), 1L);
    
        game.setBoard(board);
        game.setPlayers(List.of(playerA, playerB));
    
        Mockito.when(gameRepository.findById(3L)).thenReturn(Optional.of(game));
        Mockito.when(gameRepository.save(Mockito.any(Game.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    
        Game result = gameService.updateGameState(3L, "valid-token", new GamePostDTO());
    
        Player updatedA = result.getPlayers().get(0);
        Player updatedB = result.getPlayers().get(1);
    
        assertEquals(false, updatedA.getHasLongestRoad());
        assertEquals(false, updatedB.getHasLongestRoad());
        assertEquals(0, updatedA.getVictoryPoints());
    }

    @Test
    public void recalculateVictoryState_opponentBuildingBlocksRoadContinuation() {
        Game game = new Game();
        game.setId(4L);
        game.setTargetVictoryPoints(10);
    
        Player playerA = new Player();
        playerA.setId(1L);
        playerA.setSettlementPoints(0);
        playerA.setCityPoints(0);
        playerA.setDevelopmentCardVictoryPoints(0);
        playerA.setHasLongestRoad(false);
    
        Player playerB = new Player();
        playerB.setId(2L);
        playerB.setSettlementPoints(0);
        playerB.setCityPoints(0);
        playerB.setDevelopmentCardVictoryPoints(0);
        playerB.setHasLongestRoad(false);
    
        Board board = new Board();
        board.generateBoard();
    
        // full chain would be 0-1-2-3-4-5
        placeRoad(findEdge(board, 0, 1), 1L);
        placeRoad(findEdge(board, 1, 2), 1L);
        placeRoad(findEdge(board, 2, 3), 1L);
        placeRoad(findEdge(board, 3, 4), 1L);
        placeRoad(findEdge(board, 4, 5), 1L);
    
        // opponent building at intersection 3 blocks continuation through that node
        Intersection blockedIntersection = findIntersection(board, 3);
        ch.uzh.ifi.hase.soprafs26.entity.Settlement blockingSettlement =
            new ch.uzh.ifi.hase.soprafs26.entity.Settlement();
        blockingSettlement.setOwnerPlayerId(2L);
        blockingSettlement.setIntersectionId(3);
        blockedIntersection.setBuilding(blockingSettlement);
    
        game.setBoard(board);
        game.setPlayers(List.of(playerA, playerB));
    
        Mockito.when(gameRepository.findById(4L)).thenReturn(Optional.of(game));
        Mockito.when(gameRepository.save(Mockito.any(Game.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    
        Game result = gameService.updateGameState(4L, "valid-token", new GamePostDTO());
    
        Player updatedA = result.getPlayers().get(0);
        Player updatedB = result.getPlayers().get(1);
    
        assertEquals(false, updatedA.getHasLongestRoad());
        assertEquals(false, updatedB.getHasLongestRoad());
        assertEquals(0, updatedA.getVictoryPoints());
    }









    // Helper methods
    private Edge findEdge(Board board, int intersectionAId, int intersectionBId) {
        int min = Math.min(intersectionAId, intersectionBId);
        int max = Math.max(intersectionAId, intersectionBId);

        return board.getEdges().stream()
            .filter(edge -> edge != null)
            .filter(edge -> edge.getIntersectionAId() != null && edge.getIntersectionBId() != null)
            .filter(edge -> edge.getIntersectionAId() == min && edge.getIntersectionBId() == max)
            .findFirst()
            .orElseThrow(() -> new AssertionError(
                "No edge found between intersections " + min + " and " + max
            ));
    }

    private void placeRoad(Edge edge, long ownerPlayerId) {
        Road road = new Road();
        road.setOwnerPlayerId(ownerPlayerId);
        road.setEdgeId(edge.getId());
        edge.setRoad(road);
    }

    private Intersection findIntersection(Board board, int intersectionId) {
        return board.getIntersections().stream()
            .filter(intersection -> intersection != null)
            .filter(intersection -> intersection.getId() != null)
            .filter(intersection -> intersection.getId() == intersectionId)
            .findFirst()
            .orElseThrow(() -> new AssertionError(
                "No intersection found with id " + intersectionId
            ));
    }
}