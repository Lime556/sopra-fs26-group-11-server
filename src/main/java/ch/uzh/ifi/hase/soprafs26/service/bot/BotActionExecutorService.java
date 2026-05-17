package ch.uzh.ifi.hase.soprafs26.service.bot;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.service.GameService;

@Service
public class BotActionExecutorService {
    private final GameService gameService;
    private final BotFallbackService botFallbackService;
    private final BotAiService botAiService;

    public BotActionExecutorService(GameService gameService, BotFallbackService botFallbackService, BotAiService botAiService) {
        this.gameService = gameService;
        this.botFallbackService = botFallbackService;
        this.botAiService = botAiService;
    }

    public Game executeFallbackAction(Long gameId, String playerToken) {
        return executeBotAction(gameId, playerToken, false);
    }

    public Game executeBotAction(Long gameId, String playerToken, boolean useAi) {
        return executeBotActionWithResult(gameId, playerToken, useAi).game();
    }

    public BotActionExecutionResult executeBotActionWithResult(Long gameId, String playerToken, boolean useAi) {
        Game game = gameService.getGameById(gameId, playerToken);
        BotAction action;
        boolean aiConsultantUsed = false;
        boolean fallbackUsed;

        if (useAi) {
            java.util.Optional<BotAction> aiAction = botAiService.chooseAction(game);
            aiConsultantUsed = aiAction.isPresent();
            fallbackUsed = !aiConsultantUsed;
            action = aiAction.orElseGet(() -> botFallbackService.chooseFallbackAction(game));
        } else {
            fallbackUsed = true;
            action = botFallbackService.chooseFallbackAction(game);
        }

        Long playerId = action == null ? null : action.getPlayerId();

        if (action == null || action.getType() == null || BotActionType.NONE.equals(action.getType())) {
            return new BotActionExecutionResult(game, playerId, useAi, aiConsultantUsed, fallbackUsed);
        }

        try {
            return new BotActionExecutionResult(execute(gameId, playerToken, action), playerId, useAi, aiConsultantUsed, fallbackUsed);
        } catch (ResponseStatusException exception) {
            if (BotActionType.END_TURN.equals(action.getType())) {
                throw exception;
            }
            return new BotActionExecutionResult(gameService.endTurn(gameId, playerToken), playerId, useAi, false, true);
        }
    }

    private Game execute(Long gameId, String playerToken, BotAction action) {
        return switch (action.getType()) {
            case ROLL_DICE -> gameService.rollDice(gameId, playerToken);
            case MOVE_ROBBER -> gameService.moveRobberAndStealFromFirstAdjacentPlayer(
                gameId,
                playerToken,
                action.getPlayerId(),
                action.getHexId()
            );
            case BUILD_INITIAL_SETTLEMENT -> gameService.placeInitialSettlement(
                gameId,
                playerToken,
                action.getPlayerId(),
                action.getIntersectionId()
            );
            case BUILD_INITIAL_ROAD -> gameService.placeInitialRoad(
                gameId,
                playerToken,
                action.getPlayerId(),
                action.getEdgeId()
            );
            case BUILD_CITY -> gameService.upgradeSettlementToCity(
                gameId,
                playerToken,
                action.getPlayerId(),
                action.getIntersectionId()
            );
            case BUILD_SETTLEMENT -> gameService.addSettlementToPlayer(
                gameId,
                playerToken,
                action.getPlayerId(),
                action.getIntersectionId()
            );
            case BUILD_ROAD -> gameService.addRoadToPlayer(
                gameId,
                playerToken,
                action.getPlayerId(),
                action.getEdgeId()
            );
            case BUY_DEVELOPMENT_CARD -> gameService.buyDevelopmentCard(gameId, playerToken, action.getPlayerId());
            case END_TURN -> gameService.endTurn(gameId, playerToken);
            case NONE -> gameService.getGameById(gameId, playerToken);
        };
    }
}
