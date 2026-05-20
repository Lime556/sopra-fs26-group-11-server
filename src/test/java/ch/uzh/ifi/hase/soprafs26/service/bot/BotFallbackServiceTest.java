package ch.uzh.ifi.hase.soprafs26.service.bot;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.uzh.ifi.hase.soprafs26.entity.Board;
import ch.uzh.ifi.hase.soprafs26.entity.Boat;
import ch.uzh.ifi.hase.soprafs26.entity.Edge;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.GamePhase;
import ch.uzh.ifi.hase.soprafs26.entity.Intersection;
import ch.uzh.ifi.hase.soprafs26.entity.Player;
import ch.uzh.ifi.hase.soprafs26.entity.Road;
import ch.uzh.ifi.hase.soprafs26.entity.Settlement;
import ch.uzh.ifi.hase.soprafs26.entity.TurnPhase;

public class BotFallbackServiceTest {

    private BotFallbackService botFallbackService;

    @BeforeEach
    public void setup() {
        botFallbackService = new BotFallbackService();
    }

    @Test
    public void chooseFallbackAction_setupWithoutSettlement_buildsInitialSettlement() {
        Game game = setupGame();
        Player bot = botPlayer();
        game.setPlayers(List.of(bot));
        game.setCurrentTurnIndex(0);
        game.setGamePhase(GamePhase.SETUP);

        BotAction action = botFallbackService.chooseFallbackAction(game);

        assertEquals(BotActionType.BUILD_INITIAL_SETTLEMENT, action.getType());
        assertTrue(List.of(1, 2, 3).contains(action.getIntersectionId()));
    }

    @Test
    public void chooseFallbackAction_setupWithSettlement_buildsInitialRoad() {
        Game game = setupGame();
        Player bot = botPlayer();
        bot.setLastPlacedSetupSettlementIntersectionId(1);
        game.getBoard().getIntersections().get(0).setBuilding(settlement(bot.getId(), 1));
        game.setPlayers(List.of(bot));
        game.setCurrentTurnIndex(0);
        game.setGamePhase(GamePhase.SETUP);

        BotAction action = botFallbackService.chooseFallbackAction(game);

        assertEquals(BotActionType.BUILD_INITIAL_ROAD, action.getType());
        assertEquals(1, action.getEdgeId());
    }

    @Test
    public void listCandidateActions_setupAfterSettlementAndRoad_canEndTurn() {
        Game game = setupGame();
        Player bot = botPlayer();
        game.getBoard().getIntersections().get(0).setBuilding(settlement(bot.getId(), 1));
        game.getBoard().getEdges().get(0).setRoad(road(bot.getId(), 1));
        game.setPlayers(List.of(bot));
        game.setCurrentTurnIndex(0);
        game.setGamePhase(GamePhase.SETUP);

        List<BotActionCandidate> actions = botFallbackService.listCandidateActions(game);

        assertEquals(1, actions.size());
        assertEquals(BotActionType.END_TURN, actions.get(0).action().getType());
    }

    @Test
    public void chooseFallbackAction_activeRollPhase_rollsDice() {
        Game game = setupGame();
        Player bot = botPlayer();
        game.setPlayers(List.of(bot));
        game.setCurrentTurnIndex(0);
        game.setGamePhase(GamePhase.ACTIVE);
        game.setTurnPhase(TurnPhase.ROLL_DICE);

        BotAction action = botFallbackService.chooseFallbackAction(game);

        assertEquals(BotActionType.ROLL_DICE, action.getType());
    }

    @Test
    public void chooseFallbackAction_activeActionPhase_prioritizesCity() {
        Game game = setupGame();
        Player bot = botPlayer();
        bot.setWheat(2);
        bot.setOre(3);
        game.getBoard().getIntersections().get(0).setBuilding(settlement(bot.getId(), 1));
        game.setPlayers(List.of(bot));
        game.setCurrentTurnIndex(0);
        game.setGamePhase(GamePhase.ACTIVE);
        game.setTurnPhase(TurnPhase.ACTION);

        BotAction action = botFallbackService.chooseFallbackAction(game);

        assertEquals(BotActionType.BUILD_CITY, action.getType());
        assertEquals(1, action.getIntersectionId());
    }

