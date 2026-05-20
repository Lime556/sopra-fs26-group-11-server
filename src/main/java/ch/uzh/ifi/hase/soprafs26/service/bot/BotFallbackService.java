package ch.uzh.ifi.hase.soprafs26.service.bot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;

import ch.uzh.ifi.hase.soprafs26.entity.Board;
import ch.uzh.ifi.hase.soprafs26.entity.Boat;
import ch.uzh.ifi.hase.soprafs26.entity.Building;
import ch.uzh.ifi.hase.soprafs26.entity.City;
import ch.uzh.ifi.hase.soprafs26.entity.Edge;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Intersection;
import ch.uzh.ifi.hase.soprafs26.entity.Player;
import ch.uzh.ifi.hase.soprafs26.entity.Road;
import ch.uzh.ifi.hase.soprafs26.entity.Settlement;
import ch.uzh.ifi.hase.soprafs26.entity.TurnPhase;

@Service
public class BotFallbackService {

    private static final int MAX_ROADS = 15;
    private static final int MAX_SETTLEMENTS = 5;
    private static final int MAX_CITIES = 4;
    private static final int[] DICE_WEIGHTS = {0, 0, 1, 2, 3, 4, 5, 4, 5, 4, 3, 2, 1};
    private static final List<String> TRADE_RESOURCES = List.of("wood", "brick", "wool", "wheat", "ore");
    private static final Map<String, Integer> SETTLEMENT_COST = Map.of("wood", 1, "brick", 1, "wool", 1, "wheat", 1, "ore", 0);
    private static final Map<String, Integer> CITY_COST = Map.of("wood", 0, "brick", 0, "wool", 0, "wheat", 2, "ore", 3);

    public BotAction chooseFallbackAction(Game game) {
        Player bot = getCurrentBotPlayer(game);
        if (bot == null || bot.getId() == null) {
            return BotAction.of(BotActionType.NONE, null);
        }

        if (game.isSetupPhase()) {
            return chooseSetupAction(game, bot);
        }

        if (TurnPhase.ROLL_DICE.toString().equals(game.getTurnPhase())) {
            return BotAction.of(BotActionType.ROLL_DICE, bot.getId());
        }

        if (TurnPhase.DISCARD.toString().equals(game.getTurnPhase())) {
            return BotAction.of(BotActionType.NONE, bot.getId());
        }

        if (Integer.valueOf(7).equals(game.getDiceValue()) && !Boolean.TRUE.equals(game.getRobberMovedAfterSevenRoll())) {
            Integer robberHexId = firstValidRobberHex(game);
            if (robberHexId != null) {
                return BotAction.robber(bot.getId(), robberHexId);
            }
        }

        return chooseMainAction(game, bot);
    }

    public List<BotActionCandidate> listCandidateActions(Game game) {
        Player bot = getCurrentBotPlayer(game);
        if (bot == null || bot.getId() == null) {
            return Collections.emptyList();
        }

        List<BotActionCandidate> candidates = new ArrayList<>();

        if (game.isSetupPhase()) {
            addSetupCandidates(game, bot, candidates);
            return candidates;
        }

        if (TurnPhase.ROLL_DICE.toString().equals(game.getTurnPhase())) {
            addCandidate(candidates, BotAction.of(BotActionType.ROLL_DICE, bot.getId()), Map.of("t", "ROLL_DICE"));
            return candidates;
        }

        if (TurnPhase.DISCARD.toString().equals(game.getTurnPhase())) {
            return candidates;
        }

        if (Integer.valueOf(7).equals(game.getDiceValue()) && !Boolean.TRUE.equals(game.getRobberMovedAfterSevenRoll())) {
            addRobberCandidates(game, bot, candidates);
        }

        addMainCandidates(game, bot, candidates);
        return candidates;
    }

    Player getCurrentBotPlayer(Game game) {
        if (game == null || game.getPlayers() == null || game.getPlayers().isEmpty()) {
            return null;
        }
        Integer index = game.getCurrentTurnIndex();
        if (index == null || index < 0 || index >= game.getPlayers().size()) {
            return null;
        }
        Player currentPlayer = game.getPlayers().get(index);
        return currentPlayer != null && currentPlayer.isBot() ? currentPlayer : null;
    }

    private BotAction chooseSetupAction(Game game, Player bot) {
        int settlements = countPlayerSettlements(game, bot.getId());
        int roads = countPlayerRoads(game, bot.getId());

        if ((game.isFirstSetupRound() && settlements < 1) || (game.isSecondSetupRound() && settlements < 2)) {
            Integer intersectionId = randomValidSetupSettlement(game);
            if (intersectionId != null) {
                return BotAction.settlement(BotActionType.BUILD_INITIAL_SETTLEMENT, bot.getId(), intersectionId);
            }
        }

        if ((game.isFirstSetupRound() && roads < 1) || (game.isSecondSetupRound() && roads < 2)) {
            Integer edgeId = randomValidSetupRoad(game, bot);
            if (edgeId != null) {
                return BotAction.road(BotActionType.BUILD_INITIAL_ROAD, bot.getId(), edgeId);
            }
        }

        return BotAction.of(BotActionType.END_TURN, bot.getId());
    }

