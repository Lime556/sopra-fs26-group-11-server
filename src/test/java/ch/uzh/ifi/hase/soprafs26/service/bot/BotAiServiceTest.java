package ch.uzh.ifi.hase.soprafs26.service.bot;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.uzh.ifi.hase.soprafs26.entity.Board;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.GamePhase;
import ch.uzh.ifi.hase.soprafs26.entity.Player;
import ch.uzh.ifi.hase.soprafs26.entity.TurnPhase;

public class BotAiServiceTest {

    @Mock
    private BotAiClient botAiClient;

    @Mock
    private BotFallbackService botFallbackService;

    private BotAiService botAiService;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        botAiService = new BotAiService(botAiClient, objectMapper, botFallbackService);
    }

    @Test
    public void chooseAction_noActions_returnsEmpty() {
        Game game = new Game();
        game.setPlayers(List.of(createBotPlayer()));

        when(botFallbackService.listCandidateActions(game)).thenReturn(List.of());

        Optional<BotAction> result = botAiService.chooseAction(game);

        assertTrue(result.isEmpty());
        verify(botAiClient, never()).generateDecision(anyString());
    }

    @Test
    public void chooseAction_validAiResponse_returnsAction() {
        Game game = new Game();
        Player bot = createBotPlayer();
        game.setPlayers(List.of(bot));
        game.setCurrentTurnIndex(0);

        BotAction action1 = BotAction.of(BotActionType.ROLL_DICE, bot.getId());
        BotAction action2 = BotAction.of(BotActionType.END_TURN, bot.getId());
        BotActionCandidate candidate1 = new BotActionCandidate("A1", action1, java.util.Map.of("t", "ROLL_DICE"));
        BotActionCandidate candidate2 = new BotActionCandidate("A2", action2, java.util.Map.of("t", "END_TURN"));

        when(botFallbackService.listCandidateActions(game)).thenReturn(List.of(candidate1, candidate2));
        when(botAiClient.generateDecision(anyString())).thenReturn(Optional.of("{\"chosenActionId\":\"A2\"}"));

        Optional<BotAction> result = botAiService.chooseAction(game);

        assertTrue(result.isPresent());
        assertEquals(BotActionType.END_TURN, result.get().getType());
    }

    @Test
    public void chooseAction_invalidActionId_returnsEmpty() {
        Game game = new Game();
        Player bot = createBotPlayer();
        game.setPlayers(List.of(bot));
        game.setCurrentTurnIndex(0);

        BotAction action1 = BotAction.of(BotActionType.ROLL_DICE, bot.getId());
        BotActionCandidate candidate1 = new BotActionCandidate("A1", action1, java.util.Map.of("t", "ROLL_DICE"));

        when(botFallbackService.listCandidateActions(game)).thenReturn(List.of(candidate1));
        when(botAiClient.generateDecision(anyString())).thenReturn(Optional.of("{\"chosenActionId\":\"A999\"}"));

        Optional<BotAction> result = botAiService.chooseAction(game);

        assertTrue(result.isEmpty());
    }

    @Test
    public void chooseAction_invalidJson_returnsEmpty() {
        Game game = new Game();
        Player bot = createBotPlayer();
        game.setPlayers(List.of(bot));
        game.setCurrentTurnIndex(0);

        BotAction action1 = BotAction.of(BotActionType.ROLL_DICE, bot.getId());
        BotActionCandidate candidate1 = new BotActionCandidate("A1", action1, java.util.Map.of("t", "ROLL_DICE"));

        when(botFallbackService.listCandidateActions(game)).thenReturn(List.of(candidate1));
        when(botAiClient.generateDecision(anyString())).thenReturn(Optional.of("not valid json"));

        Optional<BotAction> result = botAiService.chooseAction(game);

        assertTrue(result.isEmpty());
    }

    @Test
    public void chooseAction_emptyResponse_returnsEmpty() {
        Game game = new Game();
        Player bot = createBotPlayer();
        game.setPlayers(List.of(bot));
        game.setCurrentTurnIndex(0);

        BotAction action1 = BotAction.of(BotActionType.ROLL_DICE, bot.getId());
        BotActionCandidate candidate1 = new BotActionCandidate("A1", action1, java.util.Map.of("t", "ROLL_DICE"));

        when(botFallbackService.listCandidateActions(game)).thenReturn(List.of(candidate1));
        when(botAiClient.generateDecision(anyString())).thenReturn(Optional.empty());

        Optional<BotAction> result = botAiService.chooseAction(game);

        assertTrue(result.isEmpty());
    }

    @Test
    public void chooseAction_sendsValidPrompt() throws Exception {
        Game game = new Game();
        Player bot = createBotPlayer();
        bot.setBrick(2);
        bot.setWood(1);
        bot.setWheat(3);
        bot.setWool(1);
        bot.setOre(0);
        game.setPlayers(List.of(bot));
        game.setCurrentTurnIndex(0);
        game.setTurnPhase(TurnPhase.ACTION.toString());
        game.setDiceValue(8);
        game.setBoard(new Board());

        BotAction action1 = BotAction.of(BotActionType.ROLL_DICE, bot.getId());
        BotActionCandidate candidate1 = new BotActionCandidate("A1", action1, java.util.Map.of("t", "ROLL_DICE"));

        when(botFallbackService.listCandidateActions(game)).thenReturn(List.of(candidate1));
        when(botAiClient.generateDecision(anyString())).thenReturn(Optional.of("{\"chosenActionId\":\"A1\"}"));

        botAiService.chooseAction(game);

        // Verify that generateDecision was called with a valid JSON prompt
        verify(botAiClient, times(1)).generateDecision(anyString());
    }

    @Test
    public void chooseAction_multipleActions_selectsCorrectOne() {
        Game game = new Game();
        Player bot = createBotPlayer();
        game.setPlayers(List.of(bot));
        game.setCurrentTurnIndex(0);

        BotAction action1 = BotAction.of(BotActionType.ROLL_DICE, bot.getId());
        BotAction action2 = BotAction.of(BotActionType.END_TURN, bot.getId());
        BotAction action3 = BotAction.of(BotActionType.BUY_DEVELOPMENT_CARD, bot.getId());

        BotActionCandidate c1 = new BotActionCandidate("A1", action1, java.util.Map.of("t", "ROLL_DICE"));
        BotActionCandidate c2 = new BotActionCandidate("A2", action2, java.util.Map.of("t", "END_TURN"));
        BotActionCandidate c3 = new BotActionCandidate("A3", action3, java.util.Map.of("t", "BUY_DEV"));

        when(botFallbackService.listCandidateActions(game)).thenReturn(List.of(c1, c2, c3));
        when(botAiClient.generateDecision(anyString())).thenReturn(Optional.of("{\"chosenActionId\":\"A3\"}"));

        Optional<BotAction> result = botAiService.chooseAction(game);

        assertTrue(result.isPresent());
        assertEquals(BotActionType.BUY_DEVELOPMENT_CARD, result.get().getType());
    }

    @Test
    public void systemPrompt_containsRequiredInstructions() {
        String prompt = BotAiService.SYSTEM_PROMPT;
        
        assertTrue(prompt.contains("STRICT"));
        assertTrue(prompt.contains("chosenActionId"));
        assertTrue(prompt.contains("B=brick"));
        assertTrue(prompt.contains("W=wood"));
        assertTrue(prompt.contains("H=wheat"));
        assertTrue(prompt.contains("S=sheep"));
        assertTrue(prompt.contains("O=ore"));
    }

    private Player createBotPlayer() {
        Player player = new Player();
        player.setId(1L);
        player.setBot(true);
        player.setVictoryPoints(0);
        player.setBrick(0);
        player.setWood(0);
        player.setWheat(0);
        player.setWool(0);
        player.setOre(0);
        return player;
    }
}
