package ch.uzh.ifi.hase.soprafs26.service.bot;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.service.GameService;

@Service
public class BotActionExecutorService {
    private static final Logger log = LoggerFactory.getLogger(BotActionExecutorService.class);
    private static final ConcurrentMap<Long, Object> GAME_LOCKS = new ConcurrentHashMap<>();

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
        Object lock = GAME_LOCKS.computeIfAbsent(gameId == null ? -1L : gameId, ignored -> new Object());
        synchronized (lock) {
            return executeBotActionWithResultLocked(gameId, playerToken, useAi);
        }
    }

    private BotActionExecutionResult executeBotActionWithResultLocked(Long gameId, String playerToken, boolean useAi) {
        Game game = gameService.getGameById(gameId, playerToken);
        List<BotActionCandidate> candidates = botFallbackService.listCandidateActions(game);
        BotAction action;
        boolean aiConsultantUsed = false;
        boolean fallbackUsed;
        String fallbackReason = null;

        if (candidates.isEmpty()) {
            action = BotAction.of(BotActionType.NONE, null);
            fallbackUsed = true;
            fallbackReason = "no legal bot action available";
        } else if (useAi && shouldAskAi(candidates)) {
            Long requestedVersion = game.getGameVersion();
            Optional<BotAction> aiAction = botAiService.chooseAction(game);
            Game currentGame = gameService.getGameById(gameId, playerToken);
            List<BotActionCandidate> currentCandidates = botFallbackService.listCandidateActions(currentGame);

            if (aiAction.isPresent()) {
                Optional<BotAction> stillLegalAction = findEquivalentAction(aiAction.get(), currentCandidates);
                if (stillLegalAction.isPresent()) {
                    aiConsultantUsed = true;
                    fallbackUsed = false;
                    action = stillLegalAction.get();
                    if (!sameVersion(requestedVersion, currentGame.getGameVersion())) {
                        log.info("Bot AI recommendation reused after refreshing changed game state from version {} to {}.",
                            requestedVersion, currentGame.getGameVersion());
                    }
                } else {
                    fallbackUsed = true;
                    fallbackReason = sameVersion(requestedVersion, currentGame.getGameVersion())
                        ? "AI recommendation is no longer legal"
                        : "game state changed while waiting for AI";
                    action = chooseFallbackFromFreshState(currentGame);
                }
            } else {
                fallbackUsed = true;
                fallbackReason = "AI returned no valid recommendation";
                action = chooseFallbackFromFreshState(currentGame);
            }
        } else {
            fallbackUsed = true;
            fallbackReason = useAi ? explainAiSkipped(candidates) : "AI not requested";
            action = chooseFallbackFromCandidates(candidates);
        }

        Long playerId = action == null ? null : action.getPlayerId();

        if (action == null || action.getType() == null || BotActionType.NONE.equals(action.getType())) {
            return new BotActionExecutionResult(game, playerId, useAi, aiConsultantUsed, fallbackUsed, fallbackReason);
        }

        try {
            return new BotActionExecutionResult(execute(gameId, playerToken, action), playerId, useAi, aiConsultantUsed, fallbackUsed, fallbackReason);
        } catch (ResponseStatusException exception) {
            if (BotActionType.END_TURN.equals(action.getType())) {
                throw exception;
            }
            log.info("Bot action {} failed with status {}; ending turn as fallback. Reason: {}",
                action.getType(), exception.getStatusCode(), exception.getReason());
            return new BotActionExecutionResult(gameService.endTurn(gameId, playerToken), playerId, useAi, false, true,
                "legal move validation failed: " + exception.getReason());
        }
    }

    private boolean shouldAskAi(List<BotActionCandidate> candidates) {
        List<BotActionCandidate> meaningful = meaningfulCandidates(candidates);
        if (meaningful.size() <= 1) {
            return false;
        }

        return meaningful.stream()
            .map(BotActionCandidate::action)
            .map(BotAction::getType)
            .anyMatch(type -> !BotActionType.ROLL_DICE.equals(type) && !BotActionType.END_TURN.equals(type));
    }

    private String explainAiSkipped(List<BotActionCandidate> candidates) {
        List<BotActionCandidate> meaningful = meaningfulCandidates(candidates);
        if (candidates == null || candidates.isEmpty()) {
            return "no legal bot action available";
        }
        if (meaningful.size() <= 1) {
            BotAction action = meaningful.isEmpty() ? candidates.get(0).action() : meaningful.get(0).action();
            BotActionType type = action == null ? null : action.getType();
            return type == null ? "only forced action available" : "only forced action available: " + type;
        }
        if (meaningful.stream().allMatch(candidate -> BotActionType.ROLL_DICE.equals(candidate.action().getType()))) {
            return "bot must roll dice before making decisions";
        }
        return "no meaningful AI decision available";
    }

    private List<BotActionCandidate> meaningfulCandidates(List<BotActionCandidate> candidates) {
        if (candidates == null) {
            return List.of();
        }
        List<BotActionCandidate> withoutEndTurn = candidates.stream()
            .filter(candidate -> candidate != null && candidate.action() != null)
            .filter(candidate -> !BotActionType.END_TURN.equals(candidate.action().getType()))
            .toList();
        return withoutEndTurn.isEmpty() ? candidates : withoutEndTurn;
    }

    private BotAction chooseFallbackFromFreshState(Game game) {
        List<BotActionCandidate> freshCandidates = botFallbackService.listCandidateActions(game);
        return chooseFallbackFromCandidates(freshCandidates);
    }

    private BotAction chooseFallbackFromCandidates(List<BotActionCandidate> candidates) {
        return candidates == null || candidates.isEmpty()
            ? BotAction.of(BotActionType.NONE, null)
            : candidates.stream()
                .filter(candidate -> candidate != null && candidate.action() != null)
                .max(this::compareCandidates)
                .map(BotActionCandidate::action)
                .orElse(BotAction.of(BotActionType.NONE, null));
    }

    private int compareCandidates(BotActionCandidate left, BotActionCandidate right) {
        return Integer.compare(scoreCandidate(left), scoreCandidate(right));
    }

    private int scoreCandidate(BotActionCandidate candidate) {
        if (candidate == null || candidate.action() == null) {
            return Integer.MIN_VALUE;
        }
        BotAction action = candidate.action();
        int score = switch (action.getType()) {
            case BUILD_INITIAL_SETTLEMENT, BUILD_SETTLEMENT -> 1000;
            case BUILD_CITY -> 900;
            case BUILD_INITIAL_ROAD, BUILD_ROAD -> 700;
            case BUY_DEVELOPMENT_CARD -> 500;
            case MOVE_ROBBER -> 400;
            case ROLL_DICE -> 300;
            case END_TURN -> 0;
            case NONE -> -100;
        };

        Object productionScore = candidate.promptDetails() == null ? null : candidate.promptDetails().get("score");
        if (productionScore instanceof Number number) {
            score += number.intValue() * 20;
        }
        Object threatScore = candidate.promptDetails() == null ? null : candidate.promptDetails().get("threat");
        if (threatScore instanceof Number number) {
            score += number.intValue() * 10;
        }
        Object hexSummary = candidate.promptDetails() == null ? null : candidate.promptDetails().get("hex");
        if (hexSummary instanceof List<?> adjacentHexes) {
            score += adjacentHexes.size() * 60;
        }

        return score;
    }

    private Optional<BotAction> findEquivalentAction(BotAction action, List<BotActionCandidate> candidates) {
        if (action == null || candidates == null) {
            return Optional.empty();
        }
        return candidates.stream()
            .map(BotActionCandidate::action)
            .filter(candidateAction -> sameAction(action, candidateAction))
            .findFirst();
    }

    private boolean sameAction(BotAction left, BotAction right) {
        if (left == null || right == null || left.getType() != right.getType()) {
            return false;
        }
        return java.util.Objects.equals(left.getPlayerId(), right.getPlayerId())
            && java.util.Objects.equals(left.getIntersectionId(), right.getIntersectionId())
            && java.util.Objects.equals(left.getEdgeId(), right.getEdgeId())
            && java.util.Objects.equals(left.getHexId(), right.getHexId());
    }

    private boolean sameVersion(Long left, Long right) {
        long a = left == null ? 0L : left;
        long b = right == null ? 0L : right;
        return a == b;
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