    private BotAction chooseMainAction(Game game, Player bot) {
        Integer cityIntersectionId = randomValidCity(game, bot);
        if (cityIntersectionId != null) {
            return BotAction.settlement(BotActionType.BUILD_CITY, bot.getId(), cityIntersectionId);
        }

        Integer settlementIntersectionId = randomValidSettlement(game, bot);
        if (settlementIntersectionId != null) {
            return BotAction.settlement(BotActionType.BUILD_SETTLEMENT, bot.getId(), settlementIntersectionId);
        }

        BotAction bankTrade = bestBankTrade(game, bot);
        if (bankTrade != null) {
            return bankTrade;
        }

        BotAction playerTrade = bestBotPlayerTrade(game, bot);
        if (playerTrade != null) {
            return playerTrade;
        }

        if (shouldBuyDevelopmentCard(game, bot)) {
            return BotAction.of(BotActionType.BUY_DEVELOPMENT_CARD, bot.getId());
        }

        Integer roadEdgeId = bestValidRoad(game, bot);
        if (roadEdgeId != null) {
            return BotAction.road(BotActionType.BUILD_ROAD, bot.getId(), roadEdgeId);
        }

        return BotAction.of(BotActionType.END_TURN, bot.getId());
    }

    private Integer randomValidSetupSettlement(Game game) {
        List<Integer> candidates = new ArrayList<>();
        for (Intersection intersection : intersections(game)) {
            if (intersection != null && !intersection.isOccupied() && !hasAdjacentBuilding(game, intersection.getId())) {
                candidates.add(intersection.getId());
            }
        }
        return randomCandidate(candidates);
    }

    private Integer randomValidSetupRoad(Game game, Player bot) {
        Integer settlementId = bot.getLastPlacedSetupSettlementIntersectionId();
        if (settlementId == null) {
            return null;
        }

        List<Integer> candidates = new ArrayList<>();
        for (Edge edge : edges(game)) {
            if (edge == null || edge.isOccupied()) {
                continue;
            }
            boolean connectedToNewSettlement =
                settlementId.equals(edge.getIntersectionAId()) || settlementId.equals(edge.getIntersectionBId());
            boolean connectedToOwnBuilding =
                hasOwnBuildingAtIntersection(game, edge.getIntersectionAId(), bot.getId())
                || hasOwnBuildingAtIntersection(game, edge.getIntersectionBId(), bot.getId());
            if (connectedToNewSettlement && connectedToOwnBuilding) {
                candidates.add(edge.getId());
            }
        }
        return randomCandidate(candidates);
    }

    private Integer randomValidCity(Game game, Player bot) {
        if (resourceValue(bot.getWheat()) < 2 || resourceValue(bot.getOre()) < 3) {
            return null;
        }

        if (countPlayerCities(game, bot.getId()) >= MAX_CITIES) {
            return null;
        }

        List<Integer> candidates = new ArrayList<>();
        for (Intersection intersection : intersections(game)) {
            if (intersection == null || !(intersection.getBuilding() instanceof Settlement settlement)) {
                continue;
            }
            if (bot.getId().equals(settlement.getOwnerPlayerId())) {
                candidates.add(intersection.getId());
            }
        }
        return randomCandidate(candidates);
    }

    private Integer randomValidSettlement(Game game, Player bot) {
        if (resourceValue(bot.getWood()) < 1 || resourceValue(bot.getBrick()) < 1
            || resourceValue(bot.getWool()) < 1 || resourceValue(bot.getWheat()) < 1) {
            return null;
        }

        if (countPlayerSettlements(game, bot.getId()) >= MAX_SETTLEMENTS) {
            return null;
        }

        List<Integer> candidates = new ArrayList<>();
        for (Intersection intersection : intersections(game)) {
            if (intersection == null || intersection.isOccupied() || hasAdjacentBuilding(game, intersection.getId())) {
                continue;
            }
            if (hasOwnRoadAtIntersection(game, intersection.getId(), bot.getId())) {
                candidates.add(intersection.getId());
            }
        }
        return randomCandidate(candidates);
    }

