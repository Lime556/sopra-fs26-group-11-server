package ch.uzh.ifi.hase.soprafs26.service.bot;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.service.GameService;

class BotActionExecutorServiceTest {

    @Mock
    private GameService gameService;

    @Mock
    private BotFallbackService botFallbackService;

    @Mock
    private BotAiService botAiService;

    private BotActionExecutorService service;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        service = new BotActionExecutorService(gameService, botFallbackService, botAiService);
    }

    @Test
    void executeBotActionWithResult_noCandidates_returnsNoneAndFallbackReason() {
        Game initial = gameWithVersion(1L);
        when(gameService.getGameById(1L, "token")).thenReturn(initial);
        when(botFallbackService.listCandidateActions(initial)).thenReturn(List.of());

        BotActionExecutionResult result = service.executeBotActionWithResult(1L, "token", false);

        assertEquals(initial, result.game());
        assertTrue(result.fallbackUsed());
        assertEquals("no legal bot action available", result.fallbackReason());
        assertEquals(null, result.playerId());
        verify(botAiService, never()).chooseAction(any());
    }

    @Test
    void executeBotActionWithResult_aiSkippedForcedRoll_executesRollFallback() {
        Game initial = gameWithVersion(2L);
        Game afterRoll = gameWithVersion(3L);
        BotAction roll = BotAction.of(BotActionType.ROLL_DICE, 99L);
        BotActionCandidate candidate = candidate("A1", roll, Map.of("t", "ROLL_DICE"));

        when(gameService.getGameById(1L, "token")).thenReturn(initial);
        when(botFallbackService.listCandidateActions(initial)).thenReturn(List.of(candidate));
        when(gameService.rollDice(1L, "token")).thenReturn(afterRoll);

        BotActionExecutionResult result = service.executeBotActionWithResult(1L, "token", true);

        assertEquals(afterRoll, result.game());
        assertTrue(result.fallbackUsed());
        assertFalse(result.aiConsultantUsed());
        assertEquals("only forced action available: ROLL_DICE", result.fallbackReason());
    }

    @Test
    void executeBotActionWithResult_aiReturnsLegalAction_usesAiRecommendation() {
        Game requested = gameWithVersion(10L);
        Game current = gameWithVersion(10L);
        Game afterEndTurn = gameWithVersion(11L);

        BotAction road = BotAction.road(BotActionType.BUILD_ROAD, 7L, 5);
        BotAction settlement = BotAction.settlement(BotActionType.BUILD_SETTLEMENT, 7L, 6);
        BotAction endTurn = BotAction.of(BotActionType.END_TURN, 7L);
        BotActionCandidate c1 = candidate("A1", road, Map.of("score", 1));
        BotActionCandidate c2 = candidate("A2", settlement, Map.of("score", 2));
        BotActionCandidate c3 = candidate("A3", endTurn, Map.of("score", 0));

        when(gameService.getGameById(1L, "token")).thenReturn(requested, current);
        when(botFallbackService.listCandidateActions(requested)).thenReturn(List.of(c1, c2, c3));
        when(botAiService.chooseAction(requested)).thenReturn(Optional.of(BotAction.of(BotActionType.END_TURN, 7L)));
        when(botFallbackService.listCandidateActions(current)).thenReturn(List.of(c3));
        when(gameService.endTurn(1L, "token")).thenReturn(afterEndTurn);

        BotActionExecutionResult result = service.executeBotActionWithResult(1L, "token", true);

        assertEquals(afterEndTurn, result.game());
        assertFalse(result.fallbackUsed());
        assertTrue(result.aiConsultantUsed());
        assertEquals(null, result.fallbackReason());
    }

    @Test
    void executeBotActionWithResult_aiActionNoLongerLegal_sameVersion_usesFallbackReason() {
        Game requested = gameWithVersion(10L);
        Game current = gameWithVersion(10L);
        Game afterFallback = gameWithVersion(11L);

        BotAction buildRoad = BotAction.road(BotActionType.BUILD_ROAD, 9L, 3);
        BotAction buildSettlement = BotAction.settlement(BotActionType.BUILD_SETTLEMENT, 9L, 2);
        BotActionCandidate c1 = candidate("A1", buildRoad, Map.of("score", 5));
        BotActionCandidate c2 = candidate("A2", buildSettlement, Map.of("score", 4));

        when(gameService.getGameById(1L, "token")).thenReturn(requested, current);
        when(botFallbackService.listCandidateActions(requested)).thenReturn(List.of(c1, c2));
        when(botAiService.chooseAction(requested)).thenReturn(Optional.of(BotAction.of(BotActionType.END_TURN, 9L)));
        when(botFallbackService.listCandidateActions(current)).thenReturn(List.of(c1, c2));
        when(gameService.addRoadToPlayer(1L, "token", 9L, 3)).thenReturn(afterFallback);
        when(gameService.addSettlementToPlayer(1L, "token", 9L, 2)).thenReturn(afterFallback);

        BotActionExecutionResult result = service.executeBotActionWithResult(1L, "token", true);

        assertTrue(result.fallbackUsed());
        assertEquals("AI recommendation is no longer legal", result.fallbackReason());
        assertNotNull(result.game());
    }

    @Test
    void executeBotActionWithResult_aiActionNoLongerLegal_changedVersion_usesChangedStateReason() {
        Game requested = gameWithVersion(10L);
        Game current = gameWithVersion(12L);
        Game afterFallback = gameWithVersion(13L);

        BotAction buildRoad = BotAction.road(BotActionType.BUILD_ROAD, 9L, 4);
        BotAction buildSettlement = BotAction.settlement(BotActionType.BUILD_SETTLEMENT, 9L, 6);
        BotActionCandidate c1 = candidate("A1", buildRoad, Map.of("score", 4));
        BotActionCandidate c2 = candidate("A2", buildSettlement, Map.of("score", 3));

        when(gameService.getGameById(1L, "token")).thenReturn(requested, current);
        when(botFallbackService.listCandidateActions(requested)).thenReturn(List.of(c1, c2));
        when(botAiService.chooseAction(requested)).thenReturn(Optional.of(BotAction.of(BotActionType.END_TURN, 9L)));
        when(botFallbackService.listCandidateActions(current)).thenReturn(List.of(c1, c2));
        when(gameService.addRoadToPlayer(1L, "token", 9L, 4)).thenReturn(afterFallback);
        when(gameService.addSettlementToPlayer(1L, "token", 9L, 6)).thenReturn(afterFallback);

        BotActionExecutionResult result = service.executeBotActionWithResult(1L, "token", true);

        assertTrue(result.fallbackUsed());
        assertEquals("game state changed while waiting for AI", result.fallbackReason());
        assertNotNull(result.game());
    }

    @Test
    void executeBotActionWithResult_actionFails_nonEndTurnFallsBackToEndTurn() {
        Game initial = gameWithVersion(1L);
        Game afterEndTurn = gameWithVersion(2L);
        BotAction road = BotAction.road(BotActionType.BUILD_ROAD, 3L, 8);

        when(gameService.getGameById(1L, "token")).thenReturn(initial);
        when(botFallbackService.listCandidateActions(initial)).thenReturn(List.of(candidate("A1", road, Map.of("score", 3))));
        when(gameService.addRoadToPlayer(1L, "token", 3L, 8))
            .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "road blocked"));
        when(gameService.endTurn(1L, "token")).thenReturn(afterEndTurn);

        BotActionExecutionResult result = service.executeBotActionWithResult(1L, "token", false);

        assertTrue(result.fallbackUsed());
        assertEquals(afterEndTurn, result.game());
        assertTrue(result.fallbackReason().contains("legal move validation failed"));
    }

    @Test
    void executeBotActionWithResult_endTurnFailure_rethrowsException() {
        Game initial = gameWithVersion(1L);
        BotAction endTurn = BotAction.of(BotActionType.END_TURN, 5L);

        when(gameService.getGameById(1L, "token")).thenReturn(initial);
        when(botFallbackService.listCandidateActions(initial)).thenReturn(List.of(candidate("A1", endTurn, Map.of())));
        when(gameService.endTurn(1L, "token"))
            .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "turn mismatch"));

        assertThrows(ResponseStatusException.class, () -> service.executeBotActionWithResult(1L, "token", false));
    }

    @Test
    void privateHelpers_coverConditionalBranches() {
        List<BotActionCandidate> withNulls = Arrays.asList(
            null,
            new BotActionCandidate("A1", null, Map.of()),
            candidate("A2", BotAction.of(BotActionType.END_TURN, 1L), Map.of()),
            candidate("A3", BotAction.of(BotActionType.ROLL_DICE, 1L), Map.of())
        );

        List<BotActionCandidate> meaningful = invokePrivate(service, "meaningfulCandidates",
            new Class<?>[] {List.class}, withNulls);
        assertEquals(1, meaningful.size());

        boolean askAiFalse = invokePrivate(service, "shouldAskAi", new Class<?>[] {List.class}, List.of(candidate("A1",
            BotAction.of(BotActionType.ROLL_DICE, 1L), Map.of())));
        boolean askAiTrue = invokePrivate(service, "shouldAskAi", new Class<?>[] {List.class}, List.of(
            candidate("A1", BotAction.of(BotActionType.ROLL_DICE, 1L), Map.of()),
            candidate("A2", BotAction.of(BotActionType.BUILD_ROAD, 1L), Map.of())
        ));
        assertFalse(askAiFalse);
        assertTrue(askAiTrue);

        String emptyReason = invokePrivate(service, "explainAiSkipped", new Class<?>[] {List.class}, List.of());
        String rollReason = invokePrivate(service, "explainAiSkipped", new Class<?>[] {List.class}, List.of(
            candidate("A1", BotAction.of(BotActionType.ROLL_DICE, 1L), Map.of()),
            candidate("A2", BotAction.of(BotActionType.ROLL_DICE, 1L), Map.of())
        ));
        String forcedUnknown = invokePrivate(service, "explainAiSkipped", new Class<?>[] {List.class},
            List.of(new BotActionCandidate("A9", BotAction.of(null, 1L), Map.of())));
        String forcedEndTurn = invokePrivate(service, "explainAiSkipped", new Class<?>[] {List.class},
            List.of(candidate("A8", BotAction.of(BotActionType.END_TURN, 1L), Map.of())));
        String noMeaningful = invokePrivate(service, "explainAiSkipped", new Class<?>[] {List.class}, List.of(
            candidate("A1", BotAction.of(BotActionType.ROLL_DICE, 1L), Map.of()),
            candidate("A2", BotAction.of(BotActionType.BUILD_ROAD, 1L), Map.of())
        ));
        assertEquals("no legal bot action available", emptyReason);
        assertEquals("bot must roll dice before making decisions", rollReason);
        assertEquals("only forced action available", forcedUnknown);
        assertEquals("only forced action available: END_TURN", forcedEndTurn);
        assertEquals("no meaningful AI decision available", noMeaningful);

        List<BotActionCandidate> meaningfulWhenNull = invokePrivate(service, "meaningfulCandidates",
            new Class<?>[] {List.class}, (Object) null);
        List<BotActionCandidate> meaningfulOnlyEndTurn = invokePrivate(service, "meaningfulCandidates",
            new Class<?>[] {List.class}, List.of(candidate("A1", BotAction.of(BotActionType.END_TURN, 1L), Map.of())));
        assertTrue(meaningfulWhenNull.isEmpty());
        assertEquals(1, meaningfulOnlyEndTurn.size());

        BotAction none = invokePrivate(service, "chooseFallbackFromCandidates",
            new Class<?>[] {List.class}, Arrays.asList((BotActionCandidate) null));
        assertEquals(BotActionType.NONE, none.getType());

        Integer nullScore = invokePrivate(service, "scoreCandidate", new Class<?>[] {BotActionCandidate.class}, (Object) null);
        Integer typedScore = invokePrivate(service, "scoreCandidate", new Class<?>[] {BotActionCandidate.class},
            candidate("A1", BotAction.settlement(BotActionType.BUILD_SETTLEMENT, 1L, 9),
                Map.of("score", 3, "threat", 2, "hex", List.of(1, 2))));
        assertTrue(nullScore < typedScore);

        Optional<BotAction> noneFound = invokePrivate(service, "findEquivalentAction",
            new Class<?>[] {BotAction.class, List.class}, BotAction.of(BotActionType.END_TURN, 5L), List.of());
        Optional<BotAction> noneFoundForNullArgs = invokePrivate(service, "findEquivalentAction",
            new Class<?>[] {BotAction.class, List.class}, null, null);
        Optional<BotAction> found = invokePrivate(service, "findEquivalentAction",
            new Class<?>[] {BotAction.class, List.class}, BotAction.of(BotActionType.END_TURN, 5L),
            List.of(candidate("A1", BotAction.of(BotActionType.END_TURN, 5L), Map.of())));
        assertTrue(noneFound.isEmpty());
        assertTrue(noneFoundForNullArgs.isEmpty());
        assertTrue(found.isPresent());

        boolean sameFalse = invokePrivate(service, "sameAction",
            new Class<?>[] {BotAction.class, BotAction.class},
            BotAction.of(BotActionType.END_TURN, 1L), BotAction.of(BotActionType.ROLL_DICE, 1L));
        boolean sameTrue = invokePrivate(service, "sameAction",
            new Class<?>[] {BotAction.class, BotAction.class},
            BotAction.road(BotActionType.BUILD_ROAD, 1L, 4), BotAction.road(BotActionType.BUILD_ROAD, 1L, 4));
        boolean sameFalseNull = invokePrivate(service, "sameAction",
            new Class<?>[] {BotAction.class, BotAction.class},
            null, BotAction.road(BotActionType.BUILD_ROAD, 1L, 4));
        boolean sameFalseDifferentFields = invokePrivate(service, "sameAction",
            new Class<?>[] {BotAction.class, BotAction.class},
            BotAction.road(BotActionType.BUILD_ROAD, 1L, 4), BotAction.road(BotActionType.BUILD_ROAD, 2L, 4));
        assertFalse(sameFalse);
        assertFalse(sameFalseNull);
        assertFalse(sameFalseDifferentFields);
        assertTrue(sameTrue);

        Boolean sameVersionNulls = invokePrivate(service, "sameVersion", new Class<?>[] {Long.class, Long.class}, null, null);
        Boolean sameVersionDifferent = invokePrivate(service, "sameVersion", new Class<?>[] {Long.class, Long.class}, 1L, 2L);
        assertTrue(Boolean.TRUE.equals(sameVersionNulls));
        assertFalse(Boolean.TRUE.equals(sameVersionDifferent));

        for (BotActionType type : BotActionType.values()) {
            BotAction action = switch (type) {
                case BUILD_INITIAL_SETTLEMENT, BUILD_SETTLEMENT, BUILD_CITY -> BotAction.settlement(type, 1L, 3);
                case BUILD_INITIAL_ROAD, BUILD_ROAD -> BotAction.road(type, 1L, 9);
                case MOVE_ROBBER -> BotAction.robber(1L, 2);
                default -> BotAction.of(type, 1L);
            };
            Integer score = invokePrivate(service, "scoreCandidate", new Class<?>[] {BotActionCandidate.class},
                candidate("AX", action, Map.of("score", 1, "threat", 1, "hex", List.of(1))));
            assertNotNull(score);
        }
    }

    @Test
    void executeSwitch_routesEveryActionType() {
        Game expected = new Game();
        when(gameService.rollDice(anyLong(), anyString())).thenReturn(expected);
        when(gameService.moveRobberAndStealFromFirstAdjacentPlayer(anyLong(), anyString(), anyLong(), any())).thenReturn(expected);
        when(gameService.placeInitialSettlement(anyLong(), anyString(), anyLong(), any())).thenReturn(expected);
        when(gameService.placeInitialRoad(anyLong(), anyString(), anyLong(), any())).thenReturn(expected);
        when(gameService.upgradeSettlementToCity(anyLong(), anyString(), anyLong(), any())).thenReturn(expected);
        when(gameService.addSettlementToPlayer(anyLong(), anyString(), anyLong(), any())).thenReturn(expected);
        when(gameService.addRoadToPlayer(anyLong(), anyString(), anyLong(), any())).thenReturn(expected);
        when(gameService.buyDevelopmentCard(anyLong(), anyString(), anyLong())).thenReturn(expected);
        when(gameService.endTurn(anyLong(), anyString())).thenReturn(expected);
        when(gameService.getGameById(anyLong(), anyString())).thenReturn(expected);

        BotAction[] actions = new BotAction[] {
            BotAction.of(BotActionType.ROLL_DICE, 1L),
            BotAction.robber(1L, 2),
            BotAction.settlement(BotActionType.BUILD_INITIAL_SETTLEMENT, 1L, 3),
            BotAction.road(BotActionType.BUILD_INITIAL_ROAD, 1L, 4),
            BotAction.settlement(BotActionType.BUILD_CITY, 1L, 5),
            BotAction.settlement(BotActionType.BUILD_SETTLEMENT, 1L, 6),
            BotAction.road(BotActionType.BUILD_ROAD, 1L, 7),
            BotAction.of(BotActionType.BUY_DEVELOPMENT_CARD, 1L),
            BotAction.of(BotActionType.END_TURN, 1L),
            BotAction.of(BotActionType.NONE, 1L)
        };

        for (BotAction action : actions) {
            Game result = invokePrivate(service, "execute",
                new Class<?>[] {Long.class, String.class, BotAction.class}, 1L, "token", action);
            assertNotNull(result);
        }

        verify(gameService).rollDice(1L, "token");
        verify(gameService).moveRobberAndStealFromFirstAdjacentPlayer(1L, "token", 1L, 2);
        verify(gameService).placeInitialSettlement(1L, "token", 1L, 3);
        verify(gameService).placeInitialRoad(1L, "token", 1L, 4);
        verify(gameService).upgradeSettlementToCity(1L, "token", 1L, 5);
        verify(gameService).addSettlementToPlayer(1L, "token", 1L, 6);
        verify(gameService).addRoadToPlayer(1L, "token", 1L, 7);
        verify(gameService).buyDevelopmentCard(1L, "token", 1L);
        verify(gameService).endTurn(1L, "token");
        verify(gameService).getGameById(1L, "token");
    }

    private Game gameWithVersion(Long version) {
        Game game = new Game();
        game.setGameVersion(version);
        return game;
    }

    private BotActionCandidate candidate(String id, BotAction action, Map<String, Object> details) {
        return new BotActionCandidate(id, action, details);
    }

    @SuppressWarnings("unchecked")
    private <T> T invokePrivate(Object target, String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = target.getClass().getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return (T) method.invoke(target, args);
        } catch (Exception exception) {
            throw new AssertionError("Failed to invoke private method " + methodName, exception);
        }
    }
}
