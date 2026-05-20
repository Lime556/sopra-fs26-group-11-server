package ch.uzh.ifi.hase.soprafs26.service.bot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameEventDTO;
import ch.uzh.ifi.hase.soprafs26.service.GameService;

class BotActionExecutorServiceTest {

    private GameService gameService;
    private BotFallbackService botFallbackService;
    private BotAiService botAiService;
    private BotActionExecutorService executor;

    @BeforeEach
    void setup() {
        gameService = Mockito.mock(GameService.class);
        botFallbackService = Mockito.mock(BotFallbackService.class);
        botAiService = Mockito.mock(BotAiService.class);
        executor = new BotActionExecutorService(gameService, botFallbackService, botAiService);
    }

    @Test
    void executeBotActionWithResult_aiChoosesBankTrade_executesBankTradePayload() {
        Game game = new Game();
        game.setId(77L);
        game.setGameVersion(5L);
        Game updatedGame = new Game();
        updatedGame.setId(77L);
        updatedGame.setGameVersion(6L);

        BotAction bankTrade = BotAction.bankTrade(10L, "ore", "wool", 4, 1);
        BotActionCandidate tradeCandidate = new BotActionCandidate(
            "A1",
            bankTrade,
            Map.of("t", "BANK_TRADE", "give", "ore", "receive", "wool", "enables", "settlement")
        );
        BotActionCandidate endTurnCandidate = new BotActionCandidate(
            "A2",
            BotAction.of(BotActionType.END_TURN, 10L),
            Map.of("t", "END_TURN")
        );
        List<BotActionCandidate> candidates = List.of(tradeCandidate, endTurnCandidate);

        when(gameService.getGameById(77L, "token")).thenReturn(game);
        when(botFallbackService.listCandidateActions(game)).thenReturn(candidates);
        when(botAiService.chooseAction(game)).thenReturn(Optional.of(bankTrade));
        when(gameService.applyBankTrade(eq(77L), eq("token"), any(GameEventDTO.class))).thenReturn(updatedGame);

        BotActionExecutionResult result = executor.executeBotActionWithResult(77L, "token", true);

        assertSame(updatedGame, result.game());
        assertEquals(10L, result.playerId());
        assertTrue(result.aiRequested());
        assertTrue(result.aiConsultantUsed());
        assertFalse(result.fallbackUsed());

        ArgumentCaptor<GameEventDTO> eventCaptor = ArgumentCaptor.forClass(GameEventDTO.class);
        verify(gameService).applyBankTrade(eq(77L), eq("token"), eventCaptor.capture());
        GameEventDTO event = eventCaptor.getValue();
        assertEquals("BANK_TRADE", event.getType());
        assertEquals(10L, event.getSourcePlayerId());
        assertEquals(4, event.getGiveResources().get("ore"));
        assertEquals(1, event.getReceiveResources().get("wool"));
        assertEquals(Integer.valueOf(4), event.getGiveAmount());
        assertEquals(Integer.valueOf(1), event.getReceiveAmount());
    }

    @Test
    void executeBotActionWithResult_fallbackChoosesBotPlayerTrade_executesInternalBotTrade() {
        Game game = new Game();
        game.setId(88L);
        Game updatedGame = new Game();
        updatedGame.setId(88L);

        BotAction playerTrade = BotAction.playerTrade(10L, 11L, "ore", "wool", 1, 1);
        BotActionCandidate tradeCandidate = new BotActionCandidate(
            "A1",
            playerTrade,
            Map.of("t", "PLAYER_TRADE", "targetPlayerId", 11L, "enables", "settlement")
        );
        BotActionCandidate endTurnCandidate = new BotActionCandidate(
            "A2",
            BotAction.of(BotActionType.END_TURN, 10L),
            Map.of("t", "END_TURN")
        );

        when(gameService.getGameById(88L, "token")).thenReturn(game);
        when(botFallbackService.listCandidateActions(game)).thenReturn(List.of(endTurnCandidate, tradeCandidate));
        when(gameService.applyBotPlayerTrade(eq(88L), eq("token"), any(GameEventDTO.class))).thenReturn(updatedGame);

        BotActionExecutionResult result = executor.executeBotActionWithResult(88L, "token", false);

        assertSame(updatedGame, result.game());
        assertFalse(result.aiRequested());
        assertFalse(result.aiConsultantUsed());
        assertTrue(result.fallbackUsed());
        assertEquals("AI not requested", result.fallbackReason());

        ArgumentCaptor<GameEventDTO> eventCaptor = ArgumentCaptor.forClass(GameEventDTO.class);
        verify(gameService).applyBotPlayerTrade(eq(88L), eq("token"), eventCaptor.capture());
        GameEventDTO event = eventCaptor.getValue();
        assertEquals("PLAYER_TRADE_FINALIZE", event.getType());
        assertEquals(10L, event.getSourcePlayerId());
        assertEquals(11L, event.getTargetPlayerId());
        assertEquals(1, event.getGiveResources().get("ore"));
        assertEquals(1, event.getReceiveResources().get("wool"));
    }
}
