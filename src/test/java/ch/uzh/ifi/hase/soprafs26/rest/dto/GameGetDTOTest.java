package ch.uzh.ifi.hase.soprafs26.rest.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class GameGetDTOTest {

    @Test
    void gettersAndSetters_storeGameResponseFields() {
        GameGetDTO dto = new GameGetDTO();
        PlayerGetDTO player = new PlayerGetDTO();
        BoardGetDTO board = new BoardGetDTO();
        RobberGetDTO robber = new RobberGetDTO();
        DiceGetDTO dice = new DiceGetDTO();
        DevelopmentDeckGetDTO deck = new DevelopmentDeckGetDTO();
        LocalDateTime startedAt = LocalDateTime.of(2026, 5, 17, 10, 0);
        LocalDateTime finishedAt = LocalDateTime.of(2026, 5, 17, 11, 0);
        List<String> eventLog = List.of("event");
        List<String> chatMessages = List.of("chat");
        Map<String, Integer> bankResources = Map.of("ore", 3);

        dto.setId(1L);
        dto.setGameVersion(2L);
        dto.setPlayers(List.of(player));
        dto.setBoard(board);
        dto.setCurrentTurnIndex(3);
        dto.setTurnPhase("ACTION");
        dto.setGamePhase("MAIN");
        dto.setCurrentPlayer(player);
        dto.setRobber(robber);
        dto.setDice(dice);
        dto.setDiceValue(8);
        dto.setDiceRolledAt("2026-05-17T10:00:00Z");
        dto.setTradeRequestedAt("2026-05-17T10:01:00Z");
        dto.setLatestTradeRequest("trade");
        dto.setRobberTileIndex(4);
        dto.setDevelopmentDeck(deck);
        dto.setLongestRoad(player);
        dto.setLargestArmy(player);
        dto.setTargetVictoryPoints(10);
        dto.setStartedAt(startedAt);
        dto.setFinishedAt(finishedAt);
        dto.setWinner(player);
        dto.setGameFinished(true);
        dto.setEventLog(eventLog);
        dto.setChatMessages(chatMessages);
        dto.setBankResources(bankResources);
        dto.setRobberMovedAfterSevenRoll(true);

        assertEquals(1L, dto.getId());
        assertEquals(2L, dto.getGameVersion());
        assertEquals(List.of(player), dto.getPlayers());
        assertSame(board, dto.getBoard());
        assertEquals(3, dto.getCurrentTurnIndex());
        assertEquals("ACTION", dto.getTurnPhase());
        assertEquals("MAIN", dto.getGamePhase());
        assertSame(player, dto.getCurrentPlayer());
        assertSame(robber, dto.getRobber());
        assertSame(dice, dto.getDice());
        assertEquals(8, dto.getDiceValue());
        assertEquals("2026-05-17T10:00:00Z", dto.getDiceRolledAt());
        assertEquals("2026-05-17T10:01:00Z", dto.getTradeRequestedAt());
        assertEquals("trade", dto.getLatestTradeRequest());
        assertEquals(4, dto.getRobberTileIndex());
        assertSame(deck, dto.getDevelopmentDeck());
        assertSame(player, dto.getLongestRoad());
        assertSame(player, dto.getLargestArmy());
        assertEquals(10, dto.getTargetVictoryPoints());
        assertEquals(startedAt, dto.getStartedAt());
        assertEquals(finishedAt, dto.getFinishedAt());
        assertSame(player, dto.getWinner());
        assertEquals(true, dto.getGameFinished());
        assertEquals(eventLog, dto.getEventLog());
        assertEquals(chatMessages, dto.getChatMessages());
        assertEquals(bankResources, dto.getBankResources());
        assertEquals(true, dto.getRobberMovedAfterSevenRoll());
    }
}
