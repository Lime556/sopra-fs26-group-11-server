package ch.uzh.ifi.hase.soprafs26.rest.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RobberGetDTOTest {

    @Test
    void testGettersAndSetters() {
        // create instance
        RobberGetDTO robberGetDTO = new RobberGetDTO();
        
        // define test values
        Long id = 1L;
        
        // set values
        robberGetDTO.setId(id);
        
        // verify values
        assertEquals(id, robberGetDTO.getId());
    }

    @Test
    void testIdInitiallyNull() {
        // create instance
        RobberGetDTO robberGetDTO = new RobberGetDTO();
        
        // verify initial state
        assertEquals(null, robberGetDTO.getId());
    }

    @Test
    void testMultipleSetAndGet() {
        // create instance
        RobberGetDTO robberGetDTO = new RobberGetDTO();
        
        // set first value
        robberGetDTO.setId(1L);
        assertEquals(1L, robberGetDTO.getId());
        
        // set second value
        robberGetDTO.setId(2L);
        assertEquals(2L, robberGetDTO.getId());
    }
}
