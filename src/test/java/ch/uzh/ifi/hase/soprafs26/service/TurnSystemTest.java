package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Board;
import ch.uzh.ifi.hase.soprafs26.entity.City;
import ch.uzh.ifi.hase.soprafs26.entity.Intersection;
import ch.uzh.ifi.hase.soprafs26.entity.Player;
import ch.uzh.ifi.hase.soprafs26.entity.Settlement;
import ch.uzh.ifi.hase.soprafs26.entity.TurnPhase;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GamePostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PlayerGetDTO;
import java.util.Arrays;
import java.util.Collections;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test suite for the turn system implementation.
 * Tests cover:
 * - Turn phase transitions (ROLL_DICE -> ACTION -> END_TURN)
 * - Dice rolling mechanics
 * - Phase enforcement for actions
 * - Turn progression to next player
 */
public class TurnSystemTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GameService gameService;

    private User testUser;
    private Game testGame;
    private Player player1;
    private Player player2;
    private Player player3;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        testUser = new User();
        testUser.setId(1L);
        testUser.setToken("valid-token");
        testUser.setUsername("Player1");
        testUser.setEmail("user@email.com");

        // Set up test game with 3 players
        testGame = new Game();
        testGame.setId(100L);

        player1 = new Player();
        player1.setId(1L);
        player1.setName("Player1");
        player1.setWood(10);
        player1.setBrick(10);
        player1.setWool(10);
        player1.setWheat(10);
        player1.setOre(10);

        player2 = new Player();
        player2.setId(2L);
        player2.setName("Player2");
        player2.setWood(10);
        player2.setBrick(10);
        player2.setWool(10);
        player2.setWheat(10);
        player2.setOre(10);

        player3 = new Player();
        player3.setId(3L);
        player3.setName("Player3");
        player3.setWood(10);
        player3.setBrick(10);
        player3.setWool(10);
        player3.setWheat(10);
        player3.setOre(10);

        testGame.setPlayers(Arrays.asList(player1, player2, player3));
        testGame.setCurrentTurnIndex(0);
        testGame.setTurnPhase(TurnPhase.ROLL_DICE.toString());

        Mockito.when(gameRepository.save(Mockito.any())).thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(gameRepository.findById(100L)).thenReturn(Optional.of(testGame));
        Mockito.when(userRepository.findByToken("valid-token")).thenReturn(testUser);
    }

    @Test
    public void rollDice_initialPhase_transitionsToAction() {
        assertEquals(TurnPhase.ROLL_DICE.toString(), testGame.getTurnPhase());
        assertEquals(0, testGame.getCurrentTurnIndex());

        Game updatedGame = gameService.rollDice(100L, "valid-token");

        assertEquals(TurnPhase.ACTION.toString(), updatedGame.getTurnPhase());
        assertNotNull(updatedGame.getDiceValue());
        assertTrue(updatedGame.getDiceValue() >= 2 && updatedGame.getDiceValue() <= 12);
    }

    @Test
    public void rollDice_notInRollPhase_throwsConflict() {
        testGame.setTurnPhase(TurnPhase.ACTION.toString());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> gameService.rollDice(100L, "valid-token"));

        assertEquals(409, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("ROLL_DICE phase"));
    }

    @Test
    public void rollDice_diceRollIsValid() {
        Game updatedGame = gameService.rollDice(100L, "valid-token");

        int diceValue = updatedGame.getDiceValue();
        assertTrue(diceValue >= 2 && diceValue <= 12,
                "Dice value should be between 2 and 12, got: " + diceValue);
    }

    @Test
    public void rollDice_setsDiceRolledAtTimestamp() {
        assertEquals(null, testGame.getDiceRolledAt());

        Game updatedGame = gameService.rollDice(100L, "valid-token");

        assertNotNull(updatedGame.getDiceRolledAt());
    }

    @Test
    public void distributeResourcesForDiceValue_settlement_grantsResources() {
        Board board = createUniformWoodBoardWithDice(8);
        Intersection intersection = board.getIntersections().get(0);

        Settlement settlement = new Settlement();
        settlement.setOwnerPlayerId(1L);
        settlement.setIntersectionId(intersection.getId());
        intersection.setBuilding(settlement);

        testGame.setBoard(board);

        int beforeWood = player1.getWood();
        gameService.distributeResourcesForDiceValue(testGame, 8);

        assertTrue(player1.getWood() > beforeWood);
        assertEquals(10, player2.getWood());
        assertEquals(10, player3.getWood());
    }

    @Test
    public void distributeResourcesForDiceValue_city_grantsDoubleSettlementResources() {
        Board settlementBoard = createUniformWoodBoardWithDice(9);
        Board cityBoard = createUniformWoodBoardWithDice(9);

        Player settlementOwner = new Player();
        settlementOwner.setId(1L);
        settlementOwner.setName("SettlementOwner");
        settlementOwner.setWood(0);
        settlementOwner.setBrick(0);
        settlementOwner.setWool(0);
        settlementOwner.setWheat(0);
        settlementOwner.setOre(0);

        Player cityOwner = new Player();
        cityOwner.setId(1L);
        cityOwner.setName("CityOwner");
        cityOwner.setWood(0);
        cityOwner.setBrick(0);
        cityOwner.setWool(0);
        cityOwner.setWheat(0);
        cityOwner.setOre(0);

        Game settlementGame = new Game();
        settlementGame.setBoard(settlementBoard);
        settlementGame.setPlayers(List.of(settlementOwner));

        Game cityGame = new Game();
        cityGame.setBoard(cityBoard);
        cityGame.setPlayers(List.of(cityOwner));

        Intersection settlementIntersection = settlementBoard.getIntersections().get(0);
        Settlement settlement = new Settlement();
        settlement.setOwnerPlayerId(1L);
        settlement.setIntersectionId(settlementIntersection.getId());
        settlementIntersection.setBuilding(settlement);

        Intersection cityIntersection = cityBoard.getIntersections().get(0);
        City city = new City();
        city.setOwnerPlayerId(1L);
        city.setIntersectionId(cityIntersection.getId());
        cityIntersection.setBuilding(city);

        gameService.distributeResourcesForDiceValue(settlementGame, 9);
        gameService.distributeResourcesForDiceValue(cityGame, 9);

        int settlementGain = settlementOwner.getWood();
        int cityGain = cityOwner.getWood();

        assertTrue(settlementGain > 0);
        assertEquals(settlementGain * 2, cityGain);
    }

    @Test
    public void applySevenRollEffects_playerWithMoreThanSeven_discardsHalf() {
        player1.setWood(3);
        player1.setBrick(2);
        player1.setWool(2);
        player1.setWheat(1);
        player1.setOre(0);

        int beforeTotal = player1.getWood() + player1.getBrick() + player1.getWool() + player1.getWheat() + player1.getOre();
        gameService.applySevenRollEffects(testGame);
        int afterTotal = player1.getWood() + player1.getBrick() + player1.getWool() + player1.getWheat() + player1.getOre();

        assertEquals(8, beforeTotal);
        assertEquals(4, afterTotal);
    }

    @Test
    public void applySevenRollEffects_playerWithSevenOrLess_keepsResources() {
        player2.setWood(2);
        player2.setBrick(2);
        player2.setWool(1);
        player2.setWheat(1);
        player2.setOre(1);

        int beforeTotal = player2.getWood() + player2.getBrick() + player2.getWool() + player2.getWheat() + player2.getOre();
        gameService.applySevenRollEffects(testGame);
        int afterTotal = player2.getWood() + player2.getBrick() + player2.getWool() + player2.getWheat() + player2.getOre();

        assertEquals(7, beforeTotal);
        assertEquals(7, afterTotal);
    }

    @Test
    public void rollDice_notActivePlayer_throwsForbidden() {
        testUser.setUsername("Player2");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> gameService.rollDice(100L, "valid-token"));

        assertEquals(403, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("active player"));
    }

    @Test
    public void endTurn_progressesToNextPlayer() {
        testGame.setTurnPhase(TurnPhase.ACTION.toString());
        assertEquals(0, testGame.getCurrentTurnIndex());
        Player currentPlayer = gameService.getCurrentPlayer(testGame);
        assertEquals(1L, currentPlayer.getId());

        Game updatedGame = gameService.endTurn(100L, "valid-token");

        assertEquals(1, updatedGame.getCurrentTurnIndex());
        assertEquals(TurnPhase.ROLL_DICE.toString(), updatedGame.getTurnPhase());
        assertNotNull(gameService.getCurrentPlayer(updatedGame));
        assertEquals(2L, gameService.getCurrentPlayer(updatedGame).getId());
    }

    @Test
    public void endTurn_wrapsAroundToFirstPlayer() {
        testGame.setCurrentTurnIndex(2); // Last player
        testGame.setTurnPhase(TurnPhase.ACTION.toString());

        Game updatedGame = gameService.endTurn(100L, "valid-token");

        assertEquals(0, updatedGame.getCurrentTurnIndex());
        assertEquals(1L, gameService.getCurrentPlayer(updatedGame).getId());
    }

    @Test
    public void getCurrentPlayer_returnsCorrectPlayer() {
        testGame.setCurrentTurnIndex(1);

        Player currentPlayer = gameService.getCurrentPlayer(testGame);

        assertNotNull(currentPlayer);
        assertEquals(2L, currentPlayer.getId());
        assertEquals("Player2", currentPlayer.getName());
    }

    @Test
    public void getCurrentPlayer_withNullGame_returnsNull() {
        Player currentPlayer = gameService.getCurrentPlayer(null);

        assertEquals(null, currentPlayer);
    }

    @Test
    public void getCurrentPlayer_withEmptyPlayerList_returnsNull() {
        testGame.setPlayers(Arrays.asList());

        Player currentPlayer = gameService.getCurrentPlayer(testGame);

        assertEquals(null, currentPlayer);
    }

    @Test
    public void turnPhaseEnforcement_createdGameStartsInRollDice() {
        GamePostDTO gamePostDTO = new GamePostDTO();
        PlayerGetDTO player = new PlayerGetDTO();
        player.setId(1L);
        player.setName("TestPlayer");
        gamePostDTO.setPlayers(List.of(player));

        Game createdGame = gameService.createGame("valid-token", gamePostDTO);

        assertEquals(TurnPhase.ROLL_DICE.toString(), createdGame.getTurnPhase());
    }

    @Test
    public void turnPhaseEnforcement_clearsDiceOnTurnEnd() {
        testGame.setTurnPhase(TurnPhase.ACTION.toString());
        testGame.setDiceValue(7);

        Game updatedGame = gameService.endTurn(100L, "valid-token");

        assertEquals(null, updatedGame.getDiceValue());
    }

    @Test
    public void completeTurnCycle_rollDiceActionEndTurn() {
        // Player 1's turn in ROLL_DICE phase
        assertEquals(TurnPhase.ROLL_DICE.toString(), testGame.getTurnPhase());
        assertEquals(0, testGame.getCurrentTurnIndex());

        // Roll dice: Transition to ACTION phase
        Game afterRoll = gameService.rollDice(100L, "valid-token");
        assertEquals(TurnPhase.ACTION.toString(), afterRoll.getTurnPhase());
        assertNotNull(afterRoll.getDiceValue());

        // End turn: Transition to Player 2
        testGame.setTurnPhase(TurnPhase.ACTION.toString());
        testGame.setDiceValue(afterRoll.getDiceValue());
        Game afterEndTurn = gameService.endTurn(100L, "valid-token");
        assertEquals(1, afterEndTurn.getCurrentTurnIndex());
        assertEquals(TurnPhase.ROLL_DICE.toString(), afterEndTurn.getTurnPhase());
    }

    private Board createUniformWoodBoardWithDice(int diceValue) {
        Board board = new Board();
        board.generateBoard();
        board.setHexTiles(Collections.nCopies(19, "WOOD"));
        board.setHexTile_DiceNumbers(Collections.nCopies(19, diceValue));
        return board;
    }
}
