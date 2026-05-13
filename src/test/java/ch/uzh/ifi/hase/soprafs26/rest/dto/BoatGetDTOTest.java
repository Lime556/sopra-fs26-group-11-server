package ch.uzh.ifi.hase.soprafs26.rest.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BoatGetDTOTest {

    @Test
    void testGettersAndSetters() {
        // create instance
        BoatGetDTO boatGetDTO = new BoatGetDTO();
        
        // define test values
        Integer id = 1;
        String boatType = "merchant";
        Integer hexId = 5;
        Integer firstCorner = 0;
        Integer secondCorner = 1;
        
        // set values
        boatGetDTO.setId(id);
        boatGetDTO.setBoatType(boatType);
        boatGetDTO.setHexId(hexId);
        boatGetDTO.setFirstCorner(firstCorner);
        boatGetDTO.setSecondCorner(secondCorner);
        
        // verify values
        assertEquals(id, boatGetDTO.getId());
        assertEquals(boatType, boatGetDTO.getBoatType());
        assertEquals(hexId, boatGetDTO.getHexId());
        assertEquals(firstCorner, boatGetDTO.getFirstCorner());
        assertEquals(secondCorner, boatGetDTO.getSecondCorner());
    }

    @Test
    void testIdField() {
        BoatGetDTO boatGetDTO = new BoatGetDTO();
        Integer id = 42;
        boatGetDTO.setId(id);
        assertEquals(id, boatGetDTO.getId());
    }

    @Test
    void testBoatTypeField() {
        BoatGetDTO boatGetDTO = new BoatGetDTO();
        String boatType = "free";
        boatGetDTO.setBoatType(boatType);
        assertEquals(boatType, boatGetDTO.getBoatType());
    }

    @Test
    void testHexIdField() {
        BoatGetDTO boatGetDTO = new BoatGetDTO();
        Integer hexId = 10;
        boatGetDTO.setHexId(hexId);
        assertEquals(hexId, boatGetDTO.getHexId());
    }

    @Test
    void testFirstCornerField() {
        BoatGetDTO boatGetDTO = new BoatGetDTO();
        Integer firstCorner = 2;
        boatGetDTO.setFirstCorner(firstCorner);
        assertEquals(firstCorner, boatGetDTO.getFirstCorner());
    }

    @Test
    void testSecondCornerField() {
        BoatGetDTO boatGetDTO = new BoatGetDTO();
        Integer secondCorner = 3;
        boatGetDTO.setSecondCorner(secondCorner);
        assertEquals(secondCorner, boatGetDTO.getSecondCorner());
    }

    @Test
    void testInitialValuesAreNull() {
        BoatGetDTO boatGetDTO = new BoatGetDTO();
        assertEquals(null, boatGetDTO.getId());
        assertEquals(null, boatGetDTO.getBoatType());
        assertEquals(null, boatGetDTO.getHexId());
        assertEquals(null, boatGetDTO.getFirstCorner());
        assertEquals(null, boatGetDTO.getSecondCorner());
    }

    @Test
    void testMultipleUpdates() {
        BoatGetDTO boatGetDTO = new BoatGetDTO();
        
        // set and verify first set
        boatGetDTO.setId(1);
        assertEquals(1, boatGetDTO.getId());
        
        // update and verify
        boatGetDTO.setId(2);
        assertEquals(2, boatGetDTO.getId());
        
        boatGetDTO.setBoatType("merchant");
        assertEquals("merchant", boatGetDTO.getBoatType());
        boatGetDTO.setBoatType("free");
        assertEquals("free", boatGetDTO.getBoatType());
    }
}
