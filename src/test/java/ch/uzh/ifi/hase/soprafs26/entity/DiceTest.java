package ch.uzh.ifi.hase.soprafs26.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DiceTest {

    @Test
    void testGettersAndSetters() {
        // create instance
        Dice dice = new Dice();
        
        // define test values
        Long id = 1L;
        
        // set values
        dice.setId(id);
        
        // verify values
        assertEquals(id, dice.getId());
    }

    @Test
    void testIdInitiallyNull() {
        // create instance
        Dice dice = new Dice();
        
        // verify initial state
        assertEquals(null, dice.getId());
    }

    @Test
    void testMultipleSetAndGet() {
        // create instance
        Dice dice = new Dice();
        
        // set first value
        dice.setId(1L);
        assertEquals(1L, dice.getId());
        
        // set second value
        dice.setId(2L);
        assertEquals(2L, dice.getId());
    }

    @Test
    void testLargeIdValue() {
        Dice dice = new Dice();
        Long largeId = 999999999L;
        dice.setId(largeId);
        assertEquals(largeId, dice.getId());
    }

    @Test
    void testZeroId() {
        Dice dice = new Dice();
        dice.setId(0L);
        assertEquals(0L, dice.getId());
    }
}