    @Test
    public void chooseFallbackAction_activeActionPhaseWithoutUsefulAction_endsTurn() {
        Game game = setupGame();
        Player bot = botPlayer();
        game.setPlayers(List.of(bot));
        game.setCurrentTurnIndex(0);
        game.setGamePhase(GamePhase.ACTIVE);
        game.setTurnPhase(TurnPhase.ACTION);

        BotAction action = botFallbackService.chooseFallbackAction(game);

        assertEquals(BotActionType.END_TURN, action.getType());
    }

    @Test
    public void chooseFallbackAction_oneResourceFromSettlement_usesBankTrade() {
        Game game = setupGame();
        Player bot = botPlayer();
        bot.setWood(1);
        bot.setBrick(1);
        bot.setWheat(1);
        bot.setOre(4);
        game.getBoard().getEdges().get(0).setRoad(road(bot.getId(), 1));
        game.setBankWool(19);
        game.setPlayers(List.of(bot));
        game.setCurrentTurnIndex(0);
        game.setGamePhase(GamePhase.ACTIVE);
        game.setTurnPhase(TurnPhase.ACTION);

        BotAction action = botFallbackService.chooseFallbackAction(game);

        assertEquals(BotActionType.BANK_TRADE, action.getType());
        assertEquals("ore", action.getGiveResource());
        assertEquals(Integer.valueOf(4), action.getGiveAmount());
        assertEquals("wool", action.getReceiveResource());
        assertEquals(Integer.valueOf(1), action.getReceiveAmount());
    }

    @Test
    public void chooseFallbackAction_oneResourceFromSettlement_usesSpecificPortRatio() {
        Game game = new Game();
        Board board = new Board();
        board.generateBoard();
        Boat woodPort = board.getBoats().stream()
            .filter(boat -> "WOOD".equals(boat.getBoatType()))
            .findFirst()
            .orElseThrow();
        Integer portIntersectionId = board.getIntersectionIdsForHex(woodPort.getHexId()).get(woodPort.getFirstCorner());
        board.getIntersections().get(portIntersectionId).setBuilding(settlement(10L, portIntersectionId));
        Edge remoteEdge = board.getEdges().stream()
            .filter(edge -> !portIntersectionId.equals(edge.getIntersectionAId()) && !portIntersectionId.equals(edge.getIntersectionBId()))
            .findFirst()
            .orElseThrow();
        remoteEdge.setRoad(road(10L, remoteEdge.getId()));
        game.setBoard(board);

        Player bot = botPlayer();
        bot.setWood(3);
        bot.setBrick(1);
        bot.setWheat(1);
        game.setBankWool(19);
        game.setPlayers(List.of(bot));
        game.setCurrentTurnIndex(0);
        game.setGamePhase(GamePhase.ACTIVE);
        game.setTurnPhase(TurnPhase.ACTION);

        BotAction action = botFallbackService.chooseFallbackAction(game);

        assertEquals(BotActionType.BANK_TRADE, action.getType());
        assertEquals("wood", action.getGiveResource());
        assertEquals(Integer.valueOf(2), action.getGiveAmount());
        assertEquals("wool", action.getReceiveResource());
    }

    @Test
    public void chooseFallbackAction_oneResourceFromSettlement_tradesWithBotWhenBankUnavailable() {
        Game game = setupGame();
        Player bot = botPlayer();
        bot.setWood(1);
        bot.setBrick(1);
        bot.setWheat(1);
        bot.setOre(1);
        Player targetBot = botPlayer();
        targetBot.setId(11L);
        targetBot.setName("Bot 2");
        targetBot.setWool(2);
        targetBot.setWheat(2);
        targetBot.setOre(2);
        game.getBoard().getEdges().get(0).setRoad(road(bot.getId(), 1));
        game.getBoard().getIntersections().get(2).setBuilding(settlement(targetBot.getId(), 3));
        game.setBankWool(0);
        game.setPlayers(List.of(bot, targetBot));
        game.setCurrentTurnIndex(0);
        game.setGamePhase(GamePhase.ACTIVE);
        game.setTurnPhase(TurnPhase.ACTION);

        BotAction action = botFallbackService.chooseFallbackAction(game);

        assertEquals(BotActionType.PLAYER_TRADE, action.getType());
        assertEquals(targetBot.getId(), action.getTargetPlayerId());
        assertEquals("ore", action.getGiveResource());
        assertEquals("wool", action.getReceiveResource());
    }

