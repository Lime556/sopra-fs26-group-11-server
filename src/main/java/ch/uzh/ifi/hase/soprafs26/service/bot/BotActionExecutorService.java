package ch.uzh.ifi.hase.soprafs26.service.bot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameEventDTO;
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
            return candidates != null
                && candidates.stream().anyMatch(candidate -> candidate != null
                    && candidate.action() != null
                    && isTradeAction(candidate.action().getType()))
                && candidates.stream().anyMatch(candidate -> candidate != null
                    && candidate.action() != null
                    && BotActionType.END_TURN.equals(candidate.action().getType()));
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
            case BUILD_INITIAL_SETTLEMENT -> 1200;
            case BUILD_SETTLEMENT -> 1100;
            case BUILD_CITY -> 1050;
            case BUILD_INITIAL_ROAD -> 700;
            case BUILD_ROAD -> 180;
            case BUY_DEVELOPMENT_CARD -> 120;
            case BANK_TRADE -> 1060;
            case PLAYER_TRADE -> 980;
            case MOVE_ROBBER -> 400;
            case ROLL_DICE -> 300;
            case END_TURN -> 220;
            case NONE -> -100;
        };

        Object productionScore = candidate.promptDetails() == null ? null : candidate.promptDetails().get("score");
        if (productionScore instanceof Number number) {
            score += number.intValue() * 20;
        }
        Object expansionScore = candidate.promptDetails() == null ? null : candidate.promptDetails().get("expansionScore");
        if (expansionScore instanceof Number number) {
            score += number.intValue() * 16;
        }
        Object opensSettlement = candidate.promptDetails() == null ? null : candidate.promptDetails().get("opensSettlement");
        if (Boolean.TRUE.equals(opensSettlement)) {
            score += 360;
        }
        Object savingForSettlement = candidate.promptDetails() == null ? null : candidate.promptDetails().get("savingForSettlement");
        Object freeRoad = candidate.promptDetails() == null ? null : candidate.promptDetails().get("freeRoad");
        if (Boolean.TRUE.equals(savingForSettlement)
            && !Boolean.TRUE.equals(freeRoad)
            && (action.getType() == BotActionType.BUILD_ROAD || action.getType() == BotActionType.BUY_DEVELOPMENT_CARD)) {
            score -= 350;
        }
        Object enables = candidate.promptDetails() == null ? null : candidate.promptDetails().get("enables");
        if ("settlement".equals(enables)) {
            score += 90;
        } else if ("city".equals(enables)) {
            score += 60;
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
            && java.util.Objects.equals(left.getHexId(), right.getHexId())
            && java.util.Objects.equals(left.getTargetPlayerId(), right.getTargetPlayerId())
            && java.util.Objects.equals(left.getGiveResource(), right.getGiveResource())
            && java.util.Objects.equals(left.getReceiveResource(), right.getReceiveResource())
            && java.util.Objects.equals(left.getGiveAmount(), right.getGiveAmount())
            && java.util.Objects.equals(left.getReceiveAmount(), right.getReceiveAmount());
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
            case BANK_TRADE -> gameService.applyBankTrade(gameId, playerToken, tradeEvent(action, "BANK_TRADE"));
            case PLAYER_TRADE -> gameService.applyBotPlayerTrade(gameId, playerToken, tradeEvent(action, "PLAYER_TRADE_FINALIZE"));
            case END_TURN -> gameService.endTurn(gameId, playerToken);
            case NONE -> gameService.getGameById(gameId, playerToken);
        };
    }

    private boolean isTradeAction(BotActionType type) {
        return BotActionType.BANK_TRADE.equals(type) || BotActionType.PLAYER_TRADE.equals(type);
    }

    private GameEventDTO tradeEvent(BotAction action, String type) {
        GameEventDTO event = new GameEventDTO();
        event.setType(type);
        event.setSourcePlayerId(action.getPlayerId());
        event.setTargetPlayerId(action.getTargetPlayerId());
        event.setGiveResources(singleResourceBundle(action.getGiveResource(), action.getGiveAmount()));
        event.setReceiveResources(singleResourceBundle(action.getReceiveResource(), action.getReceiveAmount()));
        event.setGiveResource(action.getGiveResource());
        event.setReceiveResource(action.getReceiveResource());
        event.setGiveAmount(action.getGiveAmount());
        event.setReceiveAmount(action.getReceiveAmount());
        event.setAmount(action.getReceiveAmount());
        return event;
    }

    private Map<String, Integer> singleResourceBundle(String resource, Integer amount) {
        Map<String, Integer> bundle = new HashMap<>();
        bundle.put("wood", 0);
        bundle.put("brick", 0);
        bundle.put("wool", 0);
        bundle.put("wheat", 0);
        bundle.put("ore", 0);
        if (resource != null) {
            String normalizedResource = resource.toLowerCase(java.util.Locale.ROOT);
            if (bundle.containsKey(normalizedResource)) {
                bundle.put(normalizedResource, amount == null ? 0 : amount);
            }
        }
        return bundle;
    }
}