    private Integer bestValidRoad(Game game, Player bot) {
        if (resourceValue(bot.getFreeRoadBuildsRemaining()) <= 0
            && (resourceValue(bot.getWood()) < 1 || resourceValue(bot.getBrick()) < 1)) {
            return null;
        }

        if (countPlayerRoads(game, bot.getId()) >= MAX_ROADS) {
            return null;
        }

        boolean savingForSettlement = isSavingForSettlement(bot);
        Integer bestEdgeId = null;
        int bestScore = Integer.MIN_VALUE;
        for (Edge edge : edges(game)) {
            if (edge == null || edge.isOccupied()) {
                continue;
            }
            boolean connectedToOwnBuilding =
                hasOwnBuildingAtIntersection(game, edge.getIntersectionAId(), bot.getId())
                || hasOwnBuildingAtIntersection(game, edge.getIntersectionBId(), bot.getId());
            boolean connectedToOwnRoad =
                hasOwnRoadAtIntersection(game, edge.getIntersectionAId(), bot.getId())
                || hasOwnRoadAtIntersection(game, edge.getIntersectionBId(), bot.getId());
            if (connectedToOwnBuilding || connectedToOwnRoad) {
                int expansionScore = roadExpansionScore(game, edge);
                boolean opensSettlement = roadOpensSettlement(game, edge);
                if (savingForSettlement && !opensSettlement && resourceValue(bot.getFreeRoadBuildsRemaining()) <= 0) {
                    continue;
                }

                int score = expansionScore + (opensSettlement ? 12 : 0);
                if (score > bestScore) {
                    bestScore = score;
                    bestEdgeId = edge.getId();
                }
            }
        }
        return bestEdgeId;
    }

    private boolean canBuyDevelopmentCard(Game game, Player bot) {
        int remaining = resourceValue(game.getDevelopmentKnightRemaining())
            + resourceValue(game.getDevelopmentVictoryPointRemaining())
            + resourceValue(game.getDevelopmentRoadBuildingRemaining())
            + resourceValue(game.getDevelopmentYearOfPlentyRemaining())
            + resourceValue(game.getDevelopmentMonopolyRemaining());

        return remaining > 0
            && resourceValue(bot.getWool()) >= 1
            && resourceValue(bot.getWheat()) >= 1
            && resourceValue(bot.getOre()) >= 1;
    }

    private boolean shouldBuyDevelopmentCard(Game game, Player bot) {
        if (!canBuyDevelopmentCard(game, bot)) {
            return false;
        }
        if (isSavingForSettlement(bot) || isSavingForCity(bot)) {
            return false;
        }
        return countPlayerSettlements(game, bot.getId()) + countPlayerCities(game, bot.getId()) >= 2
            || countPlayerRoads(game, bot.getId()) >= 4;
    }

    private boolean isSavingForSettlement(Player bot) {
        int missing = 0;
        missing += resourceValue(bot.getWood()) >= 1 ? 0 : 1;
        missing += resourceValue(bot.getBrick()) >= 1 ? 0 : 1;
        missing += resourceValue(bot.getWool()) >= 1 ? 0 : 1;
        missing += resourceValue(bot.getWheat()) >= 1 ? 0 : 1;
        return missing <= 1;
    }

    private boolean isSavingForCity(Player bot) {
        int missingWheat = Math.max(0, 2 - resourceValue(bot.getWheat()));
        int missingOre = Math.max(0, 3 - resourceValue(bot.getOre()));
        return missingWheat + missingOre <= 2;
    }

    private int roadExpansionScore(Game game, Edge edge) {
        if (edge == null) {
            return 0;
        }
        return Math.max(
            productionScoreForIntersection(game, edge.getIntersectionAId()),
            productionScoreForIntersection(game, edge.getIntersectionBId())
        );
    }

    private boolean roadOpensSettlement(Game game, Edge edge) {
        if (edge == null) {
            return false;
        }
        return canEventuallyBuildSettlementAt(game, edge.getIntersectionAId())
            || canEventuallyBuildSettlementAt(game, edge.getIntersectionBId());
    }

    private boolean canEventuallyBuildSettlementAt(Game game, Integer intersectionId) {
        Intersection intersection = findIntersectionById(game, intersectionId);
        return intersection != null
            && !intersection.isOccupied()
            && !hasAdjacentBuilding(game, intersectionId)
            && productionScoreForIntersection(game, intersectionId) > 0;
    }