    @Test
    public void chooseFallbackAction_sevenRolledAfterRobberMoved_doesNotMoveRobberAgain() {
        Game game = setupGame();
        Player bot = botPlayer();
        game.setPlayers(List.of(bot));
        game.setCurrentTurnIndex(0);
        game.setGamePhase(GamePhase.ACTIVE);
        game.setTurnPhase(TurnPhase.ACTION);
        game.setDiceValue(7);
        game.setRobberMovedAfterSevenRoll(true);

        BotAction action = botFallbackService.chooseFallbackAction(game);

        assertEquals(BotActionType.END_TURN, action.getType());
    }

    @Test
    public void chooseFallbackAction_sevenRolledBeforeRobberMoved_movesRobber() {
        Game game = setupGame();
        Player bot = botPlayer();
        game.setPlayers(List.of(bot));
        game.setCurrentTurnIndex(0);
        game.setGamePhase(GamePhase.ACTIVE);
        game.setTurnPhase(TurnPhase.ACTION);
        game.setDiceValue(7);
        game.setRobberMovedAfterSevenRoll(false);

        BotAction action = botFallbackService.chooseFallbackAction(game);

        assertEquals(BotActionType.MOVE_ROBBER, action.getType());
        assertEquals(bot.getId(), action.getPlayerId());
        assertTrue(List.of(2).contains(action.getHexId()));
    }

    @Test
    public void chooseFallbackAction_discardPhase_waitsForDiscards() {
        Game game = setupGame();
        Player bot = botPlayer();
        game.setPlayers(List.of(bot));
        game.setCurrentTurnIndex(0);
        game.setGamePhase(GamePhase.ACTIVE);
        game.setTurnPhase(TurnPhase.DISCARD);
        game.setDiceValue(7);
        game.setRobberMovedAfterSevenRoll(false);

        BotAction action = botFallbackService.chooseFallbackAction(game);

        assertEquals(BotActionType.NONE, action.getType());
    }

    private Game setupGame() {
        Game game = new Game();
        Board board = new Board();
        board.setHexTiles(List.of("WOOD", "BRICK"));
        board.setIntersections(List.of(intersection(1), intersection(2), intersection(3)));
        board.setEdges(List.of(edge(1, 1, 2), edge(2, 2, 3)));
        game.setBoard(board);
        game.setRobberTileIndex(1);
        return game;
    }

    private Player botPlayer() {
        Player player = new Player();
        player.setId(10L);
        player.setName("Bot 1");
        player.setBot(true);
        player.setWood(0);
        player.setBrick(0);
        player.setWool(0);
        player.setWheat(0);
        player.setOre(0);
        return player;
    }

    private Intersection intersection(Integer id) {
        Intersection intersection = new Intersection();
        intersection.setId(id);
        return intersection;
    }

    private Edge edge(Integer id, Integer a, Integer b) {
        Edge edge = new Edge();
        edge.setId(id);
        edge.setIntersectionAId(a);
        edge.setIntersectionBId(b);
        return edge;
    }

    private Settlement settlement(Long playerId, Integer intersectionId) {
        Settlement settlement = new Settlement();
        settlement.setOwnerPlayerId(playerId);
        settlement.setIntersectionId(intersectionId);
        return settlement;
    }

    @SuppressWarnings("unused")
    private Road road(Long playerId, Integer edgeId) {
        Road road = new Road();
        road.setOwnerPlayerId(playerId);
        road.setEdgeId(edgeId);
        return road;
    }
}
