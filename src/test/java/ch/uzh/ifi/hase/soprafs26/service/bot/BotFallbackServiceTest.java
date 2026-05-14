package ch.uzh.ifi.hase.soprafs26.service.bot;

import ch.uzh.ifi.hase.soprafs26.entity.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

class BotFallbackServiceTest {

    private final BotFallbackService botFallbackService = new BotFallbackService();

    @Test
    void chooseFallbackAction_doesNotSuggestRoad_whenLimitReached() {
        Player bot = new Player();
        bot.setId(1L);
        bot.setBot(true);
        bot.setWood(10); bot.setBrick(10); // Bot has resources

        Board board = new Board();
        board.setEdges(new ArrayList<>());
        board.setIntersections(new ArrayList<>());

        // Give bot 15 roads
        for (int i = 0; i < 15; i++) {
            Edge edge = new Edge();
            edge.setId(i);
            edge.setIntersectionAId(i);
            edge.setIntersectionBId(i+1);
            Road road = new Road();
            road.setOwnerPlayerId(bot.getId());
            edge.setRoad(road);
            board.getEdges().add(edge);
        }

        // Add a valid candidate edge that is not occupied but connected
        Edge candidate = new Edge();
        candidate.setId(99);
        candidate.setIntersectionAId(0); 
        candidate.setIntersectionBId(100);
        board.getEdges().add(candidate);

        Game game = new Game();
        game.setBoard(board);
        game.setPlayers(List.of(bot));
        game.setCurrentTurnIndex(0);
        game.setTurnPhase(TurnPhase.ACTION.toString());
        game.setGamePhase("ACTIVE");

        BotAction action = botFallbackService.chooseFallbackAction(game);

        // Bot should NOT suggest building a road because it has 15
        assertNotEquals(BotActionType.BUILD_ROAD, action.getType());
    }
}