package ch.uzh.ifi.hase.soprafs26.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DevelopmentDeckTest {

    @Test
    void testGettersAndSetters() {
        // create instance
        DevelopmentDeck developmentDeck = new DevelopmentDeck();
        
        // define test values
        Long id = 1L;
        
        // set values
        developmentDeck.setId(id);
        
        // verify values
        assertEquals(id, developmentDeck.getId());
    }

    @Test
    void testIdInitiallyNull() {
        // create instance
        DevelopmentDeck developmentDeck = new DevelopmentDeck();
        
        // verify initial state
        assertEquals(null, developmentDeck.getId());
    }

    @Test
    void testMultipleSetAndGet() {
        // create instance
        DevelopmentDeck developmentDeck = new DevelopmentDeck();
        
        // set first value
        developmentDeck.setId(1L);
        assertEquals(1L, developmentDeck.getId());
        
        // set second value
        developmentDeck.setId(2L);
        assertEquals(2L, developmentDeck.getId());
    }

    @Test
    void testLargeIdValue() {
        DevelopmentDeck developmentDeck = new DevelopmentDeck();
        Long largeId = 999999999L;
        developmentDeck.setId(largeId);
        assertEquals(largeId, developmentDeck.getId());
    }

    @Test
    void testZeroId() {
        DevelopmentDeck developmentDeck = new DevelopmentDeck();
        developmentDeck.setId(0L);
        assertEquals(0L, developmentDeck.getId());
    }
}
