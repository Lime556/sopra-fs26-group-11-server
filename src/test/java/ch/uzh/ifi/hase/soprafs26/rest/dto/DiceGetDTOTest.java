package ch.uzh.ifi.hase.soprafs26.rest.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DiceGetDTOTest {

    @Test
    void testGettersAndSetters() {
        // create instance
        DiceGetDTO diceGetDTO = new DiceGetDTO();
        
        // define test values
        Long id = 1L;
        
        // set values
        diceGetDTO.setId(id);
        
        // verify values
        assertEquals(id, diceGetDTO.getId());
    }

    @Test
    void testIdInitiallyNull() {
        // create instance
        DiceGetDTO diceGetDTO = new DiceGetDTO();
        
        // verify initial state
        assertEquals(null, diceGetDTO.getId());
    }

    @Test
    void testMultipleSetAndGet() {
        // create instance
        DiceGetDTO diceGetDTO = new DiceGetDTO();
        
        // set first value
        diceGetDTO.setId(1L);
        assertEquals(1L, diceGetDTO.getId());
        
        // set second value
        diceGetDTO.setId(2L);
        assertEquals(2L, diceGetDTO.getId());
    }

    @Test
    void testLargeIdValue() {
        DiceGetDTO diceGetDTO = new DiceGetDTO();
        Long largeId = 999999999L;
        diceGetDTO.setId(largeId);
        assertEquals(largeId, diceGetDTO.getId());
    }

    @Test
    void testZeroId() {
        DiceGetDTO diceGetDTO = new DiceGetDTO();
        diceGetDTO.setId(0L);
        assertEquals(0L, diceGetDTO.getId());
    }
}
