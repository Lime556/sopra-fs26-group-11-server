package ch.uzh.ifi.hase.soprafs26.rest.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GameStartGetDTOTest {

    @Test
    void testGettersAndSetters() {
        // create instance
        GameStartGetDTO gameStartGetDTO = new GameStartGetDTO();
        
        // define test values
        Long gameId = 1L;
        
        // set values
        gameStartGetDTO.setGameId(gameId);
        
        // verify values
        assertEquals(gameId, gameStartGetDTO.getGameId());
    }

    @Test
    void testGameIdInitiallyNull() {
        // create instance
        GameStartGetDTO gameStartGetDTO = new GameStartGetDTO();
        
        // verify initial state
        assertEquals(null, gameStartGetDTO.getGameId());
    }

    @Test
    void testMultipleSetAndGet() {
        // create instance
        GameStartGetDTO gameStartGetDTO = new GameStartGetDTO();
        
        // set first value
        gameStartGetDTO.setGameId(1L);
        assertEquals(1L, gameStartGetDTO.getGameId());
        
        // set second value
        gameStartGetDTO.setGameId(2L);
        assertEquals(2L, gameStartGetDTO.getGameId());
    }

    @Test
    void testLargeGameId() {
        GameStartGetDTO gameStartGetDTO = new GameStartGetDTO();
        Long largeId = 999999999L;
        gameStartGetDTO.setGameId(largeId);
        assertEquals(largeId, gameStartGetDTO.getGameId());
    }

    @Test
    void testZeroGameId() {
        GameStartGetDTO gameStartGetDTO = new GameStartGetDTO();
        gameStartGetDTO.setGameId(0L);
        assertEquals(0L, gameStartGetDTO.getGameId());
    }
}
