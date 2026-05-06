package ch.uzh.ifi.hase.soprafs26.service.bot;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import ch.uzh.ifi.hase.soprafs26.entity.Board;
import ch.uzh.ifi.hase.soprafs26.entity.Building;
import ch.uzh.ifi.hase.soprafs26.entity.Edge;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Intersection;
import ch.uzh.ifi.hase.soprafs26.entity.Player;
import ch.uzh.ifi.hase.soprafs26.entity.Road;
import ch.uzh.ifi.hase.soprafs26.entity.Settlement;
import ch.uzh.ifi.hase.soprafs26.entity.TurnPhase;

@Service
public class BotFallbackService {

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

        if (Integer.valueOf(7).equals(game.getDiceValue()) && !Boolean.TRUE.equals(game.getRobberMovedAfterSevenRoll())) {
            Integer robberHexId = firstValidRobberHex(game);
            if (robberHexId != null) {
                return BotAction.robber(bot.getId(), robberHexId);
            }
        }

        return chooseMainAction(game, bot);
    }

    private BotAction chooseSetupAction(Game game, Player bot) {
        int settlements = countPlayerSettlements(game, bot.getId());
        int roads = countPlayerRoads(game, bot.getId());

        if ((game.isFirstSetupRound() && settlements < 1) || (game.isSecondSetupRound() && settlements < 2)) {
            Integer intersectionId = firstValidSetupSettlement(game);
            if (intersectionId != null) {
                return BotAction.settlement(BotActionType.BUILD_INITIAL_SETTLEMENT, bot.getId(), intersectionId);
            }
        }

        if ((game.isFirstSetupRound() && roads < 1) || (game.isSecondSetupRound() && roads < 2)) {
            Integer edgeId = firstValidSetupRoad(game, bot);
            if (edgeId != null) {
                return BotAction.road(BotActionType.BUILD_INITIAL_ROAD, bot.getId(), edgeId);
            }
        }

        return BotAction.of(BotActionType.END_TURN, bot.getId());
    }

    private BotAction chooseMainAction(Game game, Player bot) {
        Integer cityIntersectionId = firstValidCity(game, bot);
        if (cityIntersectionId != null) {
            return BotAction.settlement(BotActionType.BUILD_CITY, bot.getId(), cityIntersectionId);
        }

        Integer settlementIntersectionId = firstValidSettlement(game, bot);
        if (settlementIntersectionId != null) {
            return BotAction.settlement(BotActionType.BUILD_SETTLEMENT, bot.getId(), settlementIntersectionId);
        }

        Integer roadEdgeId = firstValidRoad(game, bot);
        if (roadEdgeId != null) {
            return BotAction.road(BotActionType.BUILD_ROAD, bot.getId(), roadEdgeId);
        }

        if (canBuyDevelopmentCard(game, bot)) {
            return BotAction.of(BotActionType.BUY_DEVELOPMENT_CARD, bot.getId());
        }

        return BotAction.of(BotActionType.END_TURN, bot.getId());
    }

    private Player getCurrentBotPlayer(Game game) {
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

    private Integer firstValidSetupSettlement(Game game) {
        for (Intersection intersection : intersections(game)) {
            if (intersection != null && !intersection.isOccupied() && !hasAdjacentBuilding(game, intersection.getId())) {
                return intersection.getId();
            }
        }
        return null;
    }

    private Integer firstValidSetupRoad(Game game, Player bot) {
        Integer settlementId = bot.getLastPlacedSetupSettlementIntersectionId();
        if (settlementId == null) {
            return null;
        }

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
                return edge.getId();
            }
        }
        return null;
    }

    private Integer firstValidCity(Game game, Player bot) {
        if (resourceValue(bot.getWheat()) < 2 || resourceValue(bot.getOre()) < 3) {
            return null;
        }

        for (Intersection intersection : intersections(game)) {
            if (intersection == null || !(intersection.getBuilding() instanceof Settlement settlement)) {
                continue;
            }
            if (bot.getId().equals(settlement.getOwnerPlayerId())) {
                return intersection.getId();
            }
        }
        return null;
    }

    private Integer firstValidSettlement(Game game, Player bot) {
        if (resourceValue(bot.getWood()) < 1 || resourceValue(bot.getBrick()) < 1
            || resourceValue(bot.getWool()) < 1 || resourceValue(bot.getWheat()) < 1) {
            return null;
        }

        for (Intersection intersection : intersections(game)) {
            if (intersection == null || intersection.isOccupied() || hasAdjacentBuilding(game, intersection.getId())) {
                continue;
            }
            if (hasOwnRoadAtIntersection(game, intersection.getId(), bot.getId())) {
                return intersection.getId();
            }
        }
        return null;
    }

    private Integer firstValidRoad(Game game, Player bot) {
        if (resourceValue(bot.getFreeRoadBuildsRemaining()) <= 0
            && (resourceValue(bot.getWood()) < 1 || resourceValue(bot.getBrick()) < 1)) {
            return null;
        }

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
                return edge.getId();
            }
        }
        return null;
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

    private Integer firstValidRobberHex(Game game) {
        Board board = game.getBoard();
        if (board == null || board.getHexTiles() == null) {
            return null;
        }
        for (int i = 0; i < board.getHexTiles().size(); i++) {
            int hexId = i + 1;
            if (!Integer.valueOf(hexId).equals(game.getRobberTileIndex())) {
                return hexId;
            }
        }
        return null;
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
        for (Edge edge : edges(game)) {
            Road road = edge == null ? null : edge.getRoad();
            if (road != null && playerId.equals(road.getOwnerPlayerId())
                && (intersectionId.equals(edge.getIntersectionAId()) || intersectionId.equals(edge.getIntersectionBId()))) {
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
}
