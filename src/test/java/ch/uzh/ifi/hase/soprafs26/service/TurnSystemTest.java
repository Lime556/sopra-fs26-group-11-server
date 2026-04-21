package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Player;
import ch.uzh.ifi.hase.soprafs26.entity.TurnPhase;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GamePostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PlayerGetDTO;
import java.util.Arrays;
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
}
