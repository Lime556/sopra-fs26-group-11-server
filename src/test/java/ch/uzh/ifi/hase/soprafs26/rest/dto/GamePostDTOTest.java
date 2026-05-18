package ch.uzh.ifi.hase.soprafs26.rest.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class GamePostDTOTest {

    @Test
    void gettersAndSetters_storeGamePayloadFields() {
        GamePostDTO dto = new GamePostDTO();
        PlayerGetDTO player = new PlayerGetDTO();
        BoardGetDTO board = new BoardGetDTO();
        RobberGetDTO robber = new RobberGetDTO();
        DiceGetDTO dice = new DiceGetDTO();
        DevelopmentDeckGetDTO deck = new DevelopmentDeckGetDTO();
        LocalDateTime startedAt = LocalDateTime.of(2026, 5, 17, 10, 0);
        LocalDateTime finishedAt = LocalDateTime.of(2026, 5, 17, 11, 0);
        List<String> eventLog = List.of("event");
        List<String> chatMessages = List.of("chat");
        Map<String, Integer> bankResources = Map.of("wood", 1);

        dto.setId(1L);
        dto.setPlayers(List.of(player));
        dto.setBoard(board);
        dto.setCurrentTurnIndex(2);
        dto.setTurnPhase("ACTION");
        dto.setCurrentPlayer(player);
        dto.setRobber(robber);
        dto.setDice(dice);
        dto.setDiceValue(7);
        dto.setRobberTileIndex(4);
        dto.setDevelopmentDeck(deck);
        dto.setLongestRoad(player);
        dto.setLargestArmy(player);
        dto.setTargetVictoryPoints(10);
        dto.setStartedAt(startedAt);
        dto.setFinishedAt(finishedAt);
        dto.setWinner(player);
        dto.setEventLog(eventLog);
        dto.setChatMessages(chatMessages);
        dto.setBankResources(bankResources);

        assertEquals(1L, dto.getId());
        assertEquals(List.of(player), dto.getPlayers());
        assertSame(board, dto.getBoard());
        assertEquals(2, dto.getCurrentTurnIndex());
        assertEquals("ACTION", dto.getTurnPhase());
        assertSame(player, dto.getCurrentPlayer());
        assertSame(robber, dto.getRobber());
        assertSame(dice, dto.getDice());
        assertEquals(7, dto.getDiceValue());
        assertEquals(4, dto.getRobberTileIndex());
        assertSame(deck, dto.getDevelopmentDeck());
        assertSame(player, dto.getLongestRoad());
        assertSame(player, dto.getLargestArmy());
        assertEquals(10, dto.getTargetVictoryPoints());
        assertEquals(startedAt, dto.getStartedAt());
        assertEquals(finishedAt, dto.getFinishedAt());
        assertSame(player, dto.getWinner());
        assertEquals(eventLog, dto.getEventLog());
        assertEquals(chatMessages, dto.getChatMessages());
        assertEquals(bankResources, dto.getBankResources());
    }
}
