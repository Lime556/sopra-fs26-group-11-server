package ch.uzh.ifi.hase.soprafs26.rest.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GameStateDTOTest {

    @Test
    void constructorAndSetters_storeValues() {
        GameStateDTO dto = new GameStateDTO(1L, 2, "ACTION", 7, 10L, "Player1", true, false);

        assertEquals(1L, dto.getGameId());
        assertEquals(2, dto.getCurrentTurnIndex());
        assertEquals("ACTION", dto.getTurnPhase());
        assertEquals(7, dto.getDiceValue());
        assertEquals(10L, dto.getCurrentPlayerId());
        assertEquals("Player1", dto.getCurrentPlayerName());
        assertEquals(true, dto.getGameFinished());
        assertEquals(false, dto.getCurrentPlayerMustDiscard());

        dto.setGameId(3L);
        dto.setCurrentTurnIndex(1);
        dto.setTurnPhase("DISCARD");
        dto.setDiceValue(8);
        dto.setCurrentPlayerId(11L);
        dto.setCurrentPlayerName("Player2");
        dto.setGameFinished(false);
        dto.setCurrentPlayerMustDiscard(true);

        assertEquals(3L, dto.getGameId());
        assertEquals(1, dto.getCurrentTurnIndex());
        assertEquals("DISCARD", dto.getTurnPhase());
        assertEquals(8, dto.getDiceValue());
        assertEquals(11L, dto.getCurrentPlayerId());
        assertEquals("Player2", dto.getCurrentPlayerName());
        assertEquals(false, dto.getGameFinished());
        assertEquals(true, dto.getCurrentPlayerMustDiscard());
    }
}
