package ch.uzh.ifi.hase.soprafs26.service.bot;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.service.GameService;

@Service
public class BotActionExecutorService {
    private final GameService gameService;
    private final BotFallbackService botFallbackService;

    public BotActionExecutorService(GameService gameService, BotFallbackService botFallbackService) {
        this.gameService = gameService;
        this.botFallbackService = botFallbackService;
    }

    public Game executeFallbackAction(Long gameId, String playerToken) {
        Game game = gameService.getGameById(gameId, playerToken);
        BotAction action = botFallbackService.chooseFallbackAction(game);

        if (action == null || action.getType() == null || BotActionType.NONE.equals(action.getType())) {
            return game;
        }

        try {
            return execute(gameId, playerToken, action);
        } catch (ResponseStatusException exception) {
            if (BotActionType.END_TURN.equals(action.getType())) {
                throw exception;
            }
            return gameService.endTurn(gameId, playerToken);
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
