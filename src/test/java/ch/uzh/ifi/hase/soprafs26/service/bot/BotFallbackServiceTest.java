package ch.uzh.ifi.hase.soprafs26.service.bot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.uzh.ifi.hase.soprafs26.entity.Board;
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
        assertEquals(1, action.getIntersectionId());
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
