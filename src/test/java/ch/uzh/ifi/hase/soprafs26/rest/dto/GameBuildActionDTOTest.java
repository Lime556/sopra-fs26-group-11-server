package ch.uzh.ifi.hase.soprafs26.rest.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GameBuildActionDTOTest {

    @Test
    void testGettersAndSetters() {
        // create instance
        GameBuildActionDTO gameBuildActionDTO = new GameBuildActionDTO();
        
        // define test values
        Long playerId = 1L;
        Integer edgeId = 5;
        Integer intersectionId = 10;
        
        // set values
        gameBuildActionDTO.setPlayerId(playerId);
        gameBuildActionDTO.setEdgeId(edgeId);
        gameBuildActionDTO.setIntersectionId(intersectionId);
        
        // verify values
        assertEquals(playerId, gameBuildActionDTO.getPlayerId());
        assertEquals(edgeId, gameBuildActionDTO.getEdgeId());
        assertEquals(intersectionId, gameBuildActionDTO.getIntersectionId());
    }

    @Test
    void testPlayerIdField() {
        GameBuildActionDTO gameBuildActionDTO = new GameBuildActionDTO();
        Long playerId = 42L;
        gameBuildActionDTO.setPlayerId(playerId);
        assertEquals(playerId, gameBuildActionDTO.getPlayerId());
    }

    @Test
    void testEdgeIdField() {
        GameBuildActionDTO gameBuildActionDTO = new GameBuildActionDTO();
        Integer edgeId = 15;
        gameBuildActionDTO.setEdgeId(edgeId);
        assertEquals(edgeId, gameBuildActionDTO.getEdgeId());
    }

    @Test
    void testIntersectionIdField() {
        GameBuildActionDTO gameBuildActionDTO = new GameBuildActionDTO();
        Integer intersectionId = 25;
        gameBuildActionDTO.setIntersectionId(intersectionId);
        assertEquals(intersectionId, gameBuildActionDTO.getIntersectionId());
    }

    @Test
    void testInitialValuesAreNull() {
        GameBuildActionDTO gameBuildActionDTO = new GameBuildActionDTO();
        assertEquals(null, gameBuildActionDTO.getPlayerId());
        assertEquals(null, gameBuildActionDTO.getEdgeId());
        assertEquals(null, gameBuildActionDTO.getIntersectionId());
    }

    @Test
    void testMultipleUpdates() {
        GameBuildActionDTO gameBuildActionDTO = new GameBuildActionDTO();
        
        // set and verify first value
        gameBuildActionDTO.setPlayerId(1L);
        assertEquals(1L, gameBuildActionDTO.getPlayerId());
        
        // update and verify
        gameBuildActionDTO.setPlayerId(2L);
        assertEquals(2L, gameBuildActionDTO.getPlayerId());
        
        gameBuildActionDTO.setEdgeId(5);
        assertEquals(5, gameBuildActionDTO.getEdgeId());
        gameBuildActionDTO.setEdgeId(10);
        assertEquals(10, gameBuildActionDTO.getEdgeId());
    }

    @Test
    void testZeroValues() {
        GameBuildActionDTO gameBuildActionDTO = new GameBuildActionDTO();
        gameBuildActionDTO.setPlayerId(0L);
        gameBuildActionDTO.setEdgeId(0);
        gameBuildActionDTO.setIntersectionId(0);
        
        assertEquals(0L, gameBuildActionDTO.getPlayerId());
        assertEquals(0, gameBuildActionDTO.getEdgeId());
        assertEquals(0, gameBuildActionDTO.getIntersectionId());
    }
}