    private Integer firstValidRobberHex(Game game) {
        Board board = game.getBoard();
        if (board == null || board.getHexTiles() == null) {
            return null;
        }
        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < board.getHexTiles().size(); i++) {
            int hexId = i + 1;
            if (!Integer.valueOf(hexId).equals(game.getRobberTileIndex())) {
                candidates.add(hexId);
            }
        }
        return randomCandidate(candidates);
    }

    private Integer randomCandidate(List<Integer> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    private int countPlayerSettlements(Game game, Long playerId) {
        int count = 0;
        for (Intersection intersection : intersections(game)) {
            if (intersection != null && intersection.getBuilding() instanceof Settlement settlement
                && playerId.equals(settlement.getOwnerPlayerId())) {
                count++;
            }
        }
        return count;
    }

    private int countPlayerCities(Game game, Long playerId) {
        int count = 0;
        for (Intersection intersection : intersections(game)) {
            if (intersection != null && intersection.getBuilding() instanceof City city
                && playerId.equals(city.getOwnerPlayerId())) {
                count++;
            }
        }
        return count;
    }

    private int countPlayerRoads(Game game, Long playerId) {
        int count = 0;
        for (Edge edge : edges(game)) {
            if (edge != null && edge.getRoad() != null && playerId.equals(edge.getRoad().getOwnerPlayerId())) {
                count++;
            }
        }
        return count;
    }

    private boolean hasAdjacentBuilding(Game game, Integer intersectionId) {
        if (intersectionId == null) {
            return false;
        }

        for (Edge edge : edges(game)) {
            if (edge == null) {
                continue;
            }
            Integer neighborId = null;
            if (intersectionId.equals(edge.getIntersectionAId())) {
                neighborId = edge.getIntersectionBId();
            }
            else if (intersectionId.equals(edge.getIntersectionBId())) {
                neighborId = edge.getIntersectionAId();
            }
            Intersection neighbor = findIntersectionById(game, neighborId);
            if (neighbor != null && neighbor.getBuilding() != null) {
                return true;
            }
        }
        return false;
    }

    private boolean hasOwnRoadAtIntersection(Game game, Integer intersectionId, Long playerId) {
        if (intersectionId == null || playerId == null) {
            return false;
        }

        for (Edge edge : edges(game)) {
            Road road = edge == null ? null : edge.getRoad();
            Integer intersectionAId = edge == null ? null : edge.getIntersectionAId();
            Integer intersectionBId = edge == null ? null : edge.getIntersectionBId();
            if (road != null && playerId.equals(road.getOwnerPlayerId())
                && ((intersectionAId != null && intersectionAId.equals(intersectionId))
                    || (intersectionBId != null && intersectionBId.equals(intersectionId)))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasOwnBuildingAtIntersection(Game game, Integer intersectionId, Long playerId) {
        Intersection intersection = findIntersectionById(game, intersectionId);
        Building building = intersection == null ? null : intersection.getBuilding();
        return building != null && playerId.equals(building.getOwnerPlayerId());
    }

    private void addSetupCandidates(Game game, Player bot, List<BotActionCandidate> candidates) {
        int settlements = countPlayerSettlements(game, bot.getId());
        int roads = countPlayerRoads(game, bot.getId());

        if ((game.isFirstSetupRound() && settlements < 1) || (game.isSecondSetupRound() && settlements < 2)) {
            for (Intersection intersection : intersections(game)) {
                if (intersection != null && !intersection.isOccupied() && !hasAdjacentBuilding(game, intersection.getId())) {
                    addCandidate(
                        candidates,
                        BotAction.settlement(BotActionType.BUILD_INITIAL_SETTLEMENT, bot.getId(), intersection.getId()),
                        Map.of(
                            "t", "BUILD_INITIAL_SETTLEMENT",
                            "at", intersection.getId(),
                            "hex", buildAdjacentHexSummary(game, intersection.getId()),
                            "score", productionScoreForIntersection(game, intersection.getId())
                        )
                    );
                }
            }
            return;
        }

        if ((game.isFirstSetupRound() && roads < 1) || (game.isSecondSetupRound() && roads < 2)) {
            Integer settlementId = bot.getLastPlacedSetupSettlementIntersectionId();
            if (settlementId == null) {
                return;
            }

            for (Edge edge : edges(game)) {
                if (edge == null || edge.isOccupied()) {
                    continue;
                }

                boolean connectedToNewSettlement = settlementId.equals(edge.getIntersectionAId()) || settlementId.equals(edge.getIntersectionBId());
                boolean connectedToOwnBuilding =
                    hasOwnBuildingAtIntersection(game, edge.getIntersectionAId(), bot.getId())
                        || hasOwnBuildingAtIntersection(game, edge.getIntersectionBId(), bot.getId());
                if (connectedToNewSettlement && connectedToOwnBuilding) {
                    addCandidate(
                        candidates,
                        BotAction.road(BotActionType.BUILD_INITIAL_ROAD, bot.getId(), edge.getId()),
                        Map.of(
                            "t", "BUILD_INITIAL_ROAD",
                            "edgeId", edge.getId(),
                            "connects", List.of(edge.getIntersectionAId(), edge.getIntersectionBId())
                        )
                    );
                }
            }
            return;
        }

        boolean firstSetupComplete = game.isFirstSetupRound() && settlements >= 1 && roads >= 1;
        boolean secondSetupComplete = game.isSecondSetupRound() && settlements >= 2 && roads >= 2;
        if (firstSetupComplete || secondSetupComplete) {
            addCandidate(candidates, BotAction.of(BotActionType.END_TURN, bot.getId()), Map.of("t", "END_TURN"));
        }
    }

    private void addMainCandidates(Game game, Player bot, List<BotActionCandidate> candidates) {
        for (Intersection intersection : intersections(game)) {
            if (intersection == null || !(intersection.getBuilding() instanceof Settlement settlement)) {
                continue;
            }
            if (!bot.getId().equals(settlement.getOwnerPlayerId())) {
                continue;
            }
            if (resourceValue(bot.getWheat()) >= 2 && resourceValue(bot.getOre()) >= 3 && countPlayerCities(game, bot.getId()) < MAX_CITIES) {
                addCandidate(
                    candidates,
                    BotAction.settlement(BotActionType.BUILD_CITY, bot.getId(), intersection.getId()),
                    Map.of(
                        "t", "BUILD_CITY",
                        "at", intersection.getId(),
                        "c", List.of(0, 0, 2, 0, 3),
                        "gain", buildProductionGain(game, intersection.getId()),
                        "score", productionScoreForIntersection(game, intersection.getId())
                    )
                );
            }
        }

        if (resourceValue(bot.getWood()) >= 1 && resourceValue(bot.getBrick()) >= 1
            && resourceValue(bot.getWool()) >= 1 && resourceValue(bot.getWheat()) >= 1
            && countPlayerSettlements(game, bot.getId()) < MAX_SETTLEMENTS) {
            for (Intersection intersection : intersections(game)) {
                if (intersection == null || intersection.isOccupied() || hasAdjacentBuilding(game, intersection.getId())) {
                    continue;
                }
                if (hasOwnRoadAtIntersection(game, intersection.getId(), bot.getId())) {
                    addCandidate(
                        candidates,
                        BotAction.settlement(BotActionType.BUILD_SETTLEMENT, bot.getId(), intersection.getId()),
                        Map.of(
                            "t", "BUILD_SETTLEMENT",
                            "at", intersection.getId(),
                            "c", List.of(1, 1, 1, 1, 0),
                            "hex", buildAdjacentHexSummary(game, intersection.getId()),
                            "score", productionScoreForIntersection(game, intersection.getId())
                        )
                    );
                }
            }
        }

        if ((resourceValue(bot.getFreeRoadBuildsRemaining()) > 0 || (resourceValue(bot.getWood()) >= 1 && resourceValue(bot.getBrick()) >= 1))
            && countPlayerRoads(game, bot.getId()) < MAX_ROADS) {
            for (Edge edge : edges(game)) {
                if (edge == null || edge.isOccupied()) {
                    continue;
                }

                boolean connectedToOwnBuilding =
                    hasOwnBuildingAtIntersection(game, edge.getIntersectionAId(), bot.getId())
                        || hasOwnBuildingAtIntersection(game, edge.getIntersectionBId(), bot.getId());
                boolean connectedToOwnRoad =
                    hasOwnRoadAtIntersection(game, edge.getIntersectionAId(), bot.getId())
                        || hasOwnRoadAtIntersection(game, edge.getIntersectionBId(), bot.getId());

                if (connectedToOwnBuilding || connectedToOwnRoad) {
                    int expansionScore = roadExpansionScore(game, edge);
                    boolean opensSettlement = roadOpensSettlement(game, edge);
                    addCandidate(
                        candidates,
                        BotAction.road(BotActionType.BUILD_ROAD, bot.getId(), edge.getId()),
                        Map.of(
                            "t", "BUILD_ROAD",
                            "edgeId", edge.getId(),
                            "connects", List.of(edge.getIntersectionAId(), edge.getIntersectionBId()),
                            "expansionScore", expansionScore,
                            "opensSettlement", opensSettlement,
                            "savingForSettlement", isSavingForSettlement(bot),
                            "freeRoad", resourceValue(bot.getFreeRoadBuildsRemaining()) > 0
                        )
                    );
                }
            }
        }

        if (shouldBuyDevelopmentCard(game, bot)) {
            addCandidate(
                candidates,
                BotAction.of(BotActionType.BUY_DEVELOPMENT_CARD, bot.getId()),
                Map.of(
                    "t", "BUY_DEVELOPMENT_CARD",
                    "cost", List.of(0, 0, 1, 1, 1),
                    "savingForSettlement", isSavingForSettlement(bot),
                    "settlements", countPlayerSettlements(game, bot.getId()),
                    "cities", countPlayerCities(game, bot.getId())
                )
            );
        }

        BotAction bankTrade = bestBankTrade(game, bot);
        if (bankTrade != null) {
            addCandidate(
                candidates,
                bankTrade,
                Map.of(
                    "t", "BANK_TRADE",
                    "give", bankTrade.getGiveResource(),
                    "giveAmount", bankTrade.getGiveAmount(),
                    "receive", bankTrade.getReceiveResource(),
                    "receiveAmount", bankTrade.getReceiveAmount(),
                    "enables", tradeEnablesPlan(bot, bankTrade.getReceiveResource())
                )
            );
        }

        BotAction playerTrade = bestBotPlayerTrade(game, bot);
        if (playerTrade != null) {
            addCandidate(
                candidates,
                playerTrade,
                Map.of(
                    "t", "PLAYER_TRADE",
                    "targetPlayerId", playerTrade.getTargetPlayerId(),
                    "give", playerTrade.getGiveResource(),
                    "giveAmount", playerTrade.getGiveAmount(),
                    "receive", playerTrade.getReceiveResource(),
                    "receiveAmount", playerTrade.getReceiveAmount(),
                    "enables", tradeEnablesPlan(bot, playerTrade.getReceiveResource())
                )
            );
        }

        if (Integer.valueOf(7).equals(game.getDiceValue()) && !Boolean.TRUE.equals(game.getRobberMovedAfterSevenRoll())) {
            addRobberCandidates(game, bot, candidates);
        }

        addCandidate(candidates, BotAction.of(BotActionType.END_TURN, bot.getId()), Map.of("t", "END_TURN"));
    }

    private void addRobberCandidates(Game game, Player bot, List<BotActionCandidate> candidates) {
        Board board = game.getBoard();
        if (board == null || board.getHexTiles() == null) {
            return;
        }

        for (int i = 0; i < board.getHexTiles().size(); i++) {
            int hexId = i + 1;
            if (Integer.valueOf(hexId).equals(game.getRobberTileIndex())) {
                continue;
            }

            addCandidate(
                candidates,
                BotAction.robber(bot.getId(), hexId),
                Map.of(
                    "t", "MOVE_ROBBER",
                    "hexId", hexId,
                    "threat", robberThreatScore(game, hexId)
                )
            );
        }
    }

    private int robberThreatScore(Game game, int hexId) {
        Board board = game == null ? null : game.getBoard();
        Player bot = getCurrentBotPlayer(game);
        if (board == null || bot == null || bot.getId() == null) {
            return 0;
        }

        int score = 0;
        for (Integer intersectionId : board.getIntersectionIdsForHex(hexId)) {
            Intersection intersection = findIntersectionById(game, intersectionId);
            if (intersection == null || intersection.getBuilding() == null || intersection.getBuilding().getOwnerPlayerId() == null) {
                continue;
            }
            if (!bot.getId().equals(intersection.getBuilding().getOwnerPlayerId())) {
                score += 1;
            }
        }

        return score;
    }

    private BotAction bestBankTrade(Game game, Player bot) {
        TradeNeed need = bestImmediateBuildNeed(game, bot);
        if (need == null || bankResourceValue(game, need.resource()) < 1) {
            return null;
        }

        for (String giveResource : TRADE_RESOURCES) {
            if (giveResource.equals(need.resource())) {
                continue;
            }
            int ratio = bestBankRatio(game, bot, giveResource);
            int requiredAfterTrade = need.cost().getOrDefault(giveResource, 0);
            if (resourceValueForName(bot, giveResource) >= requiredAfterTrade + ratio) {
                return BotAction.bankTrade(bot.getId(), giveResource, need.resource(), ratio, 1);
            }
        }

        return null;
    }

    private BotAction bestBotPlayerTrade(Game game, Player bot) {
        TradeNeed need = bestImmediateBuildNeed(game, bot);
        if (need == null || game == null || game.getPlayers() == null) {
            return null;
        }

        String receiveResource = need.resource();
        for (String giveResource : TRADE_RESOURCES) {
            if (giveResource.equals(receiveResource)) {
                continue;
            }
            if (resourceValueForName(bot, giveResource) < need.cost().getOrDefault(giveResource, 0) + 1) {
                continue;
            }

            for (Player target : game.getPlayers()) {
                if (target == null || bot.getId().equals(target.getId())) {
                    continue;
                }
                if (resourceValueForName(target, receiveResource) < 1) {
                    continue;
                }
                if (!target.isBot() && ThreadLocalRandom.current().nextInt(100) >= 35) {
                    continue;
                }
                if (targetNeedsResourceForImmediateBuild(game, target, giveResource)
                    && resourceValueForName(target, receiveResource) > resourceRequirementForBestNeed(game, target, receiveResource)) {
                    return BotAction.playerTrade(bot.getId(), target.getId(), giveResource, receiveResource, 1, 1);
                }
                if (!target.isBot() && resourceValueForName(target, receiveResource) >= 2) {
                    return BotAction.playerTrade(bot.getId(), target.getId(), giveResource, receiveResource, 1, 1);
                }
            }
        }

        return null;
    }

    private TradeNeed bestImmediateBuildNeed(Game game, Player bot) {
        if (canBuildSettlementSpot(game, bot)) {
            String missingSettlementResource = singleMissingResource(bot, SETTLEMENT_COST);
            if (missingSettlementResource != null) {
                return new TradeNeed("settlement", missingSettlementResource, SETTLEMENT_COST);
            }
        }

        if (canUpgradeAnySettlement(game, bot)) {
            String missingCityResource = singleMissingResource(bot, CITY_COST);
            if (missingCityResource != null) {
                return new TradeNeed("city", missingCityResource, CITY_COST);
            }
        }

        return null;
    }

    private String singleMissingResource(Player player, Map<String, Integer> cost) {
        String missingResource = null;
        int missingTotal = 0;
        for (String resource : TRADE_RESOURCES) {
            int missing = Math.max(0, cost.getOrDefault(resource, 0) - resourceValueForName(player, resource));
            if (missing > 0) {
                missingTotal += missing;
                missingResource = resource;
            }
        }
        return missingTotal == 1 ? missingResource : null;
    }

    private boolean canBuildSettlementSpot(Game game, Player bot) {
        if (countPlayerSettlements(game, bot.getId()) >= MAX_SETTLEMENTS) {
            return false;
        }
        for (Intersection intersection : intersections(game)) {
            if (intersection != null
                && !intersection.isOccupied()
                && !hasAdjacentBuilding(game, intersection.getId())
                && hasOwnRoadAtIntersection(game, intersection.getId(), bot.getId())) {
                return true;
            }
        }
        return false;
    }

    private boolean canUpgradeAnySettlement(Game game, Player bot) {
        if (countPlayerCities(game, bot.getId()) >= MAX_CITIES) {
            return false;
        }
        for (Intersection intersection : intersections(game)) {
            if (intersection != null
                && intersection.getBuilding() instanceof Settlement settlement
                && bot.getId().equals(settlement.getOwnerPlayerId())) {
                return true;
            }
        }
        return false;
    }

    private boolean targetNeedsResourceForImmediateBuild(Game game, Player target, String resource) {
        TradeNeed need = bestImmediateBuildNeed(game, target);
        return need != null && resource.equals(need.resource());
    }

    private int resourceRequirementForBestNeed(Game game, Player player, String resource) {
        TradeNeed need = bestImmediateBuildNeed(game, player);
        return need == null ? 0 : need.cost().getOrDefault(resource, 0);
    }

    private String tradeEnablesPlan(Player bot, String receivedResource) {
        if (receivedResource == null) {
            return "build";
        }
        if (singleMissingResource(bot, SETTLEMENT_COST) != null) {
            return "settlement";
        }
        if (singleMissingResource(bot, CITY_COST) != null) {
            return "city";
        }
        return "build";
    }

    private int bestBankRatio(Game game, Player player, String resource) {
        if (hasPortAccess(game, player, resource)) {
            return 2;
        }
        if (hasPortAccess(game, player, "3:1")) {
            return 3;
        }
        return 4;
    }

    private boolean hasPortAccess(Game game, Player player, String portType) {
        Board board = game == null ? null : game.getBoard();
        if (board == null || player == null || portType == null || board.getBoats() == null) {
            return false;
        }

        for (Intersection intersection : intersections(game)) {
            if (intersection == null
                || intersection.getBuilding() == null
                || !player.getId().equals(intersection.getBuilding().getOwnerPlayerId())) {
                continue;
            }
            if (isIntersectionAPortOfType(board, intersection.getId(), portType)) {
                return true;
            }
        }
        return false;
    }

    private boolean isIntersectionAPortOfType(Board board, Integer intersectionId, String portType) {
        if (board == null || intersectionId == null || portType == null) {
            return false;
        }

        for (Boat boat : board.getBoats()) {
            if (boat == null || boat.getHexId() == null || boat.getFirstCorner() == null || boat.getSecondCorner() == null) {
                continue;
            }
            List<Integer> hexIntersections = board.getIntersectionIdsForHex(boat.getHexId());
            if (hexIntersections == null || hexIntersections.size() <= Math.max(boat.getFirstCorner(), boat.getSecondCorner())) {
                continue;
            }
            Integer firstIntersectionId = hexIntersections.get(boat.getFirstCorner());
            Integer secondIntersectionId = hexIntersections.get(boat.getSecondCorner());
            if (!intersectionId.equals(firstIntersectionId) && !intersectionId.equals(secondIntersectionId)) {
                continue;
            }
            if (boatTypeMatchesPortType(boat.getBoatType(), portType)) {
                return true;
            }
        }
        return false;
    }

    private boolean boatTypeMatchesPortType(String boatType, String portType) {
        if (boatType == null || portType == null) {
            return false;
        }
        String normalizedBoatType = boatType.trim().toUpperCase(java.util.Locale.ROOT);
        String normalizedPortType = portType.trim().toLowerCase(java.util.Locale.ROOT);
        return switch (normalizedBoatType) {
            case "STANDARD" -> "3:1".equals(normalizedPortType);
            case "WOOD" -> "wood".equals(normalizedPortType);
            case "BRICK" -> "brick".equals(normalizedPortType);
            case "SHEEP" -> "wool".equals(normalizedPortType);
            case "WHEAT" -> "wheat".equals(normalizedPortType);
            case "STONE" -> "ore".equals(normalizedPortType);
            default -> false;
        };
    }

    private int bankResourceValue(Game game, String resource) {
        if (game == null || resource == null) {
            return 0;
        }
        return switch (resource.toLowerCase(java.util.Locale.ROOT)) {
            case "wood" -> resourceValue(game.getBankWood());
            case "brick" -> resourceValue(game.getBankBrick());
            case "wool" -> resourceValue(game.getBankWool());
            case "wheat" -> resourceValue(game.getBankWheat());
            case "ore" -> resourceValue(game.getBankOre());
            default -> 0;
        };
    }

    private List<List<Object>> buildAdjacentHexSummary(Game game, Integer intersectionId) {
        Board board = game == null ? null : game.getBoard();
        if (board == null || intersectionId == null) {
            return List.of();
        }

        List<List<Object>> hexSummary = new ArrayList<>();
        List<Integer> hexIds = board.buildIntersectionToHexIdsMap().getOrDefault(intersectionId, List.of());
        List<String> hexTypes = board.getHexTiles();
        List<Integer> diceNumbers = board.getHexTile_DiceNumbers();
        Integer robberTileIndex = game == null ? null : game.getRobberTileIndex();

        for (Integer hexId : hexIds) {
            if (hexId == null || hexId < 1 || hexId > safeSize(hexTypes)) {
                continue;
            }

            String resourceType = hexTypes.get(hexId - 1);
            Integer diceNumber = hexId <= safeSize(diceNumbers) ? diceNumbers.get(hexId - 1) : null;
            boolean blocked = robberTileIndex != null && robberTileIndex.equals(hexId);
            hexSummary.add(List.of(resourceCode(resourceType), diceNumber, blocked));
        }

        return hexSummary;
    }

    private List<Integer> buildProductionGain(Game game, Integer intersectionId) {
        int[] gain = {0, 0, 0, 0, 0};
        Board board = game == null ? null : game.getBoard();
        if (board == null || intersectionId == null) {
            return List.of(0, 0, 0, 0, 0);
        }

        List<Integer> hexIds = board.buildIntersectionToHexIdsMap().getOrDefault(intersectionId, List.of());
        List<String> hexTypes = board.getHexTiles();
        List<Integer> diceNumbers = board.getHexTile_DiceNumbers();
        Integer robberTileIndex = game == null ? null : game.getRobberTileIndex();

        for (Integer hexId : hexIds) {
            if (hexId == null || hexId < 1 || hexId > safeSize(hexTypes)) {
                continue;
            }

            String resourceType = hexTypes.get(hexId - 1);
            Integer diceNumber = hexId <= safeSize(diceNumbers) ? diceNumbers.get(hexId - 1) : null;
            if (resourceType == null || "DESERT".equalsIgnoreCase(resourceType) || diceNumber == null) {
                continue;
            }
            if (robberTileIndex != null && robberTileIndex.equals(hexId)) {
                continue;
            }

            int weight = diceWeight(diceNumber);
            switch (resourceCode(resourceType)) {
                case "B" -> gain[0] += weight;
                case "W" -> gain[1] += weight;
                case "H" -> gain[2] += weight;
                case "S" -> gain[3] += weight;
                case "O" -> gain[4] += weight;
                default -> {
                }
            }
        }

        return List.of(gain[0], gain[1], gain[2], gain[3], gain[4]);
    }

    private int productionScoreForIntersection(Game game, Integer intersectionId) {
        int score = 0;
        for (Integer value : buildProductionGain(game, intersectionId)) {
            score += value == null ? 0 : value;
        }
        return score;
    }

    private void addCandidate(List<BotActionCandidate> candidates, BotAction action, Map<String, Object> details) {
        if (candidates == null || action == null || action.getType() == null) {
            return;
        }

        String id = "A" + (candidates.size() + 1);
        candidates.add(new BotActionCandidate(id, action, details));
    }

    private int diceWeight(Integer diceNumber) {
        if (diceNumber == null || diceNumber < 0 || diceNumber >= DICE_WEIGHTS.length) {
            return 0;
        }
        return DICE_WEIGHTS[diceNumber];
    }

    private String resourceCode(String resourceType) {
        if (resourceType == null) {
            return "D";
        }

        return switch (resourceType.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "BRICK" -> "B";
            case "WOOD" -> "W";
            case "WHEAT" -> "H";
            case "SHEEP", "WOOL" -> "S";
            case "ORE" -> "O";
            default -> "D";
        };
    }

    private int safeSize(List<?> values) {
        return values == null ? 0 : values.size();
    }

    private Intersection findIntersectionById(Game game, Integer intersectionId) {
        if (intersectionId == null) {
            return null;
        }
        for (Intersection intersection : intersections(game)) {
            if (intersection != null && intersectionId.equals(intersection.getId())) {
                return intersection;
            }
        }
        return null;
    }

    private List<Intersection> intersections(Game game) {
        Board board = game == null ? null : game.getBoard();
        return board == null || board.getIntersections() == null ? Collections.emptyList() : board.getIntersections();
    }

    private List<Edge> edges(Game game) {
        Board board = game == null ? null : game.getBoard();
        return board == null || board.getEdges() == null ? Collections.emptyList() : board.getEdges();
    }

    private int resourceValue(Integer value) {
        return value == null ? 0 : value;
    }

    private int resourceValueForName(Player player, String resource) {
        if (player == null || resource == null) {
            return 0;
        }
        return switch (resource.toLowerCase(java.util.Locale.ROOT)) {
            case "wood" -> resourceValue(player.getWood());
            case "brick" -> resourceValue(player.getBrick());
            case "wool" -> resourceValue(player.getWool());
            case "wheat" -> resourceValue(player.getWheat());
            case "ore" -> resourceValue(player.getOre());
            default -> 0;
        };
    }

    private record TradeNeed(String plan, String resource, Map<String, Integer> cost) {
    }
}
